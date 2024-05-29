/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.forms.layout.export;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.platform.forms.layout.api.service.LayoutStore;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.model.view.TemplateView;
import org.nuxeo.runtime.api.Framework;

@Path("layout-manager")
public class RootResource {

    protected TemplateView getTemplate(String name, WebContext webContext) {
        var uriInfo = webContext.getUriInfo();
        String baseURL = uriInfo.getAbsolutePath().toString();
        if (!baseURL.endsWith("/")) {
            baseURL += "/";
        }
        return new TemplateView(webContext, this, name).arg("baseURL", baseURL);
    }

    @GET
    public TemplateView doGet(@Context WebContext webContext) {
        LayoutStore service = Framework.getService(LayoutStore.class);
        // XXX: use hard coded "jsf" category for now
        int nbWidgetTypes = service.getWidgetTypeDefinitions("jsf").size();
        int nbLayoutTypes = service.getLayoutTypeDefinitions("jsf").size();
        int nbLayouts = service.getLayoutDefinitionNames("jsf").size();
        return getTemplate("index.ftl", webContext).arg("nbWidgetTypes", Integer.valueOf(nbWidgetTypes))
                                                   .arg("nbLayouts", Integer.valueOf(nbLayouts))
                                                   .arg("nbLayoutTypes", Integer.valueOf(nbLayoutTypes));
    }

    @Path("layouts")
    public LayoutResource getLayouts() {
        // XXX: use hard coded "jsf" category for now
        return new LayoutResource("jsf");
    }

    @Path("widget-types")
    public WidgetTypeResource getWidgetTypes(@QueryParam("widgetTypeCategory") String widgetTypeCategory) {
        if (StringUtils.isBlank(widgetTypeCategory)) {
            widgetTypeCategory = "jsf";
        }
        return new WidgetTypeResource(widgetTypeCategory);
    }

    @Path("layout-types")
    public LayoutTypeResource getLayoutTypes(@QueryParam("layoutTypeCategory") String layoutTypeCategory) {
        if (StringUtils.isBlank(layoutTypeCategory)) {
            layoutTypeCategory = "jsf";
        }
        return new LayoutTypeResource(layoutTypeCategory);
    }

}
