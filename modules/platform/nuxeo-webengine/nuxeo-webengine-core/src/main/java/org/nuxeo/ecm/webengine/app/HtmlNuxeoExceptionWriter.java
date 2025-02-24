/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
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

import static org.nuxeo.ecm.webengine.app.JsonWebengineWriter.getExceptionMessage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;

import javax.inject.Inject;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.webengine.model.Template;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 2025.0
 */
@Provider
@Produces(MediaType.TEXT_HTML)
public class HtmlNuxeoExceptionWriter implements MessageBodyWriter<NuxeoException> {

    @Inject
    protected WebContext webContext;

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return NuxeoException.class.isAssignableFrom(type)
                // during mediaType resolution, see ContainerResponse#write, writers are checked with a '*/*' mediaType,
                // when there's no Accept header, to deduce the response mediaType from the @Produces annotation
                // enforce mediaType check during this mechanism to let JsonNuxeoExceptionWriter be chosen
                && mediaType != null && mediaType.toString().startsWith(MediaType.TEXT_HTML);
    }

    @Override
    public long getSize(NuxeoException e, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(NuxeoException e, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException, WebApplicationException {
        var exceptionArgs = new HashMap<String, Object>();
        exceptionArgs.put("message", getExceptionMessage(e));
        exceptionArgs.put("statusCode", e.getStatusCode());
        if (Framework.isDevModeSet()) {
            try (var stream = new ByteArrayOutputStream(); var printer = new PrintWriter(stream, true)) {
                e.printStackTrace(printer);
                var stackTrace = stream.toString();
                exceptionArgs.put("stackTrace", stackTrace);
            }
        }
        new Template(webContext.getRoot(), "error.html.ftl") //
                                                            .arg("exception", exceptionArgs)
                                                            .arg("isDevModeSet", Framework.isDevModeSet())
                                                            .render(entityStream);
    }
}
