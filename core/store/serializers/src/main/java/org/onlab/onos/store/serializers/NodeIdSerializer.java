package org.onlab.onos.store.serializers;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.onlab.onos.cluster.NodeId;

/**
 * Kryo Serializer for {@link org.onlab.onos.cluster.NodeId}.
 */
public final class NodeIdSerializer extends Serializer<NodeId> {

    @Override
    public void write(Kryo kryo, Output output, NodeId object) {
        kryo.writeObject(output, object.toString());
    }

    @Override
    public NodeId read(Kryo kryo, Input input, Class<NodeId> type) {
        final String id = kryo.readObject(input, String.class);
        return new NodeId(id);
    }
}
