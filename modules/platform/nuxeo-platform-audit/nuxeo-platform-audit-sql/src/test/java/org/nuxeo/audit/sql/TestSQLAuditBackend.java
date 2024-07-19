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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_DOC_UUID;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_EVENT_ID;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.audit.api.AuditQueryBuilder;
import org.nuxeo.audit.service.AuditBackend;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.event.impl.UnboundEventContext;
import org.nuxeo.ecm.core.query.sql.model.Predicates;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * @since 2025.0
 * @deprecated since 2025.0, to follow other deprecation
 */
@SuppressWarnings("removal")
@Deprecated(since = "2025.0", forRemoval = true)
@RunWith(FeaturesRunner.class)
@Features(SQLAuditFeature.class)
@RepositoryConfig(init = TestSQLAuditBackend.MyInit.class, cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.audit.tests:test-audit-contrib.xml")
public class TestSQLAuditBackend {

    public static class MyInit extends DefaultRepositoryInit {

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
    protected TransactionalFeature transactionalFeature;

    @Test
    public void testLogMiscMessage() {
        var sqlBackend = (SQLAuditBackend) backend;

        List<String> eventIds = sqlBackend.getLoggedEventIds();
        int n = eventIds.size();

        EventContext ctx = new EventContextImpl(); // not:DocumentEventContext
        Event event = ctx.newEvent("documentDuplicated"); // auditable
        event.setInline(false);
        event.setImmediate(true);
        eventService.fireEvent(event);
        transactionalFeature.nextTransaction();

        eventIds = sqlBackend.getLoggedEventIds();
        assertEquals(n + 1, eventIds.size());
    }

    @Test
    public void testSyncLogCreation() {
        DocumentModel rootDocument = session.getRootDocument();
        long count = backend.syncLogCreationEntries(session.getRepositoryName(), rootDocument.getPathAsString(), true);
        assertEquals(7, count);

        String query = String.format("log.docUUID = '%s' and log.eventId = 'documentCreated'", rootDocument.getId());

        var entries = backend.nativeQueryLogs(query, 1, 1);
        assertEquals(1, entries.size());

        var entry = entries.getFirst();
        assertEquals("eventDocumentCategory", entry.getCategory());
        assertNull(entry.getComment());
        assertEquals("/", entry.getDocPath());
        assertEquals("Root", entry.getDocType());
        assertEquals("documentCreated", entry.getEventId());
        assertEquals(SecurityConstants.SYSTEM_USERNAME, entry.getPrincipalName());
    }

    @Test
    public void setSimplePrincipalNameIsLoggedAsPrincipalName() {
        // Given a simple principal
        NuxeoPrincipal principal = new UserPrincipal("testuser", null, false, false);
        // I get it in the logs
        doTestPrincipalName("testuser", principal);
    }

    @Test
    public void testPrincipalNameIsActingUser() {
        // Given a Nuxeo principal with an acting user
        NuxeoPrincipal principal = new NuxeoPrincipalImpl("mysystem", false, true);
        principal.setOriginatingUser("actualuser");
        // I get it in the logs
        doTestPrincipalName("actualuser", principal);
    }

    protected void doTestPrincipalName(String expected, NuxeoPrincipal principal) {
        // Given a principal
        // When i fire an event with it
        int oldCount = backend.getEventsCount("loginSuccess").intValue();
        EventContext ctx = new UnboundEventContext(principal, new HashMap<>());
        eventService.fireEvent(ctx.newEvent("loginSuccess"));
        transactionalFeature.nextTransaction();

        // Then then event is logged with the originating principal's name
        assertEquals(1, backend.getEventsCount("loginSuccess").intValue() - oldCount);
        var logEntry = backend.nativeQueryLogs("log.eventId = 'loginSuccess' order by log.id desc", 1, 1).getFirst();
        assertEquals(expected, logEntry.getPrincipalName());
    }

    @Test
    public void testExtendedInfos() {
        DocumentModel rootDocument = session.getRootDocument();
        DocumentModel model = session.createDocumentModel(rootDocument.getPathAsString(), "youps", "File");
        model.setProperty("dublincore", "title", "huum");
        model = session.createDocument(model);
        long count = backend.syncLogCreationEntries(session.getRepositoryName(), model.getPathAsString(), true);
        assertEquals(1, count);

        String query = String.format("log.docUUID = '%s' and log.eventId = 'documentCreated'", model.getId());

        var entries = backend.nativeQueryLogs(query, 1, 1);
        assertEquals(1, entries.size());

        var entry = entries.getFirst();
        assertEquals("eventDocumentCategory", entry.getCategory());
        assertEquals("test", entry.getRepositoryId());
        assertEquals("huum", entry.getExtendedValue("title"));
        assertEquals("/", entry.getExtendedValue("parentPath"));

        session.removeDocument(model.getRef());
        session.save();

        transactionalFeature.nextTransaction();

        entries = backend.queryLogs(new AuditQueryBuilder().predicate(Predicates.eq(LOG_DOC_UUID, model.getId()))
                                                           .and(Predicates.eq(LOG_EVENT_ID, "documentRemoved"))
                                                           .defaultOrder());
        assertEquals(1, entries.size());
        Map<String, Object> infos = entries.getFirst().getExtended();
        assertEquals("/", infos.get("parentPath"));
        // For the original audit implementation using a post-commit event listeners,
        // we only have a DeletedDocumentModel so no title available.
        // For the Stream-based audit implementation, extended infos are computed early with
        // the full DocumentModel so we have actual values.
        var title = (String) infos.get("title");
        assertEquals("huum", title);
    }
}
