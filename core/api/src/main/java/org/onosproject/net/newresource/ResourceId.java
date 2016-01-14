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
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents identifier of resource.
 * This class is exposed to public, but intended to use only in ResourceStore implementations.
 */
@Beta
public final class ResourceId {
    static final ResourceId ROOT = new ResourceId();

    final ImmutableList<Object> components;

    static ResourceId of(DeviceId device, Object... components) {
        return new ResourceId(ImmutableList.builder()
                .add(device)
                .add(components)
                .build());
    }

    static ResourceId of(DeviceId device, PortNumber port, Object... components) {
        return new ResourceId(ImmutableList.builder()
                .add(device)
                .add(port)
                .add(components)
                .build());
    }

    private ResourceId(ImmutableList<Object> components) {
        this.components = checkNotNull(components);
    }

    // for serializer
    private ResourceId() {
        this.components = ImmutableList.of();
    }

    // IndexOutOfBoundsException is raised when the instance is equal to ROOT
    ResourceId parent() {
        if (components.size() == 1) {
            return ROOT;
        } else {
            return new ResourceId(components.subList(0, components.size() - 1));
        }
    }

    ResourceId child(Object child) {
        return new ResourceId(ImmutableList.builder()
                .addAll(components)
                .add(child)
                .build());
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
        if (!(obj instanceof ResourceId)) {
            return false;
        }

        ResourceId other = (ResourceId) obj;
        return Objects.equals(this.components, other.components);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("components", components)
                .toString();
    }
}
