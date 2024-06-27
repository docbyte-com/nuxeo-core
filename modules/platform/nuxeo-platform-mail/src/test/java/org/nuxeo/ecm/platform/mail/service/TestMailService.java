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
 *     Alexandre Russel
 */
package org.nuxeo.ecm.platform.mail.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import jakarta.inject.Inject;
import jakarta.mail.Session;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.mail.action.MessageActionPipe;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @author Alexandre Russel
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.platform.mail:OSGI-INF/nxmail-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.mail:OSGI-INF/nxmail-framework.xml")
public class TestMailService {

    @Inject
    protected MailService mailService;

    @Inject
    protected HotDeployer hotDeployer;

    @Test
    public void testServiceRegistration() throws Exception {

        MessageActionPipe pipe = mailService.getPipe("nxmail");
        assertNotNull(pipe);
        assertEquals(4, pipe.size());
        assertEquals(pipe.get(0).getClass().getSimpleName(), "StartAction");
        assertEquals(pipe.get(1).getClass().getSimpleName(), "ExtractMessageInformationAction");
        assertEquals(pipe.get(2).getClass().getSimpleName(), "CheckMailUnicity");
        assertEquals(pipe.get(3).getClass().getSimpleName(), "CreateDocumentsAction");
        // assertEquals(pipe.get(4).getClass().getSimpleName(), "EndAction");
        // test contribution merge
        hotDeployer.deploy("org.nuxeo.ecm.platform.mail.test:OSGI-INF/mailService-test-contrib.xml");

        pipe = mailService.getPipe("nxmail");
        assertNotNull(pipe);
        assertEquals(4, pipe.size());
        assertEquals(pipe.get(0).getClass().getSimpleName(), "StartAction");
        assertEquals(pipe.get(1).getClass().getSimpleName(), "ExtractMessageInformationAction");
        assertEquals(pipe.get(2).getClass().getSimpleName(), "CreateDocumentsAction");
        assertEquals(pipe.get(3).getClass().getSimpleName(), "CreateDocumentsAction");
        // assertEquals(pipe.get(4).getClass().getSimpleName(), "EndAction");
        // test contribution override
        hotDeployer.deploy("org.nuxeo.ecm.platform.mail.test:OSGI-INF/mailService-override-test-contrib.xml");

        pipe = mailService.getPipe("nxmail");
        assertNotNull(pipe);
        assertEquals(2, pipe.size());
        assertEquals(pipe.get(0).getClass().getSimpleName(), "ExtractMessageInformationAction");
        assertEquals(pipe.get(1).getClass().getSimpleName(), "CreateDocumentsAction");

    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.mail.test:OSGI-INF/mailService-session-factory-test-contrib.xml")
    public void testSessionFactoryUnique() {

        Session session1 = mailService.getSession("testFactory");
        assertNotNull(session1);
        // check we get the same session by getting a session again
        Session session1a = mailService.getSession("testFactory");
        assertNotNull(session1a);
        // check equality by reference
        assertSame("Sessions should be equals", session1, session1a);

        // now get a new session
        Session session2 = mailService.getSession("testFactory2");
        assertNotNull(session2);
        assertNotSame("Sessions shouldn't be equals", session1, session2);
    }

}
