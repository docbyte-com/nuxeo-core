/*
 * (C) Copyright 2015-2024 Nuxeo (http://nuxeo.com/) and others.
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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core;

import static org.apache.commons.lang3.ArrayUtils.nullToEmpty;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.automation.AutomationAdmin;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.ChainException;
import org.nuxeo.ecm.automation.OperationParameters;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.context.ContextHelperDescriptor;
import org.nuxeo.ecm.automation.context.ContextService;
import org.nuxeo.ecm.automation.context.ContextServiceImpl;
import org.nuxeo.ecm.automation.core.events.EventHandler;
import org.nuxeo.ecm.automation.core.events.EventHandlerRegistry;
import org.nuxeo.ecm.automation.core.exception.ChainExceptionFilter;
import org.nuxeo.ecm.automation.core.exception.ChainExceptionImpl;
import org.nuxeo.ecm.automation.core.impl.ChainTypeImpl;
import org.nuxeo.ecm.automation.core.impl.OperationServiceImpl;
import org.nuxeo.ecm.automation.core.impl.TypeAdapterKey;
import org.nuxeo.ecm.automation.core.trace.TracerFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.RuntimeMessage.Level;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.ServerLocator;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Nuxeo component that provide an implementation of the {@link AutomationService} and handle extensions registrations.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 */
public class AutomationComponent extends DefaultComponent {

    private static final Logger log = LogManager.getLogger(AutomationComponent.class);

    /** @since 2025.0 */
    public static final String AUTOMATION_COMPONENT_NAME = "org.nuxeo.ecm.core.operation.OperationServiceComponent";

    public static final String XP_OPERATIONS = "operations";

    public static final String XP_ADAPTERS = "adapters";

    public static final String XP_CHAINS = "chains";

    public static final String XP_EVENT_HANDLERS = "event-handlers";

    public static final String XP_CHAIN_EXCEPTION = "chainException";

    public static final String XP_AUTOMATION_FILTER = "automationFilter";

    public static final String XP_CONTEXT_HELPER = "contextHelpers";

    /**
     * This constant is internal, <strong>IT SHOULD NOT BE USED</strong>.
     *
     * @since 2025.0
     * @apiNote This is a fake extension point used to register all type of {@link OperationDescriptor operations}.
     */
    public static final String XP_INTERNAL_OPERATIONS = "internalOperations";

    protected static final Set<String> RESERVED_CONTEXT_HELPER_IDS = new LinkedHashSet<>(
            List.of("CurrentDate", "Context", "ctx", "This", "Session", "CurrentUser", "currentUser", "Env", "Document",
                    "currentDocument", "Documents", "params", "input"));

    protected OperationServiceImpl service;

    protected EventHandlerRegistry handlers;

    protected TracerFactory tracerFactory;

