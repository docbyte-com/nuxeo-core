/*
 * (C) Copyright 2016-2024 Nuxeo (http://nuxeo.com/) and others.
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
 *     "Guillaume Renard"
 */
package org.nuxeo.ecm.core.search;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.core.search.index.commands.ThreadLocalIndexingCommandsStacker;

/**
 * @since 8.3
 */
public class SearchFilter implements Filter {

    public static final String SEARCH_SYNC_FLAG = "nx-search-sync";

    protected static final String ES_SYNC_FLAG = "nx-es-sync";

    protected static final String ES_SYNC_FLAG_COMPAT = "nx_es_sync";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest httpRequest)) {
            chain.doFilter(request, response);
            return;
        }
        final boolean esSync = Boolean.parseBoolean(httpRequest.getHeader(SEARCH_SYNC_FLAG))
                // NXP-32506: keep compatibility with old header with es reference
                || Boolean.parseBoolean(httpRequest.getHeader(ES_SYNC_FLAG))
                // NXP-28075: keep compatibility with old header with underscores
                || Boolean.parseBoolean(httpRequest.getHeader(ES_SYNC_FLAG_COMPAT));
        if (!esSync) {
            chain.doFilter(request, response);
            return;
        }
        ThreadLocalIndexingCommandsStacker.useSyncIndexing.set(true);
        try {
            chain.doFilter(request, response);
        } finally {
            ThreadLocalIndexingCommandsStacker.useSyncIndexing.set(false);
        }
    }
}
