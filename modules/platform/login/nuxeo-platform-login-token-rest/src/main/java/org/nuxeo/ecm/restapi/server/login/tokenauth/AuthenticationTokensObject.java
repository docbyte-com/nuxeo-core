/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nelson Silva <nsilva@nuxeo.com>
 */
package org.nuxeo.ecm.restapi.server.login.tokenauth;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.tokenauth.io.AuthenticationToken;
import org.nuxeo.ecm.tokenauth.service.TokenAuthenticationService;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.AbstractResource;
import org.nuxeo.ecm.webengine.model.impl.ResourceTypeImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * Token Object
 *
 * @since 8.3
 */
@WebObject(type = "token")
@Produces(MediaType.APPLICATION_JSON)
public class AuthenticationTokensObject extends AbstractResource<ResourceTypeImpl> {

    private TokenAuthenticationService service;

    @Override
    protected void initialize(Object... args) {
        service = Framework.getService(TokenAuthenticationService.class);
    }

    @GET
    public List<AuthenticationToken> getTokens(@QueryParam("application") String applicationName) {
        DocumentModelList tokens = service.getTokenBindings(getCurrentUser().getName(), applicationName);
        return tokens.stream().map(this::asAuthenticationToken).collect(Collectors.toList());
    }

    @POST
    public Response createToken(@QueryParam("application") String applicationName,
            @QueryParam("deviceId") String deviceId, @QueryParam("deviceDescription") String deviceDescription,
            @QueryParam("permission") String permission) {
        String username = getCurrentUser().getName();
        String token = service.acquireToken(username, applicationName, deviceId, deviceDescription, permission);
        return Response.ok(token).status(Response.Status.CREATED).build();
    }

    @DELETE
    @Path("{token}")
    public void deleteToken(@PathParam("token") String tokenId) {
        if (tokenId == null) {
            return;
        }
        service.revokeToken(tokenId);
    }

    private NuxeoPrincipal getCurrentUser() {
        return getContext().getCoreSession().getPrincipal();
    }

    private AuthenticationToken asAuthenticationToken(DocumentModel entry) {
        Map<String, Object> props = entry.getProperties("authtoken");
        AuthenticationToken token = new AuthenticationToken((String) props.get("token"), (String) props.get("userName"),
                (String) props.get("applicationName"), (String) props.get("deviceId"),
                (String) props.get("deviceDescription"), (String) props.get("permission"));
        token.setCreationDate((Calendar) props.get("creationDate"));
        return token;
    }
}
