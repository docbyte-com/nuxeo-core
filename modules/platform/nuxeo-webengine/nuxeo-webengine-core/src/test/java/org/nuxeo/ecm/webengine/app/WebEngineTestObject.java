/*
 * (C) Copyright 2024-2025 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.webengine.app;

import java.util.Map;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;

import org.nuxeo.ecm.webengine.model.AdapterResource;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;

/**
 * @since 2025.0
 */
@WebObject(type = "WebEngineTestObject")
@Produces("application/json;charset=UTF-8")
public class WebEngineTestObject extends DefaultObject {

    protected String origin;

    @Override
    public void initialize(Object... args) {
        assert args != null && args.length == 1;
        origin = (String) args[0];
    }

    @GET
    public Map<String, String> get() {
        return Map.of("location", getClass().getSimpleName(), "origin", origin);
    }

    @GET
    @Path("/my-header")
    public Map<String, String> getMyHeader(@Context HttpHeaders headers) {
        return Map.of("header", headers.getRequestHeaders().getFirst("X-My-Header"));
    }

    @GET
    @Path("/my-resource-path")
    public Map<String, String> getMyResourcePath() {
        return Map.of("resourcePath", getPath());
    }

    @GET
    @Path("/my-object")
    public MyObject getMyObject(@Context MyObject myObject) {
        return myObject;
    }

    @Override
    @Path("@{segment}")
    public AdapterResource disptachAdapter(@PathParam("segment") String adapterName) {
        // override to provide the origin
        return newAdapter(adapterName, origin + '/' + getClass().getSimpleName());
    }
}
