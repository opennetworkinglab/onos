/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.store.serializers;

import java.util.ArrayList;
import java.util.List;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.CollectionSerializer;
import com.google.common.collect.ImmutableSet;

/**
* Kryo Serializer for {@link ImmutableSet}.
*/
public class ImmutableSetSerializer extends Serializer<ImmutableSet<?>> {

    private final CollectionSerializer serializer = new CollectionSerializer();

    /**
     * Creates {@link ImmutableSet} serializer instance.
     */
    public ImmutableSetSerializer() {
        // non-null, immutable
        super(false, true);
        serializer.setElementsCanBeNull(false);
    }

    @Override
    public void write(Kryo kryo, Output output, ImmutableSet<?> object) {
        kryo.writeObject(output, object.asList(), serializer);
    }

    @Override
    public ImmutableSet<?> read(Kryo kryo, Input input,
                                Class<ImmutableSet<?>> type) {
        List<?> elms = kryo.readObject(input, ArrayList.class, serializer);
        return ImmutableSet.copyOf(elms);
    }
}
