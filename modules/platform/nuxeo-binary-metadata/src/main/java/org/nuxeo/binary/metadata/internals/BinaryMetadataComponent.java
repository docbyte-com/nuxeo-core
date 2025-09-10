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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.binary.metadata.internals;

import static org.nuxeo.binary.metadata.api.BinaryMetadataConstants.METADATA_MAPPING_EP;
import static org.nuxeo.binary.metadata.api.BinaryMetadataConstants.METADATA_PROCESSORS_EP;
import static org.nuxeo.binary.metadata.api.BinaryMetadataConstants.METADATA_RULES_EP;

import java.util.Comparator;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.binary.metadata.api.BinaryMetadataConstants;
import org.nuxeo.binary.metadata.api.BinaryMetadataService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.metrics.MetricInvocationHandler;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Binary metadata component which registers all binary metadata contributions.
 *
 * @since 7.1
 */
public class BinaryMetadataComponent extends DefaultComponent {

    private static final Logger log = LogManager.getLogger(BinaryMetadataComponent.class);

    protected BinaryMetadataService metadataService;

    @Override
    public void start(ComponentContext context) {
        var metadataMappings = this.<MetadataMappingDescriptor> getDescriptors(METADATA_MAPPING_EP)
                                   .stream()
                                   .collect(Collectors.toMap(MetadataMappingDescriptor::getId, Function.identity()));
        var metadataProcessors = this.<MetadataProcessorDescriptor> getDescriptors(METADATA_PROCESSORS_EP)
                                     .stream()
                                     .collect(Collectors.toMap(MetadataProcessorDescriptor::getId,
                                             MetadataProcessorDescriptor::getProcessor));
        var metadataRules = this.<MetadataRuleDescriptor> getDescriptors(METADATA_RULES_EP)
                                .stream()
                                .sorted(Comparator.comparing(MetadataRuleDescriptor::getOrder,
                                        Comparator.nullsLast(Comparator.naturalOrder())))
                                .toList();
        metadataService = new BinaryMetadataServiceImpl(metadataMappings, metadataProcessors, metadataRules);
        if (Boolean.parseBoolean(Framework.getProperty(BinaryMetadataConstants.BINARY_METADATA_MONITOR,
                Boolean.toString(log.isTraceEnabled())))) {
            metadataService = MetricInvocationHandler.newProxy(metadataService, BinaryMetadataService.class);
        }
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(BinaryMetadataService.class)) {
            return adapter.cast(metadataService);
        }
        return null;
    }

}
