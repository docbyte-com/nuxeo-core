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

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.nuxeo.ecm.core.api.scroll.ScrollRequest;
import org.nuxeo.ecm.core.scroll.RepositoryScroll;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 2025.0
 */
public class SearchServiceScroll extends RepositoryScroll {

    private static final String CLIENT_OPTION = "searchClient";

    private static final String INDEX_OPTION = "searchIndex";

    protected SearchResponse response;

    protected SearchIndex searchIndex;

    @Override
    public void init(ScrollRequest request, Map<String, String> options) {
        super.init(request, options);
        if (options == null || !options.containsKey(CLIENT_OPTION)) {
            throw new IllegalArgumentException("Invalid SearchServiceScroll: 'client' option is required");
        }
        String client = options.get(CLIENT_OPTION);
        String index = options.get(INDEX_OPTION);
        SearchService service = Framework.getService(SearchService.class);
        searchIndex = service.getSearchIndexForRepository(this.request.getRepository())
                             .stream()
                             .filter(si -> si.client().equals(client))
                             .filter(si -> isBlank(index) || si.index().equals(index))
                             .findFirst()
                             .orElseThrow(() -> new IllegalArgumentException(
                                     "SearchServiceScroll no searchIndex found for repository: "
                                             + this.request.getRepository() + ", client: " + client + ", index: "
                                             + index));
    }

    @Override
    public boolean hasNext() {
        if (hasNextResult == null) {
            hasNextResult = fetch();
        }
        return hasNextResult;
    }

    @Override
    protected boolean fetch() {
        SearchService service = Framework.getService(SearchService.class);
        if (response == null) {
            response = service.search(SearchQuery.builder(searchIndex, request.getQuery(), session.getPrincipal())
                                                 .scrollSize(request.getSize())
                                                 .scrollKeepAlive(request.getTimeout())
                                                 .build());
        } else {
            response = service.searchScroll(response.getScrollContext());
        }
        return response.getHitsCount() > 0;
    }

    @Override
    public List<String> next() {
        if (hasNextResult == null) {
            hasNextResult = fetch();
        }
        if (!hasNextResult) {
            throw new NoSuchElementException();
        }
        hasNextResult = null;
        return response.getHits().stream().map(SearchHit::getDocId).toList();
    }

    @Override
    public void close() {
        super.close();
        if (response != null) {
            SearchService service = Framework.getService(SearchService.class);
            service.clearSearchScroll(response.getScrollContext());
        }
        response = null;
    }
}
