package org.onlab.onos.store.device.impl;

import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.device.DeviceDescription;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.store.common.impl.Timestamped;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Kryo Serializer for {@link InternalDeviceEvent}.
 */
public class InternalDeviceEventSerializer extends Serializer<InternalDeviceEvent> {

    /**
     * Creates a serializer for {@link InternalDeviceEvent}.
     */
    public InternalDeviceEventSerializer() {
        // does not accept null
        super(false);
    }

    @Override
    public void write(Kryo kryo, Output output, InternalDeviceEvent event) {
        kryo.writeClassAndObject(output, event.providerId());
        kryo.writeClassAndObject(output, event.deviceId());
        kryo.writeClassAndObject(output, event.deviceDescription());
    }

    @Override
    public InternalDeviceEvent read(Kryo kryo, Input input,
                               Class<InternalDeviceEvent> type) {
        ProviderId providerId = (ProviderId) kryo.readClassAndObject(input);
        DeviceId deviceId = (DeviceId) kryo.readClassAndObject(input);
        Timestamped<DeviceDescription> deviceDescription = (Timestamped<DeviceDescription>) kryo.readClassAndObject(input);

        return new InternalDeviceEvent(providerId, deviceId, deviceDescription);
    }
}
