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
package org.nuxeo.ecm.platform.picture.binary.metadata;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import jakarta.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.picture.core.ImagingCoreFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 2025.0
 */
@RunWith(FeaturesRunner.class)
@Features(ImagingCoreFeature.class)
public class TestExifToolBinaryMetadataMapping {

    @Inject
    protected CoreSession session;

    @Test
    public void testExifAndIptc() throws IOException {
        // Create folder
        DocumentModel doc = session.createDocumentModel("/", "folder", "Folder");
        doc.setPropertyValue("dc:title", "Folder");
        session.createDocument(doc);

        // Create first picture
        doc = session.createDocumentModel("/folder", "picture", "Picture");
        doc.setPropertyValue("dc:title", "picture");
        doc = session.createDocument(doc);

        // Attach EXIF sample
        File binary = FileUtils.getResourceFileFromContext("images/china.jpg");
        Blob fb = Blobs.createBlob(binary, "image/jpeg");
        DocumentHelper.addBlob(doc.getProperty("file:content"), fb);
        session.saveDocument(doc);

        // Verify
        DocumentModel picture = session.getDocument(doc.getRef());
        assertEquals("Horizontal (normal)", picture.getPropertyValue("imd:orientation"));
        assertEquals(2.4, picture.getPropertyValue("imd:fnumber"));

        // Create second picture
        doc = session.createDocumentModel("/folder", "picture1", "Picture");
        doc.setPropertyValue("dc:title", "picture");
        doc = session.createDocument(doc);

        // Attach IPTC sample
        binary = FileUtils.getResourceFileFromContext("images/iptc_sample.jpg");
        fb = Blobs.createBlob(binary, "image/jpeg");
        DocumentHelper.addBlob(doc.getProperty("file:content"), fb);
        session.saveDocument(doc);

        // Verify
        picture = session.getDocument(doc.getRef());
        assertEquals("DDP", picture.getPropertyValue("dc:source"));
        assertEquals("ImageForum", picture.getPropertyValue("dc:rights"));
        assertEquals("Music", picture.getPropertyValue("dc:description").toString().substring(0, 5));
    }
}
