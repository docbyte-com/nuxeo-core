/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.elasticsearch;

public final class ElasticSearchConstants {

    public static final String ID_FIELD = "_id";

    /**
     * Elasticsearch type name used to index Nuxeo documents
     */
    public static final String DOC_TYPE = "doc";

    /**
     * Elasticsearch type name used to index Nuxeo audit entries
     */
    public static final String ENTRY_TYPE = "entry";

    public static final String ACL_FIELD = "ecm:acl";

    public static final String BINARYTEXT_FIELD = "ecm:binarytext";

    public static final String ALL_FIELDS = "*";

    public static final String ES_ENABLED_PROPERTY = "elasticsearch.enabled";

    public static final String FETCH_DOC_FROM_ES_PROPERTY = "elasticsearch.fetchDocFromEs";

    private ElasticSearchConstants() {
    }

}
