package org.onlab.netty;

import org.onlab.util.KryoNamespace;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

//FIXME: Should be move out to test or app
/**
 * Kryo Serializer.
 */
public class KryoSerializer {

    private KryoNamespace serializerPool;

    public KryoSerializer() {
        setupKryoPool();
    }

    /**
     * Sets up the common serialzers pool.
     */
    protected void setupKryoPool() {
        // FIXME Slice out types used in common to separate pool/namespace.
        serializerPool = KryoNamespace.newBuilder()
                .register(ArrayList.class,
                          HashMap.class,
                          ArrayList.class,
                          InternalMessage.class,
                          Endpoint.class,
                          byte[].class
                )
                .build()
                .populate(1);
    }


    public <T> T decode(byte[] data) {
        return serializerPool.deserialize(data);
    }

    public byte[] encode(Object payload) {
        return serializerPool.serialize(payload);
    }

    public <T> T decode(ByteBuffer buffer) {
        return serializerPool.deserialize(buffer);
    }

    public void encode(Object obj, ByteBuffer buffer) {
        serializerPool.serialize(obj, buffer);
    }
}
