/*
 * Copyright 2015-2016 Open Networking Laboratory
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
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

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

    static DiscreteResource discrete(DeviceId device) {
        return new DiscreteResource(ResourceId.discrete(device));
    }

    /**
     * Creates an resource path which represents a discrete-type resource from the specified components.
     *
     * @param device device ID which is the first component of the path
     * @param components following components of the path. The order represents hierarchical structure of the resource.
     * @return resource path instance
     */
    static DiscreteResource discrete(DeviceId device, Object... components) {
        return new DiscreteResource(ResourceId.discrete(device, components));
    }

    /**
     * Creates an resource path which represents a discrete-type resource from the specified components.
     *
     * @param device device ID which is the first component of the path
     * @param port port number which is the second component of the path
     * @param components following components of the path. The order represents hierarchical structure of the resource.
     * @return resource path instance
     */
    static DiscreteResource discrete(DeviceId device, PortNumber port, Object... components) {
        return new DiscreteResource(ResourceId.discrete(device, port, components));
    }

    /**
     * Creates an resource path which represents a continuous-type resource from the specified components.
     *
     * @param value amount of the resource
     * @param device device ID which is the first component of the path
     * @param components following components of the path. The order represents hierarchical structure of the resource.
     *                   The last element of this list must be an {@link Class} instance. Otherwise, this method throws
     *                   an IllegalArgumentException.
     * @return resource path instance
     */
    static ContinuousResource continuous(double value, DeviceId device, Object... components) {
        checkArgument(components.length > 0,
                "Length of components must be greater thant 0, but " + components.length);

        return new ContinuousResource(ResourceId.continuous(device, components), value);
    }

    /**
     * Creates an resource path which represents a continuous-type resource from the specified components.
     *
     * @param value amount of the resource
     * @param device device ID which is the first component of the path.
     * @param port port number which is the second component of the path.
     * @param components following components of the path. The order represents hierarchical structure of the resource.
     *                   The last element of this list must be an {@link Class} instance. Otherwise, this method throws
     *                   an IllegalArgumentException.
     * @return resource path instance
     */
    static ContinuousResource continuous(double value, DeviceId device, PortNumber port, Object... components) {
        return new ContinuousResource(ResourceId.continuous(device, port, components), value);
    }

    /**
     * Returns the components of this resource path.
     *
     * @return the components of this resource path
     */
    List<Object> components();

    /**
     * Returns the volume of this resource.
     *
     * @return the volume of this resource
     */
    // TODO: think about other naming possibilities. amount? quantity?
    <T> T volume();

    /**
     * Returns the parent resource path of this instance.
     * E.g. if this path is Link:1/VLAN ID:100, the return value is the resource path for Link:1.
     *
     * @return the parent resource path of this instance.
     * If there is no parent, empty instance will be returned.
     */
    Optional<DiscreteResource> parent();

    /**
     * Returns a child resource path of this instance with specifying the child object.
     * The child resource path is discrete-type.
     *
     * @param child child object
     * @return a child resource path
     */
    DiscreteResource child(Object child);

    /**
     * Returns a child resource path of this instance with specifying a child object and
     * value. The child resource path is continuous-type.
     *
     * @param child child object
     * @param value value
     * @return a child resource path
     */
    ContinuousResource child(Class<?> child, double value);

    /**
     * Returns the last component of this instance.
     *
     * @return the last component of this instance.
     * The return value is equal to the last object of {@code components()}.
     */
    Object last();

    /**
     * Returns the ID of this resource path.
     *
     * @return the ID of this resource path
     */
    ResourceId id();
}
