/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
import static org.nuxeo.ecm.csv.core.TestCSVImporterTrim.DOCS_VALUES_WITH_SPACES;
import static org.nuxeo.ecm.csv.core.TestCSVImporterTrim.doAssertDescription;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.awaitility.Duration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.csv.core.CSVImportResult;
import org.nuxeo.ecm.csv.core.CSVImportStatus;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.nuxeo.transientstore.test.TransientStoreFeature;

/**
 * @since 8.10
 */
@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class, CoreFeature.class, TransientStoreFeature.class })
@Deploy("org.nuxeo.ecm.csv.core")
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.automation.features")
@Deploy("org.nuxeo.ecm.platform.types")
@Deploy("org.nuxeo.ecm.csv.core:OSGI-INF/test-types-contrib.xml")
@Deploy("org.nuxeo.ecm.csv.core:OSGI-INF/test-ui-types-contrib.xml")
public class TestImportOperation {

    private static final int TIMEOUT_SECONDS = 20;

    private static final String DOCS_OK_CSV = "docs_ok_big.csv";

    protected DocumentModel testFolder;

    @Inject
    AutomationService service;

    OperationChain chain;

    @Inject
    private CoreSession session;

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
    public void testImportOperationTrimByDefault() throws OperationException {
        Map<String, Object> params = Map.of("path", testFolder.getPathAsString());
        Blob blob = new FileBlob(FileUtils.getResourceFileFromContext(DOCS_VALUES_WITH_SPACES));
        doTestImportOperation(params, blob, 0, 0, 2, 2);
        doAssertDescription(session, testFolder.getRef(), true);
    }

    @Test
    public void testImportOperationDoNotTrimByParam() throws OperationException {
        Map<String, Object> params = Map.of("path", testFolder.getPathAsString(), "trim", false);
        Blob blob = new FileBlob(FileUtils.getResourceFileFromContext(DOCS_VALUES_WITH_SPACES));
        doTestImportOperation(params, blob, 0, 0, 2, 2);
        doAssertDescription(session, testFolder.getRef(), false);
    }

    @Test
    @Deploy("org.nuxeo.ecm.csv.core:OSGI-INF/test-do-not-trim-contrib.xml")
    public void testImportOperationDoNotTrimByProperty() throws OperationException {
        Map<String, Object> params = Map.of("path", testFolder.getPathAsString());
        Blob blob = new FileBlob(FileUtils.getResourceFileFromContext(DOCS_VALUES_WITH_SPACES));
        doTestImportOperation(params, blob, 0, 0, 2, 2);
        doAssertDescription(session, testFolder.getRef(), false);
    }

    @Test
    @Deploy("org.nuxeo.ecm.csv.core:OSGI-INF/test-do-not-trim-contrib.xml")
    public void testImportOperationTrimByParamButNotByProperty() throws OperationException {
        Map<String, Object> params = Map.of("path", testFolder.getPathAsString(), "trim", true);
        Blob blob = new FileBlob(FileUtils.getResourceFileFromContext(DOCS_VALUES_WITH_SPACES));
        doTestImportOperation(params, blob, 0, 0, 2, 2);
        doAssertDescription(session, testFolder.getRef(), true);
    }

    @Test
    public void testImportOperation() throws OperationException {
        Map<String, Object> params = Map.of("path", testFolder.getPathAsString());
        Blob blob = new FileBlob(FileUtils.getResourceFileFromContext(DOCS_OK_CSV));
        doTestImportOperation(params, blob, 0, 0, 336, 336);
    }

    public void doTestImportOperation(Map<String, Object> params, Blob input, int expectedErrorCount,
            int expectedSkippedCount, int expectedSuccessCount, int expectedTotalCount) throws OperationException {
        var chain = new OperationChain("test-chain");
        chain.add(CSVImportOperation.ID).from(params);

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(input);

        String importId = (String) service.run(ctx, chain);

        assertNotNull(importId);
        await().pollInterval(Duration.ONE_HUNDRED_MILLISECONDS).atMost(TIMEOUT_SECONDS, TimeUnit.SECONDS).until(() -> {
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
        assertEquals(expectedErrorCount, result.getErrorLineCount());
        assertEquals(expectedSkippedCount, result.getSkippedLineCount());
        assertEquals(expectedSuccessCount, result.getSuccessLineCount());
        assertEquals(expectedTotalCount, result.getTotalLineCount());
    }
}
