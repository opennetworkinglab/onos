/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.segmentrouting.mcast;


import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.onlab.packet.IpAddress;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;

/**
 * Custom serializer for {@link McastRoleStoreKey}.
 */
class McastRoleStoreKeySerializer extends Serializer<McastRoleStoreKey> {

    /**
     * Creates {@link McastRoleStoreKeySerializer} serializer instance.
     */
    McastRoleStoreKeySerializer() {
        // non-null, immutable
        super(false, true);
    }

    @Override
    public void write(Kryo kryo, Output output, McastRoleStoreKey object) {
        kryo.writeClassAndObject(output, object.mcastIp());
        output.writeString(object.deviceId().toString());
        kryo.writeClassAndObject(output, object.source());
    }

    @Override
    public McastRoleStoreKey read(Kryo kryo, Input input, Class<McastRoleStoreKey> type) {
        IpAddress mcastIp = (IpAddress) kryo.readClassAndObject(input);
        final String str = input.readString();
        DeviceId deviceId =  DeviceId.deviceId(str);
        ConnectPoint source = (ConnectPoint) kryo.readClassAndObject(input);
        return new McastRoleStoreKey(mcastIp, deviceId, source);
    }
}
