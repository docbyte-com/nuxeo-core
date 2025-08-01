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
 *     bdelbosc
 */
package org.nuxeo.runtime.opensearch1.embed;

import java.util.Optional;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.Descriptor;

/**
 * Configuration of an embedded (same JVM as Nuxeo) OpenSearch server.
 */
@XObject(value = "server")
public class OpenSearchEmbedServerConfig implements Descriptor {

    @XNode("@name")
    protected String name;

    @XNode("@enabled")
    protected Boolean enabled;

    @XNode("nodeName")
    protected String nodeName;

    // @since 8.4
    @XNode("homePath")
    protected String homePath;

    @XNode("dataPath")
    protected String dataPath;

    @XNode("http@port")
    protected String httpPort;

    @XNode("@networkHost")
    protected String networkHost;

    // @since 8.3
    @XNode("@useExternalVersion")
    protected boolean externalVersion = true;

    @Override
    public String getId() {
        return name;
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return BooleanUtils.toBooleanDefaultIfNull(enabled, true);
    }

    public String getNodeName() {
        return StringUtils.defaultIfBlank(nodeName, "Nuxeo");
    }

    /**
     * @since 8.4
     */
    public Optional<String> getHomePath() {
        return Optional.ofNullable(homePath).filter(StringUtils::isNotBlank);
    }

    public Optional<String> getDataPath() {
        return Optional.ofNullable(dataPath).filter(StringUtils::isNotBlank);
    }

    public String getHttpPort() {
        return StringUtils.defaultIfBlank(httpPort, "9200-9300");
    }

    /**
     * @since 7.4
     */
    public String getNetworkHost() {
        return StringUtils.defaultIfBlank(networkHost, "127.0.0.1");
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
