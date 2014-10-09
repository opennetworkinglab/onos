package org.onlab.onos.store.device.impl;

import java.util.List;

import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.device.PortDescription;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.store.Timestamped;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Kryo Serializer for {@link InternalPortEvent}.
 */
public class InternalPortEventSerializer extends Serializer<InternalPortEvent> {

    /**
     * Creates a serializer for {@link InternalPortEvent}.
     */
    public InternalPortEventSerializer() {
        // does not accept null
        super(false);
    }

    @Override
    public void write(Kryo kryo, Output output, InternalPortEvent event) {
        kryo.writeClassAndObject(output, event.providerId());
        kryo.writeClassAndObject(output, event.deviceId());
        kryo.writeClassAndObject(output, event.portDescriptions());
    }

    @Override
    public InternalPortEvent read(Kryo kryo, Input input,
                               Class<InternalPortEvent> type) {
        ProviderId providerId = (ProviderId) kryo.readClassAndObject(input);
        DeviceId deviceId = (DeviceId) kryo.readClassAndObject(input);
        Timestamped<List<PortDescription>> portDescriptions
            = (Timestamped<List<PortDescription>>) kryo.readClassAndObject(input);

        return new InternalPortEvent(providerId, deviceId, portDescriptions);
    }
}
