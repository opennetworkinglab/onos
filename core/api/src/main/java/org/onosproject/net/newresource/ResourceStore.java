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
import org.onosproject.store.Store;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

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
    boolean register(List<ResourcePath> resources);

    /**
     * Unregisters the resources in transactional way.
     * The state after completion of this method is all the resources are unregistered,
     * or none of the given resources is unregistered. The whole unregistration fails when any one of the
     * resource can't be unregistered.
     *
     * @param resources resources to be unregistered
     * @return true if the registration succeeds, false otherwise
     */
    boolean unregister(List<ResourcePath> resources);

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
    boolean allocate(List<ResourcePath> resources, ResourceConsumer consumer);

    /**
     * Releases the specified resources allocated to the specified corresponding consumers
     * in transactional way. The state after completion of this method is all the resources
     * are released from the consumer, or no resource is released. The whole release fails
     * when any one of the resource can't be released. The size of the list of resources and
     * that of consumers must be equal. The resource and consumer with the same position from
     * the head of each list correspond to each other.
     *
     * @param resources resources to be released
     * @param consumers resource consumers to whom the resource allocated to
     * @return true if succeeds, otherwise false
     */
    boolean release(List<ResourcePath> resources, List<ResourceConsumer> consumers);

    /**
     * Returns the resource consumer to whom the specified resource is allocated.
     *
     * @param resource resource whose allocated consumer to be returned
     * @return resource consumer who are allocated the resource
     */
    Optional<ResourceConsumer> getConsumer(ResourcePath resource);

    /**
     * Returns a collection of the resources allocated to the specified consumer.
     *
     * @param consumer resource consumer whose allocated resource are searched for
     * @return a collection of the resources allocated to the specified consumer
     */
    Collection<ResourcePath> getResources(ResourceConsumer consumer);

    /**
     * Returns a collection of the child resources of the specified parent.
     *
     * @param parent parent of the resource to be returned
     * @return a collection of the child resources of the specified resource
     */
    Collection<ResourcePath> getChildResources(ResourcePath parent);

    /**
     * Returns a collection of the resources which are children of the specified parent and
     * whose type is the specified class.
     *
     * @param parent parent of the resources to be returned
     * @param cls class instance of the children
     * @param <T> type of the resource
     * @return a collection of the resources which belongs to the specified subject and
     * whose type is the specified class.
     */
    <T> Collection<ResourcePath> getAllocatedResources(ResourcePath parent, Class<T> cls);
}
