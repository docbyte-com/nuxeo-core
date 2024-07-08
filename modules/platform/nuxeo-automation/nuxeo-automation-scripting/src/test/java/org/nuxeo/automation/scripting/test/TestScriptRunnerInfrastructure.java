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
 *     Thierry Delprat <tdelprat@nuxeo.com>
 *     Ronan DANIELLOU <rdaniellou@nuxeo.com>
 */
package org.nuxeo.automation.scripting.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.schema.test.CommonDocumentConstants.COMMON_ARRAY_STRING_PROP;
import static org.nuxeo.ecm.core.schema.test.CommonDocumentConstants.COMMON_COMPLEXES_PROP;
import static org.nuxeo.ecm.core.schema.test.CommonDocumentConstants.COMMON_COMPLEX_PROP;
import static org.nuxeo.ecm.core.schema.test.CommonDocumentConstants.COMMON_DOC_TYPE;
import static org.nuxeo.ecm.core.schema.test.CommonDocumentConstants.COMMON_STRINGS_PROP;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.automation.scripting.AutomationScriptingFeature;
import org.nuxeo.automation.scripting.api.AutomationScriptingService;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationDocumentation.Param;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.scripting.DateWrapper;
import org.nuxeo.ecm.automation.core.scripting.MvelExpression;
import org.nuxeo.ecm.automation.core.trace.TracerFactory;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.automation.core.util.DataModelProperties;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.test.UserManagerFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;

/**
 * @since 7.2
 */
@RunWith(FeaturesRunner.class)
@Features({ AutomationScriptingFeature.class, UserManagerFeature.class })
@Deploy("org.nuxeo.ecm.platform.content.template") // needed for the default-domain creation
@Deploy("org.nuxeo.ecm.platform.dublincore") // needed for TestPropertiesAccessOnDocuments which gets dc:creator
public class TestScriptRunnerInfrastructure {

    @Inject
    protected CoreSession session;

    @Inject
    protected AutomationService automationService;

    @Inject
    protected AutomationScriptingFeature feature;

    @Inject
    protected AutomationScriptingService service;

    @Inject
    protected TracerFactory factory;

    @Inject
    protected AutomationScriptingFeature scripting;

    @Test
    public void shouldExecuteSimpleScript() throws Exception {
        DocumentModelList docs = scripting.run("simpleAutomationScript.js", session, DocumentModelList.class);
        assertEquals(10, docs.size());
    }

    @Test
    public void simpleScriptingOperationShouldBeAvailable() throws Exception {

        OperationType type = automationService.getOperation("Scripting" + ".HelloWorld");
        assertNotNull(type);

        Param[] paramDefs = type.getDocumentation().getParams();
        assertEquals(1, paramDefs.length);

        try (OperationContext ctx = new OperationContext(session)) {
            Map<String, Object> params = new HashMap<>();

            params.put("lang", "en");
            ctx.setInput("John");
            Object result = automationService.run(ctx, "Scripting.HelloWorld", params);
            assertEquals("Hello John", result.toString());
        }

        try (OperationContext ctx = new OperationContext(session)) {
            Map<String, Object> params = new HashMap<>();

            params.put("lang", "fr");
            ctx.setInput("John");
            Object result = automationService.run(ctx, "Scripting.HelloWorld", params);
            assertEquals("Bonjour John", result.toString());
        }
    }

    @Test
    public void runOperationOnSubTree() throws Exception {

        DocumentModel root = session.getRootDocument();

        for (int i = 0; i < 5; i++) {
            DocumentModel doc = session.createDocumentModel("/", "new" + i, "File");
            session.createDocument(doc);
        }

        session.save();
        DocumentModelList res = session.query("select * from File where  " + "ecm:mixinType = 'HiddenInNavigation'");
        assertEquals(0, res.size());

        try (OperationContext ctx = new OperationContext(session)) {
            Map<String, Object> params = new HashMap<>();

            params.put("facet", "HiddenInNavigation");
            params.put("type", "File");
            ctx.setInput(root);
            Object result = automationService.run(ctx, "Scripting.AddFacetInSubTree", params);
            DocumentModelList docs = (DocumentModelList) result;
            assertEquals(5, docs.size());
        }
    }

