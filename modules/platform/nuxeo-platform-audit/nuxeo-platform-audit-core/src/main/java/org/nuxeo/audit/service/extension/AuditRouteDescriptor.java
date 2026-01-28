/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.audit.service.extension;

import static org.apache.commons.lang3.BooleanUtils.toBooleanDefaultIfNull;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.Descriptor;

/**
 * @since 2025.16
 */
@XObject("route")
public class AuditRouteDescriptor implements Descriptor {

    @XNode("@name")
    protected String name;

    @XNode("backend@name")
    protected String backendName;

    @XNodeList(value = "event", type = ArrayList.class, componentType = EventDescriptor.class)
    protected List<EventDescriptor> events = new ArrayList<>();

    @Override
    public String getId() {
        return name;
    }

    public String getName() {
        return name;
    }

    public String getBackendName() {
        return backendName;
    }

    public List<EventDescriptor> getEvents() {
        return Collections.unmodifiableList(events);
    }

    /** @since 2025.16 */
    public Stream<EventDescriptor> streamEvents() {
        return events.stream();
    }

    @Override
    public AuditRouteDescriptor merge(Descriptor o) {
        var other = (AuditRouteDescriptor) o;
        var merged = new AuditRouteDescriptor();
        merged.name = name; // we merge based on name, so no name merging needed
        merged.backendName = defaultIfBlank(other.backendName, backendName);
        merged.events = merge(events, other.events);
        return merged;
    }

    @SuppressWarnings("unchecked")
    protected <D extends Descriptor> List<D> merge(List<D> first, List<D> second) {
        var map = new HashMap<String, D>();
        first.forEach(descriptor -> map.put(descriptor.getId(), descriptor));
        second.forEach(descriptor -> map.merge(descriptor.getId(), descriptor,
                (previous, current) -> (D) previous.merge(current)));
        return new ArrayList<>(map.values());
    }

    @XObject("event")
    public static class EventDescriptor implements Descriptor {

        @XNode("@name")
        protected String name;

        @XNode("@enabled")
        protected Boolean enabled;

        @Override
        public String getId() {
            return name;
        }

        public String getName() {
            return name;
        }

        public boolean isEnabled() {
            return toBooleanDefaultIfNull(enabled, true);
        }

        @Override
        public EventDescriptor merge(Descriptor o) {
            var other = (EventDescriptor) o;
            var merged = new EventDescriptor();
            merged.name = name; // we merge based on name, so no name merging needed
            merged.enabled = firstNonNull(other.enabled, enabled);
            return merged;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof EventDescriptor other)) {
                return false;
            }
            return Objects.equals(name, other.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }
    }
}
