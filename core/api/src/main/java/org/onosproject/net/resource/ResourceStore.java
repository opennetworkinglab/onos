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

import com.google.common.annotations.Beta;
import org.onosproject.store.Store;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Service for storing resource and consumer information.
 */
@Beta
public interface ResourceStore extends Store<ResourceEvent, ResourceStoreDelegate> {

    /**
     * Registers the resources in transactional way.
     * Resource registration must be done before resource allocation. The state after completion
     * of this method is all the resources are registered, or none of the given resources is registered.
     * The whole registration fails when any one of the resource can't be registered.
     *
     * @param resources resources to be registered
     * @return true if the registration succeeds, false otherwise
     */
    boolean register(List<? extends Resource> resources);

    /**
     * Unregisters the resources in transactional way.
     * The state after completion of this method is all the resources are unregistered,
     * or none of the given resources is unregistered. The whole unregistration fails when any one of the
     * resource can't be unregistered.
     *
     * @param ids resources to be unregistered
     * @return true if the registration succeeds, false otherwise
     */
    boolean unregister(List<? extends ResourceId> ids);

    /**
     * Allocates the specified resources to the specified consumer in transactional way.
     * The state after completion of this method is all the resources are allocated to the consumer,
     * or no resource is allocated to the consumer. The whole allocation fails when any one of
     * the resource can't be allocated.
     *
     * @param resources resources to be allocated
     * @param consumer resource consumer which the resources are allocated to
     * @return true if the allocation succeeds, false otherwise.
     */
    boolean allocate(List<? extends Resource> resources, ResourceConsumer consumer);

    /**
     * Releases the specified allocated resources in transactional way.
     * The state after completion of this method is all the resources
     * are released from the consumer, or no resource is released. The whole release fails
     * when any one of the resource can't be released. The size of the list of resources and
     * that of consumers must be equal. The resource and consumer with the same position from
     * the head of each list correspond to each other.
     *
     * @param allocations allocaitons to be released
     * @return true if succeeds, otherwise false
     */
    boolean release(List<ResourceAllocation> allocations);

    /**
     * Returns the resource consumers to whom the specified resource is allocated.
     * The return value is a list having only one element when the given resource is discrete type.
     * The return value may have multiple elements when the given resource is continuous type.
     *
     * @param id ID of the resource whose allocated consumer to be returned
     * @return resource consumers who are allocated the resource.
     * Returns empty list if there is no such consumer.
     */
    List<ResourceAllocation> getResourceAllocations(ResourceId id);

    /**
     * Returns the availability of the specified resource.
     *
     * @param resource resource to check the availability
     * @return true if available, otherwise false
     */
    boolean isAvailable(Resource resource);

    /**
     * Returns a collection of the resources allocated to the specified consumer.
     *
     * @param consumer resource consumer whose allocated resource are searched for
     * @return a collection of the resources allocated to the specified consumer
     */
    Collection<Resource> getResources(ResourceConsumer consumer);

    /**
     * Returns a set of the child resources of the specified parent.
     *
     * @param parent ID of the parent of the resource to be returned
     * @return a set of the child resources of the specified resource
     */
    Set<Resource> getChildResources(DiscreteResourceId parent);

    /**
     * Returns a set of the child resources of the specified parent and whose type is
     * the specified class.
     *
     * @param parent ID of the parent of the resources to be returned
     * @param cls class instance of the children
     * @param <T> type of the resource
     * @return a set of the child resources of the specified parent and whose type is
     * the specified class
     */
    <T> Set<Resource> getChildResources(DiscreteResourceId parent, Class<T> cls);

    /**
     * Returns a collection of the resources which are children of the specified parent and
     * whose type is the specified class.
     *
     * @param parent ID of the parent of the resources to be returned
     * @param cls class instance of the children
     * @param <T> type of the resource
     * @return a collection of the resources which belongs to the specified subject and
     * whose type is the specified class.
     */
    <T> Collection<Resource> getAllocatedResources(DiscreteResourceId parent, Class<T> cls);
}
