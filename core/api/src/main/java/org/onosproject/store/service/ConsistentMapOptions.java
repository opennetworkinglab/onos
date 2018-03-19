/*
 * Copyright 2018-present Open Networking Foundation
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

import java.util.function.BiFunction;

import org.onosproject.store.primitives.DistributedPrimitiveOptions;

/**
 * Builder for {@link ConsistentMap} instances.
 *
 * @param <K> type for map key
 * @param <V> type for map value
 */
public abstract class ConsistentMapOptions<O extends ConsistentMapOptions<O, K, V>, K, V>
    extends DistributedPrimitiveOptions<O> {

    private boolean nullValues = false;
    private boolean purgeOnUninstall = false;
    protected BiFunction<V, org.onosproject.core.Version, V> compatibilityFunction;

    public ConsistentMapOptions() {
        super(DistributedPrimitive.Type.CONSISTENT_MAP);
    }

    /**
     * Enables null values in the map.
     *
     * @return this builder
     */
    public O withNullValues() {
        nullValues = true;
        return (O) this;
    }

    /**
     * Clears map contents when the owning application is uninstalled.
     *
     * @return this builder
     */
    public O withPurgeOnUninstall() {
        purgeOnUninstall = true;
        return (O) this;
    }

    /**
     * Sets a compatibility function on the map.
     *
     * @param compatibilityFunction the compatibility function
     * @return the consistent map builder
     */
    @SuppressWarnings("unchecked")
    public O withCompatibilityFunction(
        BiFunction<V, org.onosproject.core.Version, V> compatibilityFunction) {
        this.compatibilityFunction = compatibilityFunction;
        return (O) this;
    }

    /**
     * Returns whether null values are supported by the map.
     *
     * @return {@code true} if null values are supported; {@code false} otherwise
     */
    public boolean nullValues() {
        return nullValues;
    }

    /**
     * Returns if map entries need to be cleared when owning application is uninstalled.
     * @return {@code true} if yes; {@code false} otherwise.
     */
    public boolean purgeOnUninstall() {
        return purgeOnUninstall;
    }

}
