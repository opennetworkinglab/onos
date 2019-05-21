/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.cluster.impl;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.common.collect.Maps;
import org.onlab.util.Tools;
import org.onosproject.cluster.NodeId;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.ClusterMessageHandler;
import org.onosproject.store.cluster.messaging.MessageSubject;
import org.onosproject.store.cluster.messaging.MessagingException;

/**
 * Cluster communication service implementation used for testing.
 */
public class TestClusterCommunicationService implements ClusterCommunicationService {
    private final NodeId localNodeId;
    private final Map<NodeId, TestClusterCommunicationService> nodes;
    private final Map<MessageSubject, Function<byte[], CompletableFuture<byte[]>>> subscribers =
            Maps.newConcurrentMap();

    public TestClusterCommunicationService(NodeId localNodeId, Map<NodeId, TestClusterCommunicationService> nodes) {
        this.localNodeId = localNodeId;
        this.nodes = nodes;
        nodes.put(localNodeId, this);
    }

    @Override
    public <M> void broadcast(M message, MessageSubject subject, Function<M, byte[]> encoder) {
        nodes.forEach((nodeId, node) -> {
            if (!nodeId.equals(localNodeId)) {
                node.handle(subject, encoder.apply(message));
            }
        });
    }

    @Override
    public <M> void broadcastIncludeSelf(M message, MessageSubject subject, Function<M, byte[]> encoder) {
        nodes.values().forEach(node -> node.handle(subject, encoder.apply(message)));
    }

    @Override
    public <M> CompletableFuture<Void> unicast(
            M message, MessageSubject subject, Function<M, byte[]> encoder, NodeId toNodeId) {
        TestClusterCommunicationService node = nodes.get(toNodeId);
        if (node != null) {
            node.handle(subject, encoder.apply(message));
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public <M> void multicast(M message, MessageSubject subject, Function<M, byte[]> encoder, Set<NodeId> nodeIds) {
        nodes.entrySet().stream()
                .filter(e -> nodeIds.contains(e.getKey()))
                .forEach(e -> e.getValue().handle(subject, encoder.apply(message)));
    }

    @Override
    public <M, R> CompletableFuture<R> sendAndReceive(
            M message,
            MessageSubject subject,
            Function<M, byte[]> encoder,
            Function<byte[], R> decoder,
            NodeId toNodeId,
            Duration duration) {
        TestClusterCommunicationService node = nodes.get(toNodeId);
        if (node == null) {
            return Tools.exceptionalFuture(new MessagingException.NoRemoteHandler());
        }
        return node.handle(subject, encoder.apply(message)).thenApply(decoder);
    }

    private CompletableFuture<byte[]> handle(MessageSubject subject, byte[] message) {
        Function<byte[], CompletableFuture<byte[]>> subscriber = subscribers.get(subject);
        if (subscriber != null) {
            return subscriber.apply(message);
        }
        return Tools.exceptionalFuture(new MessagingException.NoRemoteHandler());
    }

    private boolean isSubscriber(MessageSubject subject) {
        return subscribers.containsKey(subject);
    }

    @Override
    public <M, R> void addSubscriber(
            MessageSubject subject,
            Function<byte[], M> decoder,
            Function<M, R> handler,
            Function<R, byte[]> encoder,
            Executor executor) {
        subscribers.put(subject, message -> {
            CompletableFuture<byte[]> future = new CompletableFuture<>();
            executor.execute(() -> {
                try {
                    future.complete(encoder.apply(handler.apply(decoder.apply(message))));
                } catch (Exception e) {
                    future.completeExceptionally(new MessagingException.RemoteHandlerFailure());
                }
            });
            return future;
        });
    }

    @Override
    public <M, R> void addSubscriber(
            MessageSubject subject,
            Function<byte[], M> decoder,
            Function<M, CompletableFuture<R>> handler,
            Function<R, byte[]> encoder) {
        subscribers.put(subject, message -> {
            CompletableFuture<byte[]> future = new CompletableFuture<>();
            try {
                handler.apply(decoder.apply(message)).whenComplete((result, error) -> {
                    if (error == null) {
                        future.complete(encoder.apply(result));
                    } else {
                        future.completeExceptionally(new MessagingException.RemoteHandlerFailure());
                    }
                });
            } catch (Exception e) {
                future.completeExceptionally(new MessagingException.RemoteHandlerFailure());
            }
            return future;
        });
    }

    @Override
    public <M> void addSubscriber(
            MessageSubject subject,
            Function<byte[], M> decoder,
            Consumer<M> handler,
            Executor executor) {
        subscribers.put(subject, message -> {
            CompletableFuture<byte[]> future = new CompletableFuture<>();
            executor.execute(() -> {
                try {
                    handler.accept(decoder.apply(message));
                    future.complete(null);
                } catch (Exception e) {
                    future.completeExceptionally(new MessagingException.RemoteHandlerFailure());
                }
            });
            return future;
        });
    }

    @Override
    public void removeSubscriber(MessageSubject subject) {
        subscribers.remove(subject);
    }

    @Override
    public void addSubscriber(MessageSubject subject, ClusterMessageHandler subscriber, ExecutorService executor) {
        throw new UnsupportedOperationException();
    }
}
