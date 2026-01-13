/*
 * (C) Copyright 2006-2025 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.automation.server.rest;

import static jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.InvalidChainException;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OperationNotFoundException;
import org.nuxeo.ecm.automation.io.rest.operations.ExecutionRequest;
import org.nuxeo.ecm.automation.server.AutomationServer;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.ExceptionHelper;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class ExecutableResource extends DefaultObject {

    @Context
    protected AutomationService service;

    @Context
    protected HttpServletRequest request;

    @Context
    protected HttpServletResponse response;

    @Context
    protected CoreSession session;

    protected OperationContext createContext(ExecutionRequest xreq) {
        return xreq.createContext(request, response, session);
    }

    @POST
    public Response doPost(ExecutionRequest xreq) {
        try {
            AutomationServer srv = Framework.getService(AutomationServer.class);
            if (!srv.accept(getId(), isChain(), request)) {
                return ResponseHelper.notFound();
            }
            Object result = execute(xreq);
            int customHttpStatus = xreq.getRestOperationContext().getHttpStatus();
            return ResponseHelper.getResponse(result, request, customHttpStatus);
        } catch (OperationException | NuxeoException | IOException cause) {
            String exceptionMessage = "Failed to invoke operation: " + getId();
            if (cause instanceof OperationNotFoundException) {
                throw new WebResourceNotFoundException(exceptionMessage, cause);
            } else if (cause instanceof InvalidChainException) {
                throw new NuxeoException(exceptionMessage, cause, SC_BAD_REQUEST);
            } else if (cause instanceof NuxeoException nuxeoException) {
                nuxeoException.addInfo(exceptionMessage);
                throw nuxeoException;
            } else {
                Throwable unWrapException = ExceptionHelper.unwrapException(cause);
                if (unWrapException instanceof RestOperationException) {
                    int customHttpStatus = ((RestOperationException) unWrapException).getStatus();
                    throw new NuxeoException(exceptionMessage, cause, customHttpStatus);
                }
                throw new NuxeoException(exceptionMessage, cause);
            }
        }
    }

    public abstract String getId();

    public abstract Object execute(ExecutionRequest req) throws OperationException;

    public abstract boolean isChain();

}
