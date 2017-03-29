/*
 * Copyright 2017-present Open Networking Laboratory
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
import org.onosproject.net.resource.ResourceAllocation;
import org.onosproject.net.resource.ResourceConsumerId;
import org.onosproject.net.resource.ResourceId;
import org.onosproject.store.service.TransactionContext;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Interface for consistent resource substores.
 */
interface ConsistentResourceSubStore
        <T extends ResourceId, U extends Resource, V extends TransactionalResourceSubStore> {

    /**
     * Returns a new transactional substore.
     *
     * @param tx the transaction context
     * @return a transactional resource substore
     */
    V transactional(TransactionContext tx);

    /**
     * Returns a list of resource allocations for the given resource ID.
     *
     * @param resourceId the resource ID
     * @return a list of resource allocations for the given ID
     */
    List<ResourceAllocation> getResourceAllocations(T resourceId);

    /**
     * Returns a set of child resources for the given discrete resource.
     *
     * @param resourceId the parent resource ID
     * @return a set of child resources for the given discrete parent
     */
    Set<U> getChildResources(DiscreteResourceId resourceId);

    /**
     * Returns a set of child resources cast to the given type.
     *
     * @param resourceId the parent resource ID
     * @param type the type to which to cast resources
     * @return a set of child resources for the given discrete parent
     */
    Set<U> getChildResources(DiscreteResourceId resourceId, Class<?> type);

    /**
     * Returns a boolean indicating whether the given resource is available.
     *
     * @param resource the resource to check
     * @return indicates whether the given resource is available
     */
    boolean isAvailable(U resource);

    /**
     * Returns a stream of allocated resources for the given parent.
     *
     * @param parent the parent resource ID for which to return allocated resources
     * @param type the type to which to cast allocated resources
     * @return a stream of allocated resources for the given parent
     */
    Stream<U> getAllocatedResources(DiscreteResourceId parent, Class<?> type);

    /**
     * Returns a stream of resources for the given consumer.
     *
     * @param consumerId the consumer ID for which to return resources
     * @return a stream of resources for the given consumer
     */
    Stream<U> getResources(ResourceConsumerId consumerId);

}