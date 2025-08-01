/*
 * (C) Copyright 2006-2024 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.audit.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_DOC_UUID;
import static org.nuxeo.audit.test.TestNXAuditEventsService.MyInit.YOUPS_PATH;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.AFTER_REMOVE_LEGAL_HOLD;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.AFTER_SET_LEGAL_HOLD;

import java.time.Duration;

import jakarta.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.audit.api.AuditQueryBuilder;
import org.nuxeo.audit.service.AuditBackend;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.query.sql.model.Predicates;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * Test the event conf service.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@SuppressWarnings("removal")
@RunWith(FeaturesRunner.class)
@Features({ AuditFeature.class, CoreFeature.class })
@RepositoryConfig(init = TestNXAuditEventsService.MyInit.class, cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.audit.test.test:OSGI-INF/test-compatibility-with-NXAuditEventsService-contrib.xml")
public class TestNXAuditEventsService {

    public static class MyInit extends DefaultRepositoryInit {

        public static final String YOUPS_PATH = "/youps";

        @Override
        public void populate(CoreSession session) {
            super.populate(session);
            DocumentModel rootDocument = session.getRootDocument();
            DocumentModel model = session.createDocumentModel(rootDocument.getPathAsString(), "youps", "File");
            model.setProperty("dublincore", "title", "huum");
            session.createDocument(model);
        }
    }

    @Inject
    protected AuditBackend backend;

    @Inject
    protected EventService eventService;

    @Inject
    protected CoreSession session;

    @Inject
    protected TransactionalFeature txFeature;

    public void waitForAsyncCompletion() {
        txFeature.nextTransaction(Duration.ofSeconds(20));
    }

    @Test
    public void testLogDocumentMessageWithoutCategory() {
        DocumentModel source = session.getDocument(new PathRef(YOUPS_PATH));

        EventContext ctx = new DocumentEventContext(session, session.getPrincipal(), source);
        Event event = ctx.newEvent("documentSecurityUpdated"); // auditable
        event.setInline(false);
        event.setImmediate(true);
        eventService.fireEvent(event);
        waitForAsyncCompletion();

        var entries = backend.queryLogs(
                new AuditQueryBuilder().predicate(Predicates.eq(LOG_DOC_UUID, source.getId())).defaultOrder());
        // documentSecurityUpdated & documentCreated
        assertEquals(2, entries.size());
        var entry = entries.getFirst();
        assertEquals("documentSecurityUpdated", entry.getEventId());
        assertEquals("eventDocumentCategory", entry.getCategory());
        assertNull(entry.getComment());
        assertEquals("project", entry.getDocLifeCycle());
        assertEquals("/youps", entry.getDocPath());
        assertEquals("File", entry.getDocType());
        assertEquals("Administrator", entry.getPrincipalName());
        assertEquals("test", entry.getRepositoryId());
    }

    @Test
    public void testLogDocumentMessageWithCategory() {
        DocumentModel source = session.getDocument(new PathRef(YOUPS_PATH));

        EventContext ctx = new DocumentEventContext(session, session.getPrincipal(), source);
        ctx.setProperty("category", "myCategory");
        Event event = ctx.newEvent("documentSecurityUpdated"); // auditable
        event.setInline(false);
        event.setImmediate(true);
        eventService.fireEvent(event);
        waitForAsyncCompletion();

        var entries = backend.getLogEntriesFor(source.getId(), source.getRepositoryName());
        // documentSecurityUpdated & documentCreated
        assertEquals(2, entries.size());
        var entry = entries.getFirst();
        assertEquals("documentSecurityUpdated", entry.getEventId());
        assertEquals("myCategory", entry.getCategory());
        assertNull(entry.getComment());
        assertEquals("project", entry.getDocLifeCycle());
        assertEquals("/youps", entry.getDocPath());
        assertEquals("File", entry.getDocType());
        assertEquals("Administrator", entry.getPrincipalName());
        assertEquals("test", entry.getRepositoryId());
    }

    @Test
    public void testGetLatestLogId() {
        String repositoryId = "test";
        createLogEntry("documentModified");
        long id1 = backend.getLatestLogId(repositoryId, "documentModified");
        assertTrue("id: " + id1, id1 > 0);
        createLogEntry("documentCreated");
        long id2 = backend.getLatestLogId(repositoryId, "documentModified", "documentCreated");
        assertTrue("id2: " + id2, id2 > 0);
        assertTrue(id2 > id1);
        long id = backend.getLatestLogId(repositoryId, "documentModified");
        assertEquals(id1, id);
        id = backend.getLatestLogId(repositoryId, "unknownEvent");
        assertEquals("id: " + id, 0, id);
    }

    @Test
    public void testGetLogEntriesAfter() {
        String repositoryId = "test";
        createLogEntry("something");
        createLogEntry("documentModified");
        long id1 = backend.getLatestLogId(repositoryId, "documentModified");

        createLogEntry("documentCreated");
        long id2 = backend.getLatestLogId(repositoryId, "documentModified", "documentCreated");
        assertTrue(id2 > id1);

        createLogEntry("documentCreated");
        long id3 = backend.getLatestLogId(repositoryId, "documentModified", "documentCreated");
        assertTrue(id3 > id2);

        createLogEntry("documentCreated");
        long id4 = backend.getLatestLogId(repositoryId, "documentModified", "documentCreated");
        assertTrue(id4 > id3);

        var entries = backend.getLogEntriesAfter(id1, 5, repositoryId, "documentCreated", "documentModified");
        assertEquals(4, entries.size());
        assertEquals(id1, entries.get(0).getId());

        entries = backend.getLogEntriesAfter(id2, 5, repositoryId, "documentCreated", "documentModified");
        assertEquals(3, entries.size());
        assertEquals(id2, entries.get(0).getId());
    }

    protected void createLogEntry(String eventId) {
        DocumentModel source = session.getDocument(new PathRef(YOUPS_PATH));

        EventContext ctx = new DocumentEventContext(session, session.getPrincipal(), source);
        Event event = ctx.newEvent(eventId);
        event.setInline(false);
        event.setImmediate(true);
        eventService.fireEvent(event);
        waitForAsyncCompletion();
    }

    @Test
    public void testLogLegalHold() {
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        doc = session.createDocument(doc);
        session.makeRecord(doc.getRef());

        // set retention active - aka legal hold
        session.setLegalHold(doc.getRef(), true, "set retention active");

        // an event is logged
        waitForAsyncCompletion();
        long id = backend.getLatestLogId(session.getRepositoryName(), AFTER_SET_LEGAL_HOLD);
        assertNotEquals(0, id);
        var logEntry = backend.getLogEntryByID(id);
        assertEquals(AFTER_SET_LEGAL_HOLD, logEntry.getEventId());
        assertEquals(doc.getId(), logEntry.getDocUUID());
        assertEquals("set retention active", logEntry.getComment());

        // unset retention active - aka legal hold
        session.setLegalHold(doc.getRef(), false, "unset retention active");

        // an event is logged
        waitForAsyncCompletion();
        id = backend.getLatestLogId(session.getRepositoryName(), AFTER_REMOVE_LEGAL_HOLD);
        assertNotEquals(0, id);
        logEntry = backend.getLogEntryByID(id);
        assertEquals(AFTER_REMOVE_LEGAL_HOLD, logEntry.getEventId());
        assertEquals(doc.getId(), logEntry.getDocUUID());
        assertEquals("unset retention active", logEntry.getComment());
    }
}
