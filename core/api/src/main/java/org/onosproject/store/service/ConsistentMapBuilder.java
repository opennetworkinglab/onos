/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.store.service;

import org.onosproject.core.ApplicationId;

/**
 * Builder for consistent maps.
 *
 * @param <K> type for map key
 * @param <V> type for map value
 */
public interface ConsistentMapBuilder<K, V> {

    /**
     * Sets the name of the map.
     * <p>
     * Each consistent map is identified by a unique map name.
     * </p>
     * <p>
     * Note: This is a mandatory parameter.
     * </p>
     *
     * @param name name of the consistent map
     * @return this ConsistentMapBuilder
     */
    ConsistentMapBuilder<K, V> withName(String name);

    /**
     * Sets the owner applicationId for the map.
     * <p>
     * Note: If {@code purgeOnUninstall} option is enabled, applicationId
     * must be specified.
     * </p>
     *
     * @param id applicationId owning the consistent map
     * @return this ConsistentMapBuilder
     */
    ConsistentMapBuilder<K, V> withApplicationId(ApplicationId id);

    /**
     * Sets a serializer that can be used to serialize
     * both the keys and values inserted into the map. The serializer
     * builder should be pre-populated with any classes that will be
     * put into the map.
     * <p>
     * Note: This is a mandatory parameter.
     * </p>
     *
     * @param serializer serializer
     * @return this ConsistentMapBuilder
     */
    ConsistentMapBuilder<K, V> withSerializer(Serializer serializer);

    /**
     * Disables distribution of map entries across multiple database partitions.
     * <p>
     * When partitioning is disabled, the returned map will have a single partition
     * that spans the entire cluster. Furthermore, the changes made to the map are
     * ephemeral and do not survive a full cluster restart.
     * </p>
     * <p>
     * Disabling partitions is more appropriate when the returned map is used for
     * coordination activities such as leader election and not for long term data persistence.
     * </p>
     * <p>
     * Note: By default partitions are enabled and entries in the map are durable.
     * </p>
     * @return this ConsistentMapBuilder
     */
    ConsistentMapBuilder<K, V> withPartitionsDisabled();

    /**
     * Disables map updates.
     * <p>
     * Attempt to update the built map will throw {@code UnsupportedOperationException}.
     *
     * @return this ConsistentMapBuilder
     */
    ConsistentMapBuilder<K, V> withUpdatesDisabled();

    /**
     * Purges map contents when the application owning the map is uninstalled.
     * <p>
     * When this option is enabled, the caller must provide a applicationId via
     * the {@code withAppliationId} builder method.
     * <p>
     * By default map entries will NOT be purged when owning application is uninstalled.
     *
     * @return this ConsistentMapBuilder
     */
    ConsistentMapBuilder<K, V> withPurgeOnUninstall();

    /**
     * Instantiates Metering service to gather usage and performance metrics.
     * By default, usage data will be stored.
     *
     * @return this ConsistentMapBuilder
     */
    ConsistentMapBuilder<K, V> withMeteringDisabled();

    /**
     * Provides weak consistency for map gets.
     * <p>
     * While this can lead to improved read performance, it can also make the behavior
     * heard to reason. Only turn this on if you know what you are doing. By default
     * reads are strongly consistent.
     *
     * @return this ConsistentMapBuilder
     */
    ConsistentMapBuilder<K, V> withRelaxedReadConsistency();

    /**
     * Builds an consistent map based on the configuration options
     * supplied to this builder.
     *
     * @return new consistent map
     * @throws java.lang.RuntimeException if a mandatory parameter is missing
     */
    ConsistentMap<K, V> build();

    /**
     * Builds an async consistent map based on the configuration options
     * supplied to this builder.
     *
     * @return new async consistent map
     * @throws java.lang.RuntimeException if a mandatory parameter is missing
     */
    AsyncConsistentMap<K, V> buildAsyncMap();
}
