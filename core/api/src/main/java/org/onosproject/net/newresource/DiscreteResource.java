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

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Represents a resource path which specifies a resource which can be measured
 * as a discrete unit. A VLAN ID and a MPLS label of a link are examples of the resource.
 * <p>
 * Note: This class is exposed to the public, but intended to be used in the resource API
 * implementation only. It is not for resource API user.
 * </p>
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

    /**
     * The user of this methods must receive the return value as the correct type.
     * Otherwise, this methods throws an exception.
     *
     * @param <T> type of the return value
     * @return the volume of this resource
     */
    @SuppressWarnings("unchecked")
    @Override
    // TODO: consider receiving Class<T> as an argument. Which approach is convenient?
    public <T> T volume() {
        return (T) last();
    }

    @Override
    public List<Object> components() {
        return id.components();
    }

    @Override
    public Object last() {
        if (id.components().isEmpty()) {
            return null;
        }
        return id.components().get(id.components().size() - 1);
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
                .add("volume", volume())
                .toString();
    }
}
