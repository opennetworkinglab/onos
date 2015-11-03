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
package org.onosproject.net.newresource;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableList;
import org.onosproject.event.ListenerService;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Service for allocating/releasing resource(s) and retrieving allocation(s) and availability.
 */
@Beta
public interface ResourceService extends ListenerService<ResourceEvent, ResourceListener> {
    /**
     * Allocates the specified resource to the specified user.
     *
     * @param consumer resource user which the resource is allocated to
     * @param resource resource to be allocated
     * @return allocation information enclosed by Optional. If the allocation fails, the return value is empty
     */
    default Optional<ResourceAllocation> allocate(ResourceConsumer consumer, ResourcePath resource) {
        checkNotNull(consumer);
        checkNotNull(resource);

        List<ResourceAllocation> allocations = allocate(consumer, ImmutableList.of(resource));
        if (allocations.isEmpty()) {
            return Optional.empty();
        }

        assert allocations.size() == 1;

        ResourceAllocation allocation = allocations.get(0);

        assert allocation.resource().equals(resource);

        // cast is ensured by the assertions above
        return Optional.of(allocation);
    }

    /**
     * Transactionally allocates the specified resources to the specified user.
     * All allocations are made when this method succeeds, or no allocation is made when this method fails.
     *
     * @param consumer resource user which the resources are allocated to
     * @param resources resources to be allocated
     * @return non-empty list of allocation information if succeeded, otherwise empty list
     */
    List<ResourceAllocation> allocate(ResourceConsumer consumer, List<ResourcePath> resources);

    /**
     * Transactionally allocates the specified resources to the specified user.
     * All allocations are made when this method succeeds, or no allocation is made when this method fails.
     *
     * @param consumer resource user which the resources are allocated to
     * @param resources resources to be allocated
     * @return non-empty list of allocation information if succeeded, otherwise empty list
     */
    default List<ResourceAllocation> allocate(ResourceConsumer consumer, ResourcePath... resources) {
        checkNotNull(consumer);
        checkNotNull(resources);

        return allocate(consumer, Arrays.asList(resources));
    }

    /**
     * Releases the specified resource allocation.
     *
     * @param allocation resource allocation to be released
     * @return true if succeeded, otherwise false
     */
    default boolean release(ResourceAllocation allocation) {
        checkNotNull(allocation);

        return release(ImmutableList.of(allocation));
    }

    /**
     * Transactionally releases the specified resource allocations.
     * All allocations are released when this method succeeded, or no allocation is released when this method fails.
     *
     * @param allocations resource allocations to be released
     * @return true if succeeded, otherwise false
     */
    boolean release(List<ResourceAllocation> allocations);

    /**
     * Transactionally releases the specified resource allocations.
     * All allocations are released when this method succeeded, or no allocation is released when this method fails.
     *
     * @param allocations resource allocations to be released
     * @return true if succeeded, otherwise false
     */
    default boolean release(ResourceAllocation... allocations) {
        checkNotNull(allocations);

        return release(ImmutableList.copyOf(allocations));
    }

    /**
     * Transactionally releases the resources allocated to the specified consumer.
     * All allocations are released when this method succeeded, or no allocation is released when this method fails.
     *
     * @param consumer consumer whose allocated resources are to be released
     * @return true if succeeded, otherwise false
     */
    boolean release(ResourceConsumer consumer);

    /**
     * Returns resource allocation of the specified resource.
     *
     * @param resource resource to check the allocation
     * @return allocation information enclosed by Optional.
     * If the resource is not allocated, the return value is empty.
     */
    Optional<ResourceAllocation> getResourceAllocation(ResourcePath resource);

    /**
     * Returns allocated resources being as children of the specified parent and being the specified resource type.
     *
     * @param parent parent resource path
     * @param cls class to specify a type of resource
     * @param <T> type of the resource
     * @return non-empty collection of resource allocations if resources are allocated with the subject and type,
     * empty collection if no resource is allocated with the subject and type
     */
    <T> Collection<ResourceAllocation> getResourceAllocations(ResourcePath parent, Class<T> cls);

    /**
     * Returns resources allocated to the specified consumer.
     *
     * @param consumer consumer whose allocated resources are to be returned
     * @return resources allocated to the consumer
     */
    Collection<ResourceAllocation> getResourceAllocations(ResourceConsumer consumer);

    /**
     * Returns resource paths that point available child resources under the specified resource path.
     *
     * @param parent parent resource path
     * @return available resource paths under the specified resource path
     */
    Collection<ResourcePath> getAvailableResources(ResourcePath parent);

    /**
     * Returns the availability of the specified resource.
     *
     * @param resource resource to check the availability
     * @return true if available, otherwise false
     */
    boolean isAvailable(ResourcePath resource);

    // TODO: listener and event mechanism need to be considered
}
