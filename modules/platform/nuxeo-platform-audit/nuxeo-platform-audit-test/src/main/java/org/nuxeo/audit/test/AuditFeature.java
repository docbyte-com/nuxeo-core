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
package org.nuxeo.audit.test;

import static org.nuxeo.common.test.configuration.ThirdPartyUnderTest.AUDIT_SERVICE_VALUE;
import static org.nuxeo.common.test.configuration.ThirdPartyUnderTest.STORAGE_MEM;
import static org.nuxeo.common.test.configuration.ThirdPartyUnderTest.STORAGE_MONGODB;
import static org.nuxeo.common.test.configuration.ThirdPartyUnderTest.STORAGE_SQL;
import static org.nuxeo.common.test.logging.NuxeoLoggingConstants.MARKER_CONSOLE_OVERRIDE;

import java.util.function.IntFunction;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.audit.AuditCoreFeature;
import org.nuxeo.audit.api.LogEntry;
import org.nuxeo.audit.mem.MemAuditFeature;
import org.nuxeo.audit.mongodb.MongoDBAuditFeature;
import org.nuxeo.audit.sql.SQLAuditFeature;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.DynamicFeaturesLoader;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LoggerLevel;
import org.nuxeo.runtime.test.runner.RunnerFeature;

/**
 * @since 2025.0
 */
@Deploy("org.nuxeo.ecm.platform.dublincore")
@Deploy("org.nuxeo.audit.test")
@Features(AuditCoreFeature.class)
@LoggerLevel(klass = NXAuditEventsService.class, level = "DEBUG")
public class AuditFeature implements RunnerFeature {

    private static final Logger log = LogManager.getLogger(AuditFeature.class);

    @Inject
    protected AuditCoreFeature auditCoreFeature;

    public AuditFeature(DynamicFeaturesLoader loader) {
        var feature = switch (AUDIT_SERVICE_VALUE) {
            case STORAGE_MEM -> MemAuditFeature.class;
            case STORAGE_MONGODB -> MongoDBAuditFeature.class;
            case STORAGE_SQL -> SQLAuditFeature.class;
            default ->
                throw new UnsupportedOperationException("Audit type: " + AUDIT_SERVICE_VALUE + " is not supported");
        };
        loader.loadFeature(feature);
    }

    @Override
    public void start(FeaturesRunner runner) {
        log.info(MARKER_CONSOLE_OVERRIDE, "Deploying Audit using {}",
                () -> StringUtils.capitalize(AUDIT_SERVICE_VALUE.toLowerCase()));
    }

    public boolean isBackendSql() {
        return STORAGE_SQL.equals(AUDIT_SERVICE_VALUE);
    }

    public void generateLogEntries(String docName, String eventPrefix, String categoryPrefix, int max) {
        auditCoreFeature.generateLogEntries(docName, eventPrefix, categoryPrefix, max);
    }

    public void generateLogEntries(int nbEntries, IntFunction<LogEntry> generator) {
        auditCoreFeature.generateLogEntries(nbEntries, generator);
    }
}
