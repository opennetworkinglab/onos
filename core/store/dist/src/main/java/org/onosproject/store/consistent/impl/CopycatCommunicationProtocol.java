/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.store.consistent.impl;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

import org.onlab.util.Tools;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.MessageSubject;

import net.kuujo.copycat.protocol.AbstractProtocol;
import net.kuujo.copycat.protocol.ProtocolClient;
import net.kuujo.copycat.protocol.ProtocolHandler;
import net.kuujo.copycat.protocol.ProtocolServer;
import net.kuujo.copycat.util.Configurable;

/**
 * Protocol for Copycat communication that employs
 * {@code ClusterCommunicationService}.
 */
public class CopycatCommunicationProtocol extends AbstractProtocol {

    private static final MessageSubject COPYCAT_MESSAGE_SUBJECT =
            new MessageSubject("onos-copycat-message");

    protected ClusterService clusterService;
    protected ClusterCommunicationService clusterCommunicator;

    public CopycatCommunicationProtocol(ClusterService clusterService,
                                        ClusterCommunicationService clusterCommunicator) {
        this.clusterService = clusterService;
        this.clusterCommunicator = clusterCommunicator;
    }

    @Override
    public Configurable copy() {
        return this;
    }

    @Override
    public ProtocolClient createClient(URI uri) {
        NodeId nodeId = uriToNodeId(uri);
        if (nodeId == null) {
            throw new IllegalStateException("Unknown peer " + uri);
        }
        return new Client(nodeId);
    }

    @Override
    public ProtocolServer createServer(URI uri) {
        return new Server();
    }

    private class Server implements ProtocolServer {

        @Override
        public void handler(ProtocolHandler handler) {
            if (handler == null) {
                clusterCommunicator.removeSubscriber(COPYCAT_MESSAGE_SUBJECT);
            } else {
                clusterCommunicator.addSubscriber(COPYCAT_MESSAGE_SUBJECT,
                        ByteBuffer::wrap,
                        handler,
                        Tools::byteBuffertoArray);
                // FIXME: Tools::byteBuffertoArray involves a array copy.
            }
        }

        @Override
        public CompletableFuture<Void> listen() {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> close() {
            clusterCommunicator.removeSubscriber(COPYCAT_MESSAGE_SUBJECT);
            return CompletableFuture.completedFuture(null);
        }
    }

    private class Client implements ProtocolClient {
        private final NodeId peer;

        public Client(NodeId peer) {
            this.peer = peer;
        }

        @Override
        public CompletableFuture<ByteBuffer> write(ByteBuffer request) {
            return clusterCommunicator.sendAndReceive(request,
                    COPYCAT_MESSAGE_SUBJECT,
                    Tools::byteBuffertoArray,
                    ByteBuffer::wrap,
                    peer);
        }

        @Override
        public CompletableFuture<Void> connect() {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> close() {
            return CompletableFuture.completedFuture(null);
        }
    }

    private NodeId uriToNodeId(URI uri) {
        return clusterService.getNodes()
                             .stream()
                             .filter(node -> uri.getHost().equals(node.ip().toString()))
                             .map(ControllerNode::id)
                             .findAny()
                             .orElse(null);
    }
}
