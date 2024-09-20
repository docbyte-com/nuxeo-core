/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.audit.api;

import static org.nuxeo.audit.api.LogEntryConstants.LOG_EVENT_ID;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_ID;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_CATEGORY;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_DOC_PATH;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_DOC_UUID;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_EVENT_DATE;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_REPOSITORY_ID;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.nuxeo.audit.api.AuditQueryBuilder;
import org.nuxeo.audit.api.LogEntryConstants;
import org.nuxeo.audit.api.query.AuditQueryException;
import org.nuxeo.audit.api.query.DateRangeParser;
import org.nuxeo.audit.api.query.DateRangeQueryConstants;
import org.nuxeo.ecm.core.query.sql.model.OrderByExprs;
import org.nuxeo.ecm.core.query.sql.model.Predicates;
import org.nuxeo.ecm.core.query.sql.model.QueryBuilder;

/**
 * Interface for reading data from the Audit service.
 *
 * @author tiry
 * @param <L> to give the log entry type for the new {@link org.nuxeo.audit.service.AuditBackend} interface that defines
 *            a new entry type.
 * @deprecated since 2025.0, use {@link org.nuxeo.audit.service.AuditBackend} instead
 */
@SuppressWarnings("removal")
@Deprecated(since = "2025.0", forRemoval = true)
public interface AuditReader<L extends LogEntry> {

    /**
     * Returns the logs given a doc uuid and a repository id.
     *
     * @param uuid the document uuid
     * @param repositoryId the repository id
     * @return a list of log entries
     * @since 8.4
     */
    default List<L> getLogEntriesFor(String uuid, String repositoryId) {
        return queryLogs(new AuditQueryBuilder().predicate(Predicates.eq(LOG_DOC_UUID, uuid))
                                                .and(Predicates.eq(LOG_REPOSITORY_ID, repositoryId))
                                                .defaultOrder());
    }

    /**
     * Returns a given log entry given its id.
     *
     * @param id the log entry identifier
     * @return a LogEntry instance
     */
    L getLogEntryByID(long id);

    /**
     * Returns the logs given a collection of predicates and a default sort.
     *
     * @param builder the query builder to fetch log entries
     * @return a list of log entries
     * @since 9.3
     */
    List<L> queryLogs(QueryBuilder builder);

