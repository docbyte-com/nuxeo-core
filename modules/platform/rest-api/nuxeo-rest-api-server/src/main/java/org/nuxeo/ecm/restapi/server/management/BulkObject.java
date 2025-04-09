/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nour AL KOTOB
 *     bdelbosc
 */
package org.nuxeo.ecm.restapi.server.management;

import static jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static jakarta.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.nuxeo.ecm.core.bulk.action.IdleAction.CONCURRENCY_OPTION;
import static org.nuxeo.ecm.core.bulk.action.IdleAction.ENABLED_OPTION;
import static org.nuxeo.ecm.core.bulk.action.IdleAction.PARTITIONS_OPTION;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.action.IdleAction;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.core.bulk.message.BulkStatus.State;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.AbstractResource;
import org.nuxeo.ecm.webengine.model.impl.ResourceTypeImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 11.3
 */
@WebObject(type = ManagementObject.MANAGEMENT_OBJECT_PREFIX + "bulk")
@Produces(APPLICATION_JSON)
public class BulkObject extends AbstractResource<ResourceTypeImpl> {
    private static final Logger log = LogManager.getLogger(BulkObject.class);

    /**
     * Gets the {@link BulkStatus} for the given {@code commandId}.
     */
    @GET
    @Path("{commandId}")
    public BulkStatus doGetStatus(@PathParam("commandId") String commandId) {
        BulkStatus status = Framework.getService(BulkService.class).getStatus(commandId);
        if (status.getState() == State.UNKNOWN) {
            // the command id doesn't exist
            throw new NuxeoException("commandId doesn't exist: " + commandId, SC_NOT_FOUND);
        }
        return status;
    }

    // @since 2025.1
    @PUT
    @Path("idle")
    public BulkStatus submitIdleProcessing(@QueryParam(value = "totalDocs") long total,
            @QueryParam(value = "millisDurationPerDoc") int sleepMillis) {
        if (!Boolean.parseBoolean(Framework.getProperty(ENABLED_OPTION, "false"))) {
            throw new NuxeoException("Idle action needs to be enabled with: " + ENABLED_OPTION + "=true", SC_FORBIDDEN);
        }
        int partitions = Integer.parseInt(Framework.getProperty(PARTITIONS_OPTION));
        int concurrency = Integer.parseInt(Framework.getProperty(CONCURRENCY_OPTION));
        BulkCommand command = new BulkCommand.Builder(IdleAction.ACTION_NAME, Long.valueOf(total).toString(),
                "system").useGenericScroller().param("sleepMillis", Integer.valueOf(sleepMillis).toString()).build();
        String commandId = Framework.getService(BulkService.class).submit(command);
        long duration = sleepMillis * total;
        long bestDuration = duration / partitions;
        log.warn(
                "Submit Idle bulk command: {}, processing duration: {}s, best estimation: {}s ({} docs, {} ms/doc, {} partitions, {} threads/node)",
                commandId, duration / 1000.0, bestDuration / 1000.0, total, sleepMillis, partitions, concurrency);
        return doGetStatus(commandId);
    }

}
