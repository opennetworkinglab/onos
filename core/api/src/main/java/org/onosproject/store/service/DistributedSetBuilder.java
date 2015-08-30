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
 * Builder for distributed set.
 *
 * @param <E> type set elements.
 */
public interface DistributedSetBuilder<E> {

    /**
     * Sets the name of the set.
     * <p>
     * Each set is identified by a unique name.
     * </p>
     * <p>
     * Note: This is a mandatory parameter.
     * </p>
     *
     * @param name name of the set
     * @return this DistributedSetBuilder
     */
    DistributedSetBuilder<E> withName(String name);

    /**
     * Sets the owner applicationId for the set.
     * <p>
     * Note: If {@code purgeOnUninstall} option is enabled, applicationId
     * must be specified.
     * </p>
     *
     * @param id applicationId owning the set
     * @return this DistributedSetBuilder
     */
    DistributedSetBuilder<E> withApplicationId(ApplicationId id);

    /**
     * Sets a serializer that can be used to serialize
     * the elements add to the set. The serializer
     * builder should be pre-populated with any classes that will be
     * put into the set.
     * <p>
     * Note: This is a mandatory parameter.
     * </p>
     *
     * @param serializer serializer
     * @return this DistributedSetBuilder
     */
    DistributedSetBuilder<E> withSerializer(Serializer serializer);

    /**
     * Disables set updates.
     * <p>
     * Attempt to update the built set will throw {@code UnsupportedOperationException}.
     *
     * @return this DistributedSetBuilder
     */
    DistributedSetBuilder<E> withUpdatesDisabled();

    /**
     * Provides weak consistency for set reads.
     * <p>
     * While this can lead to improved read performance, it can also make the behavior
     * heard to reason. Only turn this on if you know what you are doing. By default
     * reads are strongly consistent.
     *
     * @return this DistributedSetBuilder
     */
    DistributedSetBuilder<E> withRelaxedReadConsistency();

    /**
     * Disables distribution of set entries across multiple database partitions.
     * <p>
     * When partitioning is disabled, the returned set will have a single partition
     * that spans the entire cluster. Furthermore, the changes made to the set are
     * ephemeral and do not survive a full cluster restart.
     * </p>
     * <p>
     * Disabling partitions is more appropriate when the returned set is used for
     * simple coordination activities and not for long term data persistence.
     * </p>
     * <p>
     * Note: By default partitions are enabled and entries in the set are durable.
     * </p>
     * @return this DistributedSetBuilder
     */
    DistributedSetBuilder<E> withPartitionsDisabled();

    /**
     * Instantiate Metrics service to gather usage and performance metrics.
     * By default usage information is enabled
     * @return this DistributedSetBuilder
     */
    DistributedSetBuilder<E> withMeteringDisabled();

    /**
     * Purges set contents when the application owning the set is uninstalled.
     * <p>
     * When this option is enabled, the caller must provide a applicationId via
     * the {@code withAppliationId} builder method.
     * <p>
     * By default set contents will NOT be purged when owning application is uninstalled.
     *
     * @return this DistributedSetBuilder
     */
    DistributedSetBuilder<E> withPurgeOnUninstall();

    /**
     * Builds an set based on the configuration options
     * supplied to this builder.
     *
     * @return new set
     * @throws java.lang.RuntimeException if a mandatory parameter is missing
     */
    DistributedSet<E> build();
}
