package org.onlab.onos.store.cluster.messaging;

public interface ClusterMessageHandler {
    public void handle(ClusterMessage message);
}