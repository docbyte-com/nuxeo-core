/*
 * (C) Copyright 2018-2024 Nuxeo (http://nuxeo.com/) and others.
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
 *     Gethin James
 */
package org.nuxeo.audit.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_CATEGORY;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_DOC_PATH;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_DOC_UUID;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_EVENT_DATE;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_EVENT_ID;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_REPOSITORY_ID;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import jakarta.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.audit.api.AuditQueryBuilder;
import org.nuxeo.audit.api.LogEntry;
import org.nuxeo.audit.service.AuditBackend;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.query.sql.model.Predicates;
import org.nuxeo.ecm.core.query.sql.model.QueryBuilder;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * Audit tests that are common across all storage backends.
 * 
 * @since 2025.0
 */
@RunWith(FeaturesRunner.class)
@Features(AuditFeature.class)
public class TestAuditBackend {

    public static final String ID_FOR_AUDIT_STORAGE_TESTS = "ID_FOR_AUDIT_STORAGE_TESTS";

    public static final int NUM_OF_EVENTS = 56;

    @Inject
    protected AuditBackend backend;

    @Inject
    protected CoreSession session;

    @Inject
    protected AuditFeature auditFeature;

    @Inject
    protected TransactionalFeature transactionalFeature;

    @Test
    public void shouldLogInAudit() {
        // generate events
        DocumentModel doc = session.createDocumentModel("/", "a-file", "File");
        doc.setPropertyValue("dc:title", "A File");
        doc = session.createDocument(doc);

        transactionalFeature.nextTransaction();

        doc.setPropertyValue("dc:title", "A modified File");
        doc = session.saveDocument(doc);

        transactionalFeature.nextTransaction();

        // test audit trail
        List<LogEntry> trail = backend.queryLogs(
                new AuditQueryBuilder().predicate(Predicates.eq(LOG_DOC_UUID, doc.getId()))
                                       .and(Predicates.eq(LOG_REPOSITORY_ID, session.getRepositoryName()))
                                       .defaultOrder());

        assertNotNull(trail);
        assertEquals(2, trail.size());

        LogEntry entry = trail.get(0);
        // the hibernate sequencer is not reset so the assertion will fail if the test is not the first one to run
        if (!auditFeature.isBackendSql()) {
            assertEquals(2L, entry.getId());
        }
        assertEquals("documentModified", entry.getEventId());
        assertEquals("eventDocumentCategory", entry.getCategory());
        assertEquals("A modified File", entry.getExtendedValue("title"));

        entry = trail.get(1);
        if (!auditFeature.isBackendSql()) {
            assertEquals(1L, entry.getId());
        }
        assertEquals("documentCreated", entry.getEventId());
        assertEquals("eventDocumentCategory", entry.getCategory());
        assertEquals("A File", entry.getExtendedValue("title"));

        LogEntry entryById = backend.getLogEntryByID(entry.getId());
        assertEquals(entry.getId(), entryById.getId());

        entryById = backend.getLogEntryByID(123L);
        assertNull(entryById);

        assertEquals(1L, backend.getEventsCount("documentModified").longValue());
    }

    @Test
    public void shouldSupportMultiCriteriaQueries() {
        auditFeature.generateLogEntries("mydoc", "evt", "cat", 9);

        // simple Query
        List<LogEntry> res = backend.queryLogs(
                new AuditQueryBuilder().predicate(Predicates.in(LOG_EVENT_ID, "evt1", "evt2")));
        assertNotNull(res);
        assertEquals(2, res.size());

        res = backend.queryLogs(new AuditQueryBuilder().predicate(Predicates.in(LOG_EVENT_ID, "evt1")));
        assertEquals(1, res.size());

        res = backend.queryLogs(new AuditQueryBuilder().predicate(Predicates.in(LOG_EVENT_ID, "evt")));
        assertEquals(0, res.size());

        // multi Query
        res = backend.queryLogs(new AuditQueryBuilder().predicate(Predicates.in(LOG_EVENT_ID, "evt1", "evt2"))
                                                       .and(Predicates.in(LOG_CATEGORY, "cat1")));
        assertEquals(1, res.size());

        res = backend.queryLogs(new AuditQueryBuilder().predicate(Predicates.in(LOG_EVENT_ID, "evt1", "evt2"))
                                                       .and(Predicates.in(LOG_CATEGORY, "cat1", "cat0")));
        assertEquals(2, res.size());

        // test page size
        res = backend.queryLogs(
                new AuditQueryBuilder().predicate(Predicates.eq(LOG_DOC_PATH, "/mydoc")).offset(0).limit(5));
        assertEquals(5, res.size());

        res = backend.queryLogs(
                new AuditQueryBuilder().predicate(Predicates.eq(LOG_DOC_PATH, "/mydoc")).offset(5).limit(5));
        assertEquals(4, res.size());

    }

    @Test
    public void testGetLatestLogId() {
        auditFeature.generateLogEntries("mydoc", "documentModified", "cat", 1);
        long id1 = backend.getLatestLogId("test", "documentModified0");
        assertTrue("id: " + id1, id1 > 0);

        auditFeature.generateLogEntries("mydoc", "documentCreated", "cat", 1);
        long id2 = backend.getLatestLogId("test", "documentModified0", "documentCreated0");
        assertTrue("id2: " + id2, id2 > 0);
        assertTrue(id2 > id1);

        long id = backend.getLatestLogId("test", "documentModified0");
        assertEquals(id1, id);
        id = backend.getLatestLogId("test", "unknown");
        assertEquals(0, id);
    }