    @Test
    public void simpleScriptingOperationsInChain() throws Exception {

        try (OperationContext ctx = new OperationContext(session)) {
            Map<String, Object> params = new HashMap<>();

            ctx.setInput("John");
            Object result = automationService.run(ctx, "Scripting.ChainedHello", params);
            assertEquals("Hello Bonjour John", result.toString());
        }

    }

    @Test
    public void simpleCallToScriptingOperationsChain() throws Exception {
        String message = scripting.run("simpleCallToChain.js", session, String.class);
        assertEquals("Hello Bonjour John", message);

    }

    @Test
    public void testOperationCtx() throws OperationException {
        try (OperationContext ctx = new OperationContext(session)) {
            Map<String, Object> params = new HashMap<>();
            ctx.put("test", "odd");
            DocumentModel result = (DocumentModel) automationService.run(ctx, "Scripting.TestOperationCtx", params);
            assertEquals("odd", result.getPropertyValue("dc:nature"));
            assertEquals("modifiedValue", result.getPropertyValue("dc:description"));
            assertEquals("newEntry", result.getPropertyValue("dc:title"));
            assertEquals("Administrator", result.getPropertyValue("dc:creator"));
        }
    }

    @Test
    public void testOperationWithBlob() throws IOException, OperationException {
        // upload file blob
        File fieldAsJsonFile = FileUtils.getResourceFileFromContext("creationFields.json");
        Blob fb = Blobs.createBlob(fieldAsJsonFile);
        fb.setMimeType("image/jpeg");

        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(fb);
            Map<String, Object> params = new HashMap<>();
            params.put("document", "/newDoc");
            DocumentModel result = (DocumentModel) automationService.run(ctx, "Scripting.TestBlob", params);
            final Blob blob = (Blob) result.getPropertyValue("file:content");
            assertEquals("New Title", result.getTitle());
            assertEquals("creationFields.json", blob.getFilename());
        }
    }

    @Test
    public void testWithSetBlobOperation() throws IOException, OperationException {
        // upload file blob
        File fieldAsJsonFile = FileUtils.getResourceFileFromContext("creationFields.json");
        Blob fb = Blobs.createBlob(fieldAsJsonFile);
        fb.setMimeType("image/jpeg");

        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(fb);
            Map<String, Object> params = new HashMap<>();
            params.put("document", "/newDoc");
            DocumentModel result = (DocumentModel) automationService.run(ctx, "Scripting.TestSetBlob", params);
            assertEquals("creationFields.json", ((Blob) result.getPropertyValue("file:content")).getFilename());
        }
    }

    @Test
    public void testComplexProperties() throws OperationException {
        String creationProps = """
                 tcs:long=1720444934
                 tcs:string=Some string
                 tcs:strings=["att1", "att2", "att3"]
                 tcc:complexes=[\
                  {\
                    "string": "complexes 0 string",\
                    "strings": ["att4", "att5", "att6"]\
                  },\
                  {\
                    "string": "complexes 1 string",\
                    "strings": ["att4", "att5", "att6"]\
                  }\
                ]
                """;
        try (OperationContext ctx = new OperationContext(session)) {
            Map<String, Object> params = new HashMap<>();
            params.put("properties", creationProps);
            params.put("type", COMMON_DOC_TYPE);
            params.put("name", "testDoc");
            DocumentModel result = (DocumentModel) automationService.run(ctx, "Scripting.TestComplexProperties",
                    params);
            assertEquals("complexes 0 string",
                    ((Map<?, ?>) ((List<?>) result.getPropertyValue(COMMON_COMPLEXES_PROP)).get(0)).get("string"));
        }
    }

    @Test
    public void testClassFilter() throws Exception {
        try {
            scripting.run("classFilterScript.js", session, Void.class);
        } catch (RuntimeException cause) {
            assertEquals(ClassNotFoundException.class, cause.getCause().getClass());
        }
    }

    @Test
    @Deploy("org.nuxeo.ecm.automation.scripting.tests:OSGI-INF/classfilter-contrib.xml")
    public void testClassFilterAllowed() throws Exception {
        // injected fields in features aren't recomputed correctly (bug), so pass service explicitly
        feature.run(service, "classFilterScript.js", session, Void.class);
    }

    @Test
    @Deploy("org.nuxeo.ecm.automation.scripting.tests:OSGI-INF/classfilter2-contrib.xml")
    public void testClassFilterDenied() throws Exception {
        // injected fields in features aren't recomputed correctly (bug), so pass service explicitly
        try {
            feature.run(service, "classFilterScript.js", session, Void.class);
        } catch (RuntimeException cause) {
            assertEquals(ClassNotFoundException.class, cause.getCause().getClass());
        }
    }

    @Test
    public void testFn() throws Exception {
        // Test platform functions injection
        String message = scripting.run("platformFunctions.js", session, String.class);
        assertEquals("devnull@nuxeo.com", message);
    }

    @Test
    public void handleDocumentListAsInput() throws OperationException {
        try (OperationContext ctx = new OperationContext(session)) {
            DocumentModelList result = (DocumentModelList) automationService.run(ctx,
                    "Scripting.TestInputDocumentList");
            assertNotNull(result);
        }
    }

    @Test
    public void handleWorkflowVariables() throws OperationException {
        try (OperationContext ctx = new OperationContext(session)) {
            Map<String, Object> wfVars = new HashMap<>();
            Map<String, Object> nodeVars = new HashMap<>();
            wfVars.put("var", "workflow");
            nodeVars.put("var", "node");
            ctx.put(Constants.VAR_WORKFLOW, wfVars);
            ctx.put(Constants.VAR_WORKFLOW_NODE, nodeVars);
            DocumentModel result = (DocumentModel) automationService.run(ctx, "Scripting.TestOperationWF");
            assertEquals("workflow", result.getPropertyValue("dc:title"));
            assertEquals("node", result.getPropertyValue("dc:description"));
        }
    }

    @Test
    public void canUseChainWithDashes() throws OperationException {
        runChain("Scripting.TestChainWithDashes");
    }

    @Test
    @Deploy("org.nuxeo.ecm.automation.scripting.tests:chain-with-category-dashes-contrib.xml")
    public void canUseChainWithCategoryDashes() throws OperationException {
        runChain("Scripting.TestChainWithCategoryDashes");
    }

    protected void runChain(String chainId) throws OperationException {
        try (OperationContext ctx = new OperationContext(session)) {
            DocumentModel root = session.getRootDocument();
            ctx.setInput(root);
            DocumentModel result = (DocumentModel) automationService.run(ctx, chainId);
            assertNotNull(result);
        }
    }

    @Test
    public void canManageDocumentModelWrappers() throws OperationException {
        try (OperationContext ctx = new OperationContext(session)) {
            DocumentModel root = session.getRootDocument();
            root.setPropertyValue("dc:title", "New Title");
            session.saveDocument(root);
            ctx.setInput(root);
            ctx.put("var", root);
            Map<String, Object> params = new HashMap<>();
            params.put("param", "root");
            Object result = automationService.run(ctx, "Scripting.TestWrappers", params);
            assertTrue(result instanceof DocumentModel);
            Object doc = ctx.get("var");
            assertNotNull(doc);
            assertTrue(doc instanceof DocumentModel);
            assertTrue((Boolean) ctx.get("entry"));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void canHandleJavaListMap() throws OperationException {
        DocumentModel doc = session.createDocumentModel("/", "doc", COMMON_DOC_TYPE);
        doc.setPropertyValue(COMMON_STRINGS_PROP, (Serializable) List.of("attr1", "attr2", "attr3"));
        doc.setPropertyValue(COMMON_ARRAY_STRING_PROP, (Serializable) List.of("attr3", "attr4", "attr5"));
        doc.setPropertyValue(COMMON_COMPLEX_PROP, (Serializable) Map.of("string", "vlad"));
        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(session.createDocument(doc));
            DocumentModel result = (DocumentModel) automationService.run(ctx, "Scripting.TestList");
            assertEquals("newValue", ((List<String>) result.getPropertyValue(COMMON_STRINGS_PROP)).get(0));
            assertEquals("newValue", ((String[]) result.getPropertyValue(COMMON_ARRAY_STRING_PROP))[2]);
            assertEquals("vlad", ((Map<?, ?>) result.getPropertyValue(COMMON_COMPLEX_PROP)).get("string"));
        }
    }

    @Test
    public void canHandleLoginAsCtx() throws OperationException {
        CoreSession session = CoreInstance.getCoreSession(this.session.getRepositoryName(), "jdoe");
        try (OperationContext ctx = new OperationContext(session)) {
            String username = (String) automationService.run(ctx, "my-chain-with-loginasctx");
            assertEquals("Administrator", username);
        }
    }

    @Test
    public void canHandleLoginAsOp() throws OperationException {
        CoreSession session = CoreInstance.getCoreSession(this.session.getRepositoryName(), "jdoe");
        try (OperationContext ctx = new OperationContext(session)) {
            String username = (String) automationService.run(ctx, "my-chain-with-loginasop");
            assertEquals("Administrator", username);
        }
    }

    @Test
    public void canUnwrapContextDocListing() throws OperationException {
        try (OperationContext ctx = new OperationContext(session)) {
            DocumentModel root = session.getRootDocument();
            DocumentModelList docs = new DocumentModelListImpl();
            docs.add(root);
            docs.add(root);
            ctx.put("docs", docs);
            Object result = automationService.run(ctx, "Scripting.SimpleScript");
            assertNotNull(result);
        }
    }

    /*
     * NXP-19012
     */
    @Test
    public void canUnwrapContextWithTrace() throws OperationException {
        if (!factory.getRecordingState()) {
            factory.toggleRecording();
        }

        try (OperationContext ctx = new OperationContext(session)) {
            DocumentModel root = session.getRootDocument();
            DocumentModelList docs = new DocumentModelListImpl();
            docs.add(root);
            docs.add(root);
            ctx.put("docs", docs);
            ctx.setInput(root);
            Map<String, Object> params = new HashMap<>();
            Object result = automationService.run(ctx, "Scripting.ChainWithScripting", params);
            assertNotNull(result);
            // check if the context has been unwrapped correctly
            assertTrue(
                    ctx.get("docs") instanceof DocumentModelList && ((DocumentModelList) ctx.get("docs")).size() == 2);
        }
    }

    @Test
    public void testMVELScriptResolver() throws Exception {
        try (OperationContext ctx = new OperationContext(session)) {
            Object mvelResult = automationService.run(ctx, "my-chain-with-mvelresolver");
            assertEquals("Foo Bar", mvelResult);
        }
    }

    /*
     * NXP-19444
     */
    @Test
    public void testSet() throws Exception {
        try (OperationContext ctx = new OperationContext(session)) {
            DocumentModel root = session.getRootDocument();
            ctx.setInput(root);
            root = (DocumentModel) automationService.run(ctx, "Scripting.TestSet");
            assertEquals("TitleFromTest", root.getProperty("dc:title").getValue());
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(0L);
            assertEquals(cal, root.getProperty("dc:created").getValue());
        }
    }

    /*
     * NXP-19444
     */
    @Test
    public void testSetPropertyValue() throws Exception {
        try (OperationContext ctx = new OperationContext(session)) {
            DocumentModel root = session.getRootDocument();
            ctx.setInput(root);
            root = (DocumentModel) automationService.run(ctx, "Scripting.TestSetPropertyValue");
            assertEquals("TitleFromTest", root.getProperty("dc:title").getValue());
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(0L);
            assertEquals(cal, root.getProperty("dc:created").getValue());
        }
    }

    /*
     * NXP-19444
     */
    @Test
    public void testSetArray() throws Exception {
        try (OperationContext ctx = new OperationContext(session)) {
            DocumentModel root = session.getRootDocument();
            ctx.setInput(root);
            root = (DocumentModel) automationService.run(ctx, "Scripting.TestSetArray");
            assertArrayEquals(new String[] { "sciences", "society" },
                    (Object[]) root.getProperty("dc:subjects").getValue());
        }
    }

    /*
     * NXP-19444
     */
    @Test
    public void testSetPropertyValueArray() throws Exception {
        try (OperationContext ctx = new OperationContext(session)) {
            DocumentModel root = session.getRootDocument();
            ctx.setInput(root);
            root = (DocumentModel) automationService.run(ctx, "Scripting.TestSetPropertyValueArray");
            assertArrayEquals(new String[] { "sciences", "society" },
                    (Object[]) root.getProperty("dc:subjects").getValue());
        }
    }

    /*
     * NXP-19176
     */
    @Test
    public void testBlobListAsInputToAnotherOperation() throws IOException, OperationException {
        // Init parameters
        File fieldAsJsonFile = FileUtils.getResourceFileFromContext("creationFields.json");
        Blob fb = Blobs.createBlob(fieldAsJsonFile);
        fb.setMimeType("image/jpeg");

        DocumentModel doc = session.createDocumentModel("/", "docWithBlobs", "File");
        doc = session.createDocument(doc);
        DocumentHelper.addBlob(doc.getProperty("files:files"), fb);
        DocumentHelper.addBlob(doc.getProperty("files:files"), fb);
        session.saveDocument(doc);

        try (OperationContext ctx = new OperationContext(session)) {
            BlobList result = (BlobList) automationService.run(ctx, "Scripting.TestBlobListInputToAnotherOperation");
            assertNotNull(result);
            assertEquals(2, result.size());
            // We added two blobs to context
            BlobList blobs = (BlobList) ctx.pop(Constants.O_BLOBS);
            assertNotNull(blobs);
            assertEquals(2, blobs.size());
        }
    }

    /*
     * NXP-19176
     */
    @Test
    public void testBlobArrayAsInputToAnotherOperation() throws IOException, OperationException {
        // Init parameters
        File fieldAsJsonFile = FileUtils.getResourceFileFromContext("creationFields.json");
        Blob fb = Blobs.createBlob(fieldAsJsonFile);
        fb.setMimeType("image/jpeg");

        DocumentModel doc = session.createDocumentModel("/", "docWithBlobs", "File");
        doc = session.createDocument(doc);
        DocumentHelper.addBlob(doc.getProperty("files:files"), fb);
        DocumentHelper.addBlob(doc.getProperty("files:files"), fb);
        session.saveDocument(doc);

        try (OperationContext ctx = new OperationContext(session)) {
            BlobList result = (BlobList) automationService.run(ctx, "Scripting.TestBlobArrayInputToAnotherOperation");
            assertNotNull(result);
            assertEquals(2, result.size());
            // We added two blobs to context
            BlobList blobs = (BlobList) ctx.pop(Constants.O_BLOBS);
            assertNotNull(blobs);
            assertEquals(2, blobs.size());
        }
    }

    /*
     * NXP-26847
     */
    @Test
    public void testFnCalendarOperation() throws OperationException {
        try (OperationContext ctx = new OperationContext(session)) {
            Date now = new Date();
            ctx.setInput(now);
            DateWrapper result = (DateWrapper) automationService.run(ctx, "Scripting.TestFnCalendar");
            assertEquals(now, result.getDate());
        }
    }

    @Test
    public void testArrayObjectParametersOperation() throws OperationException {
        try (OperationContext ctx = new OperationContext(session)) {
            DocumentModel root = session.getRootDocument();
            ctx.setInput(root);
            root = (DocumentModel) automationService.run(ctx, "Scripting.TestArrayObjectProperties");
            assertArrayEquals(new String[] { "sciences", "society" },
                    (Object[]) root.getProperty("dc:subjects").getValue());
        }
    }

    @Test
    public void testNotInlinedContext() throws OperationException {
        try (OperationContext ctx = new OperationContext(session)) {
            ctx.put("today", new MvelExpression("CurrentDate.date"));
            ctx.put("tomorrow", new MvelExpression("CurrentDate.days(1).date"));
            DataModelProperties props = (DataModelProperties) automationService.run(ctx, "Scripting.TestParams");
            Assertions.assertThat(props.getMap()).containsOnlyKeys("today");
        }
    }

    @Test
    public void testLoopWithDifferentParameters() throws OperationException {
        try (OperationContext ctx = new OperationContext(session)) {
            Object result = automationService.run(ctx, "Scripting.TestLoopWithDifferentParameters");
            assertEquals("P1 P2 P3 P4 P5 P6 P7 P8 P9 P10 ", String.valueOf(result));
        }
    }

    @Test
    public void testDocumentPathInputAdaptedAsDocument() throws OperationException {
        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput("/default-domain");
            DocumentModel doc = (DocumentModel) automationService.run(ctx, "Scripting.TestInputAdaptedAsDocument");
            assertEquals("/default-domain", doc.getPathAsString());
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDocumentPathsInputAdaptedAsDocuments() throws OperationException {
        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput("/default-domain,/default-domain/workspaces");
            List<DocumentModel> docs = (List<DocumentModel>) automationService.run(ctx,
                    "Scripting.TestInputAdaptedAsDocuments");
            assertEquals(2, docs.size());
            assertEquals("/default-domain", docs.get(0).getPathAsString());
            assertEquals("/default-domain/workspaces", docs.get(1).getPathAsString());
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDocumentsInputAdaptedAsDocuments() throws OperationException {
        DocumentModel domain = session.getDocument(new PathRef("/default-domain"));
        DocumentModel workspaces = session.getDocument(new PathRef("/default-domain/workspaces"));
        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(List.of(domain, workspaces));
            List<DocumentModel> docs = (List<DocumentModel>) automationService.run(ctx,
                    "Scripting.TestInputAdaptedAsDocuments");
            assertEquals(2, docs.size());
            assertEquals("/default-domain", docs.get(0).getPathAsString());
            assertEquals("/default-domain/workspaces", docs.get(1).getPathAsString());
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testBlobsInputAdaptedAsBlobs() throws Exception {
        Blob blob1 = Blobs.createBlob("Blob1");
        Blob blob2 = Blobs.createBlob("Blob2");
        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(List.of(blob1, blob2));
            List<Blob> blobs = (List<Blob>) automationService.run(ctx, "Scripting.TestInputAdaptedAsBlobs");
            assertEquals(2, blobs.size());
            assertEquals("Blob1", blobs.get(0).getString());
            assertEquals("Blob2", blobs.get(1).getString());
        }
    }

    @Test
    public void testUpdateProperties() throws OperationException {
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        doc.setPropertyValue("dc:description", "description");
        doc.setPropertyValue("dc:source", "source");
        doc = session.createDocument(doc);
        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(doc);
            DocumentModel res = (DocumentModel) automationService.run(ctx, "Scripting.TestUpdateProperties");
            assertEquals("foo", res.getPropertyValue("dc:description"));
            assertEquals("bar", res.getPropertyValue("dc:source"));
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateComplexProperties() throws OperationException {
        DocumentModel doc = session.createDocumentModel("/", "docComplex", COMMON_DOC_TYPE);
        doc.setPropertyValue(COMMON_COMPLEX_PROP, (Serializable) Map.of("string", "Nuxeo", "integer", 1_000));
        doc = session.createDocument(doc);
        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(doc);
            DocumentModel res = (DocumentModel) automationService.run(ctx, "Scripting.TestUpdateComplexProperties");
            Map<String, Serializable> complexItem = (Map<String, Serializable>) res.getPropertyValue(
                    COMMON_COMPLEX_PROP);
            assertEquals("Hyland", complexItem.get("string"));
            assertEquals(10_000L, complexItem.get("integer"));
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPropertiesAccessOnDocuments() throws OperationException {
        DocumentModel domain = session.getDocument(new PathRef("/default-domain"));
        DocumentModel workspaces = session.getDocument(new PathRef("/default-domain/workspaces"));
        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(List.of(domain, workspaces));
            List<DocumentModel> docs = (List<DocumentModel>) automationService.run(ctx,
                    "Scripting.TestPropertiesAccessOnDocuments");
            assertEquals(2, docs.size());
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDocumentProperty() throws OperationException {
        DocumentModel domain = session.getDocument(new PathRef("/default-domain"));
        DocumentModel doc = session.createDocumentModel("/", "file", "FileWithDocumentFields");
        doc.setPropertyValue("df:documentId", computeDocumentFieldRef(domain.getId()));
        doc.setPropertyValue("df:documentPath", computeDocumentFieldRef(domain.getPathAsString()));
        doc = session.createDocument(doc);
        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(doc);
            List<DocumentModel> docs = (List<DocumentModel>) automationService.run(ctx,
                    "Scripting.TestDocumentProperty");
            assertEquals(2, docs.size());
            assertEquals("/default-domain", docs.get(0).getPathAsString());
            assertEquals("/default-domain", docs.get(1).getPathAsString());
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void tesDocumentListProperty() throws OperationException {
        DocumentModel domain = session.getDocument(new PathRef("/default-domain"));
        DocumentModel workspaces = session.getDocument(new PathRef("/default-domain/workspaces"));
        DocumentModel doc = session.createDocumentModel("/", "file", "FileWithDocumentFields");
        doc.setPropertyValue("df:documentIds", (Serializable) List.of(computeDocumentFieldRef(domain.getId()),
                computeDocumentFieldRef(workspaces.getId())));
        doc.setPropertyValue("df:documentPaths",
                (Serializable) List.of(computeDocumentFieldRef(workspaces.getPathAsString()),
                        computeDocumentFieldRef(domain.getPathAsString())));
        doc = session.createDocument(doc);
        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(doc);
            List<DocumentModel> docs = (List<DocumentModel>) automationService.run(ctx,
                    "Scripting.TestDocumentListProperty");
            assertEquals(4, docs.size());
            assertEquals("/default-domain", docs.get(0).getPathAsString());
            assertEquals("/default-domain/workspaces", docs.get(1).getPathAsString());
            assertEquals("/default-domain/workspaces", docs.get(2).getPathAsString());
            assertEquals("/default-domain", docs.get(3).getPathAsString());
        }
    }

    protected String computeDocumentFieldRef(String value) {
        return session.getRepositoryName() + ":" + value;
    }

    /**
     * Tests that the default classFilter contrib allows some non-standard imports.
     */
    @Test
    public void testImportWithClassFilter() throws IOException, OperationException {
        try (OperationContext ctx = new OperationContext(session)) {
            Blob blob = (Blob) automationService.run(ctx, "Scripting.TestImport", Map.of());
            assertEquals("application/json", blob.getMimeType());
            String string = blob.getString();
            assertTrue(string, string.startsWith("{'uuid': "));
        }
    }

    @Test
    @WithFrameworkProperty(name = "nuxeo.automation.scripting.optimistic.types.enabled", value = "true")
    public void testOptimisticTypesConfiguration() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "file", "FileWithDocumentFields");
        doc.setPropertyValue("df:documentIds", (Serializable) List.of(session.getRootDocument().getId()));
        doc = session.createDocument(doc);

        feature.runScriptWithFrameworkProperties(doc, Map.of(), "testRemoveItemFromListProperty.js", session);
    }

}
