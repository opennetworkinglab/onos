package org.onlab.onos.store.serializers;

import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.store.cluster.messaging.ClusterMessage;
import org.onlab.onos.store.cluster.messaging.MessageSubject;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public final class ClusterMessageSerializer extends Serializer<ClusterMessage> {

    public ClusterMessageSerializer() {
        // does not accept null
        super(false);
    }

    @Override
    public void write(Kryo kryo, Output output, ClusterMessage object) {
        kryo.writeClassAndObject(output, object.sender());
        kryo.writeClassAndObject(output, object.subject());
        // TODO: write bytes serialized using ClusterMessage specified serializer
        // write serialized payload size
        //output.writeInt(...);
        // write serialized payload
        //output.writeBytes(...);
    }

    @Override
    public ClusterMessage read(Kryo kryo, Input input,
                               Class<ClusterMessage> type) {
        // TODO Auto-generated method stub
        NodeId sender = (NodeId) kryo.readClassAndObject(input);
        MessageSubject subject = (MessageSubject) kryo.readClassAndObject(input);
        int size = input.readInt();
        byte[] payloadBytes = input.readBytes(size);
        // TODO: deserialize payload using ClusterMessage specified serializer
        Object payload = null;
        return new ClusterMessage(sender, subject, payload);
    }

}
