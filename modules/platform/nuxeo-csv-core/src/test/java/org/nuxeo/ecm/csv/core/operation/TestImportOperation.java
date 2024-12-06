/*
 * (C) Copyright 2006-2024 Nuxeo (http://nuxeo.com/) and others.
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
 *
 * Contributors:
 *     anechaev
 */
package org.nuxeo.ecm.csv.core.operation;

import static junit.framework.TestCase.assertNotNull;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.AutomationCoreFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.transientstore.TransientStoreFeature;
import org.nuxeo.ecm.csv.core.CSVImportResult;
import org.nuxeo.ecm.csv.core.CSVImportStatus;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @since 8.10
 */
@RunWith(FeaturesRunner.class)
@Features({ AutomationCoreFeature.class, PlatformFeature.class, TransientStoreFeature.class })
@Deploy("org.nuxeo.ecm.csv.core")
@Deploy("org.nuxeo.ecm.platform.types")
@Deploy("org.nuxeo.ecm.csv.core:OSGI-INF/test-types-contrib.xml")
@Deploy("org.nuxeo.ecm.csv.core:OSGI-INF/test-ui-types-contrib.xml")
public class TestImportOperation {

    private static final int TIMEOUT_SECONDS = 20;

    private static final String DOCS_OK_CSV = "docs_ok_big.csv";

    @Inject
    protected CoreSession session;

    @Inject
    protected AutomationService service;

    protected DocumentModel testFolder;

    @Before
    public void setUp() {
        DocumentModel testWorkspace = session.createDocumentModel("/default-domain/workspaces", "testWorkspace",
                "Workspace");
        testWorkspace = session.createDocument(testWorkspace);
        testFolder = session.createDocumentModel(testWorkspace.getPathAsString(), "TestCSVImport", "Folder");
        testFolder = session.createDocument(testFolder);
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
    }

    @Test
    public void testImportOperation() throws OperationException {
        Map<String, Object> params = new HashMap<>();
        params.put("path", testFolder.getPathAsString());

        var chain = new OperationChain("test-chain");
        chain.add(CSVImportOperation.ID).from(params);

        OperationContext ctx = new OperationContext(session);
        File csv = FileUtils.getResourceFileFromContext(DOCS_OK_CSV);
        Blob blob = new FileBlob(csv);
        ctx.setInput(blob);

        String importId = (String) service.run(ctx, chain);

        assertNotNull(importId);

        await().pollInterval(Duration.ofMillis(100)).atMost(TIMEOUT_SECONDS, TimeUnit.SECONDS).until(() -> {
            var statusChain = new OperationChain("test-chain");
            statusChain.add(CSVImportStatusOperation.ID);

            var context = new OperationContext(session);
            context.setInput(importId);

            var status = (CSVImportStatus) service.run(context, statusChain);
            assertNotNull(status);
            return status.isComplete();
        });

        chain = new OperationChain("test-chain");
        chain.add(CSVImportResultOperation.ID);

        ctx = new OperationContext(session);
        ctx.setInput(importId);

        CSVImportResult result = (CSVImportResult) service.run(ctx, chain);

        assertNotNull(result);
        assertEquals(0, result.getErrorLineCount());
        assertEquals(0, result.getSkippedLineCount());
        assertEquals(336, result.getSuccessLineCount());
        assertEquals(336, result.getTotalLineCount());
    }
}
