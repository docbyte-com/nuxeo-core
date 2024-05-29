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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;

import org.glassfish.jersey.media.multipart.FormDataParam;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.versioning.VersioningService;
import org.nuxeo.ecm.platform.web.common.ServletHelper;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.ecm.webengine.model.exceptions.IllegalParameterException;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.impl.DefaultAdapter;

/**
 * File Service - manages attachments to a document.
 * <p>
 * Accepts the following methods:
 * <ul>
 * <li>GET - get the attached file
 * <li>POST - create an attachment
 * </ul>
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@WebAdapter(name = "file", type = "FileService", targetType = "Document")
public class FileService extends DefaultAdapter {

    @GET
    public Response doGet(@FormParam("property") String xpath, @Context Request request) {
        DocumentModel doc = getTarget().getAdapter(DocumentModel.class);
        if (xpath == null) {
            if (doc.hasSchema("file")) {
                xpath = "file:content";
            } else {
                throw new IllegalParameterException(
                        "Missing request parameter named 'property' that specify the blob property xpath to fetch");
            }
        }
        try {
            Property p = doc.getProperty(xpath);
            Blob blob = (Blob) p.getValue();
            if (blob == null) {
                throw new WebResourceNotFoundException("No attached file at " + xpath);
            }

            String fileName = blob.getFilename();
            if (fileName == null) {
                fileName = "Unknown";
            }

            String digest = blob.getDigest();
            EntityTag etag = digest == null ? null : new EntityTag(digest);

            if (etag != null) {
                ResponseBuilder builder = request.evaluatePreconditions(etag);
                if (builder != null) {
                    return builder.build();
                }
            }

            // TODO probably not needed as DownloadService already does it
            String contentDisposition = ServletHelper.getRFC2231ContentDisposition(ctx.getRequest(), fileName);

            // cached resource did change or no ETag -> serve updated content
            ResponseBuilder builder = Response.ok(blob)
                                              .header("Content-Disposition", contentDisposition)
                                              .type(blob.getMimeType());
            if (etag != null) {
                builder.tag(etag);
            }
            return builder.build();

        } catch (NuxeoException e) {
            e.addInfo("Failed to get the attached file");
            throw e;
        }
    }

    @POST
    public Response doPost(MultivaluedMap<String, String> formParams, @FormParam("property") String xpath,
            @FormParam("versioning") @DefaultValue("NONE") VersioningOption versioningOption,
            @FormDataParam("content") Blob blob) {
        DocumentModel doc = getTarget().getAdapter(DocumentModel.class);
        DocumentHelper.fillDocument(doc, formParams);
        if (xpath == null) {
            if (doc.hasSchema("file")) {
                xpath = "file:content";
            } else if (doc.hasSchema("files")) {
                xpath = "files:files";
            } else {
                throw new IllegalArgumentException("Missing request parameter named 'property' that specifies "
                        + "the blob property xpath to fetch");
            }
        }
        if (blob == null) {
            throw new IllegalArgumentException("Could not find any uploaded file");
        }
        try {
            Property p = doc.getProperty(xpath);
            if (p.isList()) { // add the file to the list
                if ("files".equals(p.getSchema().getName())) { // treat the
                    // files schema
                    // separately
                    Map<String, Serializable> map = new HashMap<>();
                    map.put("file", (Serializable) blob);
                    p.addValue(map);
                } else {
                    p.addValue(blob);
                }
            } else {
                p.setValue(blob);
            }
            // make snapshot
            doc.putContextData(VersioningService.VERSIONING_OPTION, versioningOption);
            CoreSession session = ctx.getCoreSession();
            session.saveDocument(doc);
            session.save();
            return redirect(getTarget().getPath());
        } catch (NuxeoException e) {
            e.addInfo("Failed to attach file");
            throw e;
        }
    }

    @GET
    @Path("delete")
    public Response remove(@FormParam("property") String xpath) {
        return doDelete(xpath);
    }

    @DELETE
    public Response doDelete(@FormParam("property") String xpath) {
        DocumentModel doc = getTarget().getAdapter(DocumentModel.class);
        if (xpath == null) {
            if (doc.hasSchema("file")) {
                xpath = "file:content";
            } else {
                throw new IllegalArgumentException("Missing request parameter named 'property' that specifies "
                        + "the blob property xpath to fetch");
            }
        }
        try {
            doc.getProperty(xpath).remove();
            CoreSession session = ctx.getCoreSession();
            session.saveDocument(doc);
            session.save();
        } catch (NuxeoException e) {
            e.addInfo("Failed to delete attached file");
            throw e;
        }
        return redirect(getTarget().getPath());
    }

}
