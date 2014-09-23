package org.onlab.onos.store.device.impl;

import org.onlab.onos.net.PortNumber;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

// TODO move to util, etc.
/**
 * Serializer for {@link PortNumber}.
 */
public final class PortNumberSerializer extends
        Serializer<PortNumber> {

    /**
     * Default constructor.
     */
    public PortNumberSerializer() {
        // non-null, immutable
        super(false, true);
    }

    @Override
    public void write(Kryo kryo, Output output, PortNumber object) {
        output.writeLong(object.toLong());
    }

    @Override
    public PortNumber read(Kryo kryo, Input input,
            Class<PortNumber> type) {
        return PortNumber.portNumber(input.readLong());
    }
}
