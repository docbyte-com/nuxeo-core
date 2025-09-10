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

import org.nuxeo.audit.mem.MemAuditFeature;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.management.ManagementFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.LoggerLevel;
import org.nuxeo.runtime.test.runner.RunnerFeature;

/**
 * @deprecated since 2025.0 this feature now deploys an in-memory backend, use
 *             {@code org.nuxeo.platform.audit.test.AuditFeature} to have a real backend instead
 */
@Features({ MemAuditFeature.class, ManagementFeature.class, PlatformFeature.class })
@Deprecated(since = "2025.0", forRemoval = true)
// hide deprecation message about AuditBackend/AuditLogger/AuditReader/Logs usage
@LoggerLevel(klass = NXAuditEventsService.class, level = "ERROR")
public class AuditFeature implements RunnerFeature {
}
