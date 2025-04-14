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
 *     bdelbosc
 */
package org.nuxeo.ecm.core.search;

import static org.junit.Assert.assertEquals;

import java.util.List;

import jakarta.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.search.client.mock.MockSearchClientFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 2025.0
 */
@RunWith(FeaturesRunner.class)
@Features(MockSearchClientFeature.class)
public class TestSearchService {

    @Inject
    public SearchService service;

    @Test
    public void testService() {
        String defaultRepo = "test";
        String defaultClient = "default";
        assertEquals(defaultRepo, service.getDefaultRepositoryName());
        assertEquals("Set of repository names: " + service.getRepositoryNames(), 1,
                service.getRepositoryNames().size());

        assertEquals("idx", service.getDefaultIndexName());
        assertEquals(SearchIndex.of(defaultRepo, defaultClient, "idx"),
                service.getSearchIndex(service.getDefaultIndexName()));
        assertEquals(SearchIndex.of(defaultRepo, "repository", "repository"), service.getSearchIndex("repository"));
        assertEquals(List.of("repository", "idx"), service.getIndexNames(defaultRepo));
    }
}
