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

import com.google.common.collect.ImmutableSet;
import org.onosproject.net.resource.DiscreteResource;
import org.onosproject.net.resource.DiscreteResourceId;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Represents an empty set of discrete resource.
 */
final class EmptyDiscreteResources implements DiscreteResources {
    static final DiscreteResources INSTANCE = new EmptyDiscreteResources();

    // for serializer
    private EmptyDiscreteResources() {}

    @Override
    public Optional<DiscreteResource> lookup(DiscreteResourceId id) {
        return Optional.empty();
    }

    @Override
    public DiscreteResources difference(DiscreteResources other) {
        return this;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public boolean containsAny(List<DiscreteResource> other) {
        return false;
    }

    @Override
    public DiscreteResources add(DiscreteResources other) {
        return other;
    }

    @Override
    public DiscreteResources remove(List<DiscreteResource> removed) {
        return this;
    }

    @Override
    public Set<DiscreteResource> values() {
        return ImmutableSet.of();
    }
}
