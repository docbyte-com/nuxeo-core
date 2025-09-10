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
package org.nuxeo.ecm.platform.commandline.executor.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;

import jakarta.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * Test service and EPs.
 *
 * @author tiry
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.platform.commandline.executor")
public class TestService {

    @Inject
    protected CommandLineExecutorService cles;

    @Inject
    protected HotDeployer hotDeployer;

    @Test
    @Deploy("org.nuxeo.ecm.platform.commandline.executor.test:OSGI-INF/commandline-aspell-test-contrib.xml")
    public void testCmdRegistration() throws Exception {

        List<String> cmds = cles.getRegistredCommands();
        assertNotNull(cmds);
        assertEquals(2, cmds.size());
        assertTrue(cmds.contains("aspell"));

        hotDeployer.deploy(
                "org.nuxeo.ecm.platform.commandline.executor.test:OSGI-INF/commandline-imagemagick-test-contrib.xml");

        cmds = cles.getRegistredCommands();
        assertNotNull(cmds);
        assertEquals(4, cmds.size());
        assertTrue(cmds.contains("identify"));

        hotDeployer.deploy(
                "org.nuxeo.ecm.platform.commandline.executor.test:OSGI-INF/commandline-imagemagick-test-contrib2.xml");

        cmds = cles.getRegistredCommands();
        assertNotNull(cmds);
        assertEquals(3, cmds.size());
        assertFalse(cmds.contains("identify"));
    }

    @Test
    public void testCmdException() {
        var e = assertThrows(CommandNotAvailable.class, () -> cles.execCommand("IDon'tExist", null));
        String msg = e.getErrorMessage();
        assertNotNull(msg);
    }

}
