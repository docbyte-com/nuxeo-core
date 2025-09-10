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

import java.util.Date;
import java.util.Map;

/**
 * @since 2025.0
 */
@SuppressWarnings("removal")
public interface LogEntry extends org.nuxeo.ecm.platform.audit.api.LogEntry {

    /**
     * @return the log identifier
     */
    long getId();

    /**
     * Returns the name of the principal who originated the log entry.
     *
     * @return the name of the principal who originated the log entry
     */
    String getPrincipalName();

    /**
     * Returns the identifier of the event that originated the log entry.
     *
     * @return the identifier of the event that originated the log entry
     */
    String getEventId();

    /**
     * @return the date of the log insertion: this up to max transaction timeout later than eventDate. This date is
     *         useful for services such as Nuxeo Drive that need fine grained incremental near-monotonic access to the
     *         audit log.
     * @since 5.7
     * @since 5.6-HF16
     */
    Date getLogDate();

    /**
     * Returns the date of the event that originated the log entry.
     *
     * @return the date of the event that originated the log entry
     */
    Date getEventDate();

    /**
     * Returns the doc UUID related to the log entry.
     * <p>
     * It might be null if the event that originated the event is noe bound to any document.
     *
     * @return the doc UUID related to the log entry.
     */
    String getDocUUID();

    /**
     * Returns the doc path related to the log entry.
     * <p>
     * It might be null if the event that originated the event is noe bound to any document.
     *
     * @return the doc path related to the log entry.
     */
    String getDocPath();

    /**
     * Returns the doc type related to the log entry.
     * <p>
     * It might be null if the event that originated the event is not bound to any document.
     *
     * @return the doc type related to the log entry.
     */
    String getDocType();

    /**
     * Returns the category for this log entry.
     * <p>
     * This is defined at client level. Categories are not restricted in any ways.
     *
     * @return the category for this log entry.
     */
    String getCategory();

    /**
     * Returns the associated comment for this log entry.
     *
     * @return the associated comment for this log entry
     */
    String getComment();

    /**
     * Return the life cycle if the document related to the log entry.
     * <p>
     * It might be null if the event that originated the event is noe bound to any document.
     *
     * @return the life cycle if the document related to the log entry.
     */
    String getDocLifeCycle();

    /**
     * Returns the repository id related to the log entry.
     *
     * @return the repository id
     */
    String getRepositoryId();

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
     * @since 2025.0
     */
    default LogEntryBuilder builder() {
        return new LogEntryBuilder(this);
    }

    /**
     * Returns a {@link LogEntryBuilder} to instantiate a {@link org.nuxeo.ecm.platform.audit.api.LogEntry}.
     *
     * @since 2025.0
     */
    static LogEntryBuilder builder(String eventId, Date eventDate) {
        return new LogEntryBuilder(eventId, eventDate);
    }
}
