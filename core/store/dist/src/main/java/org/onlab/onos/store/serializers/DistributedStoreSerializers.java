package org.onlab.onos.store.serializers;

import org.onlab.onos.store.common.impl.MastershipBasedTimestamp;
import org.onlab.onos.store.common.impl.Timestamped;
import org.onlab.util.KryoPool;

public final class DistributedStoreSerializers {

    /**
     * KryoPool which can serialize ON.lab misc classes.
     */
    public static final KryoPool COMMON = KryoPool.newBuilder()
            .register(KryoPoolUtil.API)
            .register(Timestamped.class)
            .register(MastershipBasedTimestamp.class, new MastershipBasedTimestampSerializer())
            .build();

    // avoid instantiation
    private DistributedStoreSerializers() {}
}