    protected ContextService contextService;

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (contribution instanceof OperationDescriptor descriptor) {
            OperationDescriptor existing = getDescriptor(XP_INTERNAL_OPERATIONS, descriptor.getId());
            if (existing != null) {
                // check if descriptor can be replaced
                if (!descriptor.replace()) {
                    throw new NuxeoException("An operation is already bound to: " + descriptor.getId()
                            + ". Use 'replace=true' to replace an existing operation");
                }
                // check operation can be merged
                if (!descriptor.getClass().equals(existing.getClass())) {
                    // instantiate the type to have a better message
                    throw new UnsupportedOperationException("Can't merge operations with id: " + existing.getId()
                            + ". The type " + descriptor.toType() + " cannot be merged in " + existing.toType() + ".");
                }
            }
        }
        if (XP_OPERATIONS.equals(extensionPoint) || XP_CHAINS.equals(extensionPoint)) {
            // also register the contribution to internalOperations
            super.registerContribution(contribution, XP_INTERNAL_OPERATIONS, contributor);
        }
        super.registerContribution(contribution, extensionPoint, contributor);
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (XP_OPERATIONS.equals(extensionPoint) || XP_CHAINS.equals(extensionPoint)) {
            // also unregister the contribution to internalOperations
            super.unregisterContribution(contribution, XP_INTERNAL_OPERATIONS, contributor);
        }
        super.unregisterContribution(contribution, extensionPoint, contributor);
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == AutomationService.class || adapter == AutomationAdmin.class) {
            return adapter.cast(service);
        } else if (adapter == EventHandlerRegistry.class) {
            return adapter.cast(handlers);
        } else if (adapter == TracerFactory.class) {
            return adapter.cast(tracerFactory);
        } else if (adapter == ContextService.class) {
            return adapter.cast(contextService);
        }
        return null;
    }

    @Override
    public void start(ComponentContext context) {
        var operations = this.<OperationDescriptor> getDescriptors(XP_INTERNAL_OPERATIONS)
                             .stream()
                             .filter(OperationDescriptor::isEnabled)
                             .map(OperationDescriptor::toType)
                             .<OperationTypeWithId> mapMulti((operationType, consumer) -> {
                                 consumer.accept(new OperationTypeWithId(operationType.getId(), operationType));
                                 for (var alias : nullToEmpty(operationType.getAliases())) {
                                     consumer.accept(new OperationTypeWithId(alias, operationType));
                                 }
                             })
                             .collect(Collectors.toMap(OperationTypeWithId::idOrAlias,
                                     OperationTypeWithId::operationType, (operationType1, operationType2) -> {
                                         throw new NuxeoException(
                                                 "The operation: " + operationType2 + " is overriding: "
                                                         + operationType1 + " with its alias which is not permitted");
                                     }));
        var chainExceptions = this.<ChainExceptionDescriptor> getDescriptors(XP_CHAIN_EXCEPTION)
                                  .stream()
                                  .map(ChainExceptionImpl::new)
                                  .collect(Collectors.toMap(ChainExceptionImpl::getId,
                                          Function.<ChainException> identity()));
        var filters = this.<AutomationFilterDescriptor> getDescriptors(XP_AUTOMATION_FILTER)
                          .stream()
                          .map(ChainExceptionFilter::new)
                          .collect(Collectors.toMap(ChainExceptionFilter::getId, Function.identity()));
        var adapters = this.<TypeAdapterContribution> getDescriptors(XP_ADAPTERS)
                           .stream()
                           .collect(Collectors.toMap(
                                   descriptor -> new TypeAdapterKey(descriptor.accept, descriptor.produce),
                                   descriptor -> {
                                       try {
                                           return descriptor.clazz.getDeclaredConstructor().newInstance();
                                       } catch (ReflectiveOperationException e) {
                                           throw new NuxeoException("Unable to instantiate adapter", e);
                                       }
                                   }));
        var eventHandlers = this.<EventHandler> getDescriptors(XP_EVENT_HANDLERS)
                                .stream()
                                .filter(EventHandler::isEnabled)
                                .collect(Collectors.partitioningBy(EventHandler::isPostCommit,
                                        // flatMap the handlers for each of them eventIds
                                        Collectors.flatMapping(
                                                handler -> handler.getEvents()
                                                                  .stream()
                                                                  .map(eventId -> new EventHandlerWithEventId(eventId,
                                                                          handler)),
                                                // groupBy eventIds -> List<EventHandler>
                                                Collectors.groupingBy(EventHandlerWithEventId::eventId,
                                                        Collectors.mapping(EventHandlerWithEventId::handler,
                                                                Collectors.toList())))));
        var contextHelpers = this.<ContextHelperDescriptor> getDescriptors(XP_CONTEXT_HELPER)
                                 .stream()
                                 .filter(ContextHelperDescriptor::isEnabled)
                                 .filter(descriptor -> {
                                     if (RESERVED_CONTEXT_HELPER_IDS.contains(descriptor.getId())) {
                                         log.warn("The context helper with id: {} cannot be registered because the id "
                                                 + "is reserved. Please use another one. The Nuxeo reserved aliases are: {}",
                                                 descriptor.getId(), String.join(",", RESERVED_CONTEXT_HELPER_IDS));
                                         return false;
                                     }
                                     return true;
                                 })
                                 .collect(Collectors.toMap(ContextHelperDescriptor::getId,
                                         ContextHelperDescriptor::instantiateContextHelper));

        service = new OperationServiceImpl(operations, chainExceptions, filters, adapters);
        handlers = new EventHandlerRegistry(eventHandlers.get(Boolean.FALSE), eventHandlers.get(Boolean.TRUE));
        tracerFactory = new TracerFactory();
        contextService = new ContextServiceImpl(contextHelpers);

        checkOperationChains(); // TODO move it to register to have the contributing component?
        if (!tracerFactory.getRecordingState()) {
            log.info("You can activate automation trace mode to get more information on automation executions");
        }
        try {
            bindManagement();
        } catch (JMException e) {
            throw new RuntimeException("Cannot bind management", e);
        }
    }

    /**
     * Checks operation references in chains
     *
     * @since 11.3
     */
    protected void checkOperationChains() {
        for (var operationType : service.getOperations()) {
            if (operationType instanceof ChainTypeImpl chainType) {
                var chain = chainType.getChain();
                for (OperationParameters opp : chain.getOperations()) {
                    if (!service.hasOperation(opp.id())) {
                        String msg = String.format(
                                "Operation chain with id '%s' references unknown operation with id '%s'", chain.getId(),
                                opp.id());
                        log.error(msg);
                        addRuntimeMessage(Level.ERROR, msg);
                    }
                }
            }
        }
    }

    @Override
    public void stop(ComponentContext context) {
        service.flushCompiledChains();
        try {
            unBindManagement();
        } catch (JMException e) {
            throw new RuntimeException("Cannot unbind management", e);
        }
    }

    protected void bindManagement() throws JMException {
        ObjectName objectName = new ObjectName("org.nuxeo.automation:name=tracerfactory");
        MBeanServer mBeanServer = Framework.getService(ServerLocator.class).lookupServer();
        mBeanServer.registerMBean(tracerFactory, objectName);
    }

    protected void unBindManagement() throws MalformedObjectNameException, NotCompliantMBeanException,
            InstanceAlreadyExistsException, MBeanRegistrationException, InstanceNotFoundException {
        final ObjectName on = new ObjectName("org.nuxeo.automation:name=tracerfactory");
        final ServerLocator locator = Framework.getService(ServerLocator.class);
        if (locator != null) {
            MBeanServer mBeanServer = locator.lookupServer();
            mBeanServer.unregisterMBean(on);
        }
    }

    record OperationTypeWithId(String idOrAlias, OperationType operationType) {
    }

    record EventHandlerWithEventId(String eventId, EventHandler handler) {
    }
}
