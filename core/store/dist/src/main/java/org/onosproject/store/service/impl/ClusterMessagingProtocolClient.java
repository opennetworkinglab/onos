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
package org.onosproject.store.service.impl;

import static com.google.common.base.Verify.verifyNotNull;
import static org.onosproject.store.service.impl.ClusterMessagingProtocol.DB_SERIALIZER;
import static org.onlab.util.Tools.namedThreads;
import static org.slf4j.LoggerFactory.getLogger;
import static java.util.concurrent.Executors.newCachedThreadPool;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import net.kuujo.copycat.cluster.TcpMember;
import net.kuujo.copycat.protocol.PingRequest;
import net.kuujo.copycat.protocol.PingResponse;
import net.kuujo.copycat.protocol.PollRequest;
import net.kuujo.copycat.protocol.PollResponse;
import net.kuujo.copycat.protocol.SubmitRequest;
import net.kuujo.copycat.protocol.SubmitResponse;
import net.kuujo.copycat.protocol.SyncRequest;
import net.kuujo.copycat.protocol.SyncResponse;
import net.kuujo.copycat.spi.protocol.ProtocolClient;

import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.ClusterMessage;
import org.onosproject.store.cluster.messaging.MessageSubject;
import org.slf4j.Logger;

/**
 * ONOS Cluster messaging based Copycat protocol client.
 */
public class ClusterMessagingProtocolClient implements ProtocolClient {

    private final Logger log = getLogger(getClass());

    public static final Duration RETRY_INTERVAL = Duration.ofMillis(2000);

    private final ClusterService clusterService;
    private final ClusterCommunicationService clusterCommunicator;
    private final ControllerNode localNode;
    private final TcpMember remoteMember;

    private ControllerNode remoteNode;
    private final AtomicBoolean connectionOK = new AtomicBoolean(true);

    private ExecutorService pool;

    public ClusterMessagingProtocolClient(
            ClusterService clusterService,
            ClusterCommunicationService clusterCommunicator,
            ControllerNode localNode,
            TcpMember remoteMember) {

        this.clusterService = clusterService;
        this.clusterCommunicator = clusterCommunicator;
        this.localNode = localNode;
        this.remoteMember = remoteMember;
    }

    @Override
    public CompletableFuture<PingResponse> ping(PingRequest request) {
        return requestReply(request);
    }

    @Override
    public CompletableFuture<SyncResponse> sync(SyncRequest request) {
        return requestReply(request);
    }

    @Override
    public CompletableFuture<PollResponse> poll(PollRequest request) {
        return requestReply(request);
    }

    @Override
    public CompletableFuture<SubmitResponse> submit(SubmitRequest request) {
        return requestReply(request);
    }

    @Override
    public synchronized CompletableFuture<Void> connect() {
        if (pool == null || pool.isShutdown()) {
            // TODO include remote name?
            pool = newCachedThreadPool(namedThreads("copycat-netty-messaging-client-%d"));
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public synchronized CompletableFuture<Void> close() {
        if (pool != null) {
            pool.shutdownNow();
            pool = null;
        }
        return CompletableFuture.completedFuture(null);
    }

    private <I> MessageSubject messageType(I input) {
        Class<?> clazz = input.getClass();
        if (clazz.equals(PollRequest.class)) {
            return ClusterMessagingProtocol.COPYCAT_POLL;
        } else if (clazz.equals(SyncRequest.class)) {
            return ClusterMessagingProtocol.COPYCAT_SYNC;
        } else if (clazz.equals(SubmitRequest.class)) {
            return ClusterMessagingProtocol.COPYCAT_SUBMIT;
        } else if (clazz.equals(PingRequest.class)) {
            return ClusterMessagingProtocol.COPYCAT_PING;
        } else {
            throw new IllegalArgumentException("Unknown class " + clazz.getName());
        }
    }

    private <I, O> CompletableFuture<O> requestReply(I request) {
        CompletableFuture<O> future = new CompletableFuture<>();
        if (pool == null) {
            log.info("Attempted to use closed client, connecting now. {}", request);
            connect();
        }
        pool.submit(new RPCTask<I, O>(request, future));
        return future;
    }

    private ControllerNode getControllerNode(TcpMember remoteMember) {
        final String host = remoteMember.host();
        final int port = remoteMember.port();
        for (ControllerNode node : clusterService.getNodes()) {
            if (node.ip().toString().equals(host) && node.tcpPort() == port) {
                return node;
            }
        }
        return null;
    }

    private class RPCTask<I, O> implements Runnable {

        private final I request;
        private final ClusterMessage message;
        private final CompletableFuture<O> future;

        public RPCTask(I request, CompletableFuture<O> future) {
            this.request = request;
            this.message =
                    new ClusterMessage(
                            localNode.id(),
                            messageType(request),
                            verifyNotNull(DB_SERIALIZER.encode(request)));
            this.future = future;
        }

        @Override
        public void run() {
            try {
                if (remoteNode == null) {
                    remoteNode = getControllerNode(remoteMember);
                    if (remoteNode == null) {
                        throw new IOException("Remote node is offline!");
                    }
                }
                byte[] response = clusterCommunicator
                    .sendAndReceive(message, remoteNode.id())
                    .get(RETRY_INTERVAL.toMillis(), TimeUnit.MILLISECONDS);
                if (!connectionOK.getAndSet(true)) {
                    log.info("Connectivity to {} restored", remoteNode);
                }
                future.complete(verifyNotNull(DB_SERIALIZER.decode(response)));

            } catch (IOException | TimeoutException e) {
                if (connectionOK.getAndSet(false)) {
                    log.warn("Detected connectivity issues with {}. Reason: {}", remoteNode, e.getMessage());
                }
                log.debug("RPCTask for {} failed.", request, e);
                future.completeExceptionally(e);
            } catch (ExecutionException e) {
                log.warn("RPCTask execution for {} failed: {}", request, e.getMessage());
                log.debug("RPCTask execution for {} failed.", request, e);
                future.completeExceptionally(e);
            } catch (InterruptedException e) {
                log.warn("RPCTask for {} was interrupted: {}", request, e.getMessage());
                log.debug("RPCTask for {} was interrupted.", request, e);
                future.completeExceptionally(e);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.warn("RPCTask for {} terribly failed.", request, e);
                future.completeExceptionally(e);
            }
        }
    }
}
