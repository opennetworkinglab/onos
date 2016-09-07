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

import org.onosproject.net.DeviceId;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
* Kryo Serializer for {@link DeviceId}.
*/
public final class DeviceIdSerializer extends Serializer<DeviceId> {

    private static final DeviceIdSerializer INSTANCE = new DeviceIdSerializer();

    public static final DeviceIdSerializer deviceIdSerializer() {
        return INSTANCE;
    }

    /**
     * Creates {@link DeviceId} serializer instance.
     */
    public DeviceIdSerializer() {
        // non-null, immutable
        super(false, true);
    }

    @Override
    public void write(Kryo kryo, Output output, DeviceId object) {
        output.writeString(object.toString());
    }

    @Override
    public DeviceId read(Kryo kryo, Input input, Class<DeviceId> type) {
        final String str = input.readString();
        return DeviceId.deviceId(str);
    }
}
