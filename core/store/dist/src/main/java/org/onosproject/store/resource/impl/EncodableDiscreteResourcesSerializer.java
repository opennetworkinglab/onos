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
import org.onosproject.net.resource.DiscreteResource;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Kryo serializer for {@link EncodableDiscreteResources}.
 */
class EncodableDiscreteResourcesSerializer extends Serializer<EncodableDiscreteResources> {
    @Override
    public void write(Kryo kryo, Output output, EncodableDiscreteResources object) {
        kryo.writeObject(output, object.parent());
        kryo.writeObject(output, new LinkedHashSet<>(object.rawValues().values()));
    }

    @Override
    public EncodableDiscreteResources read(Kryo kryo, Input input, Class<EncodableDiscreteResources> cls) {
        DiscreteResource parent = kryo.readObject(input, DiscreteResource.class);
        @SuppressWarnings("unchecked")
        Set<EncodedDiscreteResources> resources = kryo.readObject(input, LinkedHashSet.class);

        return new EncodableDiscreteResources(parent, resources.stream()
                .collect(Collectors.toMap(
                        EncodedDiscreteResources::encodedClass,
                        Function.identity(),
                        (v1, v2) -> v1,
                        LinkedHashMap::new)));
    }
}
