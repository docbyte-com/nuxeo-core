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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.ui.web.auth.service;

import static org.nuxeo.runtime.model.XContextValues.CONTRIBUTING_COMPONENT;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.xmap.annotation.XContext;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.Descriptor;

@XObject("authenticationChain")
public class AuthenticationChainDescriptor implements Descriptor {

    private static final Logger log = LogManager.getLogger(AuthenticationChainDescriptor.class);

    @XNodeList(value = "plugins/plugin", type = ArrayList.class, componentType = String.class)
    protected List<String> pluginsNames;

    @XContext(CONTRIBUTING_COMPONENT)
    protected ComponentInstance contributingComponent;

    @Override
    public String getId() {
        return UNIQUE_DESCRIPTOR_ID;
    }

    public List<String> getPluginsNames() {
        return pluginsNames;
    }

    @Override
    public Descriptor merge(Descriptor other) {
        log.debug("New authentication chain powered by: {}", contributingComponent::getName);
        return Descriptor.super.merge(other);
    }
}
