/*
 * Copyright 2016 Open Networking Laboratory
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
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Represents identifier of resource.
 * This class is exposed to public, but intended to use only in ResourceStore implementations.
 */
@Beta
public abstract class ResourceId {
    static final DiscreteResourceId ROOT = new DiscreteResourceId();

    static DiscreteResourceId discrete(DeviceId device, Object... components) {
        return new DiscreteResourceId(ImmutableList.builder()
                .add(device)
                .add(components)
                .build());
    }

    static DiscreteResourceId discrete(DeviceId device, PortNumber port, Object... components) {
        return new DiscreteResourceId(ImmutableList.builder()
                .add(device)
                .add(port)
                .add(components)
                .build());
    }

    static ContinuousResourceId continuous(DeviceId device, Object... components) {
        Object last = components[components.length - 1];
        checkArgument(last instanceof Class<?>);

        return new ContinuousResourceId(ImmutableList.builder()
                .add(device)
                .add(Arrays.copyOfRange(components, 0, components.length - 1)), (Class<?>) last);
    }

    static ContinuousResourceId continuous(DeviceId device, PortNumber port, Object... components) {
        Object last = components[components.length - 1];
        checkArgument(last instanceof Class<?>);

        return new ContinuousResourceId(ImmutableList.builder()
                .add(device)
                .add(port)
                .add(Arrays.copyOfRange(components, 0, components.length - 1)), (Class<?>) last);
    }

    /**
     * Returns the parent resource ID of this instance.
     *
     * @return the parent resource path of this instance.
     * If there is no parent, empty instance will be returned.
     */
    public abstract Optional<DiscreteResourceId> parent();

    /**
     * Returns a resource ID of a child of this resource based on the specified object.
     * If the given object is a {@link Class} instance, {@link IllegalArgumentException} is thrown.
     *
     * @param child the last component of the child
     * @return a child resource ID
     */
    public abstract DiscreteResourceId child(Object child);

    /**
     * Returns a resource ID of a child of this resource based on the specified object.
     *
     * @param child the last component of the child
     * @return a child resource ID
     */
    public abstract ContinuousResourceId child(Class<?> child);
}
