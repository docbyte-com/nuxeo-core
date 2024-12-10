/*
 * (C) Copyright 2018-2024 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.core.storage.sql;

import org.nuxeo.ecm.core.blob.BlobManagerFeature;
import org.nuxeo.runtime.test.runner.BlacklistComponent;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.TransactionalConfig;

/**
 * @since 10.1
 */
@Deploy("org.nuxeo.ecm.core.storage.sql.test.tests:OSGI-INF/test-repo-ds.xml")
@Features({ BlobManagerFeature.class, VCSRepositoryFeature.class })
// don't deploy the vcs repository, tests are deploying a repository in the code
@BlacklistComponent("org.nuxeo.ecm.core.storage.sql.test.repo.repository")
@TransactionalConfig(autoStart = false)
public class SQLBackendFeature implements RunnerFeature {

    @Override
    public void initialize(FeaturesRunner runner) {
        var repositoryFeature = runner.getFeature(VCSRepositoryFeature.class);
        runner.getFeature(RuntimeFeature.class).registerHandler(new SQLBackendDeployer(repositoryFeature));
    }

    public static class SQLBackendDeployer extends HotDeployer.ActionHandler {

        protected final VCSRepositoryFeature repositoryFeature;

        public SQLBackendDeployer(VCSRepositoryFeature repositoryFeature) {
            this.repositoryFeature = repositoryFeature;
        }

        @Override
        public void exec(String action, String... args) throws Exception {
            repositoryFeature.initializeDB();
            next.exec(action, args);
            repositoryFeature.tearDownDB();
        }

    }

}
