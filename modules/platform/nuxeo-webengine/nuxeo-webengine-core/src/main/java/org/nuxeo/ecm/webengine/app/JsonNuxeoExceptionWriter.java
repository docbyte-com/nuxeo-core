/*
 * (C) Copyright 2017-2024 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 *
 */
package org.nuxeo.ecm.webengine.app;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.inject.Singleton;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;

import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * @since 9.3
 */
@Singleton
@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JsonNuxeoExceptionWriter implements MessageBodyWriter<NuxeoException> {

    @Context
    protected HttpServletResponse response;

    @Override
    public long getSize(NuxeoException arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4) {
        return -1;
    }

    @Override
    public boolean isWriteable(Class<?> arg0, Type arg1, Annotation[] arg2, MediaType arg3) {
        return NuxeoException.class.isAssignableFrom(arg0);
    }

    @Override
    public void writeTo(NuxeoException nuxeoException, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType mediaType,
            MultivaluedMap<String, Object> arg5, OutputStream outputStream)
            throws IOException, WebApplicationException {
        // the ContentType header is not set when errors occurred during routing resolution since Jersey 2.x upgrade
        // this occurs if Jersey doesn't find a route or if a method returning a sub resource throws an exception
        if (response.getContentType() == null) {
            response.setContentType(MediaType.APPLICATION_JSON);
        }
        JsonWebengineWriter.writeException(outputStream, nuxeoException, mediaType);
    }

}
