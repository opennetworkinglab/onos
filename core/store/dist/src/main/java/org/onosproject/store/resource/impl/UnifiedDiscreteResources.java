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

import org.onosproject.net.resource.DiscreteResource;
import org.onosproject.net.resource.DiscreteResourceId;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Represents a set of resources containing resources that can be encoded as integer
 * and those that can't encoded as integer.
 */
final class UnifiedDiscreteResources implements DiscreteResources {
    private final DiscreteResources nonEncodables;

    static DiscreteResources empty() {
        return new UnifiedDiscreteResources();
    }

    static DiscreteResources of(List<DiscreteResource> resources) {
        return new UnifiedDiscreteResources(resources);
    }

    private UnifiedDiscreteResources() {
        this.nonEncodables = NonEncodableDiscreteResources.empty();
    }

    private UnifiedDiscreteResources(List<DiscreteResource> resources) {
        this.nonEncodables = NonEncodableDiscreteResources.of(resources);
    }

    @Override
    public Optional<DiscreteResource> lookup(DiscreteResourceId id) {
        return nonEncodables.lookup(id);
    }

    @Override
    public DiscreteResources difference(DiscreteResources other) {
        return nonEncodables.difference(other);
    }

    @Override
    public boolean isEmpty() {
        return nonEncodables.isEmpty();
    }

    @Override
    public boolean containsAny(List<DiscreteResource> other) {
        return nonEncodables.containsAny(other);
    }

    @Override
    public DiscreteResources add(DiscreteResources other) {
        return nonEncodables.add(other);
    }

    @Override
    public DiscreteResources remove(List<DiscreteResource> removed) {
        return nonEncodables.remove(removed);
    }

    @Override
    public Set<DiscreteResource> values() {
        return nonEncodables.values();
    }
}
