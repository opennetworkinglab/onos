/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.net.resource;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Service for retrieving resource information.
 */
public interface ResourceQueryService {
    /**
     * Returns resource allocations of the specified resource.
     *
     * @param id ID of the resource to check the allocation
     * @return list of allocation information.
     * If the resource is not allocated, the return value is an empty list.
     */
    List<ResourceAllocation> getResourceAllocations(ResourceId id);

    /**
     * Returns allocated resources being as children of the specified parent and being the specified resource type.
     *
     * @param parent parent resource ID
     * @param cls class to specify a type of resource
     * @param <T> type of the resource
     * @return non-empty collection of resource allocations if resources are allocated with the subject and type,
     * empty collection if no resource is allocated with the subject and type
     */
    <T> Collection<ResourceAllocation> getResourceAllocations(DiscreteResourceId parent, Class<T> cls);

    /**
     * Returns resources allocated to the specified consumer.
     *
     * @param consumer consumer whose allocated resources are to be returned
     * @return resources allocated to the consumer
     */
    Collection<ResourceAllocation> getResourceAllocations(ResourceConsumer consumer);

    /**
     * Returns resources that point available child resources under the specified resource.
     *
     * @param parent parent resource ID
     * @return available resources under the specified resource
     */
    Set<Resource> getAvailableResources(DiscreteResourceId parent);

    /**
     * Returns available resources which are child resources of the specified parent and
     * whose type is the specified type.
     *
     * @param parent parent resource ID
     * @param cls class to specify a type of resource
     * @param <T> type of the resource
     * @return available resources of the specified type under the specified parent resource
     */
    <T> Set<Resource> getAvailableResources(DiscreteResourceId parent, Class<T> cls);

    /**
     * Returns available resource values which are the values of the child resource of
     * the specified parent and whose type is the specified type.
     *
     * @param parent parent resource ID
     * @param cls class to specify a type of resource
     * @param <T> type of the resource
     * @return available resource value of the specified type under the specified parent resource
     */
    <T> Set<T> getAvailableResourceValues(DiscreteResourceId parent, Class<T> cls);

    /**
     * Returns resources registered under the specified resource.
     *
     * @param parent parent resource ID
     * @return registered resources under the specified resource
     */
    Set<Resource> getRegisteredResources(DiscreteResourceId parent);

    /**
     * Returns the availability of the specified resource.
     *
     * @param resource resource to check the availability
     * @return true if available, otherwise false
     */
    boolean isAvailable(Resource resource);
}
