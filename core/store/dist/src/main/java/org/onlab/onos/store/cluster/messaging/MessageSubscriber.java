package org.onlab.onos.store.cluster.messaging;

/**
 * Represents a message consumer.
 */
public interface MessageSubscriber {

    /**
     * Receives the specified cluster message.
     *
     * @param message message to be received
     */
    void receive(ClusterMessage message);

}
