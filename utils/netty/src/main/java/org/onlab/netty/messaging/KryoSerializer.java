package org.onlab.netty;

import org.onlab.util.KryoPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Kryo Serializer.
 */
public class KryoSerializer implements Serializer {

    private final Logger log = LoggerFactory.getLogger(getClass());

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
                          ArrayList.class
                )
                .build()
                .populate(1);
    }


    @Override
    public Object decode(byte[] data) {
        return serializerPool.deserialize(data);
    }

    @Override
    public byte[] encode(Object payload) {
        return serializerPool.serialize(payload);
    }
}
