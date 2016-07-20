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
import com.google.common.collect.ImmutableList;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import java.util.Arrays;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utility class for resource related classes.
 */
@Beta
public final class Resources {
    // public construction is prohibited
    private Resources() {}

    /**
     * Create a factory for discrete-type with the specified resource ID.
     *
     * @param id resource ID
     * @return {@link DiscreteFactory}
     */
    public static DiscreteFactory discrete(DiscreteResourceId id) {
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
    public static DiscreteFactory discrete(DiscreteResourceId parent, Object child) {
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
    public static DiscreteFactory discrete(DeviceId device) {
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
    public static DiscreteFactory discrete(DeviceId device, Object... components) {
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
    public static DiscreteFactory discrete(DeviceId device, PortNumber port, Object... components) {
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
     * Creates a factory for continuous-type with the specified parent ID and child.
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
    public static ContinuousFactory continuous(DeviceId device, Class<?> cls) {
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
    public static ContinuousFactory continuous(DeviceId device, Object... components) {
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
    public static ContinuousFactory continuous(DeviceId device, PortNumber port, Class<?> cls) {
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
    public static ContinuousFactory continuous(DeviceId device, PortNumber port, Object... components) {
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
