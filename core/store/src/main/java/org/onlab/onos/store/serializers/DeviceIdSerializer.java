package org.onlab.onos.store.serializers;

import java.net.URI;

import org.onlab.onos.net.DeviceId;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
* Kryo Serializer for {@link DeviceId}.
*/
public final class DeviceIdSerializer extends Serializer<DeviceId> {

    @Override
    public void write(Kryo kryo, Output output, DeviceId object) {
        kryo.writeObject(output, object.uri());
    }

    @Override
    public DeviceId read(Kryo kryo, Input input, Class<DeviceId> type) {
        final URI uri = kryo.readObject(input, URI.class);
        return DeviceId.deviceId(uri);
    }
}