    /**
     * Returns the list of log entries.
     * <p>
     * Note we will use NXQL in the future when the search engine will index history.
     *
     * @see DateRangeQueryConstants
     * @param eventIds the event ids.
     * @param dateRange a preset date range.
     * @return a list of log entries.
     * @deprecated since 2025.0, use {@link #queryLogs(QueryBuilder)} instead
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    default List<L> queryLogs(String[] eventIds, String dateRange) {
        return queryLogsByPage(eventIds, (String) null, (String[]) null, null, 0, 10000);
    }

    /**
     * Returns the batched list of log entries.
     * <p>
     * Note we will use NXQL in the future when the search engine will index history.
     *
     * @see DateRangeQueryConstants
     * @param eventIds the event ids.
     * @param dateRange a preset date range.
     * @param category add filter on events category
     * @param path add filter on document path
     * @param pageNb page number (ignore if &lt;=1)
     * @param pageSize number of results per page
     * @return a list of log entries.
     * @deprecated since 2025.0, use {@link #queryLogs(QueryBuilder)} instead
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    default List<L> queryLogsByPage(String[] eventIds, String dateRange, String category, String path, int pageNb,
            int pageSize) {
        return queryLogsByPage(eventIds, dateRange, new String[] { category }, path, pageNb, pageSize);
    }

    /**
     * @deprecated since 2025.0, use {@link #queryLogs(QueryBuilder)} instead
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    default List<L> queryLogsByPage(String[] eventIds, String dateRange, String[] categories, String path, int pageNb,
            int pageSize) {

        Date limit = null;
        if (dateRange != null) {
            try {
                limit = DateRangeParser.parseDateRangeQuery(new Date(), dateRange);
            } catch (AuditQueryException aqe) {
                aqe.addInfo("Wrong date range query. Query was " + dateRange);
                throw aqe;
            }
        }
        return queryLogsByPage(eventIds, limit, categories, path, pageNb, pageSize);
    }

    /**
     * Returns the batched list of log entries.
     * <p>
     * Note we will use NXQL in the future when the search engine will index history.
     *
     * @see DateRangeQueryConstants
     * @param eventIds the event ids.
     * @param limit filter events by date from limit to now
     * @param category add filter on events category
     * @param path add filter on document path
     * @param pageNb page number (ignore if &lt;=1)
     * @param pageSize number of results per page
     * @return a list of log entries.
     * @deprecated since 2025.0, use {@link #queryLogs(QueryBuilder)} instead
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    default List<L> queryLogsByPage(String[] eventIds, Date limit, String category, String path, int pageNb,
            int pageSize) {
        return queryLogsByPage(eventIds, limit, new String[] { category }, path, pageNb, pageSize);
    }

    /**
     * @deprecated since 2025.0, use {@link #queryLogs(QueryBuilder)} instead
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    default List<L> queryLogsByPage(String[] eventIds, Date limit, String[] categories, String path, int pageNb,
            int pageSize) {
        QueryBuilder builder = new AuditQueryBuilder();
        if (ArrayUtils.isNotEmpty(eventIds)) {
            if (eventIds.length == 1) {
                builder.predicate(Predicates.eq(BuiltinLogEntryData.LOG_EVENT_ID, eventIds[0]));
            } else {
                builder.predicate(Predicates.in(BuiltinLogEntryData.LOG_EVENT_ID, eventIds[0]));
            }
        }
        if (ArrayUtils.isNotEmpty(categories)) {
            if (categories.length == 1) {
                builder.predicate(Predicates.eq(LOG_CATEGORY, categories[0]));
            } else {
                builder.predicate(Predicates.in(LOG_CATEGORY, categories[0]));
            }
        }
        if (path != null) {
            builder.predicate(Predicates.eq(LOG_DOC_PATH, path));
        }
        if (limit != null) {
            builder.predicate(Predicates.lt(LOG_EVENT_DATE, limit));
        }
        builder.offset(pageNb * pageSize).limit(pageSize);
        return queryLogs(builder);
    }

    /**
     * Returns a batched list of log entries. WhereClause is a native where clause for the backend: here EJBQL 3.0 must
     * be used if implementation of audit backend is JPA (&lt; 7.3 or audit.elasticsearch.enabled=false) and JSON if
     * implementation is Elasticsearch.
     */
    @SuppressWarnings("unchecked")
    default List<L> nativeQueryLogs(String whereClause, int pageNb, int pageSize) {
        return (List<L>) nativeQuery(whereClause, pageNb, pageSize).stream()
                                                                   .filter(LogEntry.class::isInstance)
                                                                   .map(LogEntry.class::cast)
                                                                   .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Returns a batched list of entries. query string is a native query clause for the backend : here EJBQL 3.0 must be
     * used if implementation of audit backend is JPA (&lt; 7.3 or audit.elasticsearch.enabled=false) and JSON if
     * implementation is Elasticsearch.
     */
    default List<?> nativeQuery(String query, int pageNb, int pageSize) {
        return nativeQuery(query, Map.of(), pageNb, pageSize);
    }

    /**
     * Returns a batched list of entries.
     *
     * @param query a JPA query language query if implementation of audit backend is JPA (&lt; 7.3 or
     *            audit.elasticsearch.enabled=false) and JSON if implementation is Elasticsearch
     * @param params parameters for the query
     * @param pageNb the page number (starts at 1)
     * @param pageSize the number of results per page
     */
    List<?> nativeQuery(String query, Map<String, Object> params, int pageNb, int pageSize);

    /**
     * Returns the latest log id matching events and repository or 0 when no match found.
     *
     * @since 9.3
     */
    default long getLatestLogId(String repositoryId, String... eventIds) {
        QueryBuilder builder = new AuditQueryBuilder().predicate(Predicates.eq(LOG_REPOSITORY_ID, repositoryId))
                                                      .and(Predicates.in(BuiltinLogEntryData.LOG_EVENT_ID, eventIds))
                                                      .order(OrderByExprs.desc(BuiltinLogEntryData.LOG_ID))
                                                      .limit(1);
        return queryLogs(builder).stream().mapToLong(LogEntry::getId).findFirst().orElse(0L);
    }

    /**
     * Returns up to limit log entries matching events and repository with log id greater or equal to logIdOffset.
     *
     * @since 9.3
     */
    default List<L> getLogEntriesAfter(long logIdOffset, int limit, String repositoryId, String... eventIds) {
        QueryBuilder builder = new AuditQueryBuilder().predicate(
                Predicates.eq(LogEntryConstants.LOG_REPOSITORY_ID, repositoryId))
                                                      .and(Predicates.in(LOG_EVENT_ID, eventIds))
                                                      .and(Predicates.gte(LOG_ID, logIdOffset))
                                                      .order(OrderByExprs.asc(LOG_ID))
                                                      .limit(limit);
        return queryLogs(builder);
    }

}
