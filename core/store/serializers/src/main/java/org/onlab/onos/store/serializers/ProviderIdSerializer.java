package org.onlab.onos.store.serializers;

import org.onlab.onos.net.provider.ProviderId;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Serializer for {@link ProviderId}.
 */
public class ProviderIdSerializer extends Serializer<ProviderId> {

    /**
     * Creates {@link ProviderId} serializer instance.
     */
    public ProviderIdSerializer() {
        // non-null, immutable
        super(false, true);
    }

    @Override
    public void write(Kryo kryo, Output output, ProviderId object) {
        output.writeString(object.scheme());
        output.writeString(object.id());
        output.writeBoolean(object.isAncillary());
    }

    @Override
    public ProviderId read(Kryo kryo, Input input, Class<ProviderId> type) {
        String scheme = input.readString();
        String id = input.readString();
        boolean isAncillary = input.readBoolean();
        return new ProviderId(scheme, id, isAncillary);
    }

}
