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

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.nuxeo.runtime.RuntimeServiceException;

import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;

/**
 * Memory-based implementation of a Transient Data store.
 *
 * @since 2025.8
 */
public class MemTransientDataStore extends AbstractTransientDataStoreProvider {

    protected final ExpiringMap<String, Map<String, Serializable>> map;

    protected final Lock writeLock;

    public MemTransientDataStore(TransientDataStoreDescriptor descriptor) {
        super(descriptor);
        map = ExpiringMap.builder()
                         .expiration(descriptor.getTtl().toMillis(), TimeUnit.MILLISECONDS)
                         .expirationPolicy(switch (descriptor.getTtlPolicy()) {
                             case ACCESSED -> ExpirationPolicy.ACCESSED;
                             case CREATED -> ExpirationPolicy.CREATED;
                         })
                         .build();
        try {
            writeLock = (Lock) FieldUtils.readField(map, "writeLock", true);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeServiceException("Unable to retrieve ExpiringMap's lock", e);
        }
    }

    @Override
    protected <S extends Serializable> void doPut(String key, String parameter, S value) {
        map.computeIfAbsent(key, k -> new ConcurrentHashMap<>()).put(parameter, value);
    }

    @Override
    protected <S extends Serializable> void doPutAll(String key, Map<String, S> parameters) {
        parameters.entrySet()
                  .stream()
                  .filter(entry -> entry.getValue() != null)
                  .forEach(entry -> map.computeIfAbsent(key, k -> new ConcurrentHashMap<>())
                                       .put(entry.getKey(), entry.getValue()));
    }

    @Override
    protected boolean doExists(String key) {
        return map.containsKey(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <S extends Serializable> S doGet(String key, String parameter) {
        return (S) map.getOrDefault(key, Map.of()).get(parameter);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <S extends Serializable> Map<String, S> doGetAll(String key) {
        var parameters = (Map<? extends String, ? extends S>) map.get(key);
        if (parameters == null || parameters.isEmpty()) {
            return null;
        }
        return Collections.unmodifiableMap(parameters);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <S extends Serializable> S doRemove(String key, String parameter) {
        var parameters = (Map<? extends String, ? extends S>) map.get(key);
        if (parameters == null) {
            return null;
        }
        writeLock.lock();
        try {
            S value = parameters.remove(parameter);
            if (parameters.isEmpty()) {
                map.remove(key);
            }
            return value;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <S extends Serializable> Map<String, S> doRemoveAll(String key) {
        var parameters = (Map<? extends String, ? extends S>) map.remove(key);
        if (parameters == null || parameters.isEmpty()) {
            return null;
        }
        return Collections.unmodifiableMap(parameters); // not required but useful for cast
    }

    // SPI

    @Override
    public void clear() {
        map.clear();
    }
}
