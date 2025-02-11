/*
 * (C) Copyright 2020-2024 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.test;

import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;
import org.nuxeo.runtime.test.runner.WithFrameworkPropertyFeature;

@Features(WithFrameworkPropertyFeature.class)
@WithFrameworkProperty(name = FulltextStoredInBlobFeature.KEY, value = "true")
@WithFrameworkProperty(name = FulltextStoredInBlobFeature.MIGRATION_KEY, value = "true")
public class FulltextStoredInBlobFeature implements RunnerFeature {

    protected static final String KEY = "nuxeo.test.fulltext.storedInBlob";

    protected static final String MIGRATION_KEY = "nuxeo.bulk.action.fixBinaryFulltextStorage.enabled";
}
