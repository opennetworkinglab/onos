/*
 * Copyright 2015-present Open Networking Laboratory
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

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;

import org.onosproject.cluster.NodeId;

/**
 * Testing adapter for the cluster communication service.
 */
public class ClusterCommunicationServiceAdapter
        implements ClusterCommunicationService {

    @Override
    public void addSubscriber(MessageSubject subject,
                              ClusterMessageHandler subscriber,
                              ExecutorService executor) {
    }

    @Override
    public void removeSubscriber(MessageSubject subject) {}

    @Override
    public <M> void broadcast(M message, MessageSubject subject,
                              Function<M, byte[]> encoder) {
    }

    @Override
    public <M> void broadcastIncludeSelf(M message,
                                         MessageSubject subject, Function<M, byte[]> encoder) {
    }

    @Override
    public <M> CompletableFuture<Void> unicast(M message, MessageSubject subject,
                                               Function<M, byte[]> encoder, NodeId toNodeId) {
        return null;
    }

    @Override
    public <M> void multicast(M message, MessageSubject subject,
                              Function<M, byte[]> encoder, Set<NodeId> nodes) {
    }

    @Override
    public <M, R> CompletableFuture<R> sendAndReceive(M message,
                                                      MessageSubject subject, Function<M, byte[]> encoder,
                                                      Function<byte[], R> decoder, NodeId toNodeId) {
        return null;
    }

    @Override
    public <M, R> void addSubscriber(MessageSubject subject,
                                     Function<byte[], M> decoder, Function<M, R> handler,
                                     Function<R, byte[]> encoder, Executor executor) {
    }

    @Override
    public <M, R> void addSubscriber(MessageSubject subject,
                                     Function<byte[], M> decoder, Function<M, CompletableFuture<R>> handler,
                                     Function<R, byte[]> encoder) {
    }

    @Override
    public <M> void addSubscriber(MessageSubject subject,
                                  Function<byte[], M> decoder, Consumer<M> handler,
                                  Executor executor) {

    }
}
