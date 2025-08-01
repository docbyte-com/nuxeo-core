/*
 * (C) Copyright 2012-2024 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thierry Martins
 */
package org.nuxeo.ecm.automation.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.runtime.RuntimeMessage.Source.EXTENSION;

import jakarta.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.automation.scripting.internals.ScriptingOperationTypeImpl;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationNotFoundException;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.core.impl.ChainTypeImpl;
import org.nuxeo.ecm.automation.core.impl.OperationTypeImpl;
import org.nuxeo.ecm.automation.core.operations.document.CreateDocument;
import org.nuxeo.ecm.automation.core.operations.document.SaveDocument;
import org.nuxeo.ecm.automation.core.operations.login.Logout;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.automation.test.helpers.TestOperation;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;

@RunWith(FeaturesRunner.class)
@Features(AutomationFeature.class)
@Deploy("org.nuxeo.ecm.automation.test")
public class TestOperationRegistration {

    @Inject
    protected AutomationService service;

    @Inject
    protected HotDeployer hotDeployer;

    @Test
    public void testRegistration() throws Exception {
        OperationType op = service.getOperation(CreateDocument.ID);
        assertEquals(CreateDocument.class, op.getType());

        // register new operation to override existing one, but replace = false
        hotDeployer.deploy("org.nuxeo.ecm.automation.test.test:test-operation-replace-disable-contrib.xml");
        assertEquals("Failed to register extension to: service:org.nuxeo.ecm.core.operation.OperationServiceComponent, "
                + "xpoint: operations in component: service:org.nuxeo.automation.test.operation.replace.disable.contrib "
                + "(org.nuxeo.ecm.core.api.NuxeoException: An operation is already bound to: Document.Create. Use 'replace=true' to replace an existing operation)",
                getFirstExtensionMessage());
        // check nothing has changed
        op = service.getOperation(CreateDocument.ID);
        assertEquals(CreateDocument.class, op.getType());
        hotDeployer.undeploy("org.nuxeo.ecm.automation.test.test:test-operation-replace-disable-contrib.xml");

        // register new operation to override existing one with replace = true,
        hotDeployer.deploy("org.nuxeo.ecm.automation.test.test:test-operation-replace-enable-contrib.xml");
        assertNotNull(service);
        op = service.getOperation(CreateDocument.ID);
        assertEquals(DummyCreateDocument.class, op.getType());
    }

    @Test
    @Deploy("org.nuxeo.ecm.automation.test.test:operation-contrib.xml")
    @Deploy("org.nuxeo.ecm.automation.test.test:chain-scripting-operation-contrib.xml")
    public void testContributingComponent() throws Exception {
        OperationType op = service.getOperation(SaveDocument.ID);
        assertEquals("service:org.nuxeo.ecm.core.automation.coreContrib", op.getContributingComponent());
        // check operation from another component
        op = service.getOperation(TestOperation.ID);
        assertTrue(op instanceof OperationTypeImpl);
        assertEquals("service:org.nuxeo.automation.rest.test.operationContrib", op.getContributingComponent());
        // check chains
        op = service.getOperation("FileManager.ImportWithMetaData");
        assertTrue(op instanceof ChainTypeImpl);
        assertEquals("service:org.nuxeo.ecm.core.automation.features.operations", op.getContributingComponent());
        // check chain from another component
        op = service.getOperation("testChain");
        assertTrue(op instanceof ChainTypeImpl);
        assertEquals("service:org.nuxeo.automation.rest.test.operationContrib", op.getContributingComponent());
        // check chain old-style
        op = service.getOperation("testChain2");
        assertTrue(op instanceof ChainTypeImpl);
        assertEquals("service:org.nuxeo.automation.rest.test.chainScriptingOperationContrib",
                op.getContributingComponent());
        // check scripting
        op = service.getOperation("javascript.RemoteScriptWithDoc");
        assertTrue(op instanceof ScriptingOperationTypeImpl);
        assertEquals("service:org.nuxeo.automation.rest.test.operationContrib", op.getContributingComponent());
    }

    @Test
    @Deploy("org.nuxeo.ecm.automation.test.test:test-scripted-operation-contrib.xml")
    public void testDisableScriptingOperationType() throws Exception {
        service.getOperation("testScript");
        hotDeployer.deploy("org.nuxeo.ecm.automation.test.test:test-scripted-operation-disable-contrib.xml");
        var e = assertThrows(OperationNotFoundException.class, () -> service.getOperation("testScript"));
        assertEquals("No operation was bound on ID: testScript", e.getMessage());
    }

