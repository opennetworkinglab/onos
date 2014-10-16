package org.onlab.onos.store.serializers;

import java.util.List;
import java.util.Map;

import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.net.MastershipRole;
import org.onlab.onos.store.mastership.impl.RoleValue;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Serializer for RoleValues used by {@link DistributedMastershipStore}
 */
public class RoleValueSerializer extends Serializer<RoleValue> {

    //RoleValues are assumed to hold a Map of MastershipRoles (an enum)
    //to a List of NodeIds.

    @Override
    public RoleValue read(Kryo kryo, Input input, Class<RoleValue> type) {
        RoleValue rv = new RoleValue();
        int size = input.readInt();
        for (int i = 0; i < size; i++) {
            MastershipRole role = MastershipRole.values()[input.readInt()];
            int s = input.readInt();
            for (int j = 0; j < s; j++) {
                rv.add(role, new NodeId(input.readString()));
            }
        }
        return rv;
    }

    @Override
    public void write(Kryo kryo, Output output, RoleValue type) {
        output.writeInt(type.value().size());

        for (Map.Entry<MastershipRole, List<NodeId>> el :
                type.value().entrySet()) {
            output.writeInt(el.getKey().ordinal());

            List<NodeId> nodes = el.getValue();
            output.writeInt(nodes.size());
            for (NodeId n : nodes) {
                output.writeString(n.toString());
            }
        }
    }

}
