package org.onlab.onos.store.serializers;

import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.HostLocation;
import org.onlab.onos.net.PortNumber;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
* Kryo Serializer for {@link HostLocation}.
*/
public class HostLocationSerializer extends Serializer<HostLocation> {

    /**
     * Creates {@link HostLocation} serializer instance.
     */
    public HostLocationSerializer() {
        // non-null, immutable
        super(false, true);
    }

    @Override
    public void write(Kryo kryo, Output output, HostLocation object) {
        kryo.writeClassAndObject(output, object.deviceId());
        kryo.writeClassAndObject(output, object.port());
        output.writeLong(object.time());
    }

    @Override
    public HostLocation read(Kryo kryo, Input input, Class<HostLocation> type) {
        DeviceId deviceId = (DeviceId) kryo.readClassAndObject(input);
        PortNumber portNumber = (PortNumber) kryo.readClassAndObject(input);
        long time = input.readLong();
        return new HostLocation(deviceId, portNumber, time);
    }

}
