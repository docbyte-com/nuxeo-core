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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.automation.io.yaml.test;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;

import jakarta.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.core.AutomationCoreFeature;
import org.nuxeo.ecm.automation.core.OperationChainContribution.Operation;
import org.nuxeo.ecm.automation.io.AutomationIOFeature;
import org.nuxeo.ecm.automation.io.yaml.YamlWriter;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 5.9.4
 */
@RunWith(FeaturesRunner.class)
@Features({ AutomationCoreFeature.class, AutomationIOFeature.class })
@Deploy("org.nuxeo.ecm.automation.io:test-chains.xml")
public class TestAutomationYaml {

    @Inject
    protected AutomationService service;

    protected String getYamlChain(String chainId) throws Exception {
        OperationType op = service.getOperation(chainId);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        YamlWriter.toYaml(out, op.getDocumentation());
        return out.toString();
    }

    protected String getYamlOp(Operation op) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        YamlWriter.toYaml(out, op);
        return out.toString();
    }

    protected void checkEquals(String expected, String actual) {
        assertEquals(expected, actual);
    }

    @Test
    public void testEmptyChain() throws Exception {
        String chain = getYamlChain("empty_chain");
        // chain without operations should never happen
        checkEquals("{}\n", chain);
    }

    @Test
    public void testChain() throws Exception {
        String chain = getYamlChain("chain");
        String res = """
                description: My desc
                operations:
                - Context.FetchDocument
                - Document.Create:
                    type: Note
                    name: MyDoc
                    properties:
                      dc:description: My Doc desc
                      dc:title: My Doc
                """;
        checkEquals(res, chain);
    }

    @Test
    public void testChainAlt() throws Exception {
        String chain = getYamlChain("chain_props");
        String res = """
                - Context.FetchDocument
                - Document.Create:
                    type: Note
                    name: MyDoc
                    properties:
                      dc:description: My Doc desc
                      dc:title: My Doc
                """;
        checkEquals(res, chain);
    }

    @Test
    public void testChainWithParams() throws Exception {
        String chain = getYamlChain("chain_with_params");
        String res = """
                description: This is an awesome chain!
                params:
                - foo:
                    type: string
                    values:
                    - bar
                - foo2:
                    type: boolean
                    description: yop
                operations:
                - Context.FetchDocument
                - Document.Create:
                    type: Note
                    name: MyDoc
                """;
        checkEquals(res, chain);
    }

    @Test
    public void testChainSample() throws Exception {
        String chain = getYamlChain("chain_complex");
        String res = """
                - Context.FetchDocument
                - Document.Create:
                    type: Note
                    name: note
                    properties:
                      dc:title: MyDoc
                - Document.Copy:
                    target: ../../dst
                    name: note_copy
                - Document.SetProperty:
                    xpath: dc:description
                    value: mydesc
                """;
        checkEquals(res, chain);
    }

    @Test
    public void testOp() throws Exception {
        OperationType op = service.getOperation("chain_props");
        String yop = getYamlOp(op.getDocumentation().getOperations()[0]);
        assertEquals("Context.FetchDocument\n", yop);
        yop = getYamlOp(op.getDocumentation().getOperations()[1]);
        String res = """
                Document.Create:
                  type: Note
                  name: MyDoc
                  properties:
                    dc:description: My Doc desc
                    dc:title: My Doc
                """;
        checkEquals(res, yop);
    }

}
