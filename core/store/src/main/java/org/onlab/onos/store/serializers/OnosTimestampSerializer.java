package org.onlab.onos.store.serializers;

import org.onlab.onos.net.ElementId;
import org.onlab.onos.store.impl.OnosTimestamp;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Kryo Serializer for {@link OnosTimestamp}.
 */
public class OnosTimestampSerializer extends Serializer<OnosTimestamp> {

    /**
     * Default constructor.
     */
    public OnosTimestampSerializer() {
        // non-null, immutable
        super(false, true);
    }
    @Override
    public void write(Kryo kryo, Output output, OnosTimestamp object) {
        kryo.writeClassAndObject(output, object.id());
        output.writeInt(object.termNumber());
        output.writeInt(object.sequenceNumber());
    }

    @Override
    public OnosTimestamp read(Kryo kryo, Input input, Class<OnosTimestamp> type) {
        ElementId id = (ElementId) kryo.readClassAndObject(input);
        final int term = input.readInt();
        final int sequence = input.readInt();
        return new OnosTimestamp(id, term, sequence);
    }
}
