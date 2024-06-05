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
package org.nuxeo.ecm.core.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.Descriptor;

/**
 * @since 2025.0
 */
@XObject("searchClient")
public class SearchClientDescriptor implements Descriptor {

    @XNode("@enabled")
    protected boolean isEnabled = true;

    @XNode("@default")
    protected boolean isDefault = false;

    @XNode("@name")
    public String name;

    @XNode("@class")
    protected Class<SearchClient> klass;

    @XNode("connection")
    protected ConnectionDescriptor connection = new ConnectionDescriptor();

    @XObject(value = "connection")
    public static class ConnectionDescriptor implements Descriptor {

        @Override
        public String getId() {
            return "";
        }

        @XNodeMap(value = "option", key = "@name", type = HashMap.class, componentType = String.class)
        public Map<String, String> options = new HashMap<>();
    }

    @XNodeList(value = "index", type = ArrayList.class, componentType = SearchIndexDescriptor.class)
    protected List<SearchIndexDescriptor> indexes = new ArrayList<>();

    @Override
    public String getId() {
        return name;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public String getName() {
        return name;
    }

    public Class<SearchClient> getClientClass() {
        return klass;
    }

    public List<SearchIndexDescriptor> getIndexes() {
        return indexes;
    }

    public Map<String, String> getConnectionOptions() {
        return connection.options;
    }
}
