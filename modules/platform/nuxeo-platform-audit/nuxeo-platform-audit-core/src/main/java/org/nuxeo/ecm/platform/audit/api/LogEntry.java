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
 *     Julien Anguenot
 *     Thierry Delprat
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.audit.api;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import org.nuxeo.audit.api.AuditPageProvider;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.audit.api.comment.UIAuditComment;

/**
 * Log entry.
 * 
 * @deprecated since 2025.0, use {@link org.nuxeo.audit.api.LogEntry} instead
 */
@Deprecated(since = "2025.0", forRemoval = true)
public interface LogEntry extends Serializable {

    /**
     * @return the log identifier
     */
    long getId();

    /**
     * @deprecated since 2025.0, {@link LogEntry} built by Nuxeo Platform are now immutable
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    default void setId(long id) {
        throw new UnsupportedOperationException("LogEntry are immutable");
    }

    /**
     * Returns the name of the principal who originated the log entry.
     *
     * @return the name of the principal who originated the log entry
     */
    String getPrincipalName();

    /**
     * @deprecated since 2025.0, {@link LogEntry} built by Nuxeo Platform are now immutable
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    default void setPrincipalName(String principalName) {
        throw new UnsupportedOperationException("LogEntry are immutable");
    }

    /**
     * Returns the identifier of the event that originated the log entry.
     *
     * @return the identifier of the event that originated the log entry
     */
    String getEventId();

    /**
     * @deprecated since 2025.0, {@link LogEntry} built by Nuxeo Platform are now immutable
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    default void setEventId(String eventId) {
        throw new UnsupportedOperationException("LogEntry are immutable");
    }

    /**
     * @return the date of the log insertion: this up to max transaction timeout later than eventDate. This date is
     *         useful for services such as Nuxeo Drive that need fine grained incremental near-monotonic access to the
     *         audit log.
     * @since 5.7
     * @since 5.6-HF16
     */
    Date getLogDate();

    /**
     * @deprecated since 2025.0, {@link LogEntry} built by Nuxeo Platform are now immutable
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    default void setLogDate(Date logDate) {
        throw new UnsupportedOperationException("LogEntry are immutable");
    }

    /**
     * Returns the date of the event that originated the log entry.
     *
     * @return the date of the event that originated the log entry
     */
    Date getEventDate();

    /**
     * @deprecated since 2025.0, {@link LogEntry} built by Nuxeo Platform are now immutable
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    default void setEventDate(Date eventDate) {
        throw new UnsupportedOperationException("LogEntry are immutable");
    }

    /**
     * Returns the doc UUID related to the log entry.
     * <p>
     * It might be null if the event that originated the event is noe bound to any document.
     *
     * @return the doc UUID related to the log entry.
     */
    String getDocUUID();

    /**
     * @deprecated since 2025.0, {@link LogEntry} built by Nuxeo Platform are now immutable
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    default void setDocUUID(String docUUID) {
        throw new UnsupportedOperationException("LogEntry are immutable");
    }

    /**
     * @deprecated since 2025.0, {@link LogEntry} built by Nuxeo Platform are now immutable
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    default void setDocUUID(DocumentRef docRef) {
        switch (docRef.type()) {
            case DocumentRef.ID -> setDocUUID((String) docRef.reference());
            case DocumentRef.INSTANCE -> setDocUUID(((DocumentModel) docRef.reference()).getId());
            default -> throw new IllegalArgumentException("not an id reference " + docRef);
        }
    }

    /**
     * Returns the doc path related to the log entry.
     * <p>
     * It might be null if the event that originated the event is noe bound to any document.
     *
     * @return the doc path related to the log entry.
     */
    String getDocPath();

    /**
     * @deprecated since 2025.0, {@link LogEntry} built by Nuxeo Platform are now immutable
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    default void setDocPath(String docPath) {
        throw new UnsupportedOperationException("LogEntry are immutable");
    }

    /**
     * Returns the doc type related to the log entry.
     * <p>
     * It might be null if the event that originated the event is not bound to any document.
     *
     * @return the doc type related to the log entry.
     */
    String getDocType();

    /**
     * @deprecated since 2025.0, {@link LogEntry} built by Nuxeo Platform are now immutable
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    default void setDocType(String docType) {
        throw new UnsupportedOperationException("LogEntry are immutable");
    }

    /**
     * Returns the category for this log entry.
     * <p>
     * This is defined at client level. Categories are not restricted in any ways.
     *
     * @return the category for this log entry.
     */
    String getCategory();

    /**
     * @deprecated since 2025.0, {@link LogEntry} built by Nuxeo Platform are now immutable
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    default void setCategory(String category) {
        throw new UnsupportedOperationException("LogEntry are immutable");
    }

    /**
     * Returns the associated comment for this log entry.
     *
     * @return the associated comment for this log entry
     */
    String getComment();

    /**
     * @deprecated since 2025.0, {@link LogEntry} built by Nuxeo Platform are now immutable
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    default void setComment(String comment) {
        throw new UnsupportedOperationException("LogEntry are immutable");
    }

    /**
     * Return the life cycle if the document related to the log entry.
     * <p>
     * It might be null if the event that originated the event is noe bound to any document.
     *
     * @return the life cycle if the document related to the log entry.
     */
    String getDocLifeCycle();

    /**
     * @deprecated since 2025.0, {@link LogEntry} built by Nuxeo Platform are now immutable
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    default void setDocLifeCycle(String docLifeCycle) {
        throw new UnsupportedOperationException("LogEntry are immutable");
    }

    /**
     * Returns the repository id related to the log entry.
     *
     * @return the repository id
     */
    String getRepositoryId();

    /**
     * @deprecated since 2025.0, {@link LogEntry} built by Nuxeo Platform are now immutable
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    default void setRepositoryId(String repositoryId) {
        throw new UnsupportedOperationException("LogEntry are immutable");
    }

    /**
     * Returns the extended information attached to this log entry.
     *
     * @since 2025.0
     */
    Map<String, Object> getExtended();

    /**
     * @since 2025.0
     */
    @SuppressWarnings("unchecked")
    default <R> R getExtendedValue(String key) {
        return (R) getExtended().get(key);
    }

    /**
     * @deprecated since 2025.0, {@link ExtendedInfo} doesn't need a Java type anymore, use {@link #getExtended} instead
     */
    @SuppressWarnings("removal")
    @Deprecated(since = "2025.0", forRemoval = true)
    default Map<String, ExtendedInfo> getExtendedInfos() {
        throw new UnsupportedOperationException("ExtendedInfo not supported, use #getExtended instead");
    }

    /**
     * @deprecated since 2025.0, {@link LogEntry} built by Nuxeo Platform are now immutable
     */
    @SuppressWarnings("removal")
    @Deprecated(since = "2025.0", forRemoval = true)
    default void setExtendedInfos(Map<String, ExtendedInfo> infos) {
        throw new UnsupportedOperationException("LogEntry are immutable");
    }

    /**
     * Returns the comment preprocessed to be ready for display (extract info about linked documents).
     *
     * @deprecated since 2025.0, unused
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    default UIAuditComment getPreprocessedComment() {
        throw new UnsupportedOperationException("Preprocessed comment not supported, no replacement");
    }

    /**
     * @deprecated since 2025.0, {@link LogEntry} built by Nuxeo Platform are now immutable
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    default void setPreprocessedComment(UIAuditComment uiComment) {
        throw new UnsupportedOperationException("LogEntry are immutable");
    }
}
