package org.onlab.onos.store.cluster.impl;

import org.onlab.onos.store.cluster.messaging.ClusterMessage;

/**
 * Simple back interface for interacting with the communications service.
 */
public interface CommunicationsDelegate {

    /**
     * Dispatches the specified message to all registered subscribers.
     *
     * @param message message to be dispatched
     */
    void dispatch(ClusterMessage message);

    /**
     * Sets the sender.
     *
     * @param messageSender message sender
     */
    void setSender(MessageSender messageSender);

}
