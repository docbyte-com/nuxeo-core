/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Julien Anguenot
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.lifecycle.impl;

import static java.util.function.Predicate.isEqual;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.LifeCycleException;
import org.nuxeo.ecm.core.lifecycle.LifeCycle;
import org.nuxeo.ecm.core.lifecycle.LifeCycleService;
import org.nuxeo.ecm.core.lifecycle.LifeCycleState;
import org.nuxeo.ecm.core.lifecycle.extensions.LifeCycleDescriptor;
import org.nuxeo.ecm.core.lifecycle.extensions.LifeCycleTypesDescriptor;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.runtime.RuntimeMessage.Level;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Descriptor;

/**
 * Life cycle service implementation.
 *
 * @see org.nuxeo.ecm.core.lifecycle.LifeCycleService
 * @author Julien Anguenot
 * @author Florent Guillaume
 */
public class LifeCycleServiceImpl extends DefaultComponent implements LifeCycleService {

    private static final Logger log = LogManager.getLogger(LifeCycleServiceImpl.class);

    public static final ComponentName NAME = new ComponentName("org.nuxeo.ecm.core.lifecycle.LifeCycleService");

    protected static final String XP_LIFECYCLE = "lifecycle";

    protected static final String XP_TYPES = "types";

    protected final Map<String, LifeCycle> lifeCycles = new HashMap<>();

    @Override
    public void start(ComponentContext context) {
        super.start(context);
        for (Descriptor contrib : getDescriptors(XP_LIFECYCLE)) {
            var desc = (LifeCycleDescriptor) contrib;
            if (desc.isEnabled()) {
                lifeCycles.put(desc.getName(), instantiateLifeCycle(desc));
            }
            // look for delete state to warn about usage
            if (!"default".equals(desc.getName())
                    && desc.getStates().stream().map(LifeCycleState::getName).anyMatch(isEqual("deleted"))) {
                String msg = "The 'deleted' state was removed after deprecation."
                        + " Please remove it from your life cycle policy and use the trash service instead.";
                log.warn(msg);
                addRuntimeMessage(Level.WARNING, msg);
            }
        }
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        lifeCycles.clear();
    }

    @Override
    public LifeCycle getLifeCycleByName(String name) {
        return lifeCycles.get(name);
    }

    @Override
    public LifeCycle getLifeCycleFor(Document doc) {
        var name = getLifeCycleNameFor(doc.getType().getName());
        if (name == null) {
            return null;
        }
        return getLifeCycleByName(name);
    }

    @Override
    public String getLifeCycleNameFor(String typeName) {
        return this.<LifeCycleTypesDescriptor> getDescriptors(XP_TYPES)
                   .stream()
                   .filter(d -> d.getDocumentType().equals(typeName))
                   .findFirst()
                   .map(LifeCycleTypesDescriptor::getLifeCycleName)
                   .orElse(null);
    }

    @Override
    public Collection<LifeCycle> getLifeCycles() {
        return lifeCycles.values();
    }

    @Override
    public Collection<String> getTypesFor(String lifeCycleName) {
        return this.<LifeCycleTypesDescriptor> getDescriptors(XP_TYPES)
                   .stream()
                   .filter(d -> d.getLifeCycleName().equals(lifeCycleName))
                   .map(LifeCycleTypesDescriptor::getDocumentType)
                   .toList();
    }

    @Override
    public Map<String, String> getTypesMapping() {
        return this.<LifeCycleTypesDescriptor> getDescriptors(XP_TYPES)
                   .stream()
                   .collect(Collectors.toMap(LifeCycleTypesDescriptor::getDocumentType,
                           LifeCycleTypesDescriptor::getLifeCycleName));
    }

    @Override
    public void initialize(Document doc) throws LifeCycleException {
        initialize(doc, null);
    }

    @Override
    public void initialize(Document doc, String initialStateName) throws LifeCycleException {
        String lifeCycleName;
        LifeCycle documentLifeCycle = getLifeCycleFor(doc);
        if (documentLifeCycle == null) {
            lifeCycleName = "undefined";
            if (initialStateName == null) {
                initialStateName = "undefined";
            }
        } else {
            lifeCycleName = documentLifeCycle.getName();
            // set initial life cycle state
            if (initialStateName == null) {
                initialStateName = documentLifeCycle.getDefaultInitialStateName();
            } else {
                // check it's a valid state
                LifeCycleState state = documentLifeCycle.getStateByName(initialStateName);
                if (state == null) {
                    throw new LifeCycleException(String.format("State '%s' is not a valid state " + "for lifecycle %s",
                            initialStateName, lifeCycleName));
                }
            }
        }
        doc.setCurrentLifeCycleState(initialStateName);
        doc.setLifeCyclePolicy(lifeCycleName);
    }

    @Override
    public void followTransition(Document doc, String transitionName) throws LifeCycleException {
        String lifeCycleState = doc.getLifeCycleState();
        LifeCycle lifeCycle = getLifeCycleFor(doc);
        if (lifeCycle != null && lifeCycle.getAllowedStateTransitionsFrom(lifeCycleState).contains(transitionName)) {
            String destinationStateName = lifeCycle.getTransitionByName(transitionName).getDestinationStateName();
            doc.setCurrentLifeCycleState(destinationStateName);
        } else {
            throw new LifeCycleException(
                    "Not allowed to follow transition <" + transitionName + "> from state <" + lifeCycleState + '>');
        }
    }

    @Override
    public void reinitLifeCycle(Document doc) throws LifeCycleException {
        LifeCycle documentLifeCycle = getLifeCycleFor(doc);
        if (documentLifeCycle == null) {
            log.debug("No lifecycle policy for this document. Nothing to do !");
            return;
        }
        doc.setCurrentLifeCycleState(documentLifeCycle.getDefaultInitialStateName());
    }

    @Override
    public List<String> getNonRecursiveTransitionForDocType(String docTypeName) {
        return this.<LifeCycleTypesDescriptor> getDescriptors(XP_TYPES)
                   .stream()
                   .filter(d -> Objects.equals(docTypeName, d.getDocumentType()))
                   .map(LifeCycleTypesDescriptor::getNoRecursionForTransitions)
                   .filter(Objects::nonNull)
                   .map(t -> List.of(t.split(",")))
                   .findFirst()
                   .orElse(List.of());
    }

    protected LifeCycle instantiateLifeCycle(LifeCycleDescriptor desc) {
        String name = desc.getName();
        String initialStateName = desc.getInitialStateName();
        String defaultInitialStateName = desc.getDefaultInitialStateName();
        if (initialStateName != null) {
            defaultInitialStateName = initialStateName;
            log.warn(
                    "Lifecycle registration of default initial state has changed, change initial=\"{}\" to defaultInitial=\"{}\" in lifecyle '{}' definition",
                    defaultInitialStateName, defaultInitialStateName, name);
        }
        boolean defaultInitialStateFound = false;
        Collection<String> initialStateNames = new HashSet<>();
        Collection<LifeCycleState> states = desc.getStates();
        for (LifeCycleState state : states) {
            String stateName = state.getName();
            if (defaultInitialStateName.equals(stateName)) {
                defaultInitialStateFound = true;
                initialStateNames.add(stateName);
            }
            if (state.isInitial()) {
                initialStateNames.add(stateName);
            }
        }
        if (!defaultInitialStateFound) {
            log.error("Default initial state {} not found on lifecycle {}", defaultInitialStateName, name);
        }
        return new LifeCycleImpl(name, defaultInitialStateName, initialStateNames, states, desc.getTransitions());
    }
}
