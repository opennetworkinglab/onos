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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.MapSerializer;
import com.google.common.collect.ImmutableMap;

/**
* Kryo Serializer for {@link ImmutableMap}.
*/
public class ImmutableMapSerializer extends Serializer<ImmutableMap<?, ?>> {

    private final MapSerializer mapSerializer = new MapSerializer();

    /**
     * Creates {@link ImmutableMap} serializer instance.
     */
    public ImmutableMapSerializer() {
        // non-null, immutable
        super(false, true);
    }

    @Override
    public void write(Kryo kryo, Output output, ImmutableMap<?, ?> object) {
        // wrapping with unmodifiableMap proxy
        // to avoid Kryo from writing only the reference marker of this instance,
        // which will be embedded right before this method call.
        kryo.writeObject(output, Collections.unmodifiableMap(object), mapSerializer);
    }

    @Override
    public ImmutableMap<?, ?> read(Kryo kryo, Input input,
                                    Class<ImmutableMap<?, ?>> type) {
        Map<?, ?> map = kryo.readObject(input, HashMap.class, mapSerializer);
        return ImmutableMap.copyOf(map);
    }
}
