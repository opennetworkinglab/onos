/*
 * Copyright 2014-present Open Networking Foundation
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

import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Kryo Serializer for {@link Ip4Prefix}.
 */
public final class Ip4PrefixSerializer extends Serializer<Ip4Prefix> {

    /**
     * Creates {@link Ip4Prefix} serializer instance.
     */
    public Ip4PrefixSerializer() {
        // non-null, immutable
        super(false, true);
    }

    @Override
    public void write(Kryo kryo, Output output,
            Ip4Prefix object) {
        byte[] octs = object.address().toOctets();
        // It is always Ip4Address.BYTE_LENGTH
        output.writeInt(octs.length);
        output.writeBytes(octs);
        output.writeInt(object.prefixLength());
    }

    @Override
    public Ip4Prefix read(Kryo kryo, Input input,
            Class<Ip4Prefix> type) {
        int octLen = input.readInt();
        checkArgument(octLen <= Ip4Address.BYTE_LENGTH);
        byte[] octs = new byte[octLen];
        input.readBytes(octs);
        int prefLen = input.readInt();
        return Ip4Prefix.valueOf(octs, prefLen);
    }
}
