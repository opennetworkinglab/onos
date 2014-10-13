package org.onlab.onos.store.cluster.messaging;

import java.io.IOException;
import java.util.Set;

import org.onlab.onos.cluster.NodeId;

// TODO: remove IOExceptions?
/**
 * Service for assisting communications between controller cluster nodes.
 */
public interface ClusterCommunicationService {

    /**
     * Broadcast a message to all controller nodes.
     *
     * @param message  message to send
     * @return true if the message was sent successfully to all nodes; false otherwise.
     */
    boolean broadcast(ClusterMessage message) throws IOException;

    /**
     * Sends a message to the specified controller node.
     *
     * @param message  message to send
     * @param toNodeId node identifier
     * @return true if the message was sent successfully; false otherwise.
     */
    boolean unicast(ClusterMessage message, NodeId toNodeId) throws IOException;

    /**
     * Multicast a message to a set of controller nodes.
     *
     * @param message  message to send
     * @return true if the message was sent successfully to all nodes in the group; false otherwise.
     */
    boolean multicast(ClusterMessage message, Set<NodeId> nodeIds) throws IOException;

    /**
     * Adds a new subscriber for the specified message subject.
     *
     * @param subject    message subject
     * @param subscriber message subscriber
     */
    void addSubscriber(MessageSubject subject, ClusterMessageHandler subscriber);
}
