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
 *      Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.context;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.model.Descriptor;

/**
 * @since 7.3
 */
@XObject("contextHelper")
public class ContextHelperDescriptor implements Descriptor {

    private static final Logger log = LogManager.getLogger(ContextHelperDescriptor.class);

    @XNode("@id")
    protected String id;

    @XNode("@class")
    protected Class<? extends ContextHelper> contextHelperClass;

    @XNode("@enabled")
    protected boolean enabled = true;

    @Override
    public String getId() {
        return id;
    }

    public ContextHelper instantiateContextHelper() {
        try {
            return contextHelperClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException("Unable to instantiate the context helper: " + id, e);
        }
    }

    /** @since 2025.0 */
    public boolean isEnabled() {
        return enabled;
    }

    /** @since 2025.0 */
    @Override
    public Descriptor merge(Descriptor o) {
        var other = (ContextHelperDescriptor) o;
        // there's no merge on this Descriptor, but there was a log in the former registry that can fit here
        log.warn("The context helper: {} with id: {} is overriding the helper: {}", other.contextHelperClass, id,
                contextHelperClass);
        return other;
    }
}
