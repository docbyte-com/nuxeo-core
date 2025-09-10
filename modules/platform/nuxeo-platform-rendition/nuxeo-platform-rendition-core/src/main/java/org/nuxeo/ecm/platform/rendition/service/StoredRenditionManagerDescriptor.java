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
 */
package org.nuxeo.ecm.platform.rendition.service;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.Descriptor;

/**
 * Descriptor for a stored rendition manager.
 *
 * @since 8.1
 */
@XObject("storedRenditionManager")
public class StoredRenditionManagerDescriptor implements Descriptor {

    @XNode("@class")
    protected Class<StoredRenditionManager> clazz;

    protected StoredRenditionManager instance;

    @Override
    public String getId() {
        return clazz.getName();
    }

    protected synchronized StoredRenditionManager getStoredRenditionManager() {
        if (instance == null) {
            try {
                instance = clazz.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Cannot create StoredRenditionManager", e);
            }
        }
        return instance;
    }

}
