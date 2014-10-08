package org.onlab.onos.store.device.impl;

import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.device.PortDescription;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.store.common.impl.Timestamped;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Kryo Serializer for {@link InternalPortStatusEvent}.
 */
public class InternalPortStatusEventSerializer extends Serializer<InternalPortStatusEvent> {

    /**
     * Creates a serializer for {@link InternalPortStatusEvent}.
     */
    public InternalPortStatusEventSerializer() {
        // does not accept null
        super(false);
    }

    @Override
    public void write(Kryo kryo, Output output, InternalPortStatusEvent event) {
        kryo.writeClassAndObject(output, event.providerId());
        kryo.writeClassAndObject(output, event.deviceId());
        kryo.writeClassAndObject(output, event.portDescription());
    }

    @Override
    public InternalPortStatusEvent read(Kryo kryo, Input input,
                               Class<InternalPortStatusEvent> type) {
        ProviderId providerId = (ProviderId) kryo.readClassAndObject(input);
        DeviceId deviceId = (DeviceId) kryo.readClassAndObject(input);
        Timestamped<PortDescription> portDescription = (Timestamped<PortDescription>) kryo.readClassAndObject(input);

        return new InternalPortStatusEvent(providerId, deviceId, portDescription);
    }
}
