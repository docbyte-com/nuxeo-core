/*
 * (C) Copyright 2014-2024 Nuxeo (http://nuxeo.com/) and others.
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
 *     tiry
 */
package org.nuxeo.elasticsearch.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.elasticsearch.api.ESClient;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
public class TestService {

    @Inject
    protected ElasticSearchAdmin esa;

    @Inject
    protected ElasticSearchService ess;

    @Inject
    protected ElasticSearchIndexing esi;

    @Test
    public void checkDeclaredServices() {
        assertNotNull(ess);
        assertNotNull(esi);
        assertNotNull(esa);

        @SuppressWarnings("resource") // not ours to close
        ESClient client = esa.getClient();
        assertNotNull(client);

        assertEquals(0, esa.getTotalCommandProcessed());
        assertEquals(0, esa.getPendingWorkerCount());
        assertEquals(0, esa.getRunningWorkerCount());
        assertFalse(esa.isIndexingInProgress());
        assertEquals(1, esa.getRepositoryNames().size());
        assertEquals("test", esa.getRepositoryNames().get(0));
    }

    @Test
    public void verifyNodeStartedWithConfig() {

        ElasticSearchService ess = Framework.getService(ElasticSearchService.class);
        assertNotNull(ess);

        ElasticSearchAdmin esa = Framework.getService(ElasticSearchAdmin.class);
        assertNotNull(esa);

        assertTrue(esa.getClient().waitForYellowStatus(null, 10));
    }

    @Test
    public void verifyPrepareWaitForIndexing() throws Exception {
        ListenableFuture<Boolean> futureRet = esa.prepareWaitForIndexing();
        assertFalse(futureRet.isCancelled());
        assertTrue(futureRet.get());
        assertTrue(futureRet.isDone());
        assertTrue(futureRet.get());
    }

    @Test
    public void verifyPrepareWaitForIndexingTimeout() throws Exception {
        // when a worker is created it is pending
        assertFalse(esa.isIndexingInProgress());
        esi.runReindexingWorker("test", "select * from Document");
        ListenableFuture<Boolean> futureRet = esa.prepareWaitForIndexing();
        try {
            futureRet.get(0, TimeUnit.MILLISECONDS);
            // sometime we don't timeout
            assertTrue(futureRet.isDone());
        } catch (TimeoutException e) {
            assertTrue(futureRet.get());
        } finally {
            assertFalse(esa.isIndexingInProgress());
        }
    }

    @Test
    public void verifyPrepareWaitForIndexingListener() throws Exception {
        ListenableFuture<Boolean> futureRet = esa.prepareWaitForIndexing();
        final Boolean[] callbackRet = { false };
        Futures.addCallback(futureRet, new FutureCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                callbackRet[0] = true;
                // System.out.println("Success");
            }

            @Override
            public void onFailure(Throwable t) {
                fail("Fail");
            }
        }, MoreExecutors.newDirectExecutorService());

        assertTrue(futureRet.get());
        // callback are executed in async, :/
        Thread.sleep(200);
        assertTrue(callbackRet[0]);
    }

}
