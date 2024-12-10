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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.platform.routing.test;

import static org.junit.Assert.assertEquals;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.MultiRepositoryFeature;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode;
import org.nuxeo.ecm.platform.routing.core.impl.GraphRoute;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;

/**
 * @since 2025.0
 */
@Features({ WorkflowFeature.class, MultiRepositoryFeature.class })
@Deploy("org.nuxeo.ecm.platform.routing.core:OSGI-INF/test-document-routing-activation-filters.xml")
public class WorkflowActivationFilterMultiRepositoryTest extends AbstractGraphRouteTest {

    @Inject
    protected CoreSession session;

    @Inject
    @Named("other")
    protected CoreSession otherSession;

    @Inject
    protected DocumentRoutingService routing;

    @Before
    public void setUp() {
        doc = session.createDocumentModel("/", "file", "File");
        doc = session.createDocument(doc);
    }

    public void setRoute(String routeName, String activationFiltername, CoreSession session) {
        routeDoc = createRoute(routeName, session);
        DocumentModel node1 = createNode(routeDoc, "node1", session);
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        node1.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        session.saveDocument(node1);
        routeDoc.setPropertyValue(GraphRoute.PROP_VARIABLES_FACET, "FacetRoute1");
        routeDoc.setPropertyValue(GraphRoute.PROP_AVAILABILITY_FILTER, activationFiltername);
        routeDoc.addFacet("FacetRoute1");
        routeDoc = session.saveDocument(routeDoc);
        validate(routeDoc, session);
    }

    // NXP-31351
    @Test
    public void testWorkflowIsRunnableMultiRepo() {
        setRoute("testWorkflowIsRunnable", "test_wf_pass", session);
        List<DocumentRoute> runnables = routing.getRunnableWorkflows(session, List.of(doc.getId()));
        assertEquals(1, runnables.size());

        // Create another doc in the other repository
        DocumentModel doc2 = session.createDocumentModel("/", "file2", "File");
        doc2 = otherSession.createDocument(doc2);
        setRoute("testWorkflowIsRunnable", "test_wf_pass", otherSession);

        // Check we are able to retrieve the workflow model from the other repository
        runnables = routing.getRunnableWorkflows(otherSession, List.of(doc2.getId()));
        assertEquals(1, runnables.size());
    }
}
