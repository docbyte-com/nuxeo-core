/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.automation.core.operations.services;

import java.io.IOException;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.platform.audit.api.AuditReader;
import org.nuxeo.ecm.platform.audit.api.LogEntry;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @deprecated since 2025.0, use {@link AuditPageProviderOperation} instead
 */
@Deprecated(since = "2025.0", forRemoval = true)
@Operation(id = AuditQuery.ID, category = Constants.CAT_SERVICES, label = "Query Audit Service", description = "Execute a JPA query against the Audit Service. This is returning a blob with the query result. The result is a serialized JSON array. You can use the context to set query variables but you must prefix using 'audit.query.' the context variable keys that match the ones in the query.", addToStudio = false)
public class AuditQuery {

    public static final String ID = "Audit.Query";

    @Context
    protected AuditReader audit;

    @Context
    protected OperationContext ctx;

    @Param(name = "query", required = true, widget = Constants.W_MULTILINE_TEXT)
    protected String query;

    @Param(name = "pageNo", required = false)
    protected int pageNo = 1;

    @Param(name = "maxResults", required = false)
    protected int maxResults;

    @OperationMethod
    public Blob run() throws IOException {
        List<LogEntry> result = query();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (LogEntry entry : result) {
            Map<String, Object> obj = new LinkedHashMap<>();
            obj.put("eventId", entry.getEventId());
            obj.put("category", entry.getCategory());
            obj.put("eventDate", entry.getEventDate().getTime());
            obj.put("principal", entry.getPrincipalName());
            obj.put("docUUID", entry.getDocUUID());
            obj.put("docType", entry.getDocType());
            obj.put("docPath", entry.getDocPath());
            obj.put("docLifeCycle", entry.getDocLifeCycle());
            obj.put("repoId", entry.getRepositoryId());
            obj.put("comment", entry.getComment());
            // Map<String, ExtendedInfo> info = entry.getExtendedInfos();
            // if (info != null) {
            // info.get
            // }
            rows.add(obj);
        }
        return Blobs.createJSONBlobFromValue(rows);
    }

    @SuppressWarnings("unchecked")
    public List<LogEntry> query() {
        int pageSize = maxResults > 0 ? maxResults : 1000; // 1000 is the default in LogEntryProvider
        var params = ctx.entrySet()
                        .stream()
                        .filter(entry -> entry.getKey().startsWith("audit.query."))
                        .collect(Collectors.toMap(entry -> entry.getKey().substring("audit.query.".length()),
                                entry -> mapValue(entry.getValue()), (a, b) -> b, LinkedHashMap::new));
        return audit.nativeQuery(query, params, pageNo, pageSize);
    }

    protected Object mapValue(Object value) {
        if (value instanceof String string) {
            if (string.startsWith("{d ") && string.endsWith("}")) {
                string = string.substring(3, string.length() - 1).trim();
                int i = string.indexOf(' ');
                if (i == -1) {
                    return Date.valueOf(string);
                } else {
                    return Timestamp.valueOf(string);
                }
            } else {
                return string;
            }
        } else {
            return value;
        }
    }
}
