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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.events;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.api.Framework;

/**
 * TODO: This service should be moved in another project, and renamed since it's a service, not a simple registry...
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class EventHandlerRegistry {

    private static final Logger log = LogManager.getLogger(EventHandlerRegistry.class);

    protected final Map<String, List<EventHandler>> handlers;

    protected final Map<String, List<EventHandler>> postCommitHandlers;

    public EventHandlerRegistry(Map<String, List<EventHandler>> handlers,
            Map<String, List<EventHandler>> postCommitHandlers) {
        this.handlers = handlers;
        this.postCommitHandlers = postCommitHandlers;
    }

    public List<EventHandler> getEventHandlers(String eventId) {
        return handlers.get(eventId);
    }

    public List<EventHandler> getPostCommitEventHandlers(String eventId) {
        return postCommitHandlers.get(eventId);
    }

    public Set<String> getPostCommitEventNames() {
        return postCommitHandlers.keySet();
    }

    public boolean acceptEvent(Event event, List<EventHandler> handlers) {
        if (CollectionUtils.isEmpty(handlers)) {
            return false;
        }
        var eventContext = event.getContext();
        try (OperationContext ctx = getOperationContext(eventContext)) {
            ctx.put("Event", event);
            return handlers.stream().anyMatch(handler -> handler.isEnabled(ctx, eventContext, true));
        }
    }

    public void handleEvent(Event event, List<EventHandler> handlers, boolean saveSession) {
        if (handlers == null || handlers.isEmpty()) {
            return; // ignore
        }

        var eventContext = event.getContext();
        for (EventHandler handler : handlers) {
            try (OperationContext ctx = getOperationContext(eventContext)) {
                ctx.put("Event", event);
                ctx.setCommit(saveSession); // avoid reentrant events
                if (handler.isEnabled(ctx, eventContext, false)) {
                    // TODO this will save the session at each iteration!
                    Framework.getService(AutomationService.class).run(ctx, handler.getChainId());
                }
            } catch (OperationException e) {
                log.error("Failed to handle event: {} using chain: {}", event.getName(), handler.getChainId(), e);
                throw new NuxeoException(e);
            } catch (NuxeoException e) {
                log.error("Failed to handle event: {} using chain: {}", event.getName(), handler.getChainId(), e);
                throw e;
            }
        }
    }

    protected OperationContext getOperationContext(EventContext eventContext) {
        if (eventContext instanceof DocumentEventContext documentEventContext) {
            OperationContext ctx = new OperationContext(eventContext.getCoreSession());
            ctx.setInput(documentEventContext.getSourceDocument());
            return ctx;
        }
        return new OperationContext();
    }
}
