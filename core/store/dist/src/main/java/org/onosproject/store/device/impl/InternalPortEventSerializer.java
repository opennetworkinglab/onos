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
package org.onosproject.store.device.impl;

import static org.onosproject.store.serializers.DeviceIdSerializer.deviceIdSerializer;

import java.util.List;

import org.onosproject.net.DeviceId;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.store.impl.Timestamped;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Kryo Serializer for {@link InternalPortEvent}.
 */
public class InternalPortEventSerializer extends Serializer<InternalPortEvent> {

    /**
     * Creates a serializer for {@link InternalPortEvent}.
     */
    public InternalPortEventSerializer() {
        // does not accept null
        super(false);
    }

    @Override
    public void write(Kryo kryo, Output output, InternalPortEvent event) {
        kryo.writeClassAndObject(output, event.providerId());
        kryo.writeObject(output, event.deviceId(), deviceIdSerializer());
        kryo.writeClassAndObject(output, event.portDescriptions());
    }

    @Override
    public InternalPortEvent read(Kryo kryo, Input input,
                               Class<InternalPortEvent> type) {
        ProviderId providerId = (ProviderId) kryo.readClassAndObject(input);
        DeviceId deviceId = kryo.readObject(input, DeviceId.class, deviceIdSerializer());

        @SuppressWarnings("unchecked")
        Timestamped<List<PortDescription>> portDescriptions
            = (Timestamped<List<PortDescription>>) kryo.readClassAndObject(input);

        return new InternalPortEvent(providerId, deviceId, portDescriptions);
    }
}