    @Test
    @Deploy("org.nuxeo.ecm.automation.test.test:test-scripted-operation-contrib.xml")
    public void testMixingOperationTypes() throws Exception {
        var scriptedOperation = service.getOperation("testScript");
        assertEquals(ScriptingOperationTypeImpl.class, scriptedOperation.getClass());

        // Trying to merge a ChainTypeImpl into a ScriptingOperationTypeImpl
        hotDeployer.deploy(
                "org.nuxeo.ecm.automation.test.test:test-merge-chain-operation-into-scripting-operation-contrib.xml");
        assertTrue(getFirstExtensionMessage().matches("^Failed to register extension to: "
                + "service:org.nuxeo.ecm.core.operation.OperationServiceComponent, xpoint: chains in component: "
                + "service:org.nuxeo.ecm.automation.test.merge.chain.to.scripting.contrib \\(java.lang.UnsupportedOperationException: "
                + "Can't merge operations with id: testScript. The type org.nuxeo.ecm.automation.core.impl.ChainTypeImpl"
                + "@.+\\[id=testScript] cannot be merged in org.nuxeo.automation.scripting.internals.ScriptingOperationTypeImpl@.+\\[id=testScript].\\)$"));
        scriptedOperation = service.getOperation("testScript");
        assertEquals(ScriptingOperationTypeImpl.class, scriptedOperation.getClass());
        hotDeployer.undeploy(
                "org.nuxeo.ecm.automation.test.test:test-merge-chain-operation-into-scripting-operation-contrib.xml");

        var operation = service.getOperation(Logout.ID);
        assertEquals(OperationTypeImpl.class, operation.getClass());

        // Trying to merge a ChainTypeImpl into an OperationTypeImpl
        hotDeployer.deploy(
                "org.nuxeo.ecm.automation.test.test:test-merge-chain-operation-into-java-operation-contrib.xml");
        assertTrue(getFirstExtensionMessage().matches("^Failed to register extension to: "
                + "service:org.nuxeo.ecm.core.operation.OperationServiceComponent, xpoint: chains in component: "
                + "service:org.nuxeo.ecm.automation.test.merge.chain.to.java.contrib \\(java.lang.UnsupportedOperationException: "
                + "Can't merge operations with id: Auth.Logout. The type org.nuxeo.ecm.automation.core.impl.ChainTypeImpl"
                + "@.+\\[id=Auth.Logout] cannot be merged in org.nuxeo.ecm.automation.core.impl.OperationTypeImpl@.+"
                + "\\[id=Auth.Logout,type=org.nuxeo.ecm.automation.core.operations.login.Logout,params=\\{}].\\)$"));
        operation = service.getOperation(Logout.ID);
        assertEquals(OperationTypeImpl.class, operation.getClass());
        hotDeployer.undeploy(
                "org.nuxeo.ecm.automation.test.test:test-merge-chain-operation-into-java-operation-contrib.xml");

        // Trying to merge a ScriptingOperationTypeImpl into an OperationTypeImpl
        hotDeployer.deploy(
                "org.nuxeo.ecm.automation.test.test:test-merge-scripting-operation-into-java-operation-contrib.xml");
        assertTrue(getFirstExtensionMessage().matches("^Failed to register extension to: "
                + "service:org.nuxeo.automation.scripting.internals.AutomationScriptingComponent, xpoint: operation in component: "
                + "service:org.nuxeo.ecm.automation.test.merge.scripting.to.java.contrib \\(java.lang.UnsupportedOperationException: "
                + "Can't merge operations with id: Auth.Logout. The type org.nuxeo.automation.scripting.internals.ScriptingOperationTypeImpl"
                + "@.+\\[id=Auth.Logout] cannot be merged in org.nuxeo.ecm.automation.core.impl.OperationTypeImpl@.+"
                + "\\[id=Auth.Logout,type=org.nuxeo.ecm.automation.core.operations.login.Logout,params=\\{}].\\)$"));
        operation = service.getOperation(Logout.ID);
        assertEquals(OperationTypeImpl.class, operation.getClass());
    }

    protected static String getFirstExtensionMessage() {
        return Framework.getRuntime()
                        .getMessageHandler()
                        .getMessages(runtimeMessage -> runtimeMessage.getSource() == EXTENSION)
                        .getFirst();
    }
}
