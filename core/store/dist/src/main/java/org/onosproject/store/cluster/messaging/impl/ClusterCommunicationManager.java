/*
 * Copyright 2017-present Open Networking Foundation
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

import java.time.Duration;
import com.google.common.base.Objects;
import com.google.common.base.Throwables;
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
import org.onosproject.utils.MeteringAgent;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.security.AppGuard.checkPermission;
import static org.onosproject.security.AppPermission.Type.CLUSTER_WRITE;

@Component(immediate = true, service = ClusterCommunicationService.class)
public class ClusterCommunicationManager implements ClusterCommunicationService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final MeteringAgent subjectMeteringAgent = new MeteringAgent(PRIMITIVE_NAME, SUBJECT_PREFIX, false);
    private final MeteringAgent endpointMeteringAgent = new MeteringAgent(PRIMITIVE_NAME, ENDPOINT_PREFIX, false);

    private static final String PRIMITIVE_NAME = "clusterCommunication";
    private static final String SUBJECT_PREFIX = "subject";
    private static final String ENDPOINT_PREFIX = "endpoint";

    private static final String SERIALIZING = "serialization";
    private static final String DESERIALIZING = "deserialization";
    private static final String NODE_PREFIX = "node:";
    private static final String ROUND_TRIP_SUFFIX = ".rtt";
    private static final String ONE_WAY_SUFFIX = ".oneway";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
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
        checkPermission(CLUSTER_WRITE);
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
        checkPermission(CLUSTER_WRITE);
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
        checkPermission(CLUSTER_WRITE);
        try {
            byte[] payload = new ClusterMessage(
                    localNodeId,
                    subject,
                    timeFunction(encoder, subjectMeteringAgent, SERIALIZING).apply(message)
                    ).getBytes();
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
        checkPermission(CLUSTER_WRITE);
        byte[] payload = new ClusterMessage(
                localNodeId,
                subject,
                timeFunction(encoder, subjectMeteringAgent, SERIALIZING).apply(message))
                .getBytes();
        nodes.forEach(nodeId -> doUnicast(subject, payload, nodeId));
    }

    @Override
    public <M, R> CompletableFuture<R> sendAndReceive(M message,
                                                      MessageSubject subject,
                                                      Function<M, byte[]> encoder,
                                                      Function<byte[], R> decoder,
                                                      NodeId toNodeId,
                                                      Duration timeout) {
        checkPermission(CLUSTER_WRITE);
        try {
            ClusterMessage envelope = new ClusterMessage(
                    clusterService.getLocalNode().id(),
                    subject,
                    timeFunction(encoder, subjectMeteringAgent, SERIALIZING).
                            apply(message));
            return sendAndReceive(subject, envelope.getBytes(), toNodeId, timeout).
                    thenApply(bytes -> timeFunction(decoder, subjectMeteringAgent, DESERIALIZING).apply(bytes));
        } catch (Exception e) {
            return Tools.exceptionalFuture(e);
        }
    }

    private CompletableFuture<Void> doUnicast(MessageSubject subject, byte[] payload, NodeId toNodeId) {
        ControllerNode node = clusterService.getNode(toNodeId);
        checkArgument(node != null, "Unknown nodeId: %s", toNodeId);
        Endpoint nodeEp = new Endpoint(node.ip(), node.tcpPort());
        MeteringAgent.Context context = subjectMeteringAgent.startTimer(subject.toString() + ONE_WAY_SUFFIX);
        return messagingService.sendAsync(nodeEp, subject.toString(), payload).whenComplete((r, e) -> context.stop(e));
    }

    private CompletableFuture<byte[]> sendAndReceive(
        MessageSubject subject, byte[] payload, NodeId toNodeId, Duration timeout) {
        ControllerNode node = clusterService.getNode(toNodeId);
        checkArgument(node != null, "Unknown nodeId: %s", toNodeId);
        Endpoint nodeEp = new Endpoint(node.ip(), node.tcpPort());
        MeteringAgent.Context epContext = endpointMeteringAgent.
                startTimer(NODE_PREFIX + toNodeId.toString() + ROUND_TRIP_SUFFIX);
        MeteringAgent.Context subjectContext = subjectMeteringAgent.
                startTimer(subject.toString() + ROUND_TRIP_SUFFIX);
        return messagingService.sendAndReceive(nodeEp, subject.toString(), payload, timeout).
                whenComplete((bytes, throwable) -> {
                    subjectContext.stop(throwable);
                    epContext.stop(throwable);
                });
    }

    @Override
    public void addSubscriber(MessageSubject subject,
                              ClusterMessageHandler subscriber,
                              ExecutorService executor) {
        checkPermission(CLUSTER_WRITE);
        messagingService.registerHandler(subject.toString(),
                new InternalClusterMessageHandler(subscriber),
                executor);
    }

    @Override
    public void removeSubscriber(MessageSubject subject) {
        checkPermission(CLUSTER_WRITE);
        messagingService.unregisterHandler(subject.toString());
    }

    @Override
    public <M, R> void addSubscriber(MessageSubject subject,
            Function<byte[], M> decoder,
            Function<M, R> handler,
            Function<R, byte[]> encoder,
            Executor executor) {
        checkPermission(CLUSTER_WRITE);
        messagingService.registerHandler(subject.toString(),
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
        checkPermission(CLUSTER_WRITE);
        messagingService.registerHandler(subject.toString(),
                new InternalMessageResponder<>(decoder, encoder, handler));
    }

    @Override
    public <M> void addSubscriber(MessageSubject subject,
            Function<byte[], M> decoder,
            Consumer<M> handler,
            Executor executor) {
        checkPermission(CLUSTER_WRITE);
        messagingService.registerHandler(subject.toString(),
                new InternalMessageConsumer<>(decoder, handler),
                executor);
    }

    /**
     * Performs the timed function, returning the value it would while timing the operation.
     *
     * @param timedFunction the function to be timed
     * @param meter the metering agent to be used to time the function
     * @param opName the opname to be used when starting the meter
     * @param <A> The param type of the function
     * @param <B> The return type of the function
     * @return the value returned by the timed function
     */
    private <A, B> Function<A, B> timeFunction(Function<A, B> timedFunction,
                                               MeteringAgent meter, String opName) {
        checkNotNull(timedFunction);
        checkNotNull(meter);
        checkNotNull(opName);
        return new Function<A, B>() {
            @Override
            public B apply(A a) {
                final MeteringAgent.Context context = meter.startTimer(opName);
                B result = null;
                try {
                    result = timedFunction.apply(a);
                    context.stop(null);
                    return result;
                } catch (Exception e) {
                    context.stop(e);
                    Throwables.throwIfUnchecked(Throwables.getRootCause(e));
                    throw new IllegalStateException(e.getCause());
                }
            }
        };
    }


    private class InternalClusterMessageHandler implements BiFunction<Endpoint, byte[], byte[]> {
        private ClusterMessageHandler handler;

        public InternalClusterMessageHandler(ClusterMessageHandler handler) {
            this.handler = handler;
        }

        @Override
        public byte[] apply(Endpoint sender, byte[] bytes) {
            ClusterMessage message = ClusterMessage.fromBytes(bytes);
            handler.handle(message);
            return message.response();
        }
    }

    private class InternalMessageResponder<M, R> implements BiFunction<Endpoint, byte[], CompletableFuture<byte[]>> {
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
        public CompletableFuture<byte[]> apply(Endpoint sender, byte[] bytes) {
            return handler.apply(timeFunction(decoder, subjectMeteringAgent, DESERIALIZING).
                    apply(ClusterMessage.fromBytes(bytes).payload())).
                    thenApply(m -> timeFunction(encoder, subjectMeteringAgent, SERIALIZING).apply(m));
        }
    }

    private class InternalMessageConsumer<M> implements BiConsumer<Endpoint, byte[]> {
        private final Function<byte[], M> decoder;
        private final Consumer<M> consumer;

        public InternalMessageConsumer(Function<byte[], M> decoder, Consumer<M> consumer) {
            this.decoder = decoder;
            this.consumer = consumer;
        }

        @Override
        public void accept(Endpoint sender, byte[] bytes) {
            consumer.accept(timeFunction(decoder, subjectMeteringAgent, DESERIALIZING).
                    apply(ClusterMessage.fromBytes(bytes).payload()));
        }
    }
}
