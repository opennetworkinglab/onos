package org.onlab.onos.store.cluster.messaging;

import org.onlab.onos.cluster.NodeId;

/**
 * Represents a message consumer.
 */
public interface MessageSubscriber {

    /**
     * Receives the specified cluster message.
     *
     * @param message    message to be received
     * @param fromNodeId node from which the message was received
     */
    void receive(Object messagePayload, NodeId fromNodeId);

}
