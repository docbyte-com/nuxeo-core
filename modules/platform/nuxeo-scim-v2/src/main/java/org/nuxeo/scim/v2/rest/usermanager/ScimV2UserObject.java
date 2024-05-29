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
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.Response.Status.CREATED;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.nuxeo.scim.v2.rest.ScimV2Root.SCIM_V2_ENDPOINT_USERS;

import java.beans.IntrospectionException;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

import org.nuxeo.common.function.ThrowableUnaryOperator;
import org.nuxeo.ecm.core.api.DocumentModel;
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
import com.unboundid.scim2.common.messages.PatchRequest;
import com.unboundid.scim2.common.types.UserResource;
import com.unboundid.scim2.common.utils.SchemaUtils;
import com.unboundid.scim2.server.utils.ResourceTypeDefinition;

/**
 * SCIM 2.0 User object.
 *
 * @since 2023.14
 */
@WebObject(type = "users")
@Produces(APPLICATION_JSON)
public class ScimV2UserObject extends ScimV2BaseUMObject {

    @POST
    public Response createUser(UserResource user) throws ScimException {
        checkUpdateGuardPreconditions();
        return ResponseUtils.response(CREATED, prepareCreated(doCreateUser(user), user));
    }

    @GET
    @Path("{uid}")
    public ScimResource getUserResource(@PathParam("uid") String uid) throws ScimException {
        return prepareRetrieved(resolveUserRessource(uid));
    }

    @PUT
    @Path("{uid}")
    public ScimResource updateUser(@PathParam("uid") String uid, UserResource user) throws ScimException {
        checkUpdateGuardPreconditions();
        return prepareReplaced(doUpdateUser(uid, user), user);
    }

    @PATCH
    @Path("{uid}")
    public ScimResource patchUser(@PathParam("uid") String uid, PatchRequest patch) throws ScimException {
        checkUpdateGuardPreconditions();
        return this.prepareModified(doPatchUser(uid, patch), patch);
    }

    @DELETE
    @Path("{uid}")
    public Response deleteUserResource(@PathParam("uid") String uid) throws ScimException {
        checkUpdateGuardPreconditions();
        return doDeleteUser(uid);
    }

    @Override
    protected String getPrefix() {
        return SCIM_V2_ENDPOINT_USERS;
    }

    protected UserResource doCreateUser(UserResource user) throws ScimException {
        if (user == null) {
            throw new BadRequestException("Cannot create user without a user resource as request body", INVALID_SYNTAX);
        }
        var userName = user.getUserName();
        if (isBlank(userName)) {
            throw new BadRequestException("Cannot create user without a username", INVALID_SYNTAX);
        }
        DocumentModel newUser = mappingService.createNuxeoUserFromUserResource(user);
        return mappingService.getUserResourceFromNuxeoUser(newUser, baseURL);
    }

    protected UserResource resolveUserRessource(String uid) throws ScimException {
        DocumentModel userModel = um.getUserModel(uid);
        if (userModel == null) {
            throw new ResourceNotFoundException("Cannot find user: " + uid); // NOSONAR
        }
        return mappingService.getUserResourceFromNuxeoUser(userModel, baseURL);
    }

    protected UserResource doUpdateUser(String uid, UserResource user) throws ScimException {
        if (user == null) {
            throw new BadRequestException("Cannot update user without a user resource as request body", INVALID_SYNTAX);
        }
        DocumentModel userModel = mappingService.updateNuxeoUserFromUserResource(uid, user);
        return mappingService.getUserResourceFromNuxeoUser(userModel, baseURL);
    }

    protected UserResource doPatchUser(String uid, PatchRequest patch) throws ScimException {
        if (patch == null) {
            throw new BadRequestException("Cannot patch user without a patch request resource as request body",
                    INVALID_SYNTAX);
        }
        DocumentModel userModel = mappingService.patchNuxeoUser(uid, patch);
        return mappingService.getUserResourceFromNuxeoUser(userModel, baseURL);
    }

    protected Response doDeleteUser(String uid) throws ScimException {
        try {
            um.deleteUser(uid);
            return ResponseUtils.deleted();
        } catch (DirectoryException e) {
            throw new ResourceNotFoundException("Cannot find user: " + uid);
        }
    }

    @Override
    protected ListResponse<ScimResource> doSearch(ScimV2QueryContext queryCtx) throws ScimException {
        return mappingService.queryUsers(
                queryCtx.withTransform(ThrowableUnaryOperator.asUnaryOperator(r -> prepareRetrieved(r))));
    }

    @Override
    protected ResourceTypeDefinition getResourceTypeDefinition() throws ScimException {
        try {
            return new ResourceTypeDefinition.Builder("users", SCIM_V2_ENDPOINT_USERS).setCoreSchema(
                    SchemaUtils.getSchema(UserResource.class)).build();
        } catch (IntrospectionException e) {
            throw new ServerErrorException("Cannot get resource type definition");
        }
    }

}
