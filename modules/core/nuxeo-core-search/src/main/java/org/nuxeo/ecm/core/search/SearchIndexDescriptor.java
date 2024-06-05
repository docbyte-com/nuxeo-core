/*
 * (C) Copyright 2024 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.ecm.core.search;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.nuxeo.common.Environment;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.Descriptor;

/**
 * @since 2025.0
 */
@XObject("index")
public class SearchIndexDescriptor implements Descriptor {

    @XNode("@enabled")
    protected boolean isEnabled = true;

    @XNode("@name")
    protected String name;

    @XNode("@default")
    protected boolean isDefault = false;

    @XNode("@repository")
    protected String repository;

    @XNode("@create")
    protected boolean create = true;

    @XNode("settings@file")
    protected String settingsFile;

    @XNode("mapping@file")
    protected String mappingFile;

    @XNode("mapping@append")
    protected Boolean mappingAppend;

    @Override
    public String getId() {
        return name;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public String getRepositoryName() {
        return repository;
    }

    public boolean canCreateIndex() {
        return create;
    }

    public String getMapping() {
        if (mappingFile != null) {
            return contentOfFile(mappingFile);
        }
        return null;
    }

    public String getSettings() {
        if (settingsFile != null) {
            return contentOfFile(settingsFile);
        }
        return null;
    }

    protected String contentOfFile(String filename) {
        try (InputStream stream = getResourceStream(filename)) {
            return IOUtils.toString(stream, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot load resource file: " + filename, e);
        }
    }

    protected InputStream getResourceStream(String filename) {
        // First check if the resource is available on the config directory
        File file = new File(Environment.getDefault().getConfig(), filename);
        if (file.exists()) {
            try {
                return new FileInputStream(file);
            } catch (FileNotFoundException e) {
                // try another way
            }
        }

        // getResourceAsStream is needed, getResource will not work when called from another module
        InputStream ret = this.getClass().getClassLoader().getResourceAsStream(filename);
        if (ret == null) {
            // Then try to get it from jar
            ret = this.getClass().getClassLoader().getResourceAsStream(filename);
        }
        if (ret == null) {
            throw new IllegalArgumentException(
                    String.format("Resource file cannot be found: %s or %s", file.getAbsolutePath(), filename));
        }
        return ret;
    }

}
