package org.onlab.onos.store.serializers;

import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.net.device.DeviceMastershipTerm;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Kryo Serializer for {@link org.onlab.onos.net.device.DeviceMastershipTerm}.
 */
public class MastershipTermSerializer extends Serializer<DeviceMastershipTerm> {

    /**
     * Creates {@link DeviceMastershipTerm} serializer instance.
     */
    public MastershipTermSerializer() {
        // non-null, immutable
        super(false, true);
    }

    @Override
    public DeviceMastershipTerm read(Kryo kryo, Input input, Class<DeviceMastershipTerm> type) {
        final NodeId node = new NodeId(input.readString());
        final int term = input.readInt();
        return DeviceMastershipTerm.of(node, term);
    }

    @Override
    public void write(Kryo kryo, Output output, DeviceMastershipTerm object) {
        output.writeString(object.master().toString());
        output.writeInt(object.termNumber());
    }

}
