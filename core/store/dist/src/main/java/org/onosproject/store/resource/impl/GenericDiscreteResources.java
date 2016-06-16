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
package org.onosproject.store.resource.impl;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Sets;

import org.onosproject.net.resource.DiscreteResource;
import org.onosproject.net.resource.DiscreteResourceId;
import org.onosproject.net.resource.Resources;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

final class GenericDiscreteResources implements DiscreteResources {
    private final Set<DiscreteResource> values;

    static DiscreteResources of(Set<DiscreteResource> resources) {
        if (resources.isEmpty()) {
            return DiscreteResources.empty();
        }

        return new GenericDiscreteResources(resources);
    }

    private GenericDiscreteResources(Set<DiscreteResource> values) {
        this.values = values;
    }

    // for serializer
    private GenericDiscreteResources() {
        this.values = null;
    }

    @Override
    public Optional<DiscreteResource> lookup(DiscreteResourceId id) {
        DiscreteResource resource = Resources.discrete(id).resource();
        if (values.contains(resource)) {
            return Optional.of(resource);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public DiscreteResources difference(DiscreteResources other) {
        if (other instanceof GenericDiscreteResources) {
            // make sure that the set is serializable
            return of(new LinkedHashSet<>(Sets.difference(this.values(), other.values())));
        } else if (other instanceof EmptyDiscreteResources) {
            return this;
        }

        return DiscreteResources.of(Sets.difference(this.values(), other.values()));
    }

    @Override
    public boolean isEmpty() {
        return values.isEmpty();
    }

    @Override
    public boolean containsAny(Set<DiscreteResource> other) {
        return !Sets.intersection(this.values(), other).isEmpty();
    }

    // returns a new instance, not mutate the current instance
    @Override
    public DiscreteResources add(DiscreteResources other) {
        if (other instanceof GenericDiscreteResources) {
            // make sure that the set is serializable
            return of(new LinkedHashSet<>(Sets.union(this.values(), other.values())));
        } else if (other instanceof EmptyDiscreteResources) {
            return this;
        }

        return DiscreteResources.of(Sets.union(this.values(), other.values()));
    }

    @Override
    public Set<DiscreteResource> values() {
        // breaks immutability, but intentionally returns the field
        // because this class is transient
        return values;
    }

    @Override
    public <T> Set<DiscreteResource> valuesOf(Class<T> cls) {
        return values.stream()
                .filter(x -> x.isTypeOf(cls))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public int hashCode() {
        return Objects.hash(values);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final GenericDiscreteResources other = (GenericDiscreteResources) obj;
        return Objects.equals(this.values, other.values);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("values", values)
                .toString();
    }
}
