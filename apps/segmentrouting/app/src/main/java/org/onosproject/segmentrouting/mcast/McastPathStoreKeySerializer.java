/*
 * Copyright 2020-present Open Networking Foundation
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

/**
 * Custom serializer for {@link McastPathStoreKey}.
 */
class McastPathStoreKeySerializer extends Serializer<McastPathStoreKey> {

    /**
     * Creates {@link McastPathStoreKeySerializer} serializer instance.
     */
    McastPathStoreKeySerializer() {
        // non-null, immutable
        super(false, true);
    }

    @Override
    public void write(Kryo kryo, Output output, McastPathStoreKey object) {
        kryo.writeClassAndObject(output, object.mcastIp());
        kryo.writeClassAndObject(output, object.source());
    }

    @Override
    public McastPathStoreKey read(Kryo kryo, Input input, Class<McastPathStoreKey> type) {
        IpAddress mcastIp = (IpAddress) kryo.readClassAndObject(input);
        ConnectPoint source = (ConnectPoint) kryo.readClassAndObject(input);
        return new McastPathStoreKey(mcastIp, source);
    }
}
