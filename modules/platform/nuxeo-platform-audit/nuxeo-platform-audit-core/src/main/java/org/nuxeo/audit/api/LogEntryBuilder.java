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
package org.nuxeo.audit.api;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 * @since 2025.0
 */
public class LogEntryBuilder {

    protected final String eventId;

    protected final Date eventDate;

    protected final Map<String, Object> extended;

    protected Long id;

    protected String principalName;

    protected Date logDate;

    protected String docUUID;

    protected String docPath;

    protected String docType;

    protected String category;

    protected String comment;

    protected String docLifeCycle;

    protected String repositoryId;

    private LogEntryBuilder(String eventId, Date eventDate, Map<String, Object> extendedInfos) {
        this.eventId = Objects.requireNonNull(eventId, "The event name must not be null");
        this.eventDate = Objects.requireNonNull(eventDate, "The event date must not be null");
        this.extended = new HashMap<>(Objects.requireNonNullElse(extendedInfos, Map.of()));
    }

    protected LogEntryBuilder(String eventId, Date eventDate) {
        this(eventId, eventDate, Map.of());
    }

    public LogEntryBuilder(LogEntry logEntry) {
        this(logEntry.getEventId(), logEntry.getEventDate(), logEntry.getExtended());
        this.id = logEntry.getId();
        this.principalName = logEntry.getPrincipalName();
        this.logDate = logEntry.getLogDate();
        this.docUUID = logEntry.getDocUUID();
        this.docPath = logEntry.getDocPath();
        this.docType = logEntry.getDocType();
        this.category = logEntry.getCategory();
        this.comment = logEntry.getComment();
        this.docLifeCycle = logEntry.getDocLifeCycle();
        this.repositoryId = logEntry.getRepositoryId();
    }

    public String eventId() {
        return eventId;
    }

    /**
     * Sets the {@link LogEntry#getId() id}.
     * <p>
     * CAUTION: The value won't be taken into account when storing the log entry. The method exists for technical needs
     * such as un-marshalling.
     */
    public LogEntryBuilder id(Long id) {
        this.id = id;
        return this;
    }

    public LogEntryBuilder principalName(NuxeoPrincipal principal) {
        return principalName(principal.getActingUser());
    }

    public LogEntryBuilder principalName(String principalName) {
        this.principalName = principalName;
        return this;
    }

    /**
     * Sets the {@link LogEntry#getLogDate() log date}.
     * <p>
     * CAUTION: The value won't be taken into account when storing the log entry. The method exists for technical needs
     * such as un-marshalling.
     */
    public LogEntryBuilder logDate(Date logDate) {
        this.logDate = logDate;
        return this;
    }

    public LogEntryBuilder docUUID(DocumentRef documentRef) {
        return docUUID(switch (documentRef.type()) {
            case DocumentRef.ID -> (String) documentRef.reference();
            case DocumentRef.INSTANCE -> ((DocumentModel) documentRef.reference()).getId();
            default -> throw new IllegalArgumentException("not an id reference " + documentRef);
        });
    }

    public LogEntryBuilder docUUID(String docUUID) {
        this.docUUID = docUUID;
        return this;
    }

    public LogEntryBuilder docPath(String docPath) {
        this.docPath = docPath;
        return this;
    }

    public LogEntryBuilder docType(String docType) {
        this.docType = docType;
        return this;
    }

    public LogEntryBuilder category(String category) {
        this.category = category;
        return this;
    }

    public LogEntryBuilder comment(String comment) {
        this.comment = comment;
        return this;
    }

    public LogEntryBuilder docLifeCycle(String docLifeCycle) {
        this.docLifeCycle = docLifeCycle;
        return this;
    }

    public LogEntryBuilder repositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
        return this;
    }

    public LogEntryBuilder extended(Map<String, ?> extended) {
        this.extended.putAll(Objects.requireNonNullElse(extended, Map.of()));
        return this;
    }

    public LogEntryBuilder extended(String key, Object value) {
        extended.put(key, value);
        return this;
    }

    public LogEntry build() {
        return new LogEntryImpl(this);
    }

    protected static class LogEntryImpl implements LogEntry {

        protected final Long id;

        protected final String principalName;

        protected final String eventId;

        protected final Date eventDate;

        protected final Date logDate;

        protected final String docUUID;

        protected final String docPath;

        protected final String docType;

        protected final String category;

        protected final String comment;

        protected final String docLifeCycle;

        protected final String repositoryId;

        protected final Map<String, Object> extended;

        protected LogEntryImpl(LogEntryBuilder builder) {
            this.id = builder.id;
            this.principalName = builder.principalName;
            this.eventId = builder.eventId;
            this.eventDate = builder.eventDate;
            this.logDate = builder.logDate;
            this.docUUID = builder.docUUID;
            this.docPath = builder.docPath;
            this.docType = builder.docType;
            this.category = builder.category;
            this.comment = builder.comment;
            this.docLifeCycle = builder.docLifeCycle;
            this.repositoryId = builder.repositoryId;
            this.extended = Collections.unmodifiableMap(builder.extended);
        }

        @Override
        public long getId() {
            return Objects.requireNonNullElse(id, 0L);
        }

        @Override
        public String getPrincipalName() {
            return principalName;
        }

        @Override
        public String getEventId() {
            return eventId;
        }

        @Override
        public Date getEventDate() {
            return eventDate;
        }

        @Override
        public Date getLogDate() {
            return logDate;
        }

        @Override
        public String getDocUUID() {
            return docUUID;
        }

        @Override
        public String getDocPath() {
            return docPath;
        }

        @Override
        public String getDocType() {
            return docType;
        }

        @Override
        public String getCategory() {
            return category;
        }

        @Override
        public String getComment() {
            return comment;
        }

        @Override
        public String getDocLifeCycle() {
            return docLifeCycle;
        }

        @Override
        public String getRepositoryId() {
            return repositoryId;
        }

        @Override
        public Map<String, Object> getExtended() {
            return extended;
        }

        @Override
        public int hashCode() {
            return HashCodeBuilder.reflectionHashCode(this);
        }

        @Override
        public boolean equals(Object obj) {
            return EqualsBuilder.reflectionEquals(this, obj, false);
        }

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }
    }
}
