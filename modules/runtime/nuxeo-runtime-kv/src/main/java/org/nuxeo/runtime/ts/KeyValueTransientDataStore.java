/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.runtime.ts;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStoreProvider;

/**
 * @since 2025.8
 */
public class KeyValueTransientDataStore extends AbstractTransientDataStoreProvider {

    protected static final String SEP = ".";

    protected static final String DOT_CLASS = ".class";

    protected static final String DOT_VALUE = ".value";

    protected static final String DOT_PARAM_DOT = SEP + "param" + SEP;

    protected final KeyValueStoreProvider keyValueStore;

    protected final long ttlSeconds;

    protected final TransientDataStoreDescriptor.TTLPolicy ttlPolicy;

    public KeyValueTransientDataStore(TransientDataStoreDescriptor descriptor) {
        super(descriptor);
        this.keyValueStore = (KeyValueStoreProvider) Framework.getService(KeyValueService.class)
                                                              .getKeyValueStore(descriptor.getName());
        this.ttlSeconds = descriptor.getTtl().toSeconds();
        this.ttlPolicy = descriptor.getTtlPolicy();
    }

    @Override
    public <S extends Serializable> void doPut(String key, String parameter, S value) {
        storeEffectiveValues(key,
                Map.of(parameter + DOT_VALUE, value, parameter + DOT_CLASS, ClassUtils.getName(value)));
    }

    @Override
    public <S extends Serializable> void doPutAll(String key, Map<String, S> parameters) {
        storeEffectiveValues(key,
                parameters.entrySet()
                          .stream().<Map.Entry<String, ? extends Serializable>> mapMulti((entry, downstream) -> {
                              if (entry.getValue() != null) {
                                  // send the entry containing parameter value
                                  downstream.accept(Map.entry(entry.getKey() + DOT_VALUE, entry.getValue()));
                                  // send metadata
                                  downstream.accept(
                                          Map.entry(entry.getKey() + DOT_CLASS, ClassUtils.getName(entry.getValue())));
                              }
                          })
                          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    protected void storeEffectiveValues(String key, Map<String, ? extends Serializable> values) {
        values.forEach((k, v) -> {
            String realKey = key + DOT_PARAM_DOT + k;
            switch (v) {
                case Long l -> keyValueStore.put(realKey, l, ttlSeconds);
                case String string -> keyValueStore.put(realKey, string, ttlSeconds);
                case byte[] bytes -> keyValueStore.put(realKey, bytes, ttlSeconds);
                default -> {
                    byte[] bytes = SerializationUtils.serialize(v);
                    keyValueStore.put(realKey, bytes, ttlSeconds);
                }
            }
        });
    }

    @Override
    public boolean doExists(String key) {
        return keyValueStore.keyStream(key + SEP).findAny().isPresent();
    }

    @Override
    public <S extends Serializable> S doGet(String key, String parameter) {
        List<String> effectiveKeys = List.of(key + DOT_PARAM_DOT + parameter + DOT_CLASS,
                key + DOT_PARAM_DOT + parameter + DOT_VALUE);
        var values = keyValueStore.getStrings(effectiveKeys);
        if (ttlPolicy == TransientDataStoreDescriptor.TTLPolicy.ACCESSED) {
            effectiveKeys.forEach(effectiveKey -> keyValueStore.setTTL(effectiveKey, ttlSeconds));
        }
        return mapEffectiveValue(key, parameter, values);
    }

    @Override
    public <S extends Serializable> Map<String, S> doGetAll(String key) {
        return getAllAndDo(key, effectiveKeys -> {
            if (ttlPolicy == TransientDataStoreDescriptor.TTLPolicy.ACCESSED) {
                effectiveKeys.forEach(effectiveKey -> keyValueStore.setTTL(effectiveKey, ttlSeconds));
            }
        });
    }

    @Override
    protected <S extends Serializable> S doRemove(String key, String parameter) {
        var classValue = keyValueStore.removeString(key + DOT_PARAM_DOT + parameter + DOT_CLASS);
        var valueValue = keyValueStore.removeString(key + DOT_PARAM_DOT + parameter + DOT_VALUE);
        if (classValue == null || valueValue == null) {
            return null;
        }
        return mapEffectiveValue(key, parameter, Map.of(key + DOT_PARAM_DOT + parameter + DOT_CLASS, classValue,
                key + DOT_PARAM_DOT + parameter + DOT_VALUE, valueValue));
    }

    @Override
    protected <S extends Serializable> Map<String, S> doRemoveAll(String key) {
        return getAllAndDo(key, effectiveKeys -> effectiveKeys.forEach(keyValueStore::removeString));
    }

    protected <S extends Serializable> Map<String, S> getAllAndDo(String key,
            Consumer<List<String>> effectiveKeysConsumer) {
        var effectiveKeys = keyValueStore.keyStream(key + SEP).toList();
        if (effectiveKeys.isEmpty()) {
            return null;
        }
        var values = keyValueStore.getStrings(effectiveKeys);
        effectiveKeysConsumer.accept(effectiveKeys);
        var keyPattern = Pattern.compile('^' + Pattern.quote(key) + DOT_PARAM_DOT + "(.+)" + DOT_VALUE + '$');
        return values.keySet() //
                     .stream()
                     .<String> mapMulti((effectiveKey, downstream) -> {
                         var parameterMatcher = keyPattern.matcher(effectiveKey);
                         if (parameterMatcher.matches()) {
                             downstream.accept(parameterMatcher.replaceFirst("$1"));
                         }
                     })
                     .filter(parameter -> values.get(key + DOT_PARAM_DOT + parameter + DOT_CLASS) != null)
                     .collect(Collectors.toMap(Function.identity(),
                             parameter -> mapEffectiveValue(key, parameter, values)));
    }

    @SuppressWarnings("unchecked")
    protected <S extends Serializable> S mapEffectiveValue(String key, String parameter, Map<String, String> values) {
        String realParameter = key + DOT_PARAM_DOT + parameter;
        String valueClassName = values.get(realParameter + DOT_CLASS);
        if (valueClassName == null) {
            // entry does not exist return null
            return null;
        }
        var value = values.get(realParameter + DOT_VALUE);
        try {
            var valueClass = ClassUtils.getClass(valueClassName);
            if (Long.class.equals(valueClass)) {
                return (S) Long.valueOf(value);
            } else if (String.class.equals(valueClass)) {
                return (S) value;
            } else if (byte[].class.equals(valueClass)) {
                return (S) value.getBytes(UTF_8);
            } else {
                byte[] valueBytes = value.getBytes(UTF_8);
                return SerializationUtils.deserialize(valueBytes);
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeServiceException(
                    "Unable to map value for key: %s, parameter: %s".formatted(key, parameter), e);
        }
    }

    // SPI

    @Override
    public void clear() {
        keyValueStore.clear();
    }
}
