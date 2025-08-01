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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.web.resources.core;

import static org.nuxeo.runtime.model.XContextValues.CONTRIBUTING_COMPONENT;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.xmap.annotation.XContext;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.web.resources.api.Resource;
import org.nuxeo.runtime.model.ComponentInstance;

/**
 * @since 7.3
 */
@XObject("resource")
public class ResourceDescriptor implements Resource {

    private static final Logger log = LogManager.getLogger(ResourceDescriptor.class);

    @XNode("@name")
    public String name;

    @XNode("@type")
    public String type;

    /**
     * Target for this resource.
     * <p>
     * Currently only useful for JSF resources reallocation in the page.
     *
     * @since 7.10
     */
    @XNode("@target")
    public String target;

    @XNode("path")
    public String path;

    @XNodeList(value = "require", type = ArrayList.class, componentType = String.class)
    public List<String> dependencies;

    @XNodeList(value = "processors/processor", type = ArrayList.class, componentType = String.class)
    public List<String> processors;

    @XNode("shrinkable")
    public boolean shrinkable = true;

    @XNode("uri")
    protected volatile String uri;

    @XContext(CONTRIBUTING_COMPONENT)
    protected ComponentInstance contributor;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getType() {
        if (StringUtils.isBlank(type)) {
            // try to infer it from name for easier declaration
            return FileUtils.getFileExtension(name);
        }
        return type;
    }

    @Override
    public List<String> getDependencies() {
        return dependencies;
    }

    @Override
    public List<String> getProcessors() {
        return processors;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getURI() {
        if (uri == null) {
            synchronized (this) {
                if (uri == null) {
                    // build it from local classpath
                    // XXX: hacky wildcard support
                    if (path != null) {
                        boolean hasWildcard = false;
                        if (path.endsWith("*")) {
                            hasWildcard = true;
                            path = path.substring(0, path.length() - 1);
                        }
                        URL url = contributor.getContext().getLocalResource(path);
                        if (url == null) {
                            log.error("Cannot resolve local URL for resource: {} with path: {}", name, path);
                        } else {
                            String builtUri = url.toString();
                            if (hasWildcard) {
                                builtUri += "*";
                            }
                            uri = builtUri;
                        }
                    }
                }
            }
        }
        return uri;
    }

    @Override
    public boolean isShrinkable() {
        return shrinkable;
    }

    public void setURI(String uri) {
        this.uri = uri;
    }

    /**
     * @since 7.4
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @since 7.4
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @since 7.4
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * @since 7.4
     */
    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
    }

    /**
     * @since 7.4
     */
    public void setProcessors(List<String> processors) {
        this.processors = processors;
    }

    /**
     * @since 7.4
     */
    public void setShrinkable(boolean shrinkable) {
        this.shrinkable = shrinkable;
    }

    /**
     * @since 7.4
     */
    public void setUri(String uri) {
        this.uri = uri;
    }

    /**
     * @since 7.10
     */
    @Override
    public String getTarget() {
        return target;
    }

    /**
     * @since 7.10
     */
    public void setTarget(String target) {
        this.target = target;
    }

}
