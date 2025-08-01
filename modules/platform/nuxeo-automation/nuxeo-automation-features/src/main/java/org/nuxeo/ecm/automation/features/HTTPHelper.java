/*
 * (C) Copyright 2015-2024 Nuxeo (http://nuxeo.com/) and others.
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
 *      Thibaud Arguillere <targuillere@nuxeo.com>
 *      Vladimir Pasquier <vpasquier@nuxeo.com>
 *      Ricardo Dias <rdias@nuxeo.com>
 */
package org.nuxeo.ecm.automation.features;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import org.apache.http.HttpMessage;
import org.apache.http.NameValuePair;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.nuxeo.ecm.automation.context.ContextHelper;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.io.registry.MarshallerRegistry;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 7.3
 */
public class HTTPHelper implements ContextHelper {

    private static final Integer TIMEOUT = 1000 * 60 * 5; // 5min

    private static final String HTTP_CONTENT_DISPOSITION = "Content-Disposition";

    public Blob call(String username, String password, String requestType, String path) throws IOException {
        return call(username, password, requestType, path, null, null, null);
    }

    public Blob call(String username, String password, String requestType, String path, Map<String, String> headers)
            throws IOException {
        return call(username, password, requestType, path, null, null, headers);
    }

    public Blob call(String username, String password, String requestType, String path,
            MultivaluedMap<String, String> queryParams) throws IOException {
        return call(username, password, requestType, path, null, queryParams, null);
    }

    public Blob call(String username, String password, String requestType, String path, Object data)
            throws IOException {
        return call(username, password, requestType, path, data, null, null);
    }

    public Blob call(String username, String password, String requestType, String path, Object data,
            Map<String, String> headers) throws IOException {
        return call(username, password, requestType, path, data, null, headers);
    }

    public Blob call(String username, String password, String requestType, String url, Object data,
            MultivaluedMap<String, String> queryParams, Map<String, String> headers) throws IOException {
        headers = Objects.requireNonNullElseGet(headers, HashMap::new);
        if (username != null && password != null && !headers.containsKey(HttpHeaders.AUTHORIZATION)) {
            headers.putAll(basicAuthentication(username, password));
        }
        return invoke(requestType, url, data, queryParams, headers);
    }

    /**
     * @since 8.4
     */
    public Blob get(String url, Map<String, Object> options) throws IOException {
        return invoke("GET", url, null, options);
    }

    /**
     * @since 8.4
     */
    public Blob post(String url, Object data, Map<String, Object> options) throws IOException {
        return invoke("POST", url, data, options);
    }

    /**
     * @since 8.4
     */
    public Blob put(String url, Object data, Map<String, Object> options) throws IOException {
        return invoke("PUT", url, data, options);
    }

    /**
     * @since 8.4
     */
    public Blob delete(String url, Object data, Map<String, Object> options) throws IOException {
        return invoke("DELETE", url, data, options);
    }

    protected Blob invoke(String requestType, String url, Object data, Map<String, Object> options) throws IOException {
        MultivaluedMap<String, String> queryParams = getQueryParameters(options);
        Map<String, String> headers = getHeaderParameters(options);
        return invoke(requestType, url, data, queryParams, headers);
    }

