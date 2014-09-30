package org.onlab.onos.store.cluster.messaging;

import org.onlab.onos.cluster.NodeId;

import java.util.Set;

/**
 * Service for assisting communications between controller cluster nodes.
 */
public interface ClusterCommunicationService {

    /**
     * Sends a message to the specified controller node.
     *
     * @param message  message to send
     * @param toNodeId node identifier
     * @return true if the message was sent sucessfully; false if there is
     * no stream or if there was an error
     */
    boolean send(ClusterMessage message, NodeId toNodeId);

    /**
     * Adds a new subscriber for the specified message subject.
     *
     * @param subject    message subject
     * @param subscriber message subscriber
     */
    void addSubscriber(MessageSubject subject, MessageSubscriber subscriber);

    /**
     * Removes the specified subscriber from the given message subject.
     *
     * @param subject    message subject
     * @param subscriber message subscriber
     */
    void removeSubscriber(MessageSubject subject, MessageSubscriber subscriber);

    /**
     * Returns the set of subscribers for the specified message subject.
     *
     * @param subject message subject
     * @return set of message subscribers
     */
    Set<MessageSubscriber> getSubscribers(MessageSubject subject);

}
