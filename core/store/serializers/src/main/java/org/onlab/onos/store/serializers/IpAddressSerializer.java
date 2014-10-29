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
package org.onlab.onos.store.serializers;

import org.onlab.packet.IpAddress;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Kryo Serializer for {@link IpAddress}.
 */
public class IpAddressSerializer extends Serializer<IpAddress> {

    /**
     * Creates {@link IpAddress} serializer instance.
     */
    public IpAddressSerializer() {
        // non-null, immutable
        super(false, true);
    }

    @Override
    public void write(Kryo kryo, Output output, IpAddress object) {
        byte[] octs = object.toOctets();
        output.writeInt(octs.length);
        output.writeBytes(octs);
    }

    @Override
    public IpAddress read(Kryo kryo, Input input, Class<IpAddress> type) {
        final int octLen = input.readInt();
        byte[] octs = new byte[octLen];
        input.readBytes(octs);
        return IpAddress.valueOf(octs);
    }

}
