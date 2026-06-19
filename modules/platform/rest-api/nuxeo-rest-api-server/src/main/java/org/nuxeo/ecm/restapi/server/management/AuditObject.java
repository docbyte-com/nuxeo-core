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

import static org.nuxeo.ecm.core.io.marshallers.NuxeoMediaType.TEXT_PLANT_UML;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.nuxeo.audit.api.AuditRouterIntrospection;
import org.nuxeo.audit.service.AuditRouter;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.AbstractResource;
import org.nuxeo.ecm.webengine.model.impl.ResourceTypeImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 2025.16
 */
@WebObject(type = ManagementObject.MANAGEMENT_OBJECT_PREFIX + "audit")
public class AuditObject extends AbstractResource<ResourceTypeImpl> {

    @GET
    @Path("/introspection")
    @Produces(TEXT_PLANT_UML)
    public AuditRouterIntrospection routing() {
        return Framework.getService(AuditRouter.class).getIntrospection();
    }
}
