package org.onlab.onos.store.serializers;

import org.onlab.onos.cluster.MastershipTerm;
import org.onlab.onos.cluster.NodeId;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Kryo Serializer for {@link org.onlab.onos.cluster.MastershipTerm}.
 */
public class MastershipTermSerializer extends Serializer<MastershipTerm> {

    @Override
    public MastershipTerm read(Kryo kryo, Input input, Class<MastershipTerm> type) {
        final NodeId node = new NodeId(kryo.readObject(input, String.class));
        final int term = input.readInt();
        return MastershipTerm.of(node, term);
    }

    @Override
    public void write(Kryo kryo, Output output, MastershipTerm object) {
        output.writeString(object.master().toString());
        output.writeInt(object.termNumber());
    }

}
