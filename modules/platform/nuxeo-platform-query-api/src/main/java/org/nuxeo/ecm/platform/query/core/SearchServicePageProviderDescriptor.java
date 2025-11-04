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

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;

/**
 * Search Service page provider descriptor.
 *
 * @since 2025.0
 */
@XObject("searchServicePageProvider")
public class SearchServicePageProviderDescriptor extends BasePageProviderDescriptor implements PageProviderDefinition {

    @XNode("scroller")
    protected String scroller;

    @XNode("searchClient")
    protected String searchClient;

    @XNodeList(value = "searchIndex", type = ArrayList.class, componentType = String.class)
    protected List<String> searchIndexes;

    public String getScroller() {
        return scroller;
    }

    public String getSearchClient() {
        return searchClient;
    }

    public List<String> getSearchIndexes() {
        return searchIndexes;
    }

    @Override
    protected BasePageProviderDescriptor newInstance() {
        return new SearchServicePageProviderDescriptor();
    }

    @Override
    public SearchServicePageProviderDescriptor clone() {
        SearchServicePageProviderDescriptor clone = (SearchServicePageProviderDescriptor) super.cloneDescriptor();
        clone.scroller = scroller;
        clone.searchClient = searchClient;
        clone.searchIndexes = List.copyOf(searchIndexes);
        return clone;
    }

}
