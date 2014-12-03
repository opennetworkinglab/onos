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
package org.onosproject.store.cluster.messaging.impl;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.IOException;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.netty.Endpoint;
import org.onlab.netty.Message;
import org.onlab.netty.MessageHandler;
import org.onlab.netty.MessagingService;
import org.onlab.netty.NettyMessagingService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.ClusterMessage;
import org.onosproject.store.cluster.messaging.ClusterMessageHandler;
import org.onosproject.store.cluster.messaging.MessageSubject;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.serializers.KryoSerializer;
import org.onosproject.store.serializers.impl.ClusterMessageSerializer;
import org.onosproject.store.serializers.impl.MessageSubjectSerializer;
import org.onlab.util.KryoNamespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;

@Component(immediate = true)
@Service
public class ClusterCommunicationManager
        implements ClusterCommunicationService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private ClusterService clusterService;

    // TODO: This probably should not be a OSGi service.
    private MessagingService messagingService;

    private static final KryoSerializer SERIALIZER = new KryoSerializer() {
        @Override
        protected void setupKryoPool() {
            serializerPool = KryoNamespace.newBuilder()
                    .register(KryoNamespaces.API)
                    .nextId(KryoNamespaces.BEGIN_USER_CUSTOM_ID)
                    .register(new ClusterMessageSerializer(), ClusterMessage.class)
                    .register(new MessageSubjectSerializer(), MessageSubject.class)
                    .build();
        }

    };

    @Activate
    public void activate() {
        ControllerNode localNode = clusterService.getLocalNode();
        NettyMessagingService netty = new NettyMessagingService(localNode.ip().toString(), localNode.tcpPort());
        // FIXME: workaround until it becomes a service.
        try {
            netty.activate();
        } catch (Exception e) {
            log.error("NettyMessagingService#activate", e);
        }
        messagingService = netty;
        log.info("Started on {}:{}", localNode.ip(), localNode.tcpPort());
    }

    @Deactivate
    public void deactivate() {
        // TODO: cleanup messageingService if needed.
        // FIXME: workaround until it becomes a service.
        try {
            ((NettyMessagingService) messagingService).deactivate();
        } catch (Exception e) {
            log.error("NettyMessagingService#deactivate", e);
        }
        log.info("Stopped");
    }

    @Override
    public boolean broadcast(ClusterMessage message) throws IOException {
        boolean ok = true;
        final ControllerNode localNode = clusterService.getLocalNode();
        for (ControllerNode node : clusterService.getNodes()) {
            if (!node.equals(localNode)) {
                ok = unicastUnchecked(message, node.id()) && ok;
            }
        }
        return ok;
    }

    @Override
    public boolean broadcastIncludeSelf(ClusterMessage message) throws IOException {
        boolean ok = true;
        for (ControllerNode node : clusterService.getNodes()) {
            ok = unicastUnchecked(message, node.id()) && ok;
        }
        return ok;
    }

    @Override
    public boolean multicast(ClusterMessage message, Set<NodeId> nodes) throws IOException {
        boolean ok = true;
        final ControllerNode localNode = clusterService.getLocalNode();
        for (NodeId nodeId : nodes) {
            if (!nodeId.equals(localNode.id())) {
                ok = unicastUnchecked(message, nodeId) && ok;
            }
        }
        return ok;
    }

    @Override
    public boolean unicast(ClusterMessage message, NodeId toNodeId) throws IOException {
        ControllerNode node = clusterService.getNode(toNodeId);
        checkArgument(node != null, "Unknown nodeId: %s", toNodeId);
        Endpoint nodeEp = new Endpoint(node.ip().toString(), node.tcpPort());
        try {
            messagingService.sendAsync(nodeEp,
                    message.subject().value(), SERIALIZER.encode(message));
            return true;
        } catch (IOException e) {
            log.trace("Failed to send cluster message to nodeId: " + toNodeId, e);
            throw e;
        }
    }

    private boolean unicastUnchecked(ClusterMessage message, NodeId toNodeId) throws IOException {
        try {
            return unicast(message, toNodeId);
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public ListenableFuture<byte[]> sendAndReceive(ClusterMessage message, NodeId toNodeId) throws IOException {
        ControllerNode node = clusterService.getNode(toNodeId);
        checkArgument(node != null, "Unknown nodeId: %s", toNodeId);
        Endpoint nodeEp = new Endpoint(node.ip().toString(), node.tcpPort());
        try {
            return messagingService.sendAndReceive(nodeEp, message.subject().value(), SERIALIZER.encode(message));

        } catch (IOException e) {
            log.trace("Failed interaction with remote nodeId: " + toNodeId, e);
            throw e;
        }
    }

    @Override
    public void addSubscriber(MessageSubject subject,
                              ClusterMessageHandler subscriber) {
        messagingService.registerHandler(subject.value(), new InternalClusterMessageHandler(subscriber));
    }

    @Override
    public void removeSubscriber(MessageSubject subject) {
        messagingService.unregisterHandler(subject.value());
    }

    private final class InternalClusterMessageHandler implements MessageHandler {

        private final ClusterMessageHandler handler;

        public InternalClusterMessageHandler(ClusterMessageHandler handler) {
            this.handler = handler;
        }

        @Override
        public void handle(Message message) {
            final ClusterMessage clusterMessage;
            try {
                clusterMessage = SERIALIZER.decode(message.payload());
            } catch (Exception e) {
                log.error("Failed decoding ClusterMessage", e);
                throw e;
            }
            try {
                handler.handle(new InternalClusterMessage(clusterMessage, message));
            } catch (Exception e) {
                log.error("Exception caught handling {}", clusterMessage, e);
                throw e;
            }
        }
    }

    public static final class InternalClusterMessage extends ClusterMessage {

        private final Message rawMessage;

        public InternalClusterMessage(ClusterMessage clusterMessage, Message rawMessage) {
            super(clusterMessage.sender(), clusterMessage.subject(), clusterMessage.payload());
            this.rawMessage = rawMessage;
        }

        @Override
        public void respond(byte[] response) throws IOException {
            rawMessage.respond(response);
        }
    }
}
