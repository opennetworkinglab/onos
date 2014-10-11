package org.onlab.onos.store.serializers;

import org.onlab.onos.store.impl.MastershipBasedTimestamp;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

// To be used if Timestamp ever needs to cross bundle boundary.
/**
 * Kryo Serializer for {@link MastershipBasedTimestamp}.
 */
public class MastershipBasedTimestampSerializer extends Serializer<MastershipBasedTimestamp> {

    /**
     * Creates a serializer for {@link MastershipBasedTimestamp}.
     */
    public MastershipBasedTimestampSerializer() {
        // non-null, immutable
        super(false, true);
    }

    @Override
    public void write(Kryo kryo, Output output, MastershipBasedTimestamp object) {
        output.writeInt(object.termNumber());
        output.writeInt(object.sequenceNumber());
    }

    @Override
    public MastershipBasedTimestamp read(Kryo kryo, Input input, Class<MastershipBasedTimestamp> type) {
        final int term = input.readInt();
        final int sequence = input.readInt();
        return new MastershipBasedTimestamp(term, sequence);
    }
}
