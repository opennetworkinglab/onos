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
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Represents identifier of resource.
 * This class is exposed to public, but intended to use only in ResourceStore implementations.
 */
@Beta
public abstract class ResourceId {
    static final ResourceId ROOT = new DiscreteResourceId();

    final ImmutableList<Object> components;

    static ResourceId discrete(DeviceId device, Object... components) {
        return new DiscreteResourceId(ImmutableList.builder()
                .add(device)
                .add(components)
                .build());
    }

    static ResourceId discrete(DeviceId device, PortNumber port, Object... components) {
        return new DiscreteResourceId(ImmutableList.builder()
                .add(device)
                .add(port)
                .add(components)
                .build());
    }

    static ResourceId continuous(DeviceId device, Object... components) {
        Object last = components[components.length - 1];
        checkArgument(last instanceof Class<?>);

        return continuous(ImmutableList.builder()
                .add(device)
                .add(Arrays.copyOfRange(components, 0, components.length - 1)), (Class<?>) last);
    }

    static ResourceId continuous(DeviceId device, PortNumber port, Object... components) {
        Object last = components[components.length - 1];
        checkArgument(last instanceof Class<?>);

        return continuous(ImmutableList.builder()
                .add(device)
                .add(port)
                .add(Arrays.copyOfRange(components, 0, components.length - 1)), (Class<?>) last);
    }

    private static ResourceId continuous(ImmutableList.Builder<Object> parentComponents, Class<?> last) {
        return new ContinuousResourceId(parentComponents
                .add(last.getCanonicalName())
                .build(), last.getSimpleName());
    }

    protected ResourceId(ImmutableList<Object> components) {
        this.components = checkNotNull(components);
    }

    // for serializer
    protected ResourceId() {
        this.components = ImmutableList.of();
    }

    // IndexOutOfBoundsException is raised when the instance is equal to ROOT
    ResourceId parent() {
        if (components.size() == 1) {
            return ROOT;
        } else {
            return new DiscreteResourceId(components.subList(0, components.size() - 1));
        }
    }

    /**
     * Returns a resource ID of a child of this resource based on the specified object.
     * If the argument is an instance of {@link Class}, this method returns an instance of
     * {@link ContinuousResourceId}. Otherwise, it returns an instance of {@link DiscreteResourceId}
     * This method only work when the receiver is {@link DiscreteResourceId}. Otherwise,
     * this method throws an exception.
     *
     * @param child the last component of the child
     * @return a child resource ID
     */
    public ResourceId child(Object child) {
        checkState(this instanceof DiscreteResourceId);

        if (child instanceof Class<?>) {
            return continuous(ImmutableList.builder().addAll(components), (Class<?>) child);
        } else {
            return new DiscreteResourceId(ImmutableList.builder()
                    .addAll(components)
                    .add(child)
                    .build());
        }
    }

    @Override
    public int hashCode() {
        return components.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final ResourceId other = (ResourceId) obj;
        return Objects.equals(this.components, other.components);
    }

    @Override
    public String toString() {
        return components.toString();
    }

}
