package org.onlab.onos.store.cluster.impl;

import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.store.cluster.messaging.ClusterMessage;

/**
 * Created by tom on 9/29/14.
 */
public interface MessageSender {

    /**
     * Sends the specified message to the given cluster node.
     *
     * @param nodeId  node identifier
     * @param message mesage to send
     * @return true if the message was sent sucessfully; false if there is
     * no stream or if there was an error
     */
    boolean send(NodeId nodeId, ClusterMessage message);

}
