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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core;

import static org.nuxeo.runtime.model.XContextValues.CONTRIBUTING_COMPONENT;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.common.xmap.annotation.XContext;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.impl.OperationTypeImpl;
import org.nuxeo.runtime.model.ComponentInstance;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 */
@XObject("operation")
public class OperationContribution implements OperationDescriptor {

    /**
     * The operation class that must be annotated using {@link Operation} annotation.
     */
    @XNode("@class")
    public String type;

    /**
     * Put it to true to override an existing contribution having the same ID. By default, overriding is not permitted
     * and an exception is thrown when this flag is on false.
     */
    @XNode("@replace")
    public boolean replace;

    @XContext(CONTRIBUTING_COMPONENT)
    protected ComponentInstance contributingComponent;

    /** @since 2025.0 */
    @Override
    public String getId() {
        return StringUtils.defaultIfBlank(getOperationAnnotation().id(), type);
    }

    /** @since 2025.0 */
    @Override
    public boolean isEnabled() {
        return true;
    }

    /** @since 2025.0 */
    @Override
    public boolean replace() {
        return replace;
    }

    /** @since 2025.0 */
    @Override
    public OperationType toType() {
        return new OperationTypeImpl(getId(), getTypeClass(), contributingComponent.getName().getRawName());
    }

    protected Operation getOperationAnnotation() {
        var typeClass = getTypeClass();
        var annotation = typeClass.getAnnotation(Operation.class);
        if (annotation == null) {
            throw new IllegalArgumentException(
                    "Invalid operation class: " + type + ". No @Operation annotation found on class.");
        }
        return annotation;
    }

    protected Class<?> getTypeClass() {
        try {
            return Class.forName(type);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Invalid operation class: " + type + ". Class is not found.");
        }
    }
}
