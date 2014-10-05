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

    /**
     * Creates {@link NodeId} serializer instance.
     */
    public NodeIdSerializer() {
        // non-null, immutable
        super(false, true);
    }

    @Override
    public void write(Kryo kryo, Output output, NodeId object) {
        output.writeString(object.toString());
    }

    @Override
    public NodeId read(Kryo kryo, Input input, Class<NodeId> type) {
        final String id = input.readString();
        return new NodeId(id);
    }
}
