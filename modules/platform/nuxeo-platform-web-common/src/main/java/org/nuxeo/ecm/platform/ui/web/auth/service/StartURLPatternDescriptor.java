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

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.Descriptor;

@XObject("startURLPattern")
public class StartURLPatternDescriptor implements Descriptor {

    @XNodeList(value = "patterns/pattern", type = ArrayList.class, componentType = String.class)
    protected List<String> startURLPatterns;

    @Override
    public String getId() {
        return UNIQUE_DESCRIPTOR_ID;
    }

    public List<String> getStartURLPatterns() {
        return startURLPatterns;
    }

    @Override
    public Descriptor merge(Descriptor o) {
        var other = (StartURLPatternDescriptor) o;
        var merged = new StartURLPatternDescriptor();
        merged.startURLPatterns = new ArrayList<>(emptyIfNull(startURLPatterns));
        merged.startURLPatterns.addAll(emptyIfNull(other.startURLPatterns));
        return merged;
    }
}
