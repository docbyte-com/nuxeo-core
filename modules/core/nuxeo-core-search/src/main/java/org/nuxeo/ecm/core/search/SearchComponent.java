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

import java.util.List;
import java.util.stream.Collectors;

import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentStartOrders;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @since 2025.0
 */
public class SearchComponent extends DefaultComponent {

    public static final String XP_SEARCH_CLIENT = "searchClient";

    protected SearchService searchService;

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(SearchService.class)) {
            return (T) searchService;
        }
        return null;
    }

    @Override
    public int getApplicationStartedOrder() {
        return ComponentStartOrders.SEARCH;
    }

    @Override
    public void start(ComponentContext context) {
        searchService = new SearchServiceImpl(getEnabledSearchClientDescriptor());
    }

    protected List<SearchClientDescriptor> getEnabledSearchClientDescriptor() {
        List<SearchClientDescriptor> descriptors = getDescriptors(XP_SEARCH_CLIENT);
        return descriptors.stream().filter(SearchClientDescriptor::isEnabled).collect(Collectors.toList());
    }

}
