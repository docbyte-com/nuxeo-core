/*
 * (C) Copyright 2014-2024 Nuxeo (http://nuxeo.com/) and others.
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
 *     Stephane Lacoin
 */
package org.nuxeo.ecm.platform.routing.test;

import static org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants.GC_ROUTES_ACTION_NAME;
import static org.nuxeo.ecm.platform.routing.core.listener.DocumentRoutingWorkflowInstancesCleanup.CLEANUP_WORKFLOW_EVENT_NAME;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import org.junit.runners.model.FrameworkMethod;
import org.nuxeo.audit.test.AuditFeature;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.bulk.CoreBulkFeature;
import org.nuxeo.ecm.core.event.test.CapturingEventListener;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.filemanager.FileManagerFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

@Deploy("org.nuxeo.ecm.platform.content.template")
@Deploy("org.nuxeo.ecm.platform.usermanager")
@Deploy("org.nuxeo.ecm.platform.userworkspace")
@Deploy("org.nuxeo.ecm.platform.task.api")
@Deploy("org.nuxeo.ecm.platform.task.core")
@Deploy("org.nuxeo.ecm.platform.routing.api")
@Deploy("org.nuxeo.ecm.platform.routing.core")
@Deploy("org.nuxeo.elasticsearch.http.readonly")
@Deploy("org.nuxeo.ecm.platform.routing.core:OSGI-INF/test-sql-directories-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.routing.core:OSGI-INF/test-graph-operations-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.routing.core:OSGI-INF/test-graph-types-contrib.xml")
@Features({ AuditFeature.class, AutomationFeature.class, FileManagerFeature.class })
public class WorkflowFeature implements RunnerFeature {

    protected CleanupWorkflowWaiter cleanupWorkflowWaiter;

    protected CapturingEventListener capturingListener;

    @Override
    public void initialize(FeaturesRunner runner) {
        this.cleanupWorkflowWaiter = new CleanupWorkflowWaiter(runner.getFeature(CoreBulkFeature.class));
        runner.getFeature(TransactionalFeature.class).addWaiter(cleanupWorkflowWaiter);
    }

    @Override
    public void beforeSetup(FeaturesRunner runner, FrameworkMethod method, Object test) {
        this.capturingListener = new CapturingEventListener(CLEANUP_WORKFLOW_EVENT_NAME);
        this.cleanupWorkflowWaiter.setWaitForCleanup(this.capturingListener::hasCapturedEvents);
    }

    @Override
    public void afterTeardown(FeaturesRunner runner, FrameworkMethod method, Object test) {
        this.capturingListener.close();
    }

    protected static class CleanupWorkflowWaiter implements TransactionalFeature.Waiter {

        protected final CoreBulkFeature coreBulkFeature;

        protected BooleanSupplier waitForCleanup = () -> false;

        public CleanupWorkflowWaiter(CoreBulkFeature coreBulkFeature) {
            this.coreBulkFeature = coreBulkFeature;
        }

        @Override
        public boolean await(Duration duration) throws InterruptedException {
            if (waitForCleanup.getAsBoolean()) {
                // first wait for event to be processed
                long begin = System.currentTimeMillis();
                if (Framework.getService(WorkManager.class)
                             .awaitCompletion(duration.toMillis(), TimeUnit.MILLISECONDS)) {
                    var leftDuration = duration.minusMillis(System.currentTimeMillis() - begin);
                    // second wait for Bulk Action
                    return coreBulkFeature.wait(GC_ROUTES_ACTION_NAME, leftDuration);
                }
                // work manager consumed all the permitted duration
                return false;
            }
            return true;
        }

        protected void setWaitForCleanup(BooleanSupplier waitForCleanup) {
            this.waitForCleanup = waitForCleanup;
        }
    }
}
