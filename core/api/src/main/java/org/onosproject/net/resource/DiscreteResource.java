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
import com.google.common.base.MoreObjects;

import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents a resource path which specifies a resource which can be measured
 * as a discrete unit. A VLAN ID and a MPLS label of a link are examples of the resource.
 */
@Beta
public final class DiscreteResource implements Resource {
    private final DiscreteResourceId id;

    DiscreteResource(DiscreteResourceId id) {
        this.id = id;
    }

    DiscreteResource() {
        this.id = ResourceId.ROOT;
    }

    @Override
    public DiscreteResourceId id() {
        return id;
    }

    @Override
    public String simpleTypeName() {
        return id.simpleTypeName();
    }

    @Override
    public boolean isTypeOf(Class<?> type) {
        checkNotNull(type);

        return id.isTypeOf(type);
    }

    @Override
    public boolean isSubTypeOf(Class<?> ancestor) {
        checkNotNull(ancestor);

        return id.isSubTypeOf(ancestor);
    }

    @Override
    public <T> Optional<T> valueAs(Class<T> type) {
        checkNotNull(type);

        return id.lastComponentAs(type);
    }

    @Override
    public DiscreteResource child(Object child) {
        checkArgument(!(child instanceof Class<?>));

        return Resources.discrete(id.child(child)).resource();
    }

    @Override
    public ContinuousResource child(Class<?> child, double value) {
        return Resources.continuous(id.child(child)).resource(value);
    }

    @Override
    public Optional<DiscreteResource> parent() {
        return id.parent().map(x -> Resources.discrete(x).resource());
    }

    @Override
    public int hashCode() {
        // the value returing from volume() is excluded due to optimization
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final DiscreteResource other = (DiscreteResource) obj;
        // the value returing from volume() is excluded due to optimization
        return Objects.equals(this.id, other.id);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .toString();
    }
}
