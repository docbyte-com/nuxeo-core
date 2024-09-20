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
 */
package org.nuxeo.ecm.platform.audit;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import jakarta.persistence.EntityManager;

import org.nuxeo.ecm.core.persistence.PersistenceProviderFactory;
import org.nuxeo.ecm.platform.audit.api.AuditLogger;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.ManagementFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LoggerLevel;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @deprecated since 2025.0, use {@code org.nuxeo.platform.audit.test.AuditFeature} instead
 */
@Deploy("org.nuxeo.ecm.platform.audit")
@Deploy("org.nuxeo.ecm.platform.audit:nxaudit-ds.xml")
@Features({ ManagementFeature.class, PlatformFeature.class })
@Deprecated(since = "2025.0", forRemoval = true)
// hide deprecation message about AuditBackend/AuditLogger/AuditReader/Logs usage
@LoggerLevel(klass = NXAuditEventsService.class, level = "ERROR")
public class AuditFeature implements RunnerFeature {

    @Override
    public void initialize(FeaturesRunner runner) {
        runner.getFeature(TransactionalFeature.class).addWaiter(new BulkAuditWaiter());
    }

    protected static class BulkAuditWaiter implements TransactionalFeature.Waiter {
        @Override
        public boolean await(Duration duration) throws InterruptedException {
            return Framework.getService(AuditLogger.class).await(duration.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void afterRun(FeaturesRunner runner) {
        clear();
    }

    public void clear() {
        boolean started = !TransactionHelper.isTransactionActive() && TransactionHelper.startTransaction();
        try {
            doClear();
        } finally {
            if (started) {
                TransactionHelper.commitOrRollbackTransaction();
            }
        }
    }

    public void doClear() {
        EntityManager em = Framework.getService(PersistenceProviderFactory.class)
                                    .newProvider("nxaudit-logs")
                                    .acquireEntityManager();
        try {
            em.createNativeQuery("delete from nxp_logs_mapextinfos").executeUpdate();
            em.createNativeQuery("delete from nxp_logs_extinfo").executeUpdate();
            em.createNativeQuery("delete from nxp_logs").executeUpdate();
        } finally {
            em.close();
        }
    }
}
