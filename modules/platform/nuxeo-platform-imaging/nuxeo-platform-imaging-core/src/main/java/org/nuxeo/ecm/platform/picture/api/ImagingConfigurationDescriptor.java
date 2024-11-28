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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.picture.api;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.Descriptor;

/**
 * Descriptor for configuration information contribution that will be used by the ImagingService.
 *
 * @author btatar
 */
@XObject(value = "configuration")
public class ImagingConfigurationDescriptor implements Descriptor {

    @XNodeMap(value = "parameters/parameter", key = "@name", type = HashMap.class, componentType = String.class)
    protected Map<String, String> parameters = new HashMap<>();

    @Override
    public String getId() {
        return UNIQUE_DESCRIPTOR_ID;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    @Override
    public Descriptor merge(Descriptor o) {
        var other = (ImagingConfigurationDescriptor) o;
        var merged = new ImagingConfigurationDescriptor();
        merged.parameters.putAll(parameters);
        merged.parameters.putAll(other.parameters);
        return merged;
    }
}
