package org.onlab.onos.store.serializers;

import org.onlab.packet.IpPrefix;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Kryo Serializer for {@link IpPrefix}.
 */
public final class IpPrefixSerializer extends Serializer<IpPrefix> {

    /**
     * Creates {@link IpPrefix} serializer instance.
     */
    public IpPrefixSerializer() {
        // non-null, immutable
        super(false, true);
    }

    @Override
    public void write(Kryo kryo, Output output,
            IpPrefix object) {
        byte[] octs = object.toOctets();
        output.writeInt(octs.length);
        output.writeBytes(octs);
        output.writeInt(object.prefixLength());
    }

    @Override
    public IpPrefix read(Kryo kryo, Input input,
            Class<IpPrefix> type) {
        int octLen = input.readInt();
        byte[] octs = new byte[octLen];
        input.read(octs);
        int prefLen = input.readInt();
        return IpPrefix.valueOf(octs, prefLen);
    }
}
