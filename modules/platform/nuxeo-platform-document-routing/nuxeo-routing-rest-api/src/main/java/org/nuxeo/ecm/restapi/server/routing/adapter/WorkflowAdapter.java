/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 *
 */

package org.nuxeo.ecm.restapi.server.routing.adapter;

import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.core.io.WorkflowRequest;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.ecm.webengine.model.impl.DefaultAdapter;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 7.2
 */
@WebAdapter(name = WorkflowAdapter.NAME, type = "workflowAdapter")
public class WorkflowAdapter extends DefaultAdapter {

    public static final String NAME = "workflow";

    @POST
    public Response doPost(WorkflowRequest routingRequest) {
        DocumentModel doc = getTarget().getAdapter(DocumentModel.class);
        CoreSession session = doc.getCoreSession();
        String workflowModelName = routingRequest.getWorkflowModelName();
        DocumentRoutingService documentRoutingService = Framework.getService(DocumentRoutingService.class);
        if (documentRoutingService.canCreateInstance(session, List.of(doc.getId()), workflowModelName)) {
            String workflowInstanceId = documentRoutingService.createNewInstance(workflowModelName,
                    List.of(doc.getId()), routingRequest.getVariables(), session, true);
            DocumentModel result = session.getDocument(new IdRef(workflowInstanceId));
            DocumentRoute route = result.getAdapter(DocumentRoute.class);
            return Response.ok(route).status(Status.CREATED).build();
        } else {
            return Response.status(Status.BAD_REQUEST).build();
        }
    }

    @GET
    public List<DocumentRoute> doGet() {
        DocumentModel doc = getTarget().getAdapter(DocumentModel.class);
        return Framework.getService(DocumentRoutingService.class)
                        .getDocumentRelatedWorkflows(doc, getContext().getCoreSession());
    }

    @GET
    @Path("{workflowInstanceId}/task")
    public List<Task> doGetTasks(@PathParam("workflowInstanceId") String workflowInstanceId) {
        DocumentModel doc = getTarget().getAdapter(DocumentModel.class);
        return Framework.getService(DocumentRoutingService.class)
                        .getTasks(doc, null, workflowInstanceId, null, getContext().getCoreSession());
    }

}
