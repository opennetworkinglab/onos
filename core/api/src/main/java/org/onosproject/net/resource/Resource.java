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
package org.onosproject.net.resource;

import com.google.common.annotations.Beta;

import java.util.Optional;

/**
 * An object that represent a resource in a network.
 * A Resource can represents path-like hierarchical structure with its ID. An ID of resource is
 * composed of a sequence of elementary resources that are not globally identifiable. A Resource
 * can be globally identifiable by its ID.
 *
 * Two types of resource are considered. One is discrete type and the other is continuous type.
 * Discrete type resource is a resource whose amount is measured as a discrete unit. VLAN ID and
 * MPLS label are examples of discrete type resource. Continuous type resource is a resource whose
 * amount is measured as a continuous value. Bandwidth is an example of continuous type resource.
 * A double value is associated with a continuous type value.
 *
 * Users of this class must keep the semantics of resources regarding the hierarchical structure.
 * For example, resource, Device:1/Port:1/VLAN ID:100, is valid, but resource,
 * VLAN ID:100/Device:1/Port:1 is not valid because a link is not a sub-component of a VLAN ID.
 */
@Beta
public interface Resource {

    DiscreteResource ROOT = new DiscreteResource();

    /**
     * Returns the ID of this resource.
     *
     * @return the ID of this resource
     */
    ResourceId id();

    /**
     * Returns the simple type name of this resource.
     *
     * Example:<br>
     * Resource: DeviceId:1/PortNumber:1/VlanId:200<br>
     * Simple type name: VlanId<br>
     *
     * @return the simple type name of this resource
     */
    String simpleTypeName();

    /**
     * Checks if the type of this instance is the specified type.
     *
     * @param type type of resource to be checked
     * @return true if this resource is the type of the specified type. Otherwise, false.
     */
    boolean isTypeOf(Class<?> type);

    /**
     * Checks if the type of this instance is the sub-type of the specified type.
     *
     * @param ancestor type of resource to be checked.
     * @return true if this resource is under the resource whose type is the given type.
     */
    boolean isSubTypeOf(Class<?> ancestor);

    /**
     * Returns value interpreted as the specified type. If the specified type is
     * incompatible with the underlying value, an empty instance is returned.
     *
     * @param type class instance specifying the type of return value
     * @param <T> type of the return value
     * @return the value of this resource as the specified type. If type mismatches,
     * returns an empty instance.
     */
    <T> Optional<T> valueAs(Class<T> type);

    /**
     * Returns the parent resource of this instance.
     * E.g. if this resource is Link:1/VLAN ID:100, the return value is the resource for Link:1.
     *
     * @return the parent resource of this instance.
     * If there is no parent, empty instance will be returned.
     */
    Optional<DiscreteResource> parent();

    /**
     * Returns a child resource of this instance with specifying the child object.
     * It is not allowed that a continuous type resource has a child. If the instance is
     * ContinuousResource, {@link UnsupportedOperationException} is thrown. If the given
     * object is a {@link Class} instance, {@link IllegalArgumentException} is thrown.
     *
     * @param child child object
     * @return a child resource
     * @throws IllegalArgumentException if the given object is a {@link Class} instance.
     */
    DiscreteResource child(Object child);

    /**
     * Returns a child resource of this instance with specifying a child object and
     * value. It is not allowed that a continuous type resource has a child. If the instance is
     * ContinuousResource, {@link UnsupportedOperationException} is thrown.
     *
     * @param child child object
     * @param value value
     * @return a child resource
     */
    ContinuousResource child(Class<?> child, double value);
}
