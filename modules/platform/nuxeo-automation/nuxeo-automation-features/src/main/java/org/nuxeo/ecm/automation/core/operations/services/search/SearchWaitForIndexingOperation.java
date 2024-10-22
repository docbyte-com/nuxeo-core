/*
 * (C) Copyright 2015-2024 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.automation.core.operations.services.search;

import static java.lang.Math.max;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.audit.service.AuditService;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.search.SearchIndexingService;
import org.nuxeo.ecm.core.search.SearchService;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Wait for background indexing, only for testing purpose.
 *
 * @since 2025.0
 */
@Operation(id = SearchWaitForIndexingOperation.ID, category = Constants.CAT_SERVICES, label = "Wait for Indexing", since = "2025.0", description = "Wait until indexing is done, only for testing purpose.", aliases = "Elasticsearch.WaitForIndexing")
public class SearchWaitForIndexingOperation {

    private static final Logger log = LogManager.getLogger(SearchWaitForIndexingOperation.class);

    public static final String ID = "Search.WaitForIndexing";

    @Context
    protected CoreSession repo;

    @Context
    protected SearchIndexingService searchIndexingService;

    @Param(name = "timeoutSecond", required = false)
    protected Integer timeout = 60;

    @Param(name = "refresh", required = false)
    protected Boolean refresh = false;

    @Param(name = "waitForAudit", required = false)
    protected Boolean waitForAudit = false;

    @Param(name = "waitForBulkService", required = false)
    protected Boolean waitForBulkService = true;

    @Param(name = "waitForWorkManager", required = false)
    protected Boolean waitForWorkManager = true;

    @OperationMethod
    public Boolean run() {
        log.warn("Wait for indexing");
        long start = System.currentTimeMillis();
        try {
            if (waitForWorkManager) {
                var service = Framework.getService(WorkManager.class);
                if (service != null && !service.awaitCompletion(timeout, TimeUnit.SECONDS)) {
                    log.warn("Timeout on work manager, after {}s", timeout);
                    return Boolean.FALSE;
                }
            }
            if (waitForBulkService) {
                var service = Framework.getService(BulkService.class);
                if (service != null && !service.await(computeRemainingDuration(timeout, start))) {
                    log.warn("Timeout on bulk service, after {}s", timeout);
                    return Boolean.FALSE;
                }
            }
            if (!searchIndexingService.await(computeRemainingDuration(timeout, start))) {
                log.warn("Timeout on search service, after {}s", timeout);
                return Boolean.FALSE;
            }
            // wait for audit
            if (waitForAudit) {
                var service = Framework.getService(AuditService.class);
                if (service != null && !service.await(computeRemainingDuration(timeout, start))) {
                    log.warn("Timeout on audit service, after {}s", timeout);
                    return Boolean.FALSE;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted");
            return Boolean.FALSE;
        }
        if (refresh) {
            var searchService = Framework.getService(SearchService.class);
            String repository = repo.getRepositoryName();
            searchIndexingService.refresh(searchService.getDefaultSearchIndexForRepository(repository));
        }
        return Boolean.TRUE;
    }

    protected Duration computeRemainingDuration(long timeout, long start) {
        long elapsed = System.currentTimeMillis() - start;
        // at least one second
        return Duration.ofSeconds(max(timeout - TimeUnit.MILLISECONDS.toSeconds(elapsed), 1));
    }
}
