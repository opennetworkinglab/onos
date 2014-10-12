package org.onlab.onos.store.serializers;

import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.mastership.MastershipTerm;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Kryo Serializer for {@link org.onlab.onos.mastership.MastershipTerm}.
 */
public class MastershipTermSerializer extends Serializer<MastershipTerm> {

    /**
     * Creates {@link MastershipTerm} serializer instance.
     */
    public MastershipTermSerializer() {
        // non-null, immutable
        super(false, true);
    }

    @Override
    public MastershipTerm read(Kryo kryo, Input input, Class<MastershipTerm> type) {
        final NodeId node = new NodeId(input.readString());
        final int term = input.readInt();
        return MastershipTerm.of(node, term);
    }

    @Override
    public void write(Kryo kryo, Output output, MastershipTerm object) {
        output.writeString(object.master().toString());
        output.writeInt(object.termNumber());
    }

}
