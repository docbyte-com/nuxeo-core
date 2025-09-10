/*
 * (C) Copyright 2018-2024 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.jwt;

import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.ObjectUtils.getIfNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.nuxeo.runtime.model.Descriptor.UNIQUE_DESCRIPTOR_ID;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator.Builder;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The JSON Web Token Service implementation.
 *
 * @since 10.3
 */
public class JWTServiceImpl extends DefaultComponent implements JWTService {

    private static final Logger log = LogManager.getLogger(JWTServiceImpl.class);

    public static final String XP_CONFIGURATION = "configuration";

    public static final String NUXEO_ISSUER = "nuxeo";

    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    protected static final TypeReference<Map<String, Object>> MAP_STRING_OBJECT = new TypeReference<>() {
    };

    protected JWTServiceConfigurationDescriptor serviceConfiguration;

    @Override
    public void start(ComponentContext context) {
        serviceConfiguration = getIfNull(getDescriptor(XP_CONFIGURATION, UNIQUE_DESCRIPTOR_ID),
                JWTServiceConfigurationDescriptor::new);
    }

    @Override
    public void stop(ComponentContext context) {
        serviceConfiguration = null;
    }

    // -------------------- JWTService API --------------------

    @Override
    public JWTBuilder newBuilder() {
        return new JWTBuilderImpl();
    }

    /**
     * Implementation of {@link JWTBuilder} delegating to the auth0 JWT library.
     *
     * @since 10.3
     */
    public class JWTBuilderImpl implements JWTBuilder {

        public final Builder builder;

        public JWTBuilderImpl() {
            builder = JWT.create();
            // default Nuxeo issuer
            builder.withIssuer(NUXEO_ISSUER);
            // default to current principal as subject
            String subject = Optional.ofNullable(NuxeoPrincipal.getCurrent())
                                     .map(NuxeoPrincipal::getActingUser)
                                     .orElseThrow(() -> new NuxeoException("No currently logged-in user"));
            builder.withSubject(subject);
            // default TTL
            withTTL(0);
        }

        @Override
        public JWTBuilderImpl withTTL(int ttlSeconds) {
            if (ttlSeconds <= 0) {
                ttlSeconds = getDefaultTTL();
            }
            builder.withExpiresAt(Date.from(Instant.now().plusSeconds(ttlSeconds)));
            return this;
        }

        @Override
        public JWTBuilderImpl withClaim(String name, Object value) {
            switch (value) {
                case Boolean b -> builder.withClaim(name, b);
                case Date date -> builder.withClaim(name, date);
                case Double v -> builder.withClaim(name, v);
                case Integer i -> builder.withClaim(name, i);
                case Long l -> builder.withClaim(name, l);
                case String s -> builder.withClaim(name, s);
                case Integer[] integers -> builder.withArrayClaim(name, integers);
                case Long[] longs -> builder.withArrayClaim(name, longs);
                case String[] strings -> builder.withArrayClaim(name, strings);
                case null, default -> throw new NuxeoException("Unknown claim type: " + value);
            }
            return this;
        }

        @Override
        public String build() {
            try {
                Algorithm algorithm = getAlgorithm();
                if (algorithm == null) {
                    throw new NuxeoException("JWTService secret not configured");
                }
                return builder.sign(algorithm);
            } catch (JWTCreationException e) {
                throw new NuxeoException(e);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> verifyToken(String token) {
        Objects.requireNonNull(token);
        Algorithm algorithm = getAlgorithm();
        if (algorithm == null) {
            log.debug("secret not configured, cannot verify token");
            return null; // no secret
        }
        JWTVerifier verifier = JWT.require(algorithm).build();
        DecodedJWT jwt;
        try {
            jwt = verifier.verify(token);
        } catch (JWTVerificationException e) {
            log.trace("token verification failed: {}", e::toString);
            return null; // invalid
        }
        Map<String, JsonNode> tree;
        try {
            Object payload = FieldUtils.readField(jwt, "payload", true); // com.auth0.jwt.impl.PayloadImpl
            tree = (Map<String, JsonNode>) FieldUtils.readField(payload, "tree", true);
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException(e);
        }
        return tree.entrySet().stream().collect(toMap(Entry::getKey, e -> nodeToValue(e.getValue())));
    }

    /**
     * Converts a {@link JsonNode} to a Java value.
     */
    protected static Object nodeToValue(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        } else if (node.isObject()) {
            try {
                try (JsonParser parser = OBJECT_MAPPER.treeAsTokens(node)) {
                    return parser.readValueAs(MAP_STRING_OBJECT);
                }
            } catch (IOException e) {
                throw new NuxeoException("Cannot map claim value to Map", e);
            }
        } else if (node.isArray()) {
            List<Object> list = new ArrayList<>();
            for (JsonNode elem : node) {
                try {
                    list.add(OBJECT_MAPPER.treeToValue(elem, Object.class));
                } catch (IOException e) {
                    throw new NuxeoException("Cannot map Claim array value to Object", e);
                }
            }
            return list;
        } else {
            // Jackson doesn't seem to have an easy way to do this, other than checking each possible type
            Object value;
            try {
                value = FieldUtils.readField(node, "_value", true);
            } catch (ReflectiveOperationException e) {
                log.warn("Cannot extract primitive value from JsonNode: {}", () -> node.getClass().getName());
                value = null;
            }
            if (value instanceof Integer) {
                // normalize to Long for caller convenience
                value = Long.valueOf(((Integer) value).longValue());
            }
            return value;
        }
    }

    protected int getDefaultTTL() {
        return serviceConfiguration.getDefaultTTL();
    }

    protected Algorithm getAlgorithm() {
        String secret = serviceConfiguration.getSecret();
        if (isBlank(secret)) {
            return null;
        }
        return Algorithm.HMAC512(secret);
    }

}
