package org.onlab.onos.store.serializers;

import org.onlab.onos.net.device.DeviceMastershipRole;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Kryo Serializer for {@link org.onlab.onos.net.device.DeviceMastershipRole}.
 */
public class MastershipRoleSerializer extends Serializer<DeviceMastershipRole> {

    /**
     * Creates {@link DeviceMastershipRole} serializer instance.
     */
    public MastershipRoleSerializer() {
        // non-null, immutable
        super(false, true);
    }

    @Override
    public DeviceMastershipRole read(Kryo kryo, Input input, Class<DeviceMastershipRole> type) {
        final String role = kryo.readObject(input, String.class);
        return DeviceMastershipRole.valueOf(role);
    }

    @Override
    public void write(Kryo kryo, Output output, DeviceMastershipRole object) {
        kryo.writeObject(output, object.toString());
    }

}
