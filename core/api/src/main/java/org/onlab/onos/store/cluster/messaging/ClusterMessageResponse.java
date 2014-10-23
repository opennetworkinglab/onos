package org.onlab.onos.store.cluster.messaging;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.onlab.onos.cluster.NodeId;

public interface ClusterMessageResponse extends Future<byte[]> {

    public NodeId sender();

    // TODO InterruptedException, ExecutionException removed from original
    // Future declaration. Revisit if we ever need those.
    @Override
    public byte[] get(long timeout, TimeUnit unit) throws TimeoutException;

}
