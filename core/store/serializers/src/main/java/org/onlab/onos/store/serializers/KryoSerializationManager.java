package org.onlab.onos.store.serializers;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * Serialization service using Kryo.
 */
@Component(immediate = true)
@Service
public class KryoSerializationManager implements KryoSerializationService {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private KryoPool serializerPool;


    @Activate
    public void activate() {
        setupKryoPool();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    /**
     * Sets up the common serialzers pool.
     */
    protected void setupKryoPool() {
        serializerPool = KryoPool.newBuilder()
                .register(KryoPoolUtil.API)
                .build()
                .populate(1);
    }

    @Override
    public byte[] encode(final Object obj) {
        return serializerPool.serialize(obj);
    }

    @Override
    public <T> T decode(final byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        return serializerPool.deserialize(bytes);
    }

    @Override
    public void encode(Object obj, ByteBuffer buffer) {
        serializerPool.serialize(obj, buffer);
    }

    @Override
    public <T> T decode(ByteBuffer buffer) {
        return serializerPool.deserialize(buffer);
    }

}
