package org.onlab.onos.store.cluster.messaging;

/**
 * Interface for handling cluster messages.
 */
public interface ClusterMessageHandler {

    /**
     * Handles/Processes the cluster message.
     * @param message cluster message.
     */
    public void handle(ClusterMessage message);
}