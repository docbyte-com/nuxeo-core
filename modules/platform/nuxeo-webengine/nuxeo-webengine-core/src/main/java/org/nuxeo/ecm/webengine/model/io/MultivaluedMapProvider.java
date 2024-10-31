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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.webengine.model.io;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;

/**
 * {@link MultivaluedMap} can be used as parameter type of Jakarta RS method to retrieve the Form sent through the
 * request body, but as soon as one calls {@link HttpServletRequest#getParameter(String)} or other similar APIs, the
 * request body is lost and Jersey/Jakarta RS doesn't read anything.
 * <p>
 * This provider is used to work around that, and maps the {@link HttpServletRequest#getParameterMap()} to
 * {@link MultivaluedMap}. This brings a consistent behavior, independently if something has called the parameter
 * methods or not.
 * 
 * @since 2025.0
 */
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
public class MultivaluedMapProvider implements MessageBodyReader<MultivaluedMap<String, String>> {

    @Context
    protected HttpServletRequest request;

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == MultivaluedMap.class;
    }

    @Override
    public MultivaluedMap<String, String> readFrom(Class<MultivaluedMap<String, String>> type, Type genericType,
            Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders,
            InputStream entityStream) {
        return request.getParameterMap()
                      .entrySet()
                      .stream()
                      .collect(Collectors.toMap(Map.Entry::getKey, e -> List.of(e.getValue()), (e1, e2) -> {
                          var result = new ArrayList<>(e1);
                          result.addAll(e2);
                          return result;
                      }, MultivaluedHashMap::new));
    }
}
