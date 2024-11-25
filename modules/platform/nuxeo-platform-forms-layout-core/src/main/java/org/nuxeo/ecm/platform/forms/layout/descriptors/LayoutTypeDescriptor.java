/*
 * (C) Copyright 2014-2024 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.forms.layout.descriptors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutTypeConfiguration;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutTypeDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.impl.LayoutTypeDefinitionImpl;
import org.nuxeo.runtime.model.Descriptor;

/**
 * @since 6.0
 */
@XObject("layoutType")
public class LayoutTypeDescriptor implements Descriptor {

    @XNode("@name")
    protected String name;

    @XNodeList(value = "aliases/alias", type = ArrayList.class, componentType = String.class)
    protected List<String> aliases;

    @XNodeMap(value = "templates/template", key = "@mode", type = HashMap.class, componentType = String.class)
    protected Map<String, String> templates = new HashMap<>();

    @XNode("configuration")
    protected LayoutTypeConfigurationDescriptor configuration;

    @XNodeList(value = "categories/category", type = String[].class, componentType = String.class)
    protected String[] categories = new String[0];

    @Override
    public String getId() {
        return name;
    }

    public String getName() {
        return name;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public Map<String, String> getTemplates() {
        return templates;
    }

    public LayoutTypeConfiguration getConfiguration() {
        if (configuration == null) {
            return null;
        }
        return configuration.getLayoutTypeConfiguration();
    }

    public String[] getCategories() {
        return categories;
    }

    public LayoutTypeDefinition getLayoutTypeDefinition() {
        LayoutTypeDefinitionImpl res = new LayoutTypeDefinitionImpl(name, templates, getConfiguration());
        res.setAliases(getAliases());
        return res;
    }

}
