package org.onlab.onos.store.serializers;

import org.onlab.onos.store.impl.MastershipBasedTimestamp;
import org.onlab.onos.store.impl.Timestamped;
import org.onlab.onos.store.impl.WallClockTimestamp;
import org.onlab.util.KryoNamespace;

public final class DistributedStoreSerializers {

    /**
     * KryoNamespace which can serialize ON.lab misc classes.
     */
    public static final KryoNamespace COMMON = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(Timestamped.class)
            .register(MastershipBasedTimestamp.class, new MastershipBasedTimestampSerializer())
            .register(WallClockTimestamp.class)
            .build();

    // avoid instantiation
    private DistributedStoreSerializers() {}
}
