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
package org.onosproject.store.serializers.custom;

import org.onosproject.cluster.NodeId;
import org.onosproject.store.cluster.messaging.ClusterMessage;
import org.onosproject.store.cluster.messaging.MessageSubject;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public final class ClusterMessageSerializer extends Serializer<ClusterMessage> {

    /**
     * Creates a serializer for {@link ClusterMessage}.
     */
    public ClusterMessageSerializer() {
        // does not accept null
        super(false);
    }

    @Override
    public void write(Kryo kryo, Output output, ClusterMessage message) {
        kryo.writeClassAndObject(output, message.sender());
        kryo.writeClassAndObject(output, message.subject());
        output.writeInt(message.payload().length);
        output.writeBytes(message.payload());
    }

    @Override
    public ClusterMessage read(Kryo kryo, Input input,
                               Class<ClusterMessage> type) {
        NodeId sender = (NodeId) kryo.readClassAndObject(input);
        MessageSubject subject = (MessageSubject) kryo.readClassAndObject(input);
        int payloadSize = input.readInt();
        byte[] payload = input.readBytes(payloadSize);
        return new ClusterMessage(sender, subject, payload);
    }
}
