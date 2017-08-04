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

import java.nio.ByteBuffer;

import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.packet.DefaultOutboundPacket;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Serializer for a default outbound packet.
 */
public class DefaultOutboundPacketSerializer extends Serializer<DefaultOutboundPacket> {

    /**
     * Creates {@link DefaultOutboundPacket} serializer instance.
     */
    public DefaultOutboundPacketSerializer() {
        // non-null, immutable
        super(false, true);
    }

    @Override
    public DefaultOutboundPacket read(Kryo kryo, Input input,
            Class<DefaultOutboundPacket> type) {
        DeviceId sendThrough = (DeviceId) kryo.readClassAndObject(input);
        TrafficTreatment treatment = (TrafficTreatment) kryo.readClassAndObject(input);
        byte[] data = (byte[]) kryo.readClassAndObject(input);
        return new DefaultOutboundPacket(sendThrough, treatment, ByteBuffer.wrap(data));
    }

    @Override
    public void write(Kryo kryo, Output output, DefaultOutboundPacket object) {
        kryo.writeClassAndObject(output, object.sendThrough());
        kryo.writeClassAndObject(output, object.treatment());
        kryo.writeClassAndObject(output, object.data().array());
    }

}
