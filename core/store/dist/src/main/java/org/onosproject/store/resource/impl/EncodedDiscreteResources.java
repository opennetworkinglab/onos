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

import org.onlab.util.Tools;
import org.onosproject.net.resource.DiscreteResource;
import org.onosproject.net.resource.DiscreteResourceCodec;
import org.onosproject.net.resource.DiscreteResourceId;
import org.onosproject.net.resource.Resources;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents discrete resources encoded by a codec.
 */
final class EncodedDiscreteResources {
    private final Set<Integer> rawValues;
    private final DiscreteResourceCodec codec;

    EncodedDiscreteResources(Set<Integer> rawValues, DiscreteResourceCodec codec) {
        this.rawValues = rawValues;
        this.codec = codec;
    }

    static EncodedDiscreteResources of(Set<DiscreteResource> resources, DiscreteResourceCodec codec) {
        Set<Integer> rawValues = resources.stream()
                .map(x -> x.valueAs(Object.class))
                .flatMap(Tools::stream)
                .map(x -> codec.encode(x))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return new EncodedDiscreteResources(rawValues, codec);
    }

    Set<Integer> rawValues() {
        return rawValues;
    }

    DiscreteResourceCodec codec() {
        return codec;
    }

    Set<DiscreteResource> resources(DiscreteResourceId parent) {
        return rawValues.stream()
                .map(x -> codec.decode(x))
                .map(x -> Resources.discrete(parent, x).resource())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @SuppressWarnings("unchecked")
    boolean contains(DiscreteResource resource) {
        return rawValues.contains(codec.encode(resource));
    }

    boolean isEmpty() {
        return rawValues.isEmpty();
    }
}
