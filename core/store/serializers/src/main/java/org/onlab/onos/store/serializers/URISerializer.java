package org.onlab.onos.store.serializers;

import java.net.URI;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Serializer for {@link URI}.
 */
public class URISerializer extends Serializer<URI> {

    /**
     * Creates {@link URI} serializer instance.
     */
    public URISerializer() {
        super(false);
    }

    @Override
    public void write(Kryo kryo, Output output, URI object) {
        output.writeString(object.toString());
    }

    @Override
    public URI read(Kryo kryo, Input input, Class<URI> type) {
        return URI.create(input.readString());
    }
}
