/*
 * Copyright 2016-present Open Networking Foundation
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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.onosproject.net.resource.DiscreteResource;
import org.onosproject.net.resource.DiscreteResourceCodec;
import org.onosproject.net.resource.DiscreteResourceId;
import org.onosproject.net.resource.Resources;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A set of discrete resources that can be encoded as integers.
 */
final class EncodableDiscreteResources implements DiscreteResources {
    private static final Codecs CODECS = Codecs.getInstance();
    private final DiscreteResource parent;
    private final Map<Class<?>, EncodedDiscreteResources> map;

    private static Class<?> getClass(DiscreteResource resource) {
        return resource.valueAs(Object.class).map(Object::getClass).get();
    }

    static DiscreteResources of(Set<DiscreteResource> resources) {
        if (resources.isEmpty()) {
            return DiscreteResources.empty();
        }

        DiscreteResource parent = resources.iterator().next().parent().get();
        return of(parent, resources);
    }

    static EncodableDiscreteResources of(DiscreteResource parent, Set<DiscreteResource> resources) {
        Map<Class<?>, Set<DiscreteResource>> grouped = resources.stream()
                .collect(Collectors.groupingBy(x -> getClass(x), Collectors.toCollection(LinkedHashSet::new)));

        Map<Class<?>, EncodedDiscreteResources> values = new LinkedHashMap<>();
        for (Map.Entry<Class<?>, Set<DiscreteResource>> entry : grouped.entrySet()) {
            DiscreteResourceCodec<?> codec = CODECS.getCodec(entry.getKey());
            values.put(entry.getKey(), EncodedDiscreteResources.of(entry.getValue(), codec));
        }

        return new EncodableDiscreteResources(parent, values);
    }

    private static DiscreteResources of(DiscreteResource parent, Map<Class<?>, EncodedDiscreteResources> map) {
        if (isEmpty(map)) {
            return DiscreteResources.empty();
        }
        return new EncodableDiscreteResources(parent, map);
    }

    private static boolean isEmpty(Map<Class<?>, EncodedDiscreteResources> map) {
        return map.values().stream().allMatch(EncodedDiscreteResources::isEmpty);
    }

    EncodableDiscreteResources(DiscreteResource parent, Map<Class<?>, EncodedDiscreteResources> map) {
        this.parent = parent;
        this.map = map;
    }

    @Override
    public Optional<DiscreteResource> lookup(DiscreteResourceId id) {
        if (!id.parent().filter(parent.id()::equals).isPresent()) {
            return Optional.empty();
        }
        DiscreteResource resource = Resources.discrete(id).resource();
        Class<?> cls = getClass(resource);
        return Optional.ofNullable(map.get(cls))
                .filter(x -> x.contains(resource))
                .map(x -> resource);
    }

    @Override
    public DiscreteResources difference(DiscreteResources other) {
        if (other instanceof EncodableDiscreteResources) {
            EncodableDiscreteResources cast = (EncodableDiscreteResources) other;

            Map<Class<?>, EncodedDiscreteResources> newMap = new LinkedHashMap<>();
            for (Entry<Class<?>, EncodedDiscreteResources> e : this.map.entrySet()) {
                Class<?> key = e.getKey();
                EncodedDiscreteResources thisValues = e.getValue();
                EncodedDiscreteResources otherValues = cast.map.get(key);
                if (otherValues == null) {
                    newMap.put(key, thisValues);
                    continue;
                }
                EncodedDiscreteResources diff = thisValues.difference(otherValues);
                // omit empty resources from a new resource set
                // empty EncodedDiscreteResources can't deserialize due to
                // inability to reproduce a Class<?> instance from the serialized data
                if (diff.isEmpty()) {
                    continue;
                }
                newMap.put(key, diff);
            }

            return of(parent, newMap);
        } else if (other instanceof EmptyDiscreteResources) {
            return this;
        }

        return DiscreteResources.of(Sets.difference(values(), other.values()));
    }

    @Override
    public boolean isEmpty() {
        return isEmpty(map);
    }

    @Override
    public boolean containsAny(Set<DiscreteResource> other) {
        return other.stream()
                .filter(x -> map.containsKey(getClass(x)))
                .anyMatch(x -> map.get(getClass(x)).contains(x));
    }

    @Override
    public DiscreteResources add(DiscreteResources other) {
        if (other instanceof EncodableDiscreteResources) {
            EncodableDiscreteResources cast = (EncodableDiscreteResources) other;
            LinkedHashMap<Class<?>, EncodedDiscreteResources> newMap =
                    Stream.concat(this.map.entrySet().stream(), cast.map.entrySet().stream())
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    Map.Entry::getValue,
                                    EncodedDiscreteResources::add,
                                    LinkedHashMap::new
                            ));
            return of(parent, newMap);
        } else if (other instanceof EmptyDiscreteResources) {
            return this;
        }

        return DiscreteResources.of(Sets.union(this.values(), other.values()));
    }

    @Override
    public Set<DiscreteResource> values() {
        return map.values().stream()
                .flatMap(x -> x.values(parent.id()).stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public <T> Set<DiscreteResource> valuesOf(Class<T> cls) {
        return Optional.ofNullable(map.get(cls))
                .map(x -> x.values(parent.id()))
                .orElse(ImmutableSet.of());
    }

    DiscreteResource parent() {
        return parent;
    }

    Map<Class<?>, EncodedDiscreteResources> rawValues() {
        return map;
    }

    @Override
    public int hashCode() {
        return Objects.hash(parent, map);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final EncodableDiscreteResources other = (EncodableDiscreteResources) obj;
        return Objects.equals(this.parent, other.parent)
                && Objects.equals(this.map, other.map);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("values", values())
                .toString();
    }
}
