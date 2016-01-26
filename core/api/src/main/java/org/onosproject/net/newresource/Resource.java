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
import com.google.common.collect.ImmutableList;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

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
     * Returns the components of this resource path.
     *
     * @return the components of this resource path
     */
    List<Object> components();

    /**
     * Returns the volume of this resource.
     *
     * @param <T> type of return value
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

    /**
     * Create a factory for discrete-type with the specified resource ID.
     *
     * @param id resource ID
     * @return {@link DiscreteFactory}
     */
    static DiscreteFactory discrete(DiscreteResourceId id) {
        checkNotNull(id);

        return new DiscreteFactory(id);
    }

    /**
     * Creates a factory for discrete-type with the specified parent ID and child.
     *
     * @param parent ID of the parent
     * @param child child
     * @return {@link DiscreteFactory}
     */
    static DiscreteFactory discrete(DiscreteResourceId parent, Object child) {
        checkNotNull(parent);
        checkNotNull(child);
        checkArgument(!(child instanceof Class<?>));

        return new DiscreteFactory(new DiscreteResourceId(ImmutableList.builder()
                .addAll(parent.components())
                .add(child)
                .build()));
    }

    /**
     * Create a factory for discrete-type with the specified device ID.
     *
     * @param device device ID
     * @return {@link DiscreteFactory}
     */
    static DiscreteFactory discrete(DeviceId device) {
        checkNotNull(device);

        return new DiscreteFactory(new DiscreteResourceId(ImmutableList.of(device)));
    }

    /**
     * Create a factory for discrete-type with the specified device ID and components.
     *
     * @param device device ID
     * @param components resource ID components other than the device ID
     * @return {@link DiscreteFactory}
     */
    static DiscreteFactory discrete(DeviceId device, Object... components) {
        checkNotNull(device);
        checkNotNull(components);

        return new DiscreteFactory(new DiscreteResourceId(ImmutableList.builder()
                .add(device)
                .add(components)
                .build()));
    }

    /**
     * Create a factory for discrete-type with the specified device ID, port number and components.
     *
     * @param device device ID
     * @param port port number
     * @param components resource ID components other than the device ID and port number
     * @return {@link DiscreteFactory}
     */
    static DiscreteFactory discrete(DeviceId device, PortNumber port, Object... components) {
        checkNotNull(device);
        checkNotNull(port);
        checkNotNull(components);

        return new DiscreteFactory(new DiscreteResourceId(ImmutableList.builder()
                .add(device)
                .add(port)
                .add(components)
                .build()));
    }

    /**
     * Create a factory for continuous-type with the specified resource ID.
     *
     * @param id resource ID
     * @return {@link ContinuousFactory}
     */
    static ContinuousFactory continuous(ContinuousResourceId id) {
        checkNotNull(id);

        return new ContinuousFactory(id);
    }

    /**
     * Creates a factory for continuous-type wit the specified parent ID and child.
     *
     * @param parent ID of the parent
     * @param child child
     * @return {@link ContinuousFactory}
     */
    static ContinuousFactory continuous(DiscreteResourceId parent, Class<?> child) {
        checkNotNull(parent);
        checkNotNull(child);

        return new ContinuousFactory(new ContinuousResourceId(ImmutableList.builder()
                .addAll(parent.components()), child));
    }

    /**
     * Create a factory for continuous-type with the specified device ID and type.
     *
     * @param device device ID
     * @param cls type of resource the returned factory will create
     * @return {@link ContinuousFactory}
     */
    static ContinuousFactory continuous(DeviceId device, Class<?> cls) {
        checkNotNull(device);
        checkNotNull(cls);

        return new ContinuousFactory(new ContinuousResourceId(ImmutableList.builder().add(device), cls));
    }

    /**
     * Create a factory for continuous-type with the specified device ID and components.
     * The last element of the components must be a {@link Class} instance. Otherwise,
     * an {@link IllegalArgumentException} is thrown.
     *
     * @param device device ID
     * @param components resource ID components other than the device ID.
     * @return {@link ContinuousFactory}
     */
    static ContinuousFactory continuous(DeviceId device, Object... components) {
        checkNotNull(device);
        checkNotNull(components);
        checkArgument(components.length > 1);

        Object last = components[components.length - 1];
        checkArgument(last instanceof Class<?>);

        return new ContinuousFactory(new ContinuousResourceId(ImmutableList.builder()
                .add(device)
                .add(Arrays.copyOfRange(components, 0, components.length - 1)), (Class<?>) last));
    }

    /**
     * Create a factory for continuous-type with the specified device ID, port number and type.
     *
     * @param device device ID
     * @param port port number
     * @param cls type of resource the returned factory will create
     * @return {@link ContinuousFactory}
     */
    static ContinuousFactory continuous(DeviceId device, PortNumber port, Class<?> cls) {
        checkNotNull(device);
        checkNotNull(port);
        checkNotNull(cls);

        return new ContinuousFactory(new ContinuousResourceId(ImmutableList.builder()
                .add(device)
                .add(port), cls));
    }

    /**
     * Create a factory for continuous-type with the specified device ID and components.
     * The last element of the components must be a {@link Class} instance. Otherwise,
     * an {@link IllegalArgumentException} is thrown.
     *
     * @param device device ID
     * @param port port number
     * @param components resource ID components other than the device ID and port number.
     * @return {@link ContinuousFactory}
     */
    static ContinuousFactory continuous(DeviceId device, PortNumber port, Object... components) {
        checkNotNull(device);
        checkNotNull(port);
        checkNotNull(components);
        checkArgument(components.length > 1);

        Object last = components[components.length - 1];
        checkArgument(last instanceof Class<?>);

        return new ContinuousFactory(new ContinuousResourceId(ImmutableList.builder()
                .add(device)
                .add(port)
                .add(Arrays.copyOfRange(components, 0, components.length - 1)), (Class<?>) last));
    }
}
