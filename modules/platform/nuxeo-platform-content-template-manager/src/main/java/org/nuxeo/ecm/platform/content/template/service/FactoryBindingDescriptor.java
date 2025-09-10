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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.content.template.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.Descriptor;

/**
 * Factory binding descriptor. Immutable.
 */
@XObject(value = "factoryBinding")
public class FactoryBindingDescriptor implements Descriptor {

    private static final Logger log = LogManager.getLogger(FactoryBindingDescriptor.class);

    @XNode("@name")
    protected String name;

    /** @since 2021.16 */
    @XNode("@enabled")
    protected boolean enabled = true;

    @XNode("@factoryName")
    protected String factoryName;

    @XNode("@targetType")
    protected String targetType;

    @XNode("@targetFacet")
    protected String targetFacet;

    @XNode("@append")
    protected Boolean append = Boolean.FALSE;

    @XNodeMap(value = "option", key = "@name", type = HashMap.class, componentType = String.class)
    protected Map<String, String> options;

    @XNodeList(value = "template/templateItem", type = ArrayList.class, componentType = TemplateItemDescriptor.class)
    protected List<TemplateItemDescriptor> template;

    // Declared as ArrayList to be serializable.
    @XNodeList(value = "acl/ace", type = ArrayList.class, componentType = ACEDescriptor.class)
    protected List<ACEDescriptor> rootAcl;

    public FactoryBindingDescriptor() {
        // default constructor
        this.options = new HashMap<>();
        this.template = new ArrayList<>();
        this.rootAcl = new ArrayList<>();
    }

    @Override
    public String getId() {
        return ObjectUtils.defaultIfNull(targetType, targetFacet);
    }

    /** @since 2021.16 */
    public boolean isEnabled() {
        return enabled;
    }

    public String getFactoryName() {
        return factoryName;
    }

    protected void setFactoryName(String factoryName) {
        this.factoryName = factoryName;
    }

    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getOptions() {
        return options;
    }

    public String getTargetType() {
        return targetType;
    }

    protected void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getTargetFacet() {
        return targetFacet;
    }

    protected void setTargetFacet(String targetFacet) {
        this.targetFacet = targetFacet;
    }

    public List<TemplateItemDescriptor> getTemplate() {
        return template;
    }

    public List<ACEDescriptor> getRootAcl() {
        return rootAcl;
    }

    public Boolean getAppend() {
        return append;
    }

    public void setAppend(Boolean append) {
        this.append = append;
    }

    @Override
    public Descriptor merge(Descriptor o) {
        var other = (FactoryBindingDescriptor) o;
        var merged = new FactoryBindingDescriptor();
        merged.enabled = other.enabled;
        if (Boolean.TRUE.equals(other.getAppend())) {
            log.info("FactoryBinding: {} is merging with: {}", name, other.getName());
            merged.factoryName = factoryName;
            merged.name = name;
            merged.targetType = targetType;
            merged.targetFacet = targetFacet;
            merged.options.putAll(options);
            merged.rootAcl.addAll(rootAcl);
            merged.template.addAll(template);
        } else {
            // this needs to be overridden by src
            merged.factoryName = other.factoryName;
            merged.name = other.name;
            merged.targetType = other.targetType;
            merged.targetFacet = other.targetFacet;
        }
        merged.options.putAll(other.options);
        merged.rootAcl.addAll(other.rootAcl);
        merged.template.addAll(other.template);
        return merged;
    }
}
