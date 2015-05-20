/*
 * Copyright 2014-2015 Open Networking Laboratory
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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.Tools;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.ClusterMessage;
import org.onosproject.store.cluster.messaging.ClusterMessageHandler;
import org.onosproject.store.cluster.messaging.Endpoint;
import org.onosproject.store.cluster.messaging.MessageSubject;
import org.onosproject.store.cluster.messaging.MessagingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@Component(immediate = true)
@Service
public class ClusterCommunicationManager
        implements ClusterCommunicationService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MessagingService messagingService;

    private NodeId localNodeId;

    @Activate
    public void activate() {
        localNodeId = clusterService.getLocalNode().id();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public <M> void broadcast(M message,
                              MessageSubject subject,
                              Function<M, byte[]> encoder) {
        multicast(message,
                  subject,
                  encoder,
                  clusterService.getNodes()
                      .stream()
                      .filter(node -> !Objects.equal(node, clusterService.getLocalNode()))
                      .map(ControllerNode::id)
                      .collect(Collectors.toSet()));
    }

    @Override
    public <M> void broadcastIncludeSelf(M message,
                                         MessageSubject subject,
                                         Function<M, byte[]> encoder) {
        multicast(message,
                  subject,
                  encoder,
                  clusterService.getNodes()
                      .stream()
                      .map(ControllerNode::id)
                      .collect(Collectors.toSet()));
    }

    @Override
    public <M> CompletableFuture<Void> unicast(M message,
                                               MessageSubject subject,
                                               Function<M, byte[]> encoder,
                                               NodeId toNodeId) {
        try {
            byte[] payload = new ClusterMessage(
                    localNodeId,
                    subject,
                    encoder.apply(message)).getBytes();
            return doUnicast(subject, payload, toNodeId);
        } catch (Exception e) {
            return Tools.exceptionalFuture(e);
        }
    }

    @Override
    public <M> void multicast(M message,
                              MessageSubject subject,
                              Function<M, byte[]> encoder,
                              Set<NodeId> nodes) {
        byte[] payload = new ClusterMessage(
                localNodeId,
                subject,
                encoder.apply(message)).getBytes();
        nodes.forEach(nodeId -> doUnicast(subject, payload, nodeId));
    }

    @Override
    public <M, R> CompletableFuture<R> sendAndReceive(M message,
                                                      MessageSubject subject,
                                                      Function<M, byte[]> encoder,
                                                      Function<byte[], R> decoder,
                                                      NodeId toNodeId) {
        try {
            ClusterMessage envelope = new ClusterMessage(
                    clusterService.getLocalNode().id(),
                    subject,
                    encoder.apply(message));
            return sendAndReceive(subject, envelope.getBytes(), toNodeId).thenApply(decoder);
        } catch (Exception e) {
            return Tools.exceptionalFuture(e);
        }
    }

    private CompletableFuture<Void> doUnicast(MessageSubject subject, byte[] payload, NodeId toNodeId) {
        ControllerNode node = clusterService.getNode(toNodeId);
        checkArgument(node != null, "Unknown nodeId: %s", toNodeId);
        Endpoint nodeEp = new Endpoint(node.ip(), node.tcpPort());
        return messagingService.sendAsync(nodeEp, subject.value(), payload);
    }

    private CompletableFuture<byte[]> sendAndReceive(MessageSubject subject, byte[] payload, NodeId toNodeId) {
        ControllerNode node = clusterService.getNode(toNodeId);
        checkArgument(node != null, "Unknown nodeId: %s", toNodeId);
        Endpoint nodeEp = new Endpoint(node.ip(), node.tcpPort());
        return messagingService.sendAndReceive(nodeEp, subject.value(), payload);
    }

    @Override
    public void addSubscriber(MessageSubject subject,
                              ClusterMessageHandler subscriber,
                              ExecutorService executor) {
        messagingService.registerHandler(subject.value(),
                new InternalClusterMessageHandler(subscriber),
                executor);
    }

    @Override
    public void removeSubscriber(MessageSubject subject) {
        messagingService.unregisterHandler(subject.value());
    }

    @Override
    public <M, R> void addSubscriber(MessageSubject subject,
            Function<byte[], M> decoder,
            Function<M, R> handler,
            Function<R, byte[]> encoder,
            Executor executor) {
        messagingService.registerHandler(subject.value(),
                new InternalMessageResponder<M, R>(decoder, encoder, m -> {
                    CompletableFuture<R> responseFuture = new CompletableFuture<>();
                    executor.execute(() -> {
                        try {
                            responseFuture.complete(handler.apply(m));
                        } catch (Exception e) {
                            responseFuture.completeExceptionally(e);
                        }
                    });
                    return responseFuture;
                }));
    }

    @Override
    public <M, R> void addSubscriber(MessageSubject subject,
            Function<byte[], M> decoder,
            Function<M, CompletableFuture<R>> handler,
            Function<R, byte[]> encoder) {
        messagingService.registerHandler(subject.value(),
                new InternalMessageResponder<>(decoder, encoder, handler));
    }

    @Override
    public <M> void addSubscriber(MessageSubject subject,
            Function<byte[], M> decoder,
            Consumer<M> handler,
            Executor executor) {
        messagingService.registerHandler(subject.value(),
                new InternalMessageConsumer<>(decoder, handler),
                executor);
    }

    private class InternalClusterMessageHandler implements Function<byte[], byte[]> {
        private ClusterMessageHandler handler;

        public InternalClusterMessageHandler(ClusterMessageHandler handler) {
            this.handler = handler;
        }

        @Override
        public byte[] apply(byte[] bytes) {
            ClusterMessage message = ClusterMessage.fromBytes(bytes);
            handler.handle(message);
            return message.response();
        }
    }

    private class InternalMessageResponder<M, R> implements Function<byte[], CompletableFuture<byte[]>> {
        private final Function<byte[], M> decoder;
        private final Function<R, byte[]> encoder;
        private final Function<M, CompletableFuture<R>> handler;

        public InternalMessageResponder(Function<byte[], M> decoder,
                                        Function<R, byte[]> encoder,
                                        Function<M, CompletableFuture<R>> handler) {
            this.decoder = decoder;
            this.encoder = encoder;
            this.handler = handler;
        }

        @Override
        public CompletableFuture<byte[]> apply(byte[] bytes) {
            return handler.apply(decoder.apply(ClusterMessage.fromBytes(bytes).payload())).thenApply(encoder);
        }
    }

    private class InternalMessageConsumer<M> implements Consumer<byte[]> {
        private final Function<byte[], M> decoder;
        private final Consumer<M> consumer;

        public InternalMessageConsumer(Function<byte[], M> decoder, Consumer<M> consumer) {
            this.decoder = decoder;
            this.consumer = consumer;
        }

        @Override
        public void accept(byte[] bytes) {
            consumer.accept(decoder.apply(ClusterMessage.fromBytes(bytes).payload()));
        }
    }
}
