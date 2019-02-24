/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.driver.extensions.serializers;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.onlab.packet.IpAddress;
import org.onosproject.driver.extensions.NiciraNat;

/**
 * Kryo serializer for {@link NiciraNat}.
 */
public class NiciraNatSerializer extends Serializer<NiciraNat> {

    /**
     * Creates {@link NiciraNat} serializer instance.
     */
    public NiciraNatSerializer() {
        // non-null, immutable
        super(false, true);
    }

    @Override
    public void write(Kryo kryo, Output output, NiciraNat object) {
        output.writeInt(object.niciraNatFlags());
        output.writeInt(object.niciraNatPresentFlags());
        output.writeInt(object.niciraNatPortMin());
        output.writeInt(object.niciraNatPortMax());

        output.writeBytes(object.niciraNatIpAddressMin().toOctets());
        output.writeBytes(object.niciraNatIpAddressMax().toOctets());
    }

    @Override
    public NiciraNat read(Kryo kryo, Input input, Class<NiciraNat> type) {
        int natFlags = input.readInt();
        int natPresentFlags = input.readInt();
        int natPortMin = input.readInt();
        int natPortMax = input.readInt();

        // TODO: IPv6 address should also be supported
        byte[] minOcts = new byte[4];
        byte[] maxOcts = new byte[4];

        input.readBytes(minOcts);
        input.readBytes(maxOcts);

        IpAddress natIpAddressMin = IpAddress.valueOf(IpAddress.Version.INET, minOcts);
        IpAddress natIpAddressMax = IpAddress.valueOf(IpAddress.Version.INET, maxOcts);

        return new NiciraNat(natFlags, natPresentFlags, natPortMin, natPortMax,
                natIpAddressMin, natIpAddressMax);
    }
}
