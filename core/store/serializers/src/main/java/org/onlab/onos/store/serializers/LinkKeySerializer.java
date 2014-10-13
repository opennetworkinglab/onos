package org.onlab.onos.store.serializers;

import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.LinkKey;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Kryo Serializer for {@link LinkKey}.
 */
public class LinkKeySerializer extends Serializer<LinkKey> {

    /**
     * Creates {@link LinkKey} serializer instance.
     */
    public LinkKeySerializer() {
        // non-null, immutable
        super(false, true);
    }

    @Override
    public void write(Kryo kryo, Output output, LinkKey object) {
        kryo.writeClassAndObject(output, object.src());
        kryo.writeClassAndObject(output, object.dst());
    }

    @Override
    public LinkKey read(Kryo kryo, Input input, Class<LinkKey> type) {
        ConnectPoint src = (ConnectPoint) kryo.readClassAndObject(input);
        ConnectPoint dst = (ConnectPoint) kryo.readClassAndObject(input);
        return LinkKey.linkKey(src, dst);
    }
}
