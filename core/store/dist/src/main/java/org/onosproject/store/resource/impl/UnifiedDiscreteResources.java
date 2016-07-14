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

import com.google.common.collect.Sets;
import org.onosproject.net.resource.DiscreteResource;
import org.onosproject.net.resource.DiscreteResourceId;
import org.onosproject.net.resource.Resources;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
        return of(Sets.difference(values(), other.values()));
    }

    @Override
    public boolean isEmpty() {
        return generics.isEmpty() && encodables.isEmpty();
    }

    @Override
    public boolean containsAny(List<DiscreteResource> other) {
        Map<Boolean, List<DiscreteResource>> partitioned = other.stream()
                .collect(Collectors.partitioningBy(CODECS::isEncodable));
        return generics.containsAny(partitioned.get(false)) || encodables.containsAny(partitioned.get(true));
    }

    @Override
    public DiscreteResources add(DiscreteResources other) {
        return of(Sets.union(this.values(), other.values()));
    }

    @Override
    public DiscreteResources remove(List<DiscreteResource> removed) {
        return of(Sets.difference(values(), new LinkedHashSet<>(removed)));
    }

    @Override
    public Set<DiscreteResource> values() {
        return Stream.concat(encodables.values().stream(), generics.values().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
