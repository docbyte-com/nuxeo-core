/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.model.impl;

import static jakarta.servlet.http.HttpServletResponse.SC_NOT_FOUND;

import java.io.IOException;
import java.util.Date;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.Module;
import org.nuxeo.ecm.webengine.model.ModuleResource;
import org.nuxeo.ecm.webengine.model.ResourceType;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.scripting.ScriptFile;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ModuleRoot extends DefaultObject implements ModuleResource {

    @Context
    public void setContext(WebContext context) {
        // check if context has already been initialized
        if (context.getModule() != null) {
            return;
        }
        try {
            Module module = findModule();
            ResourceType type = module.getType(getClass().getAnnotation(WebObject.class).type());
            context.setModule(module);
            initialize(context, type);
            setRoot(true);
        } finally {
            context.push(this);
        }
    }

    private Module findModule() {
        Path path = getClass().getAnnotation(Path.class);
        if (path == null) {
            throw new java.lang.IllegalStateException("ModuleRoot not annotated with @Path: " + getClass());
        }
        ModuleConfiguration mc = Framework.getService(WebEngine.class)
                                          .getModuleManager()
                                          .getModuleByRootClass(getClass());
        if (mc == null) {
            throw new java.lang.IllegalStateException("No module found for root resource: " + getClass());
        }
        return mc.get();
    }

    @GET
    @Path("skin/{path:.*}")
    public Response getSkinResource(@PathParam("path") String path) {
        try {
            ScriptFile file = getModule().getSkinResource("/resources/" + path);
            if (file != null) {
                long lastModified = file.lastModified();
                ResponseBuilder resp = Response.ok(file.getFile())
                                               .lastModified(new Date(lastModified))
                                               .header("Cache-Control", "public")
                                               .header("Server", "Nuxeo/WebEngine-1.0");

                String mimeType = ctx.getEngine().getMimeType(file.getExtension());
                if (mimeType == null) {
                    mimeType = "text/plain";
                }
                resp.type(mimeType);
                return resp.build();
            }
        } catch (IOException e) {
            throw new NuxeoException("Failed to get resource file: " + path, e);
        }
        return Response.status(SC_NOT_FOUND).build();
    }

    /**
     * You should override this method to resolve objects to links. This method is usually called by a search view to
     * generate links for object that are listed
     *
     * @param doc the document
     * @return the link corresponding to that object
     */
    @Override
    public String getLink(DocumentModel doc) {
        return getPath() + "/@nxdoc/" + doc.getId();
    }

    @Override
    public Object handleError(Throwable t) {
        return t;
    }
}
