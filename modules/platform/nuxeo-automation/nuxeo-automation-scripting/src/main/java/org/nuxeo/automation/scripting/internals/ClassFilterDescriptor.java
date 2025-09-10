/*
 * (C) Copyright 2018-2024 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.automation.scripting.internals;

import java.util.HashSet;
import java.util.Set;

import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.Descriptor;

/**
 * Descriptor for classes that may be allowed or denied for use by the scripting engine.
 *
 * @since 10.2
 */
@XObject("classFilter")
public class ClassFilterDescriptor implements Descriptor {

    @XNodeList(value = "allow", type = HashSet.class, componentType = String.class)
    protected Set<String> allow;

    @XNodeList(value = "deny", type = HashSet.class, componentType = String.class)
    protected Set<String> deny;

    @Override
    public String getId() {
        return UNIQUE_DESCRIPTOR_ID;
    }

    public Set<String> getAllowedClassNames() {
        return allow;
    }

    @Override
    public Descriptor merge(Descriptor o) {
        var other = (ClassFilterDescriptor) o;
        var merged = new ClassFilterDescriptor();
        // we only merge allow list because deny is not inherited, in other words we want to allow such behavior:
        // descriptor1 - allow ClassA
        // descriptor2 - allow ClassB / deny ClassA
        // descriptor3 - allow ClassA
        // merged descriptor - allow ClassA,ClassB
        merged.allow = new HashSet<>();
        if (other.deny.contains("*")) {
            // reset the allow list
            merged.allow.addAll(other.allow);
        } else {
            merged.allow.addAll(allow);
            merged.allow.addAll(other.allow);
            other.deny.forEach(merged.allow::remove);
        }
        merged.deny = Set.of();
        return merged;
    }
}
