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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.web.resources.core.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.dag.CycleDetectedException;
import org.codehaus.plexus.util.dag.DAG;
import org.codehaus.plexus.util.dag.TopologicalSorter;
import org.nuxeo.ecm.web.resources.api.Processor;
import org.nuxeo.ecm.web.resources.api.Resource;
import org.nuxeo.ecm.web.resources.api.ResourceBundle;
import org.nuxeo.ecm.web.resources.api.ResourceContext;
import org.nuxeo.ecm.web.resources.api.ResourceType;
import org.nuxeo.ecm.web.resources.api.service.WebResourceManager;
import org.nuxeo.ecm.web.resources.core.ProcessorDescriptor;
import org.nuxeo.ecm.web.resources.core.ResourceBundleDescriptor;
import org.nuxeo.ecm.web.resources.core.ResourceDescriptor;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @since 7.3
 */
public class WebResourceManagerImpl extends DefaultComponent implements WebResourceManager {

    private static final Logger log = LogManager.getLogger(WebResourceManagerImpl.class);

    protected static final String RESOURCES_ENDPOINT = "resources";

    protected static final String RESOURCE_BUNDLES_ENDPOINT = "bundles";

    protected static final String PROCESSORS_ENDPOINT = "processors";

    protected Map<String, ProcessorDescriptor> processors;

    // Runtime Component API

    @Override
    public void start(ComponentContext context) {
        processors = this.<ProcessorDescriptor> getDescriptors(PROCESSORS_ENDPOINT)
                         .stream()
                         .filter(ProcessorDescriptor::isEnabled)
                         .collect(Collectors.toMap(ProcessorDescriptor::getName, Function.identity()));
    }

    @Override
    public void stop(ComponentContext context) {
        processors = null;
    }

    @Override
    public Resource getResource(String name) {
        // compute the resource dynamically as there's API to register/unregister Resource
        return this.<ResourceDescriptor> getDescriptors(RESOURCES_ENDPOINT)
                   .stream()
                   .filter(descriptor -> descriptor.getName().equals(name))
                   .findFirst()
                   .orElse(null);
    }

    @Override
    public ResourceBundle getResourceBundle(String name) {
        // compute the resource dynamically as there's API to register/unregister Resource
        return this.<ResourceBundleDescriptor> getDescriptors(RESOURCE_BUNDLES_ENDPOINT)
                   .stream()
                   .filter(descriptor -> descriptor.getName().equals(name))
                   .findFirst()
                   .orElse(null);
    }

    @Override
    public List<ResourceBundle> getResourceBundles() {
        return new ArrayList<>(this.<ResourceBundleDescriptor> getDescriptors(RESOURCE_BUNDLES_ENDPOINT));
    }

    @Override
    public Processor getProcessor(String name) {
        return processors.get(name);
    }

    @Override
    public List<Processor> getProcessors() {
        return new ArrayList<>(processors.values().stream().sorted().toList());
    }

    @Override
    public List<Processor> getProcessors(String type) {
        Predicate<ProcessorDescriptor> processorFilter = descriptor -> type == null
                || descriptor.getTypes().contains(type);
        return new ArrayList<>(processors.values().stream().filter(processorFilter).sorted().toList());
    }

    @Override
    public List<Resource> getResources(ResourceContext context, String bundleName, String type) {
        List<Resource> res = new ArrayList<>();
        ResourceBundle rb = getResourceBundle(bundleName);
        if (rb == null) {
            log.debug("Unknown bundle named: {}", bundleName);
            return res;
        }

        Map<String, Resource> all = new HashMap<>();
        // retrieve deps + filter depending on type + detect cycles
        DAG graph = new DAG();
        for (String rn : rb.getResources()) {
            Resource r = getResource(rn);
            if (r == null) {
                log.error("Could not resolve resource: {} on bundle: {}", rn, bundleName);
                continue;
            }
            // resolve sub resources of given type before filtering
            Map<String, Resource> subRes = getSubResources(graph, r, type);
            if (ResourceType.matches(type, r) || !subRes.isEmpty()) {
                graph.addVertex(rn);
                all.put(rn, r);
                all.putAll(subRes);
            }
        }

        for (Object rn : TopologicalSorter.sort(graph)) {
            Resource r = all.get(rn);
            if (ResourceType.matches(type, r)) {
                res.add(r);
            }
        }

        return res;
    }

    protected Map<String, Resource> getSubResources(DAG graph, Resource r, String type) {
        Map<String, Resource> res = new HashMap<>();
        List<String> deps = r.getDependencies();
        if (deps != null) {
            for (String dn : deps) {
                Resource d = getResource(dn);
                if (d == null) {
                    log.error("Unknown resource dependency named: {}", dn);
                    continue;
                }
                if (!ResourceType.matches(type, d)) {
                    continue;
                }
                res.put(dn, d);
                try {
                    graph.addEdge(r.getName(), dn);
                } catch (CycleDetectedException e) {
                    log.error("Cycle detected in resource dependencies: ", e);
                    break;
                }
                res.putAll(getSubResources(graph, d, type));
            }
        }
        return res;
    }

    @Override
    public void registerResourceBundle(ResourceBundle bundle) {
        log.info("Register resource bundle: {}", bundle::getName);
        register(RESOURCE_BUNDLES_ENDPOINT, bundle);
        log.info("Done registering resource bundle: {}", bundle::getName);
        setModifiedNow();
    }

    @Override
    public void unregisterResourceBundle(ResourceBundle bundle) {
        log.info("Removing resource bundle: {}", bundle::getName);
        unregister(RESOURCE_BUNDLES_ENDPOINT, bundle);
        log.info("Done removing resource bundle: {}", bundle::getName);
        setModifiedNow();
    }

    @Override
    public void registerResource(Resource resource) {
        log.info("Register resource: {}", resource::getName);
        register(RESOURCES_ENDPOINT, resource);
        log.info("Done registering resource: {}", resource::getName);
        setModifiedNow();
    }

    @Override
    public void unregisterResource(Resource resource) {
        log.info("Removing resource: {}", resource::getName);
        unregister(RESOURCES_ENDPOINT, resource);
        log.info("Done removing resource: {}", resource::getName);
        setModifiedNow();
    }

}
