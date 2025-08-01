/*
 * (C) Copyright 2017-2024 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.runtime.opensearch1.client;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.Descriptor;

/**
 * Descriptor to retrieve connection information to MongoDB.
 *
 * @since 9.1
 */
@XObject("client")
public class OpenSearchClientConfig implements Descriptor {

    @XNode("@id")
    protected String id;

    protected List<String> servers = new ArrayList<>();

    @XNode("embedServer")
    protected String embedServer;

    @XNode("connectionTimeout")
    protected Duration connectionTimeout;

    @XNode("socketTimeout")
    protected Duration socketTimeout;

    @XNode("sslCertificateVerification")
    protected Boolean sslCertificateVerification;

    /**
     * Username for auth to the cluster.
     */
    @XNode("username")
    protected String username;

    /**
     * Password for auth to the cluster.
     */
    @XNode("password")
    protected String password;

    @XNode("trustStore")
    protected Store trustStore;

    @XNode("keyStore")
    protected Store keyStore;

    @XNode("authenticationType")
    protected String authenticationType;

    @XNode("region")
    protected String region;

    @Override
    public String getId() {
        return id;
    }

    public List<String> getServers() {
        return Collections.unmodifiableList(servers);
    }

    @XNodeList(value = "server", type = ArrayList.class, componentType = String.class)
    protected void setServers(List<String> servers) {
        this.servers = new ArrayList<>();
        for (var server : servers) {
            if (StringUtils.contains(server, ',')) {
                this.servers.addAll(List.of(server.split(",")));
            } else {
                this.servers.add(server);
            }
        }
    }

    public Optional<String> getEmbedServer() {
        return Optional.ofNullable(embedServer).filter(StringUtils::isNotBlank);
    }

    public Optional<Duration> getConnectionTimeout() {
        return Optional.ofNullable(connectionTimeout);
    }

    public Optional<Duration> getSocketTimeout() {
        return Optional.ofNullable(socketTimeout);
    }

    public boolean isSslCertificateVerification() {
        // default is true we're interested in the disablement
        return BooleanUtils.isNotFalse(sslCertificateVerification);
    }

    public Optional<String> getUsername() {
        return Optional.ofNullable(username).filter(StringUtils::isNotBlank);
    }

    public Optional<String> getPassword() {
        return Optional.ofNullable(password).filter(StringUtils::isNotBlank);
    }

    public Optional<Store> getTrustStore() {
        return Optional.ofNullable(trustStore).filter(s -> StringUtils.isNotBlank(s.path));
    }

    public Optional<Store> getKeyStore() {
        return Optional.ofNullable(keyStore).filter(s -> StringUtils.isNotBlank(s.path));
    }

    public Optional<String> getAuthenticationType() {
        return Optional.ofNullable(authenticationType).filter(StringUtils::isNotBlank);
    }

    public Optional<String> getRegion() {
        return Optional.ofNullable(region).filter(StringUtils::isNotBlank);
    }

    @Override
    public OpenSearchClientConfig merge(Descriptor o) {
        var other = (OpenSearchClientConfig) o;
        var merged = new OpenSearchClientConfig();
        merged.id = id;
        merged.servers.addAll(other.servers.isEmpty() ? servers : other.servers);
        merged.connectionTimeout = ObjectUtils.firstNonNull(other.connectionTimeout, connectionTimeout);
        merged.socketTimeout = ObjectUtils.firstNonNull(other.socketTimeout, socketTimeout);
        merged.username = ObjectUtils.firstNonNull(other.username, username);
        merged.password = ObjectUtils.firstNonNull(other.password, password);
        merged.trustStore = ObjectUtils.firstNonNull(other.trustStore, trustStore);
        merged.keyStore = ObjectUtils.firstNonNull(other.keyStore, keyStore);
        return merged;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @XObject
    public static class Store {

        @XNode("path")
        protected String path;

        @XNode("password")
        protected String password;

        @XNode("@type")
        protected String type;

        public String getPath() {
            return path;
        }

        public Optional<String> getPassword() {
            return Optional.ofNullable(password).filter(StringUtils::isNotBlank);
        }

        public Optional<String> getType() {
            return Optional.ofNullable(type).filter(StringUtils::isNotBlank);
        }
    }
}
