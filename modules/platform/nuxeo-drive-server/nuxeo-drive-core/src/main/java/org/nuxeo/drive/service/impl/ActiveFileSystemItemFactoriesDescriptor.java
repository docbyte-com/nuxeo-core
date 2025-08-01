/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.service.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.runtime.model.Descriptor;

/**
 * XMap descriptor for the {@code activeFileSystemItemFactories} contributions to the
 * {@code activeFileSystemItemFactories} extension point of the {@link FileSystemItemAdapterService}.
 *
 * @author Antoine Taillefer
 */
@XObject("activeFileSystemItemFactories")
public class ActiveFileSystemItemFactoriesDescriptor implements Serializable, Descriptor {

    private static final Logger log = LogManager.getLogger(ActiveFileSystemItemFactoriesDescriptor.class);

    private static final long serialVersionUID = 1L;

    @XNode("@merge")
    protected boolean merge = false;

    @XNodeList(value = "factories/factory", type = ArrayList.class, componentType = ActiveFileSystemItemFactoryDescriptor.class)
    protected List<ActiveFileSystemItemFactoryDescriptor> factories;

    @Override
    public String getId() {
        return UNIQUE_DESCRIPTOR_ID;
    }

    public boolean isMerge() {
        return merge;
    }

    /**
     * @deprecated since 2025.0 unused
     */
    @Deprecated(since = "2025.0")
    public void setMerge(boolean merge) {
        this.merge = merge;
    }

    public List<ActiveFileSystemItemFactoryDescriptor> getFactories() {
        return factories;
    }

    public void setFactories(List<ActiveFileSystemItemFactoryDescriptor> factories) {
        this.factories = factories;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<merge = ");
        sb.append(merge);
        sb.append(", [");
        for (ActiveFileSystemItemFactoryDescriptor factory : factories) {
            sb.append(factory);
            sb.append(", ");
        }
        sb.append("]>");
        return sb.toString();
    }

    @Override
    public ActiveFileSystemItemFactoriesDescriptor merge(Descriptor o) {
        var other = (ActiveFileSystemItemFactoriesDescriptor) o;
        var merged = new ActiveFileSystemItemFactoriesDescriptor();
        merged.merge = other.merge;
        merged.factories = factories.stream().map(SerializationUtils::clone).collect(Collectors.toList());
        if (other.isMerge()) {
            // Merge active factories
            for (ActiveFileSystemItemFactoryDescriptor factory : other.getFactories()) {
                var existing = merged.factories.stream().filter(f -> f.getName().equals(factory.getName())).toList();
                if (!existing.isEmpty() && !factory.isEnabled()) {
                    log.trace("Removing factory {} from active factories.", factory::getName);
                    existing.getFirst().setEnabled(factory.isEnabled());
                }
                if (existing.isEmpty() && factory.isEnabled()) {
                    log.trace("Adding factory {} to active factories.", factory::getName);
                    merged.factories.add(SerializationUtils.clone(factory));
                }
            }
        } else {
            // No merge, reset active factories
            log.trace("Clearing active factories as contribution {} doesn't merge.", other);
            merged.factories.clear();
            for (ActiveFileSystemItemFactoryDescriptor factory : other.getFactories()) {
                if (factory.isEnabled()) {
                    log.trace("Adding factory {} to active factories.", factory::getName);
                    merged.factories.add(SerializationUtils.clone(factory));
                }
            }
        }
        return merged;
    }
}
