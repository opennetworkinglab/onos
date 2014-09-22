package org.onlab.onos.store.device.impl;

import java.util.ArrayList;
import java.util.Collection;

import org.onlab.onos.net.DefaultPort;
import org.onlab.onos.net.Element;
import org.onlab.onos.net.PortNumber;
import org.onlab.packet.IpPrefix;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.CollectionSerializer;
import com.google.common.collect.ImmutableSet;

// TODO move to util, etc.
public final class DefaultPortSerializer extends
        Serializer<DefaultPort> {

    private final CollectionSerializer ipAddrSerializer
        = new CollectionSerializer(IpPrefix.class,
                            new IpPrefixSerializer(), false);

    public DefaultPortSerializer() {
        // non-null, immutable
        super(false, true);
    }

    @Override
    public void write(Kryo kryo, Output output, DefaultPort object) {
        kryo.writeClassAndObject(output, object.element());
        kryo.writeObject(output, object.number());
        output.writeBoolean(object.isEnabled());
        kryo.writeObject(output, object.ipAddresses(),
                ipAddrSerializer);
    }

    @Override
    public DefaultPort read(Kryo kryo, Input input,
            Class<DefaultPort> type) {
        Element element = (Element) kryo.readClassAndObject(input);
        PortNumber number = kryo.readObject(input, PortNumber.class);
        boolean isEnabled = input.readBoolean();
        @SuppressWarnings("unchecked")
        Collection<IpPrefix> ipAddresses = kryo.readObject(
                    input, ArrayList.class, ipAddrSerializer);

        return new DefaultPort(element, number, isEnabled,
                            ImmutableSet.copyOf(ipAddresses));
    }
}
