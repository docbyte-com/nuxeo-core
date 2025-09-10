/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */
package org.nuxeo.ecm.csv.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import jakarta.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * @since 2025.0
 */
@RunWith(FeaturesRunner.class)
public class TestCSVImporterTrim extends AbstractCSVImporterTest {

    public static final String[] DESCRIPTIONS = { "  Heading and trailing spaces  ", "Trailing space " };

    public static final String DOCS_VALUES_WITH_SPACES = "docs_values_with_spaces.csv";

    @Inject
    public TransactionalFeature txFeature;

    public static void doAssertDescription(CoreSession session, DocumentRef parent, boolean expectedTrimmed) {
        String[] actualDescriptions = session.getChildren(parent)
                                             .stream()
                                             .map(doc -> doc.getPropertyValue("dc:description").toString())
                                             .sorted()
                                             .toArray(String[]::new);
        assertArrayEquals(expectedTrimmed ? Arrays.stream(DESCRIPTIONS).map(String::trim).toArray() : DESCRIPTIONS,
                actualDescriptions);
    }

    @Test
    public void testTrimByDefault() throws IOException {
        doTestTrim(true);
    }

    @Test
    @Deploy("org.nuxeo.ecm.csv.core:OSGI-INF/test-do-not-trim-contrib.xml")
    public void testDoNotTrim() throws IOException {
        doTestTrim(false);
    }

    protected void doTestTrim(boolean expectedTrimmed) throws IOException {
        CSVImporterOptions options = new CSVImporterOptions.Builder().build();
        String importId = csvImporter.launchImport(session, "/", getCSVBlob(DOCS_VALUES_WITH_SPACES), options);
        txFeature.nextTransaction();
        List<CSVImportLog> importLogs = csvImporter.getImportLogs(importId);
        assertEquals(2, importLogs.size());
        doAssertDescription(session, new PathRef("/"), expectedTrimmed);
    }
}
