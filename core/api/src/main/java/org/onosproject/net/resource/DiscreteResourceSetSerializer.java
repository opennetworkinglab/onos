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
package org.onosproject.net.resource;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.annotations.Beta;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Range;
import com.google.common.collect.TreeRangeSet;
import org.onlab.util.ClosedOpenRange;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Kryo serializer for {@link DiscreteResourceSet}.
 */
@Beta
public final class DiscreteResourceSetSerializer extends Serializer<DiscreteResourceSet> {

    public DiscreteResourceSetSerializer() {
        super(false, true);
    }

    @Override
    public void write(Kryo kryo, Output output, DiscreteResourceSet object) {
        TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
        object.values().stream()
                .map(x -> object.codec().encode(x))
                .map(Range::singleton)
                .map(x -> x.canonical(DiscreteDomain.integers()))
                .forEach(rangeSet::add);
        List<ClosedOpenRange> ranges = rangeSet.asRanges().stream()
                .map(ClosedOpenRange::of)
                .collect(Collectors.toList());
        kryo.writeObject(output, ranges);
        kryo.writeClassAndObject(output, object.codec());
        kryo.writeObject(output, object.parent());
    }

    @Override
    public DiscreteResourceSet read(Kryo kryo, Input input, Class<DiscreteResourceSet> type) {
        @SuppressWarnings("unchecked")
        List<ClosedOpenRange> ranges = kryo.readObject(input, ArrayList.class);
        DiscreteResourceCodec codec = (DiscreteResourceCodec) kryo.readClassAndObject(input);
        DiscreteResourceId parent = kryo.readObject(input, DiscreteResourceId.class);

        if (ranges.isEmpty()) {
            return DiscreteResourceSet.empty();
        }

        Set<DiscreteResource> resources = ranges.stream()
                .flatMapToInt(x -> IntStream.range(x.lowerBound(), x.upperBound()))
                .mapToObj(x -> codec.decode(parent, x))
                .collect(Collectors.toSet());

        return DiscreteResourceSet.of(resources, codec);
    }
}
