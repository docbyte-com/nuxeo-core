/*
 * (C) Copyright 2016-2025 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.elasticsearch;

import java.time.Duration;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.adapter.impl.DefaultSyncRootFolderItem;
import org.nuxeo.drive.adapter.impl.ScrollDocumentModelList;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.search.SearchQuery;
import org.nuxeo.ecm.core.search.SearchResponse;
import org.nuxeo.ecm.core.search.SearchScrollContext;
import org.nuxeo.ecm.core.search.SearchService;
import org.nuxeo.runtime.api.Framework;

/**
 * Elasticsearch implementation of a {@link DefaultSyncRootFolderItem}.
 *
 * @since 8.3
 */
public class ESSyncRootFolderItem extends DefaultSyncRootFolderItem {

    private static final Logger log = LogManager.getLogger(ESSyncRootFolderItem.class);

    public ESSyncRootFolderItem(String factoryName, FolderItem parentItem, DocumentModel doc) {
        super(factoryName, parentItem, doc);
    }

    public ESSyncRootFolderItem(String factoryName, FolderItem parentItem, DocumentModel doc,
            boolean relaxSyncRootConstraint) {
        super(factoryName, parentItem, doc, relaxSyncRootConstraint);
    }

    public ESSyncRootFolderItem(String factoryName, FolderItem parentItem, DocumentModel doc,
            boolean relaxSyncRootConstraint, boolean getLockInfo) {
        super(factoryName, parentItem, doc, relaxSyncRootConstraint, getLockInfo);
    }

    protected ESSyncRootFolderItem() {
        // Needed for JSON deserialization
    }

    @Override
    protected ScrollDocumentModelList getScrollBatch(String scrollId, int batchSize, CoreSession session,
            long keepAlive) {

        StringBuilder sb = new StringBuilder(
                String.format("SELECT * FROM Document WHERE ecm:ancestorId = '%s'", docId));
        sb.append(" AND ecm:isTrashed = 0");
        sb.append(" AND ecm:mixinType != 'HiddenInNavigation'");
        sb.append(" AND ecm:isVersion = 0");
        // Let's order by path to make it easier for Drive as it isn't that expensive with Elasticsearch
        sb.append(" ORDER BY ecm:path");
        String query = sb.toString();
        SearchService searchService = Framework.getService(SearchService.class);
        SearchQuery searchQuery = SearchQuery.builder(query, session)
                                             .scrollSize(batchSize)
                                             .scrollKeepAlive(Duration.ofSeconds(keepAlive))
                                             .build();
        SearchResponse res;
        if (StringUtils.isEmpty(scrollId)) {
            log.debug(
                    "Executing Elasticsearch initial search request to scroll through the descendants of {} with batchSize = {} and keepAlive = {}: {}",
                    docPath, batchSize, keepAlive, query);
            res = searchService.search(searchQuery);
        } else {
            log.debug("Scrolling through the descendants of {} with scrollId = {}, batchSize = {} and keepAlive = {}",
                    docPath, scrollId, batchSize, keepAlive);
            res = searchService.searchScroll(new SearchScrollContext(searchQuery, scrollId));
        }
        return new ScrollDocumentModelList(res.getScrollContext().scrollId(), res.loadDocuments(session));
    }

}
