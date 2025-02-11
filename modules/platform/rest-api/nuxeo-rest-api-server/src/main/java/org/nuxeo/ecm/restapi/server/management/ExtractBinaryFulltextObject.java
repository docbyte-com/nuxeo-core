/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.restapi.server.management;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.SYSTEM_USERNAME;

import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.core.storage.action.ExtractBinaryFulltextAction;
import org.nuxeo.ecm.core.storage.action.FixBinaryFulltextStorageAction;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.AbstractResource;
import org.nuxeo.ecm.webengine.model.impl.ResourceTypeImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * Endpoint to run binary fulltext extraction
 *
 * @since 2021.33
 */
@WebObject(type = ManagementObject.MANAGEMENT_OBJECT_PREFIX + "fulltext")
@Produces(APPLICATION_JSON)
public class ExtractBinaryFulltextObject extends AbstractResource<ResourceTypeImpl> {

    private static final Logger log = LogManager.getLogger(ExtractBinaryFulltextObject.class);

    // Proxies don't hold fulltext, avoid to run extraction on document without downloadable binaries
    public static final String ALL_DOCS = "SELECT * FROM Document WHERE ecm:isProxy = 0 AND ecm:mixinType = 'Downloadable'";

    public static final String ALL_BUT_PROXIES = "SELECT * FROM Document WHERE ecm:isProxy = 0";

    /**
     * Run fulltext extraction of binaries, force can be used when changing the configuration to nullify binary fulltext
     * on documents that are excluded with the new configuration.
     */
    @POST
    @Path("extract")
    public BulkStatus extractBinaryFulltext(@FormParam("query") String query, @FormParam("force") Boolean force) {
        final String finalQuery = StringUtils.defaultIfBlank(query, ALL_DOCS);
        BulkService bulkService = Framework.getService(BulkService.class);
        String commandId = bulkService.submit(new BulkCommand.Builder(ExtractBinaryFulltextAction.ACTION_NAME,
                finalQuery, SYSTEM_USERNAME).repository(ctx.getCoreSession().getRepositoryName())
                                            .param("force", force)
                                            .build());
        log.warn("Extracting Binary Fulltext: command: {}, query: {}, force: {}", commandId, finalQuery, force);
        return bulkService.getStatus(commandId);
    }

    /**
     * Migrate existing binary fulltext stored in backend to s3 storage. It requires to activate
     * nuxeo.bulk.action.fixBinaryFulltextStorage.enabled=true.
     */
    @POST
    @Path("fixBinaryStorage")
    public BulkStatus fixBinaryStorage(@FormParam("query") String query) {
        final String finalQuery = StringUtils.defaultIfBlank(query, ALL_BUT_PROXIES);
        BulkService bulkService = Framework.getService(BulkService.class);
        // validation will raise an illegal argument exception if repository is not configured with fulltextStoredInBlob
        String commandId = bulkService.submit(new BulkCommand.Builder(FixBinaryFulltextStorageAction.ACTION_NAME,
                finalQuery, SYSTEM_USERNAME).repository(ctx.getCoreSession().getRepositoryName()).build());
        log.warn("Migrating existing binary fulltext from repository to s3 storage, command: {}, query: {}", commandId,
                finalQuery);
        return bulkService.getStatus(commandId);
    }
}
