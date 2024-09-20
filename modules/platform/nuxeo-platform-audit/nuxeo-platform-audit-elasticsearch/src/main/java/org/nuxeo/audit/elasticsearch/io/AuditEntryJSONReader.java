/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.audit.elasticsearch.io;

import java.io.IOException;

import org.nuxeo.audit.api.LogEntry;
import org.nuxeo.ecm.core.io.registry.MarshallerHelper;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;

/**
 * @deprecated since 2025.0, use nuxeo-core-io instead.
 */
@Deprecated(since = "2025.0", forRemoval = true)
public class AuditEntryJSONReader {

    /**
     * @deprecated since 2025.0, use {@link MarshallerHelper#jsonToObject(Class, String, RenderingContext)} instead
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    public static LogEntry read(String content) throws IOException {
        return MarshallerHelper.jsonToObject(LogEntry.class, content, RenderingContext.CtxBuilder.get());
    }
}
