package org.onlab.onos.store.cluster.messaging;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.onlab.onos.cluster.NodeId;

public interface ClusterMessageResponse {
    public NodeId sender();
    public byte[] get(long timeout, TimeUnit timeunit) throws TimeoutException;
    public byte[] get(long timeout) throws InterruptedException;
}
