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
 * Contributors:
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.filemanager.api;

import java.io.IOException;
import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 * File Manager.
 * <p>
 * File Manager to handle file
 *
 * @author Andreas Kalogeropoulos
 */
public interface FileManager {

    /**
     * Returns a created or updated document based on the given {@code context}.
     * <p>
     * The document may not be persisted according to {@link FileImporterContext#isPersistDocument()}. That's the
     * caller's responsibility to actually persist the document.
     * <p>
     * Note that file importers may not use {@link FileImporterContext#isPersistDocument()} and always persist the
     * document.
     *
     * @return the created or updated document
     * @see FileImporterContext
     * @since 10.10
     */
    DocumentModel createOrUpdateDocument(FileImporterContext context) throws IOException;

    /**
     * Creates a Folder.
     *
     * @param fullname the full name of the folder
     * @param path the path were to create the folder
     * @param overwrite whether to overwrite an existing folder with the same title or not
     * @return the Folder Created
     * @since 9.1
     */
    DocumentModel createFolder(CoreSession documentManager, String fullname, String path, boolean overwrite)
            throws IOException;

    /**
     * Returns the list of document that are to be suggested to principalName as a candidate container for a new
     * document of type docType on all registered repositories.
     *
     * @return the list of candidate containers
     */
    DocumentModelList getCreationContainers(NuxeoPrincipal principal, String docType);

    /**
     * Returns the list of document that are to be suggested to the principal of documentManager as a candidate
     * container for a new document of type docType.
     *
     * @return the list of candidate containers
     */
    DocumentModelList getCreationContainers(CoreSession documentManager, String docType);

    List<DocumentLocation> findExistingDocumentWithFile(CoreSession documentManager, String path, String digest,
            NuxeoPrincipal principal);

    boolean isUnicityEnabled();

    List<String> getFields();

    String getDigestAlgorithm();

    boolean isDigestComputingEnabled();

}
