/*
 * (C) Copyright 2024 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.ecm.platform.query.core;

import org.nuxeo.ecm.platform.query.api.Bucket;

/**
 * Immutable Bucket for double value.
 *
 * @since 2025.0
 */
public final class BucketDouble implements Bucket {

    private final String key;

    private final Double value;

    public BucketDouble(String key, Double value) {
        if (key == null) {
            throw new IllegalArgumentException("key is null");
        }
        this.key = key;
        this.value = value;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public long getDocCount() {
        return ((Double) getValue()).longValue();
    }

    @Override
    public double getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.format("BucketDouble(%s, %f)", key, value);
    }
}
