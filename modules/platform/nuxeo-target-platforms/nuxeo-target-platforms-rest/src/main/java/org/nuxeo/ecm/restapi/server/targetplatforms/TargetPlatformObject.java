/*
 * (C) Copyright 2014 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.restapi.server.targetplatforms;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.targetplatforms.api.TargetPlatform;
import org.nuxeo.targetplatforms.api.impl.TargetPlatformFilterImpl;
import org.nuxeo.targetplatforms.api.service.TargetPlatformService;
import org.nuxeo.targetplatforms.io.JSONExporter;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

@WebObject(type = "target-platforms")
@Produces(MediaType.APPLICATION_JSON)
public class TargetPlatformObject extends DefaultObject {
    private static final String PUBLIC_TP_CACHE_KEY = "PUBLIC_TP";

    private static final LoadingCache<String, String> PUBLIC_CACHE = CacheBuilder //
                                                                                 .newBuilder() //
                                                                                 .expireAfterAccess(5, TimeUnit.MINUTES) //
                                                                                 .refreshAfterWrite(10,
                                                                                         TimeUnit.MINUTES) //
                                                                                 .recordStats() //
                                                                                 .maximumSize(5)
                                                                                 .build(new CacheLoader<>() {
                                                                                     @Override
                                                                                     public String load(String key) {
                                                                                         return key;
                                                                                     }
                                                                                 });

    @GET
    public Response doGet() {
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    @GET
    @Path("public")
    public Response doGetPublic() throws Exception {
        String platforms = PUBLIC_CACHE.get(PUBLIC_TP_CACHE_KEY, () -> {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                TargetPlatformService tps = Framework.getService(TargetPlatformService.class);
                List<TargetPlatform> res = Framework.doPrivileged(() -> tps.getAvailableTargetPlatforms(
                        new TargetPlatformFilterImpl(false, true, true, false, null)));
                if (res == null) {
                    res = new ArrayList<>();
                }
                JSONExporter.exportToJson(res, baos, false);
                return new String(baos.toByteArray());
            }
        });

        return Response.status(Response.Status.OK).entity(platforms).build();
    }
}