    protected Blob invoke(String requestType, String url, Object data, MultivaluedMap<String, String> queryParams,
            Map<String, String> headers) throws IOException {
        RequestConfig requestConfig = RequestConfig.custom()
                                                   .setConnectTimeout(TIMEOUT)
                                                   .setSocketTimeout(TIMEOUT)
                                                   .build();
        try (var client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build()) {
            var uriBuilder = new URIBuilder(url);
            if (queryParams != null) {
                queryParams.forEach((key, value1) -> value1.forEach(value -> uriBuilder.addParameter(key, value)));
            }
            var uri = uriBuilder.build();
            var request = switch (requestType) {
                case "HEAD", "GET" -> new HttpGet(uri);
                case "POST" -> new HttpPost(uri);
                case "PUT" -> new HttpPut(uri);
                case "DELETE" -> new HttpDelete(uri);
                default -> throw new NuxeoException("Unknown request type: " + requestType);
            };
            // set headers
            if (headers != null) {
                headers.forEach(request::addHeader);
            }
            // set the entity body
            if (data != null && request instanceof HttpEntityEnclosingRequestBase enclosingRequest) {
                var mediaType = getFirstHeaderValue(request, HttpHeaders.CONTENT_TYPE).map(MediaType::valueOf)
                                                                                      .orElse(MediaType.WILDCARD_TYPE);
                var contentType = ContentType.create(mediaType.toString());
                // check if we need to use core-io for marshalling
                if (data instanceof String || data instanceof Integer || data instanceof Long
                        || data instanceof Boolean) {
                    enclosingRequest.setEntity(new StringEntity(data.toString(), contentType));
                } else {
                    @SuppressWarnings("unchecked")
                    var type = (Class<Object>) data.getClass();
                    var writer = Framework.getService(MarshallerRegistry.class)
                                          .getWriter(RenderingContext.CtxBuilder.get(), type, mediaType);
                    try (var bao = new ByteArrayOutputStream()) {
                        writer.write(data, type, type, mediaType, bao);

                        enclosingRequest.setEntity(new StringEntity(bao.toString(), contentType));
                    }
                }
            }
            return client.execute(request, convertResponseToBlobHandler(url));
        } catch (URISyntaxException e) {
            throw new RuntimeException("Unable to build the requested url", e);
        }
    }

    @SuppressWarnings("unchecked")
    protected Map<String, String> getHeaderParameters(Map<String, Object> options) {
        if (options != null) {
            Map<String, String> headers = new HashMap<>();

            Map<String, String> authorization = (Map<String, String>) options.get("cloud/docbyte/aws/auth");
            if (authorization != null) {
                String method = authorization.get("method");
                if (method.equals("basic")) {
                    Map<String, String> header = basicAuthentication(authorization.get("username"),
                            authorization.get("password"));
                    headers.putAll(header);
                }
            }

            Map<String, String> headersOptions = (Map<String, String>) options.get("headers");
            if (headersOptions != null) {
                headers.putAll(headersOptions);
            }

            return headers;
        }
        return null;
    }

    protected Map<String, String> basicAuthentication(String username, String password) {
        if (username == null || password == null) {
            return Map.of();
        }
        return Map.of(HttpHeaders.AUTHORIZATION,
                "Basic " + Base64.getEncoder().encodeToString((username + ':' + password).getBytes()));
    }

    @SuppressWarnings("unchecked")
    protected MultivaluedMap<String, String> getQueryParameters(Map<String, Object> options) {
        if (options != null) {
            Map<String, List<String>> params = (Map<String, List<String>>) options.get("params");
            if (params != null) {
                MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
                for (String key : params.keySet()) {
                    queryParams.put(key, params.get(key));
                }
                return queryParams;
            }
        }
        return null;
    }

    protected ResponseHandler<Blob> convertResponseToBlobHandler(String url) {
        return response -> {
            var statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            // handle only successful response
            if (statusCode < 200 || statusCode >= 300) {
                return Blobs.createBlob(statusLine.toString());
            }
            try (var content = response.getEntity().getContent()) {
                // TODO change it to HttpHeaders from JAX RS when upgrading to 2.x
                String filename = getFirstHeaderValue(response, HTTP_CONTENT_DISPOSITION).map(contentDisposition -> {
                    int filenameIndex = contentDisposition.indexOf("filename=");
                    if (filenameIndex > -1) {
                        return contentDisposition.substring(filenameIndex + 9);
                    }
                    return null;
                }).orElseGet(() -> url.substring(url.lastIndexOf('/') + 1));

                var blob = Blobs.createBlob(content);
                blob.setFilename(filename);
                getFirstHeaderValue(response, HttpHeaders.CONTENT_TYPE).ifPresent(blob::setMimeType);
                getFirstHeaderValue(response, HttpHeaders.CONTENT_ENCODING).ifPresent(blob::setEncoding);
                return blob;
            }
        };
    }

    protected Optional<String> getFirstHeaderValue(HttpMessage httpMessage, String headerName) {
        return Optional.ofNullable(httpMessage.getFirstHeader(headerName)).map(NameValuePair::getValue);
    }
}
