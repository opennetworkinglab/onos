package org.onlab.onos.store.serializers;

import org.onlab.onos.net.MastershipRole;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Kryo Serializer for {@link org.onlab.onos.net.MastershipRole}.
 */
public class MastershipRoleSerializer extends Serializer<MastershipRole> {

    @Override
    public MastershipRole read(Kryo kryo, Input input, Class<MastershipRole> type) {
        final String role = kryo.readObject(input, String.class);
        return MastershipRole.valueOf(role);
    }

    @Override
    public void write(Kryo kryo, Output output, MastershipRole object) {
        kryo.writeObject(output, object.toString());
    }

}
