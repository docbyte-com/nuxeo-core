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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.forms.layout.export;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

import org.nuxeo.ecm.platform.forms.layout.api.LayoutTypeDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.impl.LayoutTypeDefinitionComparator;
import org.nuxeo.ecm.platform.forms.layout.api.service.LayoutStore;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.view.TemplateView;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 6.0
 */
public class LayoutTypeResource {

    protected final String category;

    protected LayoutStore service;

    protected final List<LayoutTypeDefinition> layoutTypes;

    public LayoutTypeResource(String category) {
        this.category = category;
        service = Framework.getService(LayoutStore.class);
        layoutTypes = service.getLayoutTypeDefinitions(category);
        // sort so that order is deterministic
        layoutTypes.sort(new LayoutTypeDefinitionComparator());
    }

    @GET
    @Path("layoutTypes")
    public LayoutTypeDefinitions getLayoutTypeDefinitions(@Context HttpServletRequest request,
            @QueryParam("all") Boolean all) {
        return new LayoutTypeDefinitions(layoutTypes);
    }

    @GET
    @Path("layoutType/{name}")
    public Response getLayoutTypeDefinition(@Context HttpServletRequest request, @PathParam("name") String name) {
        LayoutTypeDefinition def = service.getLayoutTypeDefinition(category, name);
        if (def != null) {
            return Response.ok(def).build();
        } else {
            return Response.status(401).build();
        }
    }

    public TemplateView getTemplate(WebContext webContext) {
        return getTemplate("layout-types.ftl", webContext);
    }

    protected TemplateView getTemplate(String name, WebContext webContext) {
        var uriInfo = webContext.getUriInfo();
        String baseURL = uriInfo.getAbsolutePath().toString();
        if (!baseURL.endsWith("/")) {
            baseURL += "/";
        }
        TemplateView tv = new TemplateView(webContext, this, name);
        tv.arg("layoutTypeCategory", category);
        tv.arg("layoutTypes", layoutTypes);
        tv.arg("baseURL", baseURL);
        return tv;
    }

    @GET
    public TemplateView doGet(@QueryParam("layoutType") String layoutTypeName, @Context WebContext webContext) {
        if (layoutTypeName == null) {
            return getTemplate(webContext);
        } else {
            LayoutTypeDefinition wType = service.getLayoutTypeDefinition(category, layoutTypeName);
            if (wType == null) {
                throw new WebResourceNotFoundException("No layout type found with name: " + layoutTypeName);
            }
            TemplateView tpl = getTemplate(webContext);
            tpl.arg("layoutType", wType);
            return tpl;
        }
    }

}
