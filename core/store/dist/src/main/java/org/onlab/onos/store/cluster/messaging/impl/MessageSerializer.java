package org.onlab.onos.store.cluster.messaging.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.store.cluster.messaging.MessageSubject;
import org.onlab.onos.store.cluster.messaging.SerializationService;
import org.onlab.onos.store.serializers.KryoPoolUtil;
import org.onlab.util.KryoPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//FIXME: not used any more? remove
/**
 * Factory for parsing messages sent between cluster members.
 */
@Component(immediate = true)
@Service
public class MessageSerializer implements SerializationService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final int METADATA_LENGTH = 12; // 8 + 4
    private static final int LENGTH_OFFSET = 8;

    private static final long MARKER = 0xfeedcafebeaddeadL;

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
                // TODO: Should MessageSubject be in API bundle?
                .register(MessageSubject.class)
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
}
