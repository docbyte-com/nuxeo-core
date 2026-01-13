/*
 * (C) Copyright 2017-2025 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.runtime.model;

import org.apache.commons.lang3.math.NumberUtils;

/**
 * Some default application start orders.
 *
 * @since 9.3
 */
public class ComponentStartOrders {

    /** @since 11.5 */
    public static final int CAPABILITIES = -2000;

    /** @since 2023.0 */
    public static final int CLUSTER_SERVICE = -1000;

    /** @since 2025.0 */
    public static final int KAFKA = -600;

    /** @since 2025.0 */
    public static final int STREAM = KAFKA + 10;

    /** @since 2025.0 */
    public static final int EVENT = -500;

    /** @since 2025.0 */
    public static final int KV = -500;

    /** @since 2025.8 */
    public static final int TRANSIENT_DATA_STORE = -500;

    /**
     * Let RedisComponent start before us (Redis starts before WorkManager that starts before events).
     * 
     * @since 2023.0
     */
    public static final int PUB_SUB = -500 + 10;

    /** @since 2023.0 */
    public static final int CLUSTER_ACTIONS = Math.min(CLUSTER_SERVICE, PUB_SUB) + 10;

    /**
     * Even before repository init.
     *
     * @since 2025.0
     */
    public static final int JPA = 50;

    // @since 2021.14
    public static final int MONGODB = 40;

    /** @since 2025.0 */
    public static final int OPENSEARCH = 50;

    // @since 2021.14
    public static final int ELASTIC = OPENSEARCH + 10;

    /**
     * Sequencer should start after any of its implementation.
     * 
     * @since 2025.0
     */
    public static final int SEQUENCER = NumberUtils.max(KV, JPA, MONGODB, OPENSEARCH) + 10;

    /**
     * Audit should start after Sequencer because some implementation uses them.
     *
     * @since 2025.0
     */
    public static final int AUDIT = SEQUENCER + 10;

    public static final int REPOSITORY = 100;

    /**
     * @since 2025.0
     */
    public static final int SEARCH = REPOSITORY + 10;

    /** @since 2025.0 */
    public static final int PAGE_PROVIDER = 800;

    public static final int DEFAULT = 1000;
}
