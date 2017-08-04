/*
 * Copyright 2017-present Open Networking Foundation
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

import org.onosproject.net.resource.DiscreteResourceId;
import org.onosproject.net.resource.Resource;
import org.onosproject.net.resource.ResourceConsumerId;
import org.onosproject.net.resource.ResourceId;

import java.util.Optional;
import java.util.Set;

/**
 * Interface for transaction resource substores.
 */
interface TransactionalResourceSubStore<T extends ResourceId, U extends Resource> {

    /**
     * Reads the given resource from the substore.
     *
     * @param resourceId the resource ID
     * @return an optional containing the resource if it exists
     */
    Optional<U> lookup(T resourceId);

    /**
     * Registers the given set of resources for the given parent.
     *
     * @param parent the parent for which to register the resources
     * @param resources the resources to register
     * @return indicates whether the registration was successful
     */
    boolean register(DiscreteResourceId parent, Set<U> resources);

    /**
     * Unregisters the given set of resources for the given parent.
     *
     * @param parent the parent for which to unregister the resources
     * @param resources the resources to register
     * @return indicates whether the unregistration was successful
     */
    boolean unregister(DiscreteResourceId parent, Set<U> resources);

    /**
     * Returns a boolean indicating whether the given resource is allocated.
     *
     * @param resourceId the resource ID
     * @return indicates whether the given resource is allocated
     */
    boolean isAllocated(T resourceId);

    /**
     * Allocates the given resource for the given consumer.
     *
     * @param consumerId the consumer ID
     * @param resource the resource
     * @return indicates whether the allocation was successful
     */
    boolean allocate(ResourceConsumerId consumerId, U resource);

    /**
     * Releases the given resource from the given consumer.
     *
     * @param consumerId the consumer ID
     * @param resource the resource
     * @return indicates whether the release was successful
     */
    boolean release(ResourceConsumerId consumerId, U resource);

}