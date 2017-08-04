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
        checkArgument(octLen <= IpAddress.INET6_BYTE_LENGTH);
        byte[] octs = new byte[octLen];
        input.readBytes(octs);
        // Use the address size to decide whether it is IPv4 or IPv6 address
        if (octLen == IpAddress.INET_BYTE_LENGTH) {
            return IpAddress.valueOf(IpAddress.Version.INET, octs);
        }
        if (octLen == IpAddress.INET6_BYTE_LENGTH) {
            return IpAddress.valueOf(IpAddress.Version.INET6, octs);
        }
        return null;    // Shouldn't be reached
    }
}
