package org.onlab.onos.store.common.impl;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

// To be used if Timestamp ever needs to cross bundle boundary.
/**
 * Kryo Serializer for {@link DeviceMastershipBasedTimestamp}.
 */
public class MastershipBasedTimestampSerializer extends Serializer<DeviceMastershipBasedTimestamp> {

    /**
     * Creates a serializer for {@link DeviceMastershipBasedTimestamp}.
     */
    public MastershipBasedTimestampSerializer() {
        // non-null, immutable
        super(false, true);
    }

    @Override
    public void write(Kryo kryo, Output output, DeviceMastershipBasedTimestamp object) {
        output.writeInt(object.termNumber());
        output.writeInt(object.sequenceNumber());
    }

    @Override
    public DeviceMastershipBasedTimestamp read(Kryo kryo, Input input, Class<DeviceMastershipBasedTimestamp> type) {
        final int term = input.readInt();
        final int sequence = input.readInt();
        return new DeviceMastershipBasedTimestamp(term, sequence);
    }
}
