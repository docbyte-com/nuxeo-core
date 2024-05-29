/*
 * (C) Copyright 2006-2024 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.automation.io.rest.operations;

import static jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.stream.Collectors;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;

import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.BodyPartEntity;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.MultiPartProperties;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.nuxeo.common.Environment;
import org.nuxeo.common.function.ThrowableFunction;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 2025.0
 */
@Provider
@Consumes({ "multipart/form-data", "multipart/related" })
public class MultiPartExecutionRequestReader
        implements MessageBodyReader<ExecutionRequest>, Feature, ContextResolver<MultiPartProperties> {

    @Context
    protected MessageBodyWorkers messageBodyWorkers;

    @Override
    public boolean isReadable(Class<?> arg0, Type arg1, Annotation[] arg2, MediaType arg3) {
        return ExecutionRequest.class.isAssignableFrom(arg0);
    }

    @Override
    public ExecutionRequest readFrom(Class<ExecutionRequest> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {
        // first read the multipart with Jersey
        var multiPartReader = messageBodyWorkers.getMessageBodyReader(MultiPart.class, MultiPart.class, annotations,
                mediaType);
        var multiPart = multiPartReader.readFrom(MultiPart.class, MultiPart.class, annotations, mediaType, httpHeaders,
                entityStream);

        // second validate the multipart
        var bodyParts = multiPart.getBodyParts();
        if (bodyParts.size() < 2) {
            throw new NuxeoException(
                    "Received only %s part whereas at least 2 are expected".formatted(bodyParts.size()),
                    SC_BAD_REQUEST);
        }

        // third convert it to ExecutionRequest
        var executionRequest = bodyParts.get(0).getEntityAs(ExecutionRequest.class);
        if (bodyParts.size() == 2) { // a blob
            executionRequest.setInput(readBlob(bodyParts.get(1)));
        } else { // a blob list
            executionRequest.setInput(bodyParts.subList(1, bodyParts.size())
                                               .stream()
                                               .map(ThrowableFunction.asFunction(this::readBlob))
                                               .collect(Collectors.toCollection(BlobList::new)));
        }
        return executionRequest;
    }

    protected Blob readBlob(BodyPart bodyPart) throws IOException {
        try {
            var bodyPartEntity = bodyPart.getEntityAs(BodyPartEntity.class);
            var parameterizedHeaders = bodyPart.getParameterizedHeaders();

            // create a temporary file here to let jersey delete it at the end of the request processing
            File tmp = Framework.createTempFile("nx-automation-upload-", ".tmp");
            bodyPartEntity.moveTo(tmp);
            var contentType = parameterizedHeaders.getFirst("Content-Type").getValue();
            var encoding = parameterizedHeaders.getFirst("Content-Type").getParameters().get("charset");
            var filename = decodeFilename(bodyPart.getContentDisposition().getFileName());

            return Blobs.createBlob(tmp, contentType, encoding, filename);
        } catch (ParseException e) {
            throw new IOException("Unable to parse multipart body part headers", e);
        }
    }

    protected static String decodeFilename(String filename) {
        try {
            // Jersey decodes multipart headers in ISO-8859-1 even if it encodes them in UTF-8
            // get back the original filename header bytes and try to decode them using UTF-8
            // if decoding succeeds, use it as the new filename, otherwise keep the original one
            byte[] bytes = filename.getBytes(StandardCharsets.ISO_8859_1);
            CharsetDecoder dec = StandardCharsets.UTF_8.newDecoder();
            CharBuffer buffer = dec.onUnmappableCharacter(CodingErrorAction.REPORT)
                                   .onMalformedInput(CodingErrorAction.REPORT)
                                   .decode(ByteBuffer.wrap(bytes));
            return buffer.toString();
        } catch (CharacterCodingException e) {
            return filename;
        }
    }

    @Override
    public boolean configure(FeatureContext context) {
        context.register(MultiPartFeature.class);
        return true;
    }

    @Override
    public MultiPartProperties getContext(Class<?> type) {
        return new MultiPartProperties().tempDir(Environment.getDefault().getTemp().getAbsolutePath());
    }
}
