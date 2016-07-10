/*
 * Copyright 2015-present Open Networking Laboratory
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

import org.onosproject.net.DefaultAnnotations;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.DefaultSerializers;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.StringSerializer;
import com.esotericsoftware.kryo.serializers.MapSerializer;

import java.util.HashMap;
import java.util.Map;

public class AnnotationsSerializer extends Serializer<DefaultAnnotations> {

    private static final StringSerializer STR_SERIALIZER
        = new DefaultSerializers.StringSerializer();

    private static final MapSerializer MAP_SERIALIZER = stringMapSerializer();

    /**
     * Returns a MapSerializer for {@code Map<String, String>} with
     * no null key or value.
     *
     * @return serializer
     */
    private static MapSerializer stringMapSerializer() {
        MapSerializer serializer = new MapSerializer();
        serializer.setKeysCanBeNull(false);
        serializer.setKeyClass(String.class, STR_SERIALIZER);
        serializer.setValuesCanBeNull(false);
        serializer.setValueClass(String.class, STR_SERIALIZER);
        return serializer;
    }

    public AnnotationsSerializer() {
        super(false, true);
    }

    @Override
    public void write(Kryo kryo, Output output, DefaultAnnotations object) {
        kryo.writeObject(output, object.asMap(), MAP_SERIALIZER);
    }

    @Override
    public DefaultAnnotations read(Kryo kryo, Input input, Class<DefaultAnnotations> type) {
        DefaultAnnotations.Builder b = DefaultAnnotations.builder();
        Map<String, String> map = kryo.readObject(input, HashMap.class, MAP_SERIALIZER);
        map.forEach((k, v) -> b.set(k, v));

        return b.build();
    }

}
