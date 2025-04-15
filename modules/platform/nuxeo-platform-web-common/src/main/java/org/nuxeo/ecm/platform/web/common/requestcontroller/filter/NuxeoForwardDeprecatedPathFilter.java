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
package org.nuxeo.ecm.platform.web.common.requestcontroller.filter;

import java.io.IOException;
import java.util.Set;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 2025.0
 */
public class NuxeoForwardDeprecatedPathFilter extends HttpFilter {

    private static final Logger log = LogManager.getLogger(NuxeoForwardDeprecatedPathFilter.class);

    public static final String ENABLED_PROPERTY = "nuxeo.forward.deprecated.path.enabled";

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        DeprecatedPath path;
        if (Framework.isBooleanPropertyTrue(ENABLED_PROPERTY) && (path = getDeprecatedPathIfOne(request)) != null) {
            String replacedPath = path.getReplacement(request);
            log.log(Framework.isDevModeSet() ? Level.WARN : Level.INFO,
                    "The path: {} is deprecated since {}, please update your code to execute the request to: {}",
                    () -> getRequestedPath(request), () -> getDeprecatedSince(path), () -> replacedPath);
            request.getRequestDispatcher(replacedPath).forward(request, response);
        } else {
            super.doFilter(request, response, chain);
        }
    }

    protected DeprecatedPath getDeprecatedPathIfOne(HttpServletRequest request) {
        for (var path : DeprecatedPath.values()) {
            if (path.matches(request)) {
                return path;
            }
        }
        return null;
    }

    protected static String getDeprecatedSince(DeprecatedPath path) {
        try {
            return DeprecatedPath.class.getDeclaredField(path.name()).getDeclaredAnnotation(Deprecated.class).since();
        } catch (NoSuchFieldException e) {
            throw new NuxeoException("Unexpected error occurred", e);
        }
    }

    protected static String getRequestedPath(HttpServletRequest request) {
        return request.getServletPath() + request.getPathInfo();
    }

    /**
     * The deprecate paths are not meant to be removed as they exist to preserve the REST API backward compatibility.
     */
    @SuppressWarnings({ "DeprecatedIsStillUsed", "ProtectedMemberInFinalClass" })
    public enum DeprecatedPath {

        @Deprecated(since = "10.3")
        SEARCH_LANG_EXECUTE("GET", "/api/v1/search/lang/(?:(?!\\/).)+/execute", "/api/v1/search/execute"),

        @Deprecated(since = "10.3")
        SEARCH_LANG_BULK("POST", "/api/v1/search/lang/(?:(?!\\/).)+/bulk/(.+)", "/api/v1/search/bulk/$1"),

        @Deprecated(since = "10.2")
        OAUTH2_TOKEN(Set.of("GET", "PUT", "DELETE"), "/api/v1/oauth2/token/((?:(?!\\/).)+)/((?:(?!\\/).)+)",
                "/api/v1/oauth2/token/provider/$1/user/$2"),

        @Deprecated(since = "2025.0")
        MANAGEMENT_ELASTICSEARCH(Set.of("GET", "POST"), "/api/v1/management/elasticsearch/((?:.+))",
                "/api/v1/management/search/$1");

        private final Set<String> methods;

        private final Pattern original;

        private final String replacement;

        DeprecatedPath(String method, String original, String replacement) {
            this(Set.of(method), original, replacement);
        }

        DeprecatedPath(Set<String> methods, String original, String replacement) {
            this.methods = methods;
            this.original = Pattern.compile(original, Pattern.CASE_INSENSITIVE);
            this.replacement = replacement;
        }

        public boolean matches(HttpServletRequest request) {
            return matches(request.getMethod(), getRequestedPath(request));
        }

        // exists for unit tests
        protected boolean matches(String method, String path) {
            return methods.contains(method) && original.matcher(path).matches();
        }

        public String getReplacement(HttpServletRequest request) {
            return getReplacement(getRequestedPath(request));
        }

        // exists for unit tests
        protected String getReplacement(String path) {
            return original.matcher(path).replaceAll(replacement);
        }
    }
}
