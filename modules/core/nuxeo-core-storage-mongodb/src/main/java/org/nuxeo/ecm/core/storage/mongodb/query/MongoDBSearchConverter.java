/*
 * (C) Copyright 2017-2024 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.mongodb.query;

import static java.lang.Boolean.FALSE;
import static org.nuxeo.runtime.mongodb.MongoDBSerializationHelper.MONGODB_ID;

import java.util.Calendar;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * MongoDB converter used by {@link MongoDBAbstractSearchBuilder}.
 *
 * @since 2025.0
 */
public class MongoDBSearchConverter {

    // exactly 16 chars in lowercase hex
    protected static final Pattern HEX_RE = Pattern.compile("[0-9a-f]{16}");

    /** The key to use in memory to map the database native "_id". */
    protected final String idKey;

    /** The keys for booleans whose value is true or null (instead of false). */
    protected final Set<String> trueOrNullBooleanKeys;

    /** The keys whose values are ids and are stored as longs. */
    protected final Set<String> idValuesKeys;

    /**
     * Constructor for a converter that does not map the MongoDB native "_id".
     */
    public MongoDBSearchConverter() {
        this(null);
    }

    /**
     * Constructor for a converter that also knows to optionally translate the native MongoDB "_id" into a custom id.
     */
    public MongoDBSearchConverter(String idKey) {
        this(idKey, Set.of(), Set.of());
    }

    /**
     * Constructor for a converter that also knows to optionally translate the native MongoDB "_id" into a custom id.
     * <p>
     * When {@code idValuesKeys} are provided, the ids are stored as longs.
     *
     * @param idKey the key to use to map the native "_id" in memory, if not {@code null}
     * @param trueOrNullBooleanKeys the keys corresponding to boolean values that are only true or null (instead of
     *            false)
     * @param idValuesKeys the keys corresponding to values that are ids
     */
    public MongoDBSearchConverter(String idKey, Set<String> trueOrNullBooleanKeys, Set<String> idValuesKeys) {
        this.idKey = idKey;
        this.trueOrNullBooleanKeys = trueOrNullBooleanKeys;
        this.idValuesKeys = idValuesKeys;
    }

    public String keyToBson(String key) {
        if (idKey == null) {
            return key;
        } else {
            return idKey.equals(key) ? MONGODB_ID : key;
        }
    }

    public Object serializableToBson(String key, Object value) {
        if (value instanceof Calendar) {
            return ((Calendar) value).getTime();
        }
        if (valueIsId(key)) {
            return idToBson(value);
        }
        if (FALSE.equals(value) && key != null && trueOrNullBooleanKeys.contains(key)) {
            return null;
        }
        return value;
    }

    protected boolean valueIsId(String key) {
        return key != null && idValuesKeys.contains(key);
    }

    // convert hex id to long
    protected Object idToBson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            String string = (String) value;
            if (!HEX_RE.matcher(string).matches()) {
                throw new NumberFormatException(string);
            }
            return Long.parseUnsignedLong(string, 16);
        } catch (ClassCastException | NumberFormatException e) {
            return "__invalid_id__" + value;
        }
    }

}
