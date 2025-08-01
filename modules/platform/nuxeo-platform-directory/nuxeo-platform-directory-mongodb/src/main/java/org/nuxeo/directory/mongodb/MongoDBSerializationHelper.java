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
 *     Funsho David
 */
package org.nuxeo.directory.mongodb;

import java.util.Map;

import org.bson.Document;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.IntegerType;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;

/**
 * Helper for serialization/deserialization of BSON objects
 *
 * @since 9.1
 */
public class MongoDBSerializationHelper {

    public static final String MONGODB_ID = "_id";

    public static final String MONGODB_SEQ = "seq";

    private MongoDBSerializationHelper() {
        // empty
    }

    /**
     * Create a BSON object with a single field from a pair key/value
     *
     * @param key the key which corresponds to the field id in the object
     * @param value the value which corresponds to the field value in the object
     * @return the new BSON object
     */
    public static Document fieldMapToBson(String key, Object value) {
        return org.nuxeo.runtime.mongodb.MongoDBSerializationHelper.fieldMapToBson(key, value);
    }

    /**
     * Create a BSON object from a map
     *
     * @param fieldMap a map of keys/values
     * @return the new BSON object
     */
    public static Document fieldMapToBson(Map<String, Object> fieldMap) {
        return org.nuxeo.runtime.mongodb.MongoDBSerializationHelper.fieldMapToBson(fieldMap);
    }

    /**
     * Cast an object according to its instance
     *
     * @param value the object to transform
     * @return the BSON object
     */
    public static Object valueToBson(Object value) {
        return org.nuxeo.runtime.mongodb.MongoDBSerializationHelper.valueToBson(value);
    }

    /**
     * Cast an object according to its instance ans its type
     *
     * @param value the object to transform
     * @param type the object type
     * @return the BSON object
     * @since 9.2
     */
    public static Object valueToBson(Object value, Type type) {
        if (value != null && type instanceof IntegerType) {
            return Integer.valueOf(String.valueOf(value));
        } else if (value != null && type instanceof LongType) {
            return Long.valueOf(String.valueOf(value));
        } else {
            return valueToBson(value);
        }
    }

    /**
     * Create a map from a BSON object
     *
     * @param doc the BSON object to parse
     * @return the new map
     */
    public static Map<String, Object> bsonToFieldMap(Document doc) {
        return org.nuxeo.runtime.mongodb.MongoDBSerializationHelper.bsonToFieldMap(doc);
    }
}
