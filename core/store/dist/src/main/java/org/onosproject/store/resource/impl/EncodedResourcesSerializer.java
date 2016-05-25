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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Range;
import com.google.common.collect.TreeRangeSet;
import org.onlab.util.ClosedOpenRange;
import org.onosproject.net.resource.DiscreteResourceCodec;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Kryo Serializer for {@link EncodedDiscreteResources}.
 */
final class EncodedResourcesSerializer extends Serializer<EncodedDiscreteResources> {
    @Override
    public void write(Kryo kryo, Output output, EncodedDiscreteResources object) {
        TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
        object.rawValues().stream()
                .map(Range::singleton)
                .map(x -> x.canonical(DiscreteDomain.integers()))
                .forEach(rangeSet::add);
        List<ClosedOpenRange> ranges = rangeSet.asRanges().stream()
                .map(ClosedOpenRange::of)
                .collect(Collectors.toList());
        kryo.writeObject(output, ranges);
        kryo.writeClassAndObject(output, object.codec());
    }

    @Override
    public EncodedDiscreteResources read(Kryo kryo, Input input, Class<EncodedDiscreteResources> cls) {
        @SuppressWarnings("unchecked")
        List<ClosedOpenRange> ranges = kryo.readObject(input, ArrayList.class);
        DiscreteResourceCodec codec = (DiscreteResourceCodec) kryo.readClassAndObject(input);

        HashSet<Integer> rawValues = ranges.stream()
                .flatMapToInt(x -> IntStream.range(x.lowerBound(), x.upperBound()))
                .boxed()
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new EncodedDiscreteResources(rawValues, codec);
    }
}
