package org.onlab.onos.store.serializers;

import org.onlab.packet.IpAddress;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Kryo Serializer for {@link IpAddress}.
 */
public class IpAddressSerializer extends Serializer<IpAddress> {

    /**
     * Creates {@link IpAddress} serializer instance.
     */
    public IpAddressSerializer() {
        // non-null, immutable
        super(false, true);
    }

    @Override
    public void write(Kryo kryo, Output output, IpAddress object) {
        byte[] octs = object.toOctets();
        output.writeInt(octs.length);
        output.writeBytes(octs);
        output.writeInt(object.prefixLength());
    }

    @Override
    public IpAddress read(Kryo kryo, Input input, Class<IpAddress> type) {
        final int octLen = input.readInt();
        byte[] octs = new byte[octLen];
        input.readBytes(octs);
        int prefLen = input.readInt();
        return IpAddress.valueOf(octs, prefLen);
    }

}
