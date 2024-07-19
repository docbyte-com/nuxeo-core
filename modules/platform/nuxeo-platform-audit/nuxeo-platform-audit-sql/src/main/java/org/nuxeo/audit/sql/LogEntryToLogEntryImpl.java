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

import java.io.Serializable;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.nuxeo.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.impl.ExtendedInfoImpl;
import org.nuxeo.ecm.platform.audit.impl.LogEntryImpl;

/**
 * @since 2025.0
 */
@SuppressWarnings("removal")
public class LogEntryToLogEntryImpl implements Function<LogEntry, org.nuxeo.ecm.platform.audit.api.LogEntry> {

    @Override
    public org.nuxeo.ecm.platform.audit.api.LogEntry apply(LogEntry logEntry) {
        if (logEntry == null) {
            return null;
        }
        var logEntrySQL = new LogEntryImpl();
        logEntrySQL.setId(logEntry.getId());
        logEntrySQL.setPrincipalName(logEntry.getPrincipalName());
        logEntrySQL.setEventId(logEntry.getEventId());
        logEntrySQL.setLogDate(logEntry.getLogDate());
        logEntrySQL.setEventDate(logEntry.getEventDate());
        logEntrySQL.setDocUUID(logEntry.getDocUUID());
        logEntrySQL.setDocPath(logEntry.getDocPath());
        logEntrySQL.setDocType(logEntry.getDocType());
        logEntrySQL.setCategory(logEntry.getCategory());
        logEntrySQL.setComment(logEntry.getComment());
        logEntrySQL.setDocLifeCycle(logEntry.getDocLifeCycle());
        logEntrySQL.setRepositoryId(logEntry.getRepositoryId());
        logEntrySQL.setExtendedInfos(
                logEntry.getExtended()
                        .entrySet()
                        .stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                e -> ExtendedInfoImpl.createExtendedInfo((Serializable) e.getValue()))));
        return logEntrySQL;
    }
}
