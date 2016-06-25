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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents a set of resources containing resources that can be encoded as integer
 * and those that can't encoded as integer.
 */
final class UnifiedDiscreteResources implements DiscreteResources {
    private final DiscreteResources generics;
    private final DiscreteResources encodables;
    private static final Codecs CODECS = Codecs.getInstance();

    static DiscreteResources of(Set<DiscreteResource> resources) {
        if (resources.isEmpty()) {
            return DiscreteResources.empty();
        }

        Map<Boolean, Set<DiscreteResource>> partitioned = resources.stream()
                .collect(Collectors.partitioningBy(CODECS::isEncodable, Collectors.toCollection(LinkedHashSet::new)));
        return new UnifiedDiscreteResources(
                GenericDiscreteResources.of(partitioned.get(false)),
                EncodableDiscreteResources.of(partitioned.get(true))
        );
    }

    private UnifiedDiscreteResources(DiscreteResources generics, DiscreteResources encodables) {
        this.generics = generics;
        this.encodables = encodables;
    }

    @Override
    public Optional<DiscreteResource> lookup(DiscreteResourceId id) {
        if (CODECS.isEncodable(Resources.discrete(id).resource())) {
            return encodables.lookup(id);
        }

        return generics.lookup(id);
    }

    @Override
    public DiscreteResources difference(DiscreteResources other) {
        if (other instanceof UnifiedDiscreteResources) {
            UnifiedDiscreteResources cast = (UnifiedDiscreteResources) other;
            return new UnifiedDiscreteResources(
                    this.generics.difference(cast.generics),
                    this.encodables.difference(cast.encodables));
        } else if (other instanceof EmptyDiscreteResources) {
            return this;
        }

        return of(Sets.difference(this.values(), other.values()));
    }

    @Override
    public boolean isEmpty() {
        return generics.isEmpty() && encodables.isEmpty();
    }

    @Override
    public boolean containsAny(Set<DiscreteResource> other) {
        Map<Boolean, Set<DiscreteResource>> partitioned = other.stream()
                .collect(Collectors.partitioningBy(CODECS::isEncodable, Collectors.toCollection(LinkedHashSet::new)));
        return generics.containsAny(partitioned.get(false)) || encodables.containsAny(partitioned.get(true));
    }

    @Override
    public DiscreteResources add(DiscreteResources other) {
        if (other instanceof UnifiedDiscreteResources) {
            UnifiedDiscreteResources cast = (UnifiedDiscreteResources) other;
            return new UnifiedDiscreteResources(
                    this.generics.add(cast.generics),
                    this.encodables.add(cast.encodables));
        } else if (other instanceof EmptyDiscreteResources) {
            return this;
        }

        return of(Sets.union(this.values(), other.values()));
    }

    @Override
    public Set<DiscreteResource> values() {
        return Stream.concat(encodables.values().stream(), generics.values().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public <T> Set<DiscreteResource> valuesOf(Class<T> cls) {
        return Stream.concat(encodables.valuesOf(cls).stream(), generics.valuesOf(cls).stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public int hashCode() {
        return Objects.hash(generics, encodables);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final UnifiedDiscreteResources other = (UnifiedDiscreteResources) obj;
        return Objects.equals(this.generics, other.generics)
                && Objects.equals(this.encodables, other.encodables);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("values", values())
                .toString();
    }
}
