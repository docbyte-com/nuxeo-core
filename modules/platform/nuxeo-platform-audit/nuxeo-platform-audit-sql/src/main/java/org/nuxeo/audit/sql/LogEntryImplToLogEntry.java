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
package org.nuxeo.audit.sql;

import java.util.function.Function;

import org.nuxeo.audit.api.LogEntry;

/**
 * @since 2025.0
 */
public class LogEntryImplToLogEntry implements Function<org.nuxeo.ecm.platform.audit.api.LogEntry, LogEntry> {

    @Override
    public LogEntry apply(org.nuxeo.ecm.platform.audit.api.LogEntry logEntry) {
        if (logEntry == null) {
            return null;
        }
        return LogEntry.builder(logEntry.getEventId(), logEntry.getEventDate())
                       .id(logEntry.getId())
                       .principalName(logEntry.getPrincipalName())
                       .logDate(logEntry.getLogDate())
                       .docUUID(logEntry.getDocUUID())
                       .docPath(logEntry.getDocPath())
                       .docType(logEntry.getDocType())
                       .category(logEntry.getCategory())
                       .comment(logEntry.getComment())
                       .docLifeCycle(logEntry.getDocLifeCycle())
                       .repositoryId(logEntry.getRepositoryId())
                       .extended(logEntry.getExtended())
                       .build();
    }
}
