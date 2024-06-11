/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.rest;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.impl.primitives.BlobProperty;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.core.api.versioning.VersioningService;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DocumentHelper {

    // Utility class.
    private DocumentHelper() {
    }

    public static DocumentModel createDocument(CoreSession session, DocumentModel parent, String name,
            MultivaluedMap<String, String> formParams) {

        String type = formParams.getFirst("doctype");
        if (type == null) {
            throw new NuxeoException("Invalid argument exception. No doc type specified");
        }
        DocumentModel newDoc = session.createDocumentModel(type);
        fillDocument(newDoc, formParams);
        if (name != null) {
            newDoc.setPropertyValue("dc:title", name);
        }
        newDoc.setPathInfo(parent.getPathAsString(),
                Framework.getService(PathSegmentService.class).generatePathSegment(newDoc));
        newDoc = session.createDocument(newDoc);
        newDoc.setPropertyValue("dc:title", newDoc.getName());
        session.saveDocument(newDoc);
        session.save();
        return newDoc;
    }

    public static DocumentModel updateDocument(CoreSession session, DocumentModel doc,
            MultivaluedMap<String, String> formParams, VersioningOption versioningOption) {
        fillDocument(doc, formParams);
        doc.putContextData(VersioningService.VERSIONING_OPTION, versioningOption);
        doc = session.saveDocument(doc);
        session.save();
        return doc;
    }

    /**
     * @since 2025.0
     */
    public static void fillDocument(DocumentModel doc, MultivaluedMap<String, String> formParams) {
        for (var entry : formParams.entrySet()) {
            String key = entry.getKey();
            if (key.indexOf(':') > -1) { // an XPATH property
                Property property;
                try {
                    property = doc.getProperty(key);
                } catch (PropertyException e) {
                    continue; // not a valid property
                }
                List<String> list = entry.getValue();
                if (list.isEmpty()) {
                    fillDocumentProperty(property, null);
                } else {
                    fillDocumentProperty(property, entry.getValue());
                }
            }
        }
    }

    protected static void fillDocumentProperty(Property property, List<String> values) {
        if (values == null || values.isEmpty()) {
            property.remove();
        } else if (property.isScalar()) {
            property.setValue(values.get(0));
        } else if (property.isList()) {
            if (!property.isContainer()) { // an array
                property.setValue(values);
            } else {
                Type elType = ((ListType) property.getType()).getFieldType();
                if (elType.isSimpleType()) {
                    property.setValue(values);
                } else if ("content".equals(elType.getName())) {
                    // list of blobs
                    List<Blob> blobs = new ArrayList<>();
                    // transform strings to blobs
                    for (var obj : values) {
                        blobs.add(Blobs.createBlob(obj));
                    }
                    property.setValue(blobs);
                } else {
                    // complex properties will be ignored
                }
            }
        } else if (property.isComplex()) {
            if (property.getClass() == BlobProperty.class) {
                // should be a file upload
                Blob blob = Blobs.createBlob(values.get(0));
                property.setValue(blob);
            } else {
                // complex properties will be ignored
            }
        }
    }

}
