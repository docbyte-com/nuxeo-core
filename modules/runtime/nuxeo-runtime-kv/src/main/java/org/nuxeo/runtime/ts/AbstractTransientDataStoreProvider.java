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
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @since 2025.8
 */
public abstract class AbstractTransientDataStoreProvider implements TransientDataStoreProvider {

    protected final TransientDataStoreDescriptor descriptor;

    protected AbstractTransientDataStoreProvider(TransientDataStoreDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    @Override
    public final <S extends Serializable> void put(String key, String parameter, S value) {
        Objects.requireNonNull(key, "The key can not be null");
        Objects.requireNonNull(parameter, "The parameter can not be null");
        if (isValueNullOrEmpty(value)) {
            throw new NullPointerException("The value can not be null or empty, nor contain null or empty values");
        }
        doPut(key, parameter, value);
    }

    protected abstract <S extends Serializable> void doPut(String key, String parameter, S value);

    @Override
    public final <S extends Serializable> void putAll(String key, Map<String, S> parameters) {
        Objects.requireNonNull(key, "The key can not be null");
        Objects.requireNonNull(parameters, "The parameters can not be null");
        if (isValueNullOrEmpty(parameters)) {
            throw new NullPointerException("The parameters can not be null or empty, nor contain null or empty values");
        }
        doPutAll(key, parameters);
    }

    protected abstract <S extends Serializable> void doPutAll(String key, Map<String, S> parameters);

    protected boolean isValueNullOrEmpty(Object value) {
        return switch (value) {
            case List<?> list -> list.stream().allMatch(this::isValueNullOrEmpty);
            case Map<?, ?> map -> map.values().stream().allMatch(this::isValueNullOrEmpty);
            case null -> true;
            default -> false;
        };
    }

    @Override
    public final boolean exists(String key) {
        Objects.requireNonNull(key);
        return doExists(key);
    }

    protected abstract boolean doExists(String key);

    @Override
    public final <S extends Serializable> S get(String key, String parameter) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(parameter);
        return doGet(key, parameter);
    }

    protected abstract <S extends Serializable> S doGet(String key, String parameter);

    @Override
    public final <S extends Serializable> Map<String, S> getAll(String key) {
        Objects.requireNonNull(key);
        return doGetAll(key);
    }

    protected abstract <S extends Serializable> Map<String, S> doGetAll(String key);

    @Override
    public final <S extends Serializable> S remove(String key, String parameter) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(parameter);
        return doRemove(key, parameter);
    }

    protected abstract <S extends Serializable> S doRemove(String key, String parameter);

    @Override
    public final <S extends Serializable> Map<String, S> removeAll(String key) {
        Objects.requireNonNull(key);
        return doRemoveAll(key);
    }

    protected abstract <S extends Serializable> Map<String, S> doRemoveAll(String key);
}
