/*
 * (C) Copyright 2007-2024 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.audit.service.extension;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.Descriptor;

/**
 * Extended info descriptor
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@XObject("extendedInfo")
public class ExtendedInfoDescriptor implements Descriptor {

    @XNode("@key")
    private String key;

    @XNode("@expression")
    private String expression;

    @XNode("@enabled")
    private Boolean enabled;

    @Override
    public String getId() {
        return key;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String value) {
        key = value;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String value) {
        expression = value;
    }

    public boolean isEnabled() {
        return BooleanUtils.toBooleanDefaultIfNull(enabled, true);
    }

    /** @deprecated since 2025.0, use {@link ExtendedInfoDescriptor#isEnabled()} instead */
    @Deprecated(since = "2025.0", forRemoval = true)
    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public int hashCode() {
        return key == null ? 0 : key.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ExtendedInfoDescriptor other = (ExtendedInfoDescriptor) obj;
        if (key == null) {
            if (other.key != null) {
                return false;
            }
        } else if (!key.equals(other.key)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public ExtendedInfoDescriptor merge(Descriptor o) {
        var other = (ExtendedInfoDescriptor) o;
        var merged = new ExtendedInfoDescriptor();
        merged.key = key;
        merged.expression = StringUtils.defaultIfBlank(other.expression, expression);
        merged.enabled = other.enabled == null ? enabled : other.enabled;
        return merged;
    }
}
