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

/**
 * Builder for constructing new AtomicValue instances.
 *
 * @param <V> atomic value type
 */
public interface AtomicValueBuilder<V> {
    /**
     * Sets the name for the atomic value.
     * <p>
     * Each atomic value is identified by a unique name.
     * </p>
     * <p>
     * Note: This is a mandatory parameter.
     * </p>
     *
     * @param name name of the atomic value
     * @return this AtomicValueBuilder for method chaining
     */
    AtomicValueBuilder<V> withName(String name);

    /**
     * Sets a serializer that can be used to serialize the value.
     * <p>
     * Note: This is a mandatory parameter.
     * </p>
     *
     * @param serializer serializer
     * @return this AtomicValueBuilder for method chaining
     */
    AtomicValueBuilder<V> withSerializer(Serializer serializer);

    /**
     * Creates this atomic value on the partition that spans the entire cluster.
     * <p>
     * When partitioning is disabled, the value state will be
     * ephemeral and does not survive a full cluster restart.
     * </p>
     * <p>
     * Note: By default partitions are enabled.
     * </p>
     * @return this AtomicValueBuilder for method chaining
     */
    AtomicValueBuilder<V> withPartitionsDisabled();

    /**
     * Instantiates Metering service to gather usage and performance metrics.
     * By default, usage data will be stored.
     *
     * @return this AtomicValueBuilder for method chaining
     */
    AtomicValueBuilder<V> withMeteringDisabled();

    /**
     * Builds a AtomicValue based on the configuration options
     * supplied to this builder.
     *
     * @return new AtomicValue
     * @throws java.lang.RuntimeException if a mandatory parameter is missing
     */
    AtomicValue<V> build();
}
