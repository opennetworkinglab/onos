/*
 * Copyright 2014-present Open Networking Laboratory
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

import static com.google.common.base.Preconditions.checkArgument;

import org.onlab.packet.Ip6Address;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Kryo Serializer for {@link Ip6Address}.
 */
public class Ip6AddressSerializer extends Serializer<Ip6Address> {

    /**
     * Creates {@link Ip6Address} serializer instance.
     */
    public Ip6AddressSerializer() {
        // non-null, immutable
        super(false, true);
    }

    @Override
    public void write(Kryo kryo, Output output, Ip6Address object) {
        byte[] octs = object.toOctets();
        // It is always Ip6Address.BYTE_LENGTH
        output.writeInt(octs.length);
        output.writeBytes(octs);
    }

    @Override
    public Ip6Address read(Kryo kryo, Input input, Class<Ip6Address> type) {
        final int octLen = input.readInt();
        checkArgument(octLen == Ip6Address.BYTE_LENGTH);
        byte[] octs = new byte[octLen];
        input.readBytes(octs);
        return Ip6Address.valueOf(octs);
    }
}
