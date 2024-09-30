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
 *     Nuxeo
 */
package org.nuxeo.runtime.opensearch1;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.nuxeo.common.Environment;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.Descriptor;

/**
 * XMap descriptor for configuring an index
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
@XObject(value = "index")
public class OpenSearchIndexConfig implements Descriptor {

    public static final String DEFAULT_SETTING_FILE = "default-settings.json";

    public static final String DEFAULT_MAPPING_FILE = "default-mapping.json";

    protected static final String WRITE_SUFFIX = "-write";

    @XNode("@name")
    protected String name;

    @XNode("@enabled")
    protected Boolean enabled;

    @XNode("@create")
    protected Boolean create;

    // @since 9.3
    @XNode("@manageAlias")
    protected Boolean manageAlias;

    // @since 9.3
    @XNode("@writeAlias")
    protected String writeAlias;

    @XNodeList(value = "client@id", type = ArrayList.class, componentType = String.class)
    protected List<String> clientIds;

    protected String settingsContent;

    protected String mappingContent;

    // @since 2021.17
    protected List<String> extraMappingContents = new ArrayList<>();

    // @since 2021.17
    @XNode("mapping@append")
    protected boolean mappingAppend;

    @Override
    public String getId() {
        return name;
    }

    /**
     * @return the index name
     */
    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return BooleanUtils.toBooleanDefaultIfNull(enabled, true);
    }

    public boolean mustCreate() {
        return BooleanUtils.toBooleanDefaultIfNull(create, true);
    }

    // @since 9.3
    public boolean manageAlias() {
        return BooleanUtils.toBooleanDefaultIfNull(manageAlias, false);
    }

    // @since 9.3
    public boolean hasExplicitWriteIndex() {
        return StringUtils.isNotBlank(writeAlias);
    }

    // @since 9.3
    public String writeIndexOrAlias() {
        // Custom alias managed outside of Nuxeo
        if (hasExplicitWriteIndex()) {
            return writeAlias;
        }
        // Nuxeo manages the write alias
        if (manageAlias) {
            return name + WRITE_SUFFIX;
        }
        // Simple index
        return name;
    }

    // @since 9.3
    public String newWriteIndexForAlias(String aliasName, String oldIndexName) {
        // TODO make the alias resolver configurable
        // TODO extract that in service that would be used by other indexer?
        // return new IncrementalIndexNameGenerator().getNextIndexName(aliasName, oldIndexName);
        return null;
    }

    public List<String> getClientIds() {
        return Collections.unmodifiableList(clientIds);
    }

    public String getSettingsContent() {
        return Objects.requireNonNullElseGet(settingsContent, () -> contentOfFile(DEFAULT_SETTING_FILE));
    }

    @XNode("settings")
    @SuppressWarnings("unused") // used by X framework when unmarshalling
    protected void setSettings(String settings) {
        // handle the empty element when file tag is used
        if (StringUtils.isNotBlank(settings)) {
            this.settingsContent = settings;
        }
    }

    @XNode("settings@file")
    @SuppressWarnings("unused") // used by X framework when unmarshalling
    protected void setSettingsFile(String settingsFile) {
        this.settingsContent = contentOfFile(settingsFile);
    }

    public String getMappingContent() {
        return Objects.requireNonNullElseGet(mappingContent, () -> contentOfFile(DEFAULT_MAPPING_FILE));
    }

    @XNode("mapping")
    @SuppressWarnings("unused") // used by X framework when unmarshalling
    protected void setMapping(String mapping) {
        // handle the empty element when file tag is used
        if (StringUtils.isNotBlank(mapping)) {
            this.mappingContent = mapping;
        }
    }

    @XNode("mapping@file")
    @SuppressWarnings("unused") // used by X framework when unmarshalling
    protected void setMappingFile(String mappingFile) {
        // TODO review this was the previous behavior, the wrong file name triggered the error when putting mapping
        this.mappingContent = mappingFile.endsWith(".json") ? contentOfFile(mappingFile) : mappingFile;
    }

    public List<String> getExtraMappingContents() {
        return Collections.unmodifiableList(extraMappingContents);
    }

    protected String contentOfFile(String filename) {
        try (InputStream stream = getResourceStream(filename)) {
            return IOUtils.toString(stream, UTF_8);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot load resource file: " + filename, e);
        }
    }

    @SuppressWarnings("resource") // closed by caller
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

        // getResourceAsStream is needed getResource will not work when called from another module
        InputStream ret = this.getClass().getClassLoader().getResourceAsStream(filename);
        if (ret == null) {
            throw new IllegalArgumentException(
                    String.format("Resource file cannot be found: %s or %s", file.getAbsolutePath(), filename));
        }
        return ret;
    }

    /**
     * Use {@code other} mapping and settings if not defined.
     */
    public OpenSearchIndexConfig merge(Descriptor o) {
        var other = (OpenSearchIndexConfig) o;
        var merged = new OpenSearchIndexConfig();
        merged.name = name;
        merged.enabled = ObjectUtils.defaultIfNull(other.enabled, enabled);
        merged.create = ObjectUtils.defaultIfNull(other.create, create);
        merged.manageAlias = ObjectUtils.defaultIfNull(other.manageAlias, manageAlias);
        merged.writeAlias = ObjectUtils.defaultIfNull(other.writeAlias, writeAlias);
        merged.clientIds = ObjectUtils.defaultIfNull(other.clientIds, clientIds);
        merged.settingsContent = ObjectUtils.defaultIfNull(other.settingsContent, settingsContent);
        // propagate extraMappings only if descriptor append mapping
        if (other.mappingAppend) {
            merged.extraMappingContents.addAll(other.extraMappingContents);
            merged.extraMappingContents.addAll(extraMappingContents);
            merged.extraMappingContents.add(other.mappingContent);
            merged.mappingContent = mappingContent;
        } else {
            merged.mappingContent = ObjectUtils.defaultIfNull(other.mappingContent, mappingContent);
        }
        return merged;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toStringInclude(this, "name", "clientIds", "create", "enabled", "manageAlias",
                "writeAlias");
    }
}
