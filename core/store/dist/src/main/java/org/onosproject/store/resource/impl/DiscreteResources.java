/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.store.resource.impl;

import org.onosproject.net.resource.DiscreteResource;
import org.onosproject.net.resource.DiscreteResourceId;

import java.util.Optional;
import java.util.Set;

/**
 * A common API for a set of discrete resources.
 */
interface DiscreteResources {
    /**
     * Returns an instance representing an empty set.
     *
     * @return a empty set.
     */
    static DiscreteResources empty() {
        return EmptyDiscreteResources.INSTANCE;
    }

    /**
     * Create an instace from the specified resources.
     *
     * @param resources resources
     * @return instance
     */
    static DiscreteResources of(Set<DiscreteResource> resources) {
        return UnifiedDiscreteResources.of(resources);
    }

    /**
     * Look up a discrete resource instance by ID.
     *
     * @param id id
     * @return found instance enclosed by Optional
     */
    Optional<DiscreteResource> lookup(DiscreteResourceId id);

    /**
     * Returns a difference set of this instance and the given instance.
     *
     * @param other other instance
     * @return a new DiscreteResources instance representing a difference set
     */
    DiscreteResources difference(DiscreteResources other);

    /**
     * Checks that this instance is empty.
     *
     * @return true if this instance is empty, otherwise false.
     */
    boolean isEmpty();

    /**
     * Checks that this instance contains any of the given resources.
     *
     * @param other resources
     * @return true this instance contains a resource included in the given resources,
     * otherwise false.
     */
    boolean containsAny(Set<DiscreteResource> other);

    /**
     * Returns a union set of this instance and the given instance.
     * Note: This method returns a new instance, not mutate the current instance
     *
     * @param other other instance
     * @return a new DiscreteResources instance representing a union set
     */
    DiscreteResources add(DiscreteResources other);

    /**
     * Returns all of resources this instance holds.
     *
     * @return all resources
     */
    Set<DiscreteResource> values();

    /**
     * Returns all of resources this instance holds and filtered by the specified type.
     *
     * @param cls class instance of the resource value
     * @param <T> type of the resource value
     * @return all of resources this instance holds and filtered by the specified type
     */
    <T> Set<DiscreteResource> valuesOf(Class<T> cls);
}
