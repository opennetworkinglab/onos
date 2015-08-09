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

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Service for allocating/releasing resource(s) and retrieving allocation(s) and availability.
 */
@Beta
public interface ResourceService {
    /**
     * Allocates the specified resource to the specified user.
     *
     * @param consumer resource user which the resource is allocated to
     * @param resource resource to be allocated
     * @param <S> type of the subject which this resource belongs to
     * @param <T> type of the resource
     * @return allocation information enclosed by Optional. If the allocation fails, the return value is empty
     */
    <S, T> Optional<ResourceAllocation<S, T>> allocate(ResourceConsumer consumer, Resource<S, T> resource);

    /**
     * Transactionally allocates the specified resources to the specified user.
     * All allocations are made when this method succeeds, or no allocation is made when this method fails.
     *
     * @param consumer resource user which the resources are allocated to
     * @param resources resources to be allocated
     * @return non-empty list of allocation information if succeeded, otherwise empty list
     */
    List<ResourceAllocation<?, ?>> allocate(ResourceConsumer consumer, List<? extends Resource<?, ?>> resources);

    /**
     * Transactionally allocates the specified resources to the specified user.
     * All allocations are made when this method succeeds, or no allocation is made when this method fails.
     *
     * @param consumer resource user which the resources are allocated to
     * @param resources resources to be allocated
     * @return non-empty list of allocation information if succeeded, otherwise empty list
     */
    List<ResourceAllocation<?, ?>> allocate(ResourceConsumer consumer, Resource<?, ?>... resources);

    /**
     * Releases the specified resource allocation.
     *
     * @param allocation resource allocation to be released
     * @param <S> type of the subject which this resource belongs to
     * @param <T> type of the device resource
     * @return true if succeeded, otherwise false
     */
    <S, T> boolean release(ResourceAllocation<S, T> allocation);

    /**
     * Transactionally releases the specified resource allocations.
     * All allocations are released when this method succeeded, or no allocation is released when this method fails.
     *
     * @param allocations resource allocations to be released
     * @return true if succeeded, otherwise false
     */
    boolean release(List<? extends ResourceAllocation<?, ?>> allocations);

    /**
     * Transactionally releases the specified resource allocations.
     * All allocations are released when this method succeeded, or no allocation is released when this method fails.
     *
     * @param allocations resource allocations to be released
     * @return true if succeeded, otherwise false
     */
    boolean release(ResourceAllocation<?, ?>... allocations);

    /**
     * Transactionally releases the resources allocated to the specified consumer.
     * All allocations are released when this method succeeded, or no allocation is released when this method fails.
     *
     * @param consumer consumer whose allocated resources are to be released
     * @return true if succeeded, otherwise false
     */
    boolean release(ResourceConsumer consumer);

    /**
     * Returns allocated resources in the specified subject regarding the specified resource type.
     *
     * @param subject subject where resource allocations are obtained
     * @param cls class to specify a type of resource
     * @param <S> type of the subject
     * @param <T> type of the resource
     * @return non-empty collection of resource allocations if resources are allocated with the subject and type,
     * empty collection if no resource is allocated with the subject and type
     */
    <S, T> Collection<ResourceAllocation<S, T>> getResourceAllocations(S subject, Class<T> cls);

    /**
     * Returns resources allocated to the specified consumer.
     *
     * @param consumer consumer whose allocated resources are to be returned
     * @return resources allocated to the consumer
     */
    Collection<ResourceAllocation<?, ?>> getResourceAllocations(ResourceConsumer consumer);

    /**
     * Returns the availability of the specified device resource.
     *
     * @param resource resource to check the availability
     * @param <S> type of the subject
     * @param <T> type of the resource
     * @return true if available, otherwise false
     */
    <S, T> boolean isAvailable(Resource<S, T> resource);

    // TODO: listener and event mechanism need to be considered
}
