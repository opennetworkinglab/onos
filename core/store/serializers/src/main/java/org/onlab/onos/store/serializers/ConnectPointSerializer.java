package org.onlab.onos.store.serializers;

import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.ElementId;
import org.onlab.onos.net.PortNumber;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Kryo Serializer for {@link ConnectPointSerializer}.
 */
public class ConnectPointSerializer extends Serializer<ConnectPoint> {

    /**
     * Default constructor.
     */
    public ConnectPointSerializer() {
        // non-null, immutable
        super(false, true);
    }

    @Override
    public void write(Kryo kryo, Output output, ConnectPoint object) {
        kryo.writeClassAndObject(output, object.elementId());
        kryo.writeClassAndObject(output, object.port());
    }

    @Override
    public ConnectPoint read(Kryo kryo, Input input, Class<ConnectPoint> type) {
        ElementId elementId = (ElementId) kryo.readClassAndObject(input);
        PortNumber portNumber = (PortNumber) kryo.readClassAndObject(input);
        return new ConnectPoint(elementId, portNumber);
    }
}
