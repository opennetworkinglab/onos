package org.onlab.netty;

import org.onlab.util.KryoPool;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Kryo Serializer.
 */
public class KryoSerializer implements Serializer {

    private KryoPool serializerPool;

    public KryoSerializer() {
        setupKryoPool();
    }

    /**
     * Sets up the common serialzers pool.
     */
    protected void setupKryoPool() {
        // FIXME Slice out types used in common to separate pool/namespace.
        serializerPool = KryoPool.newBuilder()
                .register(ArrayList.class,
                          HashMap.class,
                          ArrayList.class,
                          InternalMessage.class,
                          Endpoint.class
                )
                .build()
                .populate(1);
    }


    @Override
    public <T> T decode(byte[] data) {
        return serializerPool.deserialize(data);
    }

    @Override
    public byte[] encode(Object payload) {
        return serializerPool.serialize(payload);
    }

    @Override
    public <T> T decode(ByteBuffer buffer) {
        return serializerPool.deserialize(buffer);
    }

    @Override
    public void encode(Object obj, ByteBuffer buffer) {
        serializerPool.serialize(obj, buffer);
    }
}
