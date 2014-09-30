package org.onlab.onos.store.serializers;

import org.onlab.onos.net.DefaultPort;
import org.onlab.onos.net.Element;
import org.onlab.onos.net.PortNumber;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Kryo Serializer for {@link DefaultPort}.
 */
public final class DefaultPortSerializer extends
        Serializer<DefaultPort> {

    /**
     * Default constructor.
     */
    public DefaultPortSerializer() {
        // non-null, immutable
        super(false, true);
    }

    @Override
    public void write(Kryo kryo, Output output, DefaultPort object) {
        kryo.writeClassAndObject(output, object.element());
        kryo.writeObject(output, object.number());
        output.writeBoolean(object.isEnabled());
    }

    @Override
    public DefaultPort read(Kryo kryo, Input input,
            Class<DefaultPort> type) {
        Element element = (Element) kryo.readClassAndObject(input);
        PortNumber number = kryo.readObject(input, PortNumber.class);
        boolean isEnabled = input.readBoolean();

        return new DefaultPort(element, number, isEnabled);
    }
}
