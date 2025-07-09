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
import java.util.Map;

import jakarta.validation.constraints.NotNull;

/**
 * This is the interface for a Transient Data store, which stores dictionary values associated to keys on which the TTL
 * applies.
 * <p>
 * A Transient Data store is thread-safe.
 *
 * @since 2025.8
 */
public interface TransientDataStore {

    /**
     * Puts {@code parameter} to {@code value} in the entry with the given {@code key}.
     * <p>
     * If entry does not exist, a new entry is created. If {@code parameter} already exists in the entry, it is
     * overwritten.
     */
    <S extends Serializable> void put(@NotNull String key, @NotNull String parameter, @NotNull S value);

    /**
     * Puts {@code parameters} in the entry with the given {@code key}. Overwrites any existing parameter in the entry.
     * <p>
     * If entry does not exist, a new entry is created.
     * 
     * @apiNote an empty Map is considered as a null parameter, and so rejected
     */
    <S extends Serializable> void putAll(@NotNull String key, @NotNull Map<String, S> parameters);

    /**
     * Returns {@code true} if an entry exists with the given {@code key}.
     */
    boolean exists(@NotNull String key);

    /**
     * Gets the value of {@code parameter} in the entry with the given {@code key}.
     * <p>
     * Returns {@code null} if entry or parameter does not exist.
     */
    <S extends Serializable> S get(@NotNull String key, @NotNull String parameter);

    /**
     * Gets values of the parameters in the entry with the given {@code key}.
     * <p>
     * Returns {@code null} if entry does not exist.
     */
    <S extends Serializable> Map<String, S> getAll(@NotNull String key);

    /**
     * Removes the value of {@code parameter} in the entry with the given {@code key} and returns it.
     * <p>
     * Returns {@code null} if entry or parameter does not exist.
     */
    <S extends Serializable> S remove(@NotNull String key, @NotNull String parameter);

    /**
     * Removes values of parameters in the entry with the given {@code key} and returns them.
     * <p>
     * Returns {@code null} if entry does not exist.
     */
    <S extends Serializable> Map<String, S> removeAll(@NotNull String key);
}
