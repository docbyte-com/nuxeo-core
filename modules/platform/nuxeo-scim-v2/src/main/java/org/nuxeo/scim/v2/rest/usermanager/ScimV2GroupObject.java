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
 *     Thierry Delprat
 *     Antoine Taillefer
 */
package org.nuxeo.scim.v2.rest.usermanager;

import static com.unboundid.scim2.common.exceptions.BadRequestException.INVALID_SYNTAX;
import static com.unboundid.scim2.common.utils.ApiConstants.QUERY_PARAMETER_ATTRIBUTES;
import static com.unboundid.scim2.common.utils.ApiConstants.QUERY_PARAMETER_EXCLUDED_ATTRIBUTES;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.Response.Status.CREATED;
import static org.nuxeo.scim.v2.api.ScimV2QueryContext.FETCH_GROUP_MEMBERS_CTX_PARAM;
import static org.nuxeo.scim.v2.rest.ScimV2Root.SCIM_V2_ENDPOINT_GROUPS;

import java.beans.IntrospectionException;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

import org.nuxeo.common.function.ThrowableUnaryOperator;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.query.sql.model.Predicates;
import org.nuxeo.ecm.core.query.sql.model.QueryBuilder;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.scim.v2.api.ScimV2QueryContext;
import org.nuxeo.scim.v2.rest.marshalling.ResponseUtils;

import com.unboundid.scim2.common.ScimResource;
import com.unboundid.scim2.common.exceptions.BadRequestException;
import com.unboundid.scim2.common.exceptions.ResourceNotFoundException;
import com.unboundid.scim2.common.exceptions.ScimException;
import com.unboundid.scim2.common.exceptions.ServerErrorException;
import com.unboundid.scim2.common.messages.ListResponse;
import com.unboundid.scim2.common.types.GroupResource;
import com.unboundid.scim2.common.utils.SchemaUtils;
import com.unboundid.scim2.server.utils.ResourceTypeDefinition;

/**
 * SCIM 2.0 Group object.
 *
 * @since 2023.14
 */
@WebObject(type = "groups")
@Produces(APPLICATION_JSON)
public class ScimV2GroupObject extends ScimV2BaseUMObject {

    @POST
    public Response createGroup(GroupResource group) throws ScimException {
        checkUpdateGuardPreconditions();
        return ResponseUtils.response(CREATED, prepareCreated(doCreateGroup(group), group));
    }

    @GET
    @Path("{uid}")
    public ScimResource getGroupResource(@PathParam("uid") String uid) throws ScimException {
        return prepareRetrieved(resolveGroupRessource(uid));
    }

    @PUT
    @Path("{uid}")
    public ScimResource updateGroup(@PathParam("uid") String uid, GroupResource group) throws ScimException {
        checkUpdateGuardPreconditions();
        return prepareReplaced(doUpdateGroup(uid, group), group);
    }

    @DELETE
    @Path("{uid}")
    public Response deleteGroupResource(@PathParam("uid") String uid) throws ScimException {
        checkUpdateGuardPreconditions();
        return doDeleteGroup(uid);
    }

    @Override
    protected String getPrefix() {
        return SCIM_V2_ENDPOINT_GROUPS;
    }

    protected ScimResource doCreateGroup(GroupResource group) throws ScimException {
        if (group == null) {
            throw new BadRequestException("Cannot create group without a group resource as request body",
                    INVALID_SYNTAX);
        }
        DocumentModel newGroup = mappingService.createNuxeoGroupFromGroupResource(group);
        return mappingService.getGroupResourceFromNuxeoGroup(newGroup, baseURL);

    }

    protected GroupResource resolveGroupRessource(String uid) throws ScimException {
        DocumentModel groupModel = null;
        if (isFetchMembers()) {
            groupModel = um.getGroupModel(uid);
        } else {
            // searchGroups lazy fetches attributes such as members
            var groups = um.searchGroups(new QueryBuilder().predicate(Predicates.like("groupname", uid)).limit(1));
            if (!groups.isEmpty()) {
                groupModel = groups.get(0);
            }
        }
        if (groupModel == null) {
            throw new ResourceNotFoundException("Cannot find group: " + uid); // NOSONAR
        }
        return mappingService.getGroupResourceFromNuxeoGroup(groupModel, baseURL);
    }

    protected GroupResource doUpdateGroup(String uid, GroupResource group) throws ScimException {
        if (group == null) {
            throw new BadRequestException("Cannot update group without a group resource as request body",
                    INVALID_SYNTAX);
        }
        DocumentModel groupModel = mappingService.updateNuxeoGroupFromGroupResource(uid, group);
        return mappingService.getGroupResourceFromNuxeoGroup(groupModel, baseURL);
    }

    protected Response doDeleteGroup(String uid) throws ScimException {
        try {
            um.deleteGroup(uid);
            return ResponseUtils.deleted();
        } catch (DirectoryException e) {
            throw new ResourceNotFoundException("Cannot find group: " + uid);
        }
    }

    @Override
    protected ListResponse<ScimResource> doSearch(ScimV2QueryContext queryCtx) throws ScimException {
        queryCtx.withContextParam(FETCH_GROUP_MEMBERS_CTX_PARAM, isFetchMembers());
        return mappingService.queryGroups(
                queryCtx.withTransform(ThrowableUnaryOperator.asUnaryOperator(r -> prepareRetrieved(r))));
    }

    @Override
    protected ResourceTypeDefinition getResourceTypeDefinition() throws ScimException {
        try {
            return new ResourceTypeDefinition.Builder("groups", SCIM_V2_ENDPOINT_GROUPS).setCoreSchema(
                    SchemaUtils.getSchema(GroupResource.class)).build();
        } catch (IntrospectionException e) {
            throw new ServerErrorException("Cannot get resource type definition");
        }
    }

    protected boolean isFetchMembers() {
        var excludedAttributes = uriInfo.getQueryParameters().getFirst(QUERY_PARAMETER_EXCLUDED_ATTRIBUTES);
        var includedAttributes = uriInfo.getQueryParameters().getFirst(QUERY_PARAMETER_ATTRIBUTES);
        if ((excludedAttributes != null && excludedAttributes.contains("members"))
                || includedAttributes != null && !includedAttributes.contains("members")) {
            return false;
        }
        return true;
    }

}
