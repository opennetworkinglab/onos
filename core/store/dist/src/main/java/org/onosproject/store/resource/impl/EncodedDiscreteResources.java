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

import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import org.onlab.util.Tools;
import org.onosproject.net.resource.DiscreteResource;
import org.onosproject.net.resource.DiscreteResourceCodec;
import org.onosproject.net.resource.DiscreteResourceId;
import org.onosproject.net.resource.Resources;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Represents discrete resources encoded by a codec.
 */
final class EncodedDiscreteResources {
    private final RangeSet<Integer> rangeSet;
    private final DiscreteResourceCodec codec;

    EncodedDiscreteResources(RangeSet<Integer> rangeSet, DiscreteResourceCodec codec) {
        this.rangeSet = rangeSet;
        this.codec = codec;
    }

    static EncodedDiscreteResources of(Set<DiscreteResource> resources, DiscreteResourceCodec codec) {
        RangeSet<Integer> rangeSet = TreeRangeSet.create();
        resources.stream()
                .map(x -> x.valueAs(Object.class))
                .flatMap(Tools::stream)
                .map(x -> codec.encode(x))
                .map(Range::singleton)
                .map(x -> x.canonical(DiscreteDomain.integers()))
                .forEach(rangeSet::add);

        return new EncodedDiscreteResources(rangeSet, codec);
    }

    RangeSet<Integer> rangeSet() {
        return rangeSet;
    }

    DiscreteResourceCodec codec() {
        return codec;
    }

    Set<DiscreteResource> values(DiscreteResourceId parent) {
        return rangeSet.asRanges().stream()
                .flatMapToInt(x1 -> IntStream.range(x1.lowerEndpoint(), x1.upperEndpoint()))
                .boxed()
                .map(x -> codec.decode(x))
                .map(x -> Resources.discrete(parent, x).resource())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @SuppressWarnings("unchecked")
    boolean contains(DiscreteResource resource) {
        return resource.valueAs(Object.class)
                .map(x -> codec.encode(x))
                .map(rangeSet::contains)
                .orElse(false);
    }

    boolean isEmpty() {
        return rangeSet.isEmpty();
    }
}
