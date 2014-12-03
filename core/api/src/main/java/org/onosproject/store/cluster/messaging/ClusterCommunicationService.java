/*
 * Copyright 2014 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.store.cluster.messaging;

import java.io.IOException;
import java.util.Set;

import org.onosproject.cluster.NodeId;

import com.google.common.util.concurrent.ListenableFuture;

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
     * @throws IOException when I/O exception of some sort has occurred
     */
    boolean broadcast(ClusterMessage message) throws IOException;

    /**
     * Broadcast a message to all controller nodes including self.
     *
     * @param message  message to send
     * @return true if the message was sent successfully to all nodes; false otherwise.
     * @throws IOException when I/O exception of some sort has occurred
     */
    boolean broadcastIncludeSelf(ClusterMessage message) throws IOException;

    /**
     * Sends a message to the specified controller node.
     *
     * @param message  message to send
     * @param toNodeId node identifier
     * @return true if the message was sent successfully; false otherwise.
     * @throws IOException when I/O exception of some sort has occurred
     */
    boolean unicast(ClusterMessage message, NodeId toNodeId) throws IOException;

    /**
     * Multicast a message to a set of controller nodes.
     *
     * @param message  message to send
     * @param nodeIds  recipient node identifiers
     * @return true if the message was sent successfully to all nodes in the group; false otherwise.
     * @throws IOException when I/O exception of some sort has occurred
     */
    boolean multicast(ClusterMessage message, Set<NodeId> nodeIds) throws IOException;

    /**
     * Sends a message synchronously.
     * @param message message to send
     * @param toNodeId recipient node identifier
     * @return reply future.
     * @throws IOException when I/O exception of some sort has occurred
     */
    ListenableFuture<byte[]> sendAndReceive(ClusterMessage message, NodeId toNodeId) throws IOException;

    /**
     * Adds a new subscriber for the specified message subject.
     *
     * @param subject    message subject
     * @param subscriber message subscriber
     */
    void addSubscriber(MessageSubject subject, ClusterMessageHandler subscriber);

    /**
     * Removes a subscriber for the specified message subject.
     *
     * @param subject    message subject
     */
    void removeSubscriber(MessageSubject subject);

}