    @Test
    @SuppressWarnings("removal")
    @Deprecated(since = "2025.0", forRemoval = true)
    public void testGetLogEntriesAfter() {
        auditFeature.generateLogEntries("mydoc", "documentModified", "cat", 1);
        long id1 = backend.getLatestLogId("test", "documentModified0");

        auditFeature.generateLogEntries("mydoc", "documentModified", "cat", 1);
        long id2 = backend.getLatestLogId("test", "documentModified0");
        assertTrue(id2 > id1);

        auditFeature.generateLogEntries("mydoc", "documentModified", "cat", 1);
        long id3 = backend.getLatestLogId("test", "documentModified0");
        assertTrue(id3 > id2);

        auditFeature.generateLogEntries("mydoc", "documentModified", "cat", 1);
        long id4 = backend.getLatestLogId("test", "documentModified0");
        assertTrue(id4 > id3);

        List<LogEntry> entries = backend.getLogEntriesAfter(id1, 5, "test", "documentCreated0", "documentModified0");
        assertEquals(4, entries.size());
        assertEquals(id1, entries.get(0).getId());

        entries = backend.getLogEntriesAfter(id2, 2, "test", "documentCreated0", "documentModified0");
        assertEquals(2, entries.size());
        assertEquals(id2, entries.get(0).getId());
        assertEquals(id3, entries.get(1).getId());
    }

    @Test
    public void testStartsWith() {
        auditFeature.generateLogEntries(NUM_OF_EVENTS,
                i -> LogEntry.builder(ID_FOR_AUDIT_STORAGE_TESTS, new Date())
                             .docPath(i % 2 == 0 ? "/is/even" : "/is/odd")
                             .build());
        QueryBuilder builder = new AuditQueryBuilder().predicate(
                Predicates.eq(LOG_EVENT_ID, ID_FOR_AUDIT_STORAGE_TESTS));
        List<LogEntry> logs = backend.queryLogs(builder);
        assertEquals("Incorrect log entries: " + logs, NUM_OF_EVENTS, logs.size());

        assertStartsWithCount(NUM_OF_EVENTS, "/");
        assertStartsWithCount(NUM_OF_EVENTS, "/is");
        assertStartsWithCount(NUM_OF_EVENTS, "/is/");
        assertStartsWithCount(NUM_OF_EVENTS / 2, "/is/even");
        assertStartsWithCount(NUM_OF_EVENTS / 2, "/is/odd");

        // A partial match is supported by the sql and mongodb implementations, but not ES
        if (auditFeature.isBackendOpenSearch()) {
            assertStartsWithCount(0, "/is/eve");
            assertStartsWithCount(0, "/is/od");
        } else {
            assertStartsWithCount(NUM_OF_EVENTS / 2, "/is/eve");
            assertStartsWithCount(NUM_OF_EVENTS / 2, "/is/od");
        }
    }

    /**
     * Asserts the number of events that match the startsWith parameter
     */
    public void assertStartsWithCount(int eventsCount, String startsWith) {
        List<LogEntry> logs = backend.queryLogs(
                new AuditQueryBuilder().predicate(Predicates.eq(LOG_EVENT_ID, ID_FOR_AUDIT_STORAGE_TESTS))
                                       .and(Predicates.startsWith(LOG_DOC_PATH, startsWith)));
        assertEquals(eventsCount, logs.size());
    }

    @Test
    public void testIn() {
        auditFeature.generateLogEntries(NUM_OF_EVENTS,
                i -> LogEntry.builder(ID_FOR_AUDIT_STORAGE_TESTS, new Date())
                             .docPath(i % 2 == 0 ? "/is/even" : "/is/odd")
                             .build());
        var query = new AuditQueryBuilder().predicate(
                Predicates.in(LOG_EVENT_ID, List.of(ID_FOR_AUDIT_STORAGE_TESTS, "no-such-event-id")));
        List<LogEntry> list = backend.queryLogs(query);
        assertEquals(NUM_OF_EVENTS, list.size());
    }

    @Test
    public void testQueryWithDate() {
        // check that a query with a date doesn't raise any error
        QueryBuilder dateBuilder = new AuditQueryBuilder();
        dateBuilder.predicate(Predicates.gte(LOG_EVENT_DATE, Calendar.getInstance()));
        List<LogEntry> logs = backend.queryLogs(dateBuilder);
        assertTrue(logs.isEmpty());

        // test LT predicate too
        dateBuilder = new AuditQueryBuilder();
        dateBuilder.predicate(Predicates.lt(LOG_EVENT_DATE, Calendar.getInstance()));
        backend.queryLogs(dateBuilder);
    }

    @Test
    public void testReferenceUsedTwice() {
        // eventId = "something AND eventId != 'other'
        // won't match if parameters are incorrectly mapped to the same name
        auditFeature.generateLogEntries(NUM_OF_EVENTS,
                i -> LogEntry.builder(ID_FOR_AUDIT_STORAGE_TESTS, new Date())
                             .docPath(i % 2 == 0 ? "/is/even" : "/is/odd")
                             .build());
        QueryBuilder query = new AuditQueryBuilder().predicate(Predicates.eq(LOG_EVENT_ID, ID_FOR_AUDIT_STORAGE_TESTS))
                                                    .and(Predicates.noteq(LOG_EVENT_ID, "no-such-event-id"));
        List<LogEntry> logs = backend.queryLogs(query);
        assertEquals(NUM_OF_EVENTS, logs.size());
    }
}
