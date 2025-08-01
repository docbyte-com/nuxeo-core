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
 *     Thierry Delprat <tdelprat@nuxeo.com>
 */
package org.nuxeo.automation.scripting.internals;

import static org.nuxeo.runtime.model.XContextValues.CONTRIBUTING_COMPONENT;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.nuxeo.common.xmap.annotation.XContext;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.automation.OperationDocumentation;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.core.OperationDescriptor;
import org.nuxeo.runtime.model.ComponentInstance;

/**
 * @since 7.2
 */
@XObject("scriptedOperation")
public class ScriptingOperationDescriptor implements OperationDescriptor {

    @XNode("@id")
    protected String id;

    @XNode("@enabled")
    protected boolean enabled = true;

    @XNode("inputType")
    protected String inputType;

    @XNode("outputType")
    protected String outputType;

    @XNode("description")
    protected String description;

    @XNode("category")
    protected String category;

    @XNodeList(value = "aliases/alias", type = String[].class, componentType = String.class)
    protected String[] aliases;

    @XNodeList(value = "param", type = OperationDocumentation.Param[].class, componentType = OperationDocumentation.Param.class)
    protected OperationDocumentation.Param[] params = new OperationDocumentation.Param[0];

    @XNode("script")
    protected String source;

    /** @since 11.1 */
    @XContext(CONTRIBUTING_COMPONENT)
    protected ComponentInstance contributingComponent;

    @Override
    public String getId() {
        return id;
    }

    public String[] getAliases() {
        return aliases;
    }

    public String getInputType() {
        return inputType;
    }

    public String getOutputType() {
        return outputType;
    }

    /** @since 2021.17 */
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /** @since 2025.0 */
    @Override
    public boolean replace() {
        return true;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public OperationDocumentation.Param[] getParams() {
        return params;
    }

    /** @since 11.1 */
    public String getContributingComponent() {
        return contributingComponent.getName().getRawName();
    }

    /** @since 2025.0 */
    @Override
    public OperationType toType() {
        return new ScriptingOperationTypeImpl(this);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("id", id).append("enabled", enabled).toString();
    }
}
