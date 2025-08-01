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
 *     Nuxeo - initial API and implementation
 *     Thibaud Arguillere (Nuxeo)
 */
package org.nuxeo.ecm.platform.importer.factories;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.platform.importer.source.SourceNode;

/**
 * Default implementation for DocumentModel factory The default empty constructor create Folder for folderish file and
 * File for other. But you can specify them using the other constructor. Also, if you are using .properties files to
 * setup metada, you can use the ecm:primaryType xpath to specify the type of document to create. This will override the
 * default ones, and works for files and folders. If no .properties file is provided of it the current node has a
 * .properties file but no ecm:primaryType, the default types are created. This works for leafType but also for
 * folderish type.
 *
 * @author Thierry Delprat
 * @author Daniel Tellez
 * @author Thibaud Arguillere
 */
public class DefaultDocumentModelFactory extends AbstractDocumentModelFactory {

    public static final String DOCTYPE_KEY_NAME = "ecm:primaryType";

    public static final String FACETS_KEY_NAME = "ecm:mixinTypes";

    protected String folderishType;

    protected String leafType;

    /**
     * Instantiate a DefaultDocumentModelFactory that creates Folder and File
     */
    public DefaultDocumentModelFactory() {
        this("Folder", "File");
    }

    /**
     * Instantiate a DefaultDocumentModelFactory that creates specified types doc
     *
     * @param folderishType the folderish type
     * @param leafType the other type
     */
    public DefaultDocumentModelFactory(String folderishType, String leafType) {
        this.folderishType = folderishType;
        this.leafType = leafType;
    }

    /*
     * (non-Javadoc)
     * @seeorg.nuxeo.ecm.platform.importer.base.ImporterDocumentModelFactory#
     * createFolderishNode(org.nuxeo.ecm.core.api.CoreSession, org.nuxeo.ecm.core.api.DocumentModel,
     * org.nuxeo.ecm.platform.importer.base.SourceNode)
     */
    @Override
    public DocumentModel createFolderishNode(CoreSession session, DocumentModel parent, SourceNode node)
            throws IOException {

        String name = getValidNameFromFileName(node.getName());

        BlobHolder bh = node.getBlobHolder();
        String folderishTypeToUse = getDocTypeToUse(bh);
        if (folderishTypeToUse == null) {
            folderishTypeToUse = folderishType;
        }
        List<String> facets = getFacetsToUse(bh);

        DocumentModel doc = session.createDocumentModel(parent.getPathAsString(), name, folderishTypeToUse);
        for (String facet : facets) {
            doc.addFacet(facet);
        }
        doc.setProperty("dublincore", "title", node.getName());
        doc = session.createDocument(doc);
        if (bh != null) {
            doc = setDocumentProperties(session, bh.getProperties(), doc);
        }

        return doc;
    }

    /*
     * (non-Javadoc)
     * @seeorg.nuxeo.ecm.platform.importer.base.ImporterDocumentModelFactory#
     * createLeafNode(org.nuxeo.ecm.core.api.CoreSession, org.nuxeo.ecm.core.api.DocumentModel,
     * org.nuxeo.ecm.platform.importer.base.SourceNode)
     */
    @Override
    public DocumentModel createLeafNode(CoreSession session, DocumentModel parent, SourceNode node) throws IOException {
        return defaultCreateLeafNode(session, parent, node);
    }

    protected DocumentModel defaultCreateLeafNode(CoreSession session, DocumentModel parent, SourceNode node)
            throws IOException {

        Blob blob = null;
        Map<String, Serializable> props = null;
        String leafTypeToUse = leafType;
        BlobHolder bh = node.getBlobHolder();
        if (bh != null) {
            blob = bh.getBlob();
            props = bh.getProperties();
            String bhType = getDocTypeToUse(bh);
            if (bhType != null) {
                leafTypeToUse = bhType;
            }
        }
        String fileName = node.getName();
        String name = getValidNameFromFileName(fileName);
        DocumentModel doc = session.createDocumentModel(parent.getPathAsString(), name, leafTypeToUse);
        for (String facet : getFacetsToUse(bh)) {
            doc.addFacet(facet);
        }
        doc.setProperty("dublincore", "title", node.getName());
        if (blob != null && blob.getLength() > 0) {
            blob.setFilename(fileName);
            doc.setProperty("file", "content", blob);
        }
        doc = session.createDocument(doc);
        if (props != null) {
            doc = setDocumentProperties(session, props, doc);
        }
        return doc;
    }

    /*
     * Return null if DOCTYPE_KEY_NAME is not in the properties or has been set to nothing.
     */
    protected String getDocTypeToUse(BlobHolder inBH) {
        String type = null;

        if (inBH != null) {
            Map<String, Serializable> props = inBH.getProperties();
            if (props != null) {
                type = (String) props.get(DOCTYPE_KEY_NAME);
                if (type != null && type.isEmpty()) {
                    type = null;
                }
            }
        }

        return type;
    }

    protected List<String> getFacetsToUse(BlobHolder inBH) {
        if (inBH != null) {
            Map<String, Serializable> props = inBH.getProperties();
            if (props != null) {
                Serializable ob = props.get(FACETS_KEY_NAME);
                if (ob instanceof String facet) {
                    if (StringUtils.isNotBlank(facet)) {
                        return Collections.singletonList(facet);
                    }
                } else if (ob != null) {
                    return (List<String>) ob;
                }
            }
        }
        return Collections.emptyList();
    }

    public void setFolderishType(String folderishType) {
        this.folderishType = folderishType;
    }

    public void setLeafType(String leafType) {
        this.leafType = leafType;
    }

}
