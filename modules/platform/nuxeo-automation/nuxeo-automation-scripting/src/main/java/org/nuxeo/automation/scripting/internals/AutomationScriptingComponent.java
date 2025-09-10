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
 *     Stephane Lacoin <slacoin@nuxeo.com>
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.automation.scripting.internals;

import static org.nuxeo.ecm.automation.core.AutomationComponent.AUTOMATION_COMPONENT_NAME;
import static org.nuxeo.ecm.automation.core.AutomationComponent.XP_INTERNAL_OPERATIONS;
import static org.nuxeo.runtime.model.Descriptor.UNIQUE_DESCRIPTOR_ID;

import java.util.Optional;
import java.util.Set;

import org.nuxeo.automation.scripting.api.AutomationScriptingService;
import org.nuxeo.ecm.automation.core.AutomationComponent;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @since 7.2
 */
public class AutomationScriptingComponent extends DefaultComponent {

    protected static final String XP_OPERATION = "operation";

    protected static final String XP_CLASSFILTER = "classFilter";

    protected AutomationScriptingServiceImpl service;

    @Override
    public void start(ComponentContext context) {
        var allowedClassNames = Optional.ofNullable(
                this.<ClassFilterDescriptor> getDescriptor(XP_CLASSFILTER, UNIQUE_DESCRIPTOR_ID))
                                        .map(ClassFilterDescriptor::getAllowedClassNames)
                                        .orElseGet(Set::of);
        service = new AutomationScriptingServiceImpl(allowedClassNames);
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (XP_OPERATION.equals(extensionPoint)) {
            // also register the contribution to AutomationComponent
            getAutomationComponent().registerContribution(contribution, XP_INTERNAL_OPERATIONS, contributor);
        }
        super.registerContribution(contribution, extensionPoint, contributor);
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (XP_OPERATION.equals(extensionPoint)) {
            // also unregister the contribution to AutomationComponent
            getAutomationComponent().unregisterContribution(contribution, XP_INTERNAL_OPERATIONS, contributor);
        }
        super.unregisterContribution(contribution, extensionPoint, contributor);
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(AutomationScriptingService.class)) {
            return adapter.cast(service);
        }
        return super.getAdapter(adapter);
    }

    protected static AutomationComponent getAutomationComponent() {
        return (AutomationComponent) Framework.getRuntime().getComponent(AUTOMATION_COMPONENT_NAME);
    }
}
