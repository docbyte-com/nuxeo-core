/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.restapi.server.management;

import static org.junit.Assert.assertEquals;
import static org.nuxeo.common.test.ModuleUnderTest.getClassLoaderResourceAsString;

import jakarta.inject.Inject;

import org.junit.Test;
import org.nuxeo.audit.service.AuditBackend;
import org.nuxeo.audit.test.MultiAuditFeature;
import org.nuxeo.ecm.restapi.test.ManagementBaseTest;
import org.nuxeo.http.test.handler.StringHandler;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;

/**
 * @since 2025.16
 */
@Features(MultiAuditFeature.class)
public class TestAuditObjectMultiAudit extends ManagementBaseTest {

    @Inject
    protected AuditBackend backend;

    @Test
    public void testIntrospectionWithoutAnyRoutesToOtherBackend() {
        var auditIntrospectionPuml = httpClient.buildGetRequest("/management/audit/introspection")
                                               .execute(new StringHandler());
        String expectedPuml = getClassLoaderResourceAsString(
                "data/audit-router-introspection-management-test-multi.puml");
        expectedPuml = expectedPuml.replaceAll("\\$\\{defaultBackendImplementation}", backend.getClass().getName());
        assertEquals(expectedPuml, auditIntrospectionPuml);
    }

    @Test
    @Deploy("org.nuxeo.audit.test.test:OSGI-INF/test-multi-audit-basic-other-route.xml")
    public void testIntrospectionWithoutAnyRoutesToOtherBackend2() {
        var auditIntrospectionPuml = httpClient.buildGetRequest("/management/audit/introspection")
                                               .execute(new StringHandler());
        String expectedPuml = getClassLoaderResourceAsString(
                "data/audit-router-introspection-management-test-multi-basic-route.puml");
        expectedPuml = expectedPuml.replaceAll("\\$\\{defaultBackendImplementation}", backend.getClass().getName());
        assertEquals(expectedPuml, auditIntrospectionPuml);
    }
}
