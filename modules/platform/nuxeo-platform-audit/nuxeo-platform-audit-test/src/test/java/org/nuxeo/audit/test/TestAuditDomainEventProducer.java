/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *    bdelbosc
 */
package org.nuxeo.audit.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_DOC_UUID;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_REPOSITORY_ID;

import java.util.List;

import jakarta.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.audit.api.AuditQueryBuilder;
import org.nuxeo.audit.api.LogEntry;
import org.nuxeo.audit.service.AuditBackend;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.query.sql.model.Predicates;
import org.nuxeo.lib.stream.log.LogLag;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.runtime.stream.StreamService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * @since 11.4
 */
@RunWith(FeaturesRunner.class)
@Features(AuditFeature.class)
@Deploy("org.nuxeo.audit.test.test:OSGI-INF/test-domain-event-producer-contrib.xml")
public class TestAuditDomainEventProducer {

    @Inject
    protected AuditBackend backend;

    @Inject
    protected CoreSession session;

    @Inject
    protected StreamService streamService;

    @Inject
    protected TransactionalFeature txFeature;

    @Before
    public void isInjected() {
        assertNotNull(session);
    }

    @Test
    public void testAuditDomainEventProducer() {
        LogLag lag = streamService.getLogManager().getLag(Name.ofUrn("source/audit"), Name.ofUrn("test/reader"));

        // generate events
        DocumentModel doc = session.createDocumentModel("/", "a-file", "File");
        doc = session.createDocument(doc);
        txFeature.nextTransaction();

        // test audit trail
        List<LogEntry> trail = backend.queryLogs(
                new AuditQueryBuilder().predicate(Predicates.eq(LOG_DOC_UUID, doc.getId()))
                                       .and(Predicates.eq(LOG_REPOSITORY_ID, session.getRepositoryName()))
                                       .defaultOrder());
        assertNotNull(trail);
        assertEquals(1, trail.size());

        // test we have one more event
        LogLag lag2 = streamService.getLogManager().getLag(Name.ofUrn("source/audit"), Name.ofUrn("test/reader"));
        assertEquals(lag.lag() + 1, lag2.lag());
    }

}
