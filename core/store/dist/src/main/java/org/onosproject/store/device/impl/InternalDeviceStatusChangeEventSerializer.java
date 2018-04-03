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
package org.onosproject.store.device.impl;

import static org.onosproject.store.serializers.DeviceIdSerializer.deviceIdSerializer;

import org.onosproject.net.DeviceId;
import org.onosproject.store.Timestamp;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Kryo Serializer for {@link InternalDeviceStatusChangeEvent}.
 */
public class InternalDeviceStatusChangeEventSerializer extends Serializer<InternalDeviceStatusChangeEvent> {

    /**
     * Creates a serializer for {@link InternalDeviceStatusChangeEvent}.
     */
    public InternalDeviceStatusChangeEventSerializer() {
        // does not accept null
        super(false);
    }

    @Override
    public void write(Kryo kryo, Output output, InternalDeviceStatusChangeEvent event) {
        kryo.writeObject(output, event.deviceId(), deviceIdSerializer());
        kryo.writeClassAndObject(output, event.timestamp());
        kryo.writeClassAndObject(output, event.available());
    }

    @Override
    public InternalDeviceStatusChangeEvent read(Kryo kryo, Input input,
                                                Class<InternalDeviceStatusChangeEvent> type) {
        DeviceId deviceId = kryo.readObject(input, DeviceId.class, deviceIdSerializer());
        Timestamp timestamp = (Timestamp) kryo.readClassAndObject(input);
        Boolean available = (Boolean) kryo.readClassAndObject(input);

        return new InternalDeviceStatusChangeEvent(deviceId, timestamp, available);
    }
}
