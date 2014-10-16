package org.onlab.onos.store.serializers;

import org.onlab.packet.MacAddress;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Kryo Serializer for {@link MacAddress}.
 */
public class MacAddressSerializer extends Serializer<MacAddress> {

    /**
     * Creates {@link MacAddress} serializer instance.
     */
    public MacAddressSerializer() {
        super(false, true);
    }

    @Override
    public void write(Kryo kryo, Output output, MacAddress object) {
        output.writeBytes(object.getAddress());
    }

    @Override
    public MacAddress read(Kryo kryo, Input input, Class<MacAddress> type) {
        return MacAddress.valueOf(input.readBytes(MacAddress.MAC_ADDRESS_LENGTH));
    }

}
