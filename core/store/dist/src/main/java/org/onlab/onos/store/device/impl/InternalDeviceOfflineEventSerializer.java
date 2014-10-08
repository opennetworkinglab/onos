package org.onlab.onos.store.device.impl;

import org.onlab.onos.net.DeviceId;
import org.onlab.onos.store.Timestamp;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Kryo Serializer for {@link InternalDeviceOfflineEvent}.
 */
public class InternalDeviceOfflineEventSerializer extends Serializer<InternalDeviceOfflineEvent> {

    /**
     * Creates a serializer for {@link InternalDeviceOfflineEvent}.
     */
    public InternalDeviceOfflineEventSerializer() {
        // does not accept null
        super(false);
    }

    @Override
    public void write(Kryo kryo, Output output, InternalDeviceOfflineEvent event) {
        kryo.writeClassAndObject(output, event.deviceId());
        kryo.writeClassAndObject(output, event.timestamp());
    }

    @Override
    public InternalDeviceOfflineEvent read(Kryo kryo, Input input,
                               Class<InternalDeviceOfflineEvent> type) {
        DeviceId deviceId = (DeviceId) kryo.readClassAndObject(input);
        Timestamp timestamp = (Timestamp) kryo.readClassAndObject(input);

        return new InternalDeviceOfflineEvent(deviceId, timestamp);
    }
}
