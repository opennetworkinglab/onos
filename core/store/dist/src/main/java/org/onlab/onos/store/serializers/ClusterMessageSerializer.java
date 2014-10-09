package org.onlab.onos.store.serializers;

import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.store.cluster.messaging.ClusterMessage;
import org.onlab.onos.store.cluster.messaging.MessageSubject;
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
