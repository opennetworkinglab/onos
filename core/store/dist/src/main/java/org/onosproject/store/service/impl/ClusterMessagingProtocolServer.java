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

import static java.util.concurrent.Executors.newCachedThreadPool;
import static org.onlab.util.Tools.namedThreads;
import static org.slf4j.LoggerFactory.getLogger;
import static org.onosproject.store.service.impl.ClusterMessagingProtocol.*;
import static org.onosproject.store.service.impl.ClusterMessagingProtocol.DB_SERIALIZER;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;

import net.kuujo.copycat.protocol.PingRequest;
import net.kuujo.copycat.protocol.PollRequest;
import net.kuujo.copycat.protocol.RequestHandler;
import net.kuujo.copycat.protocol.SubmitRequest;
import net.kuujo.copycat.protocol.SyncRequest;
import net.kuujo.copycat.spi.protocol.ProtocolServer;

import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.ClusterMessage;
import org.onosproject.store.cluster.messaging.ClusterMessageHandler;
import org.slf4j.Logger;

/**
 * ONOS Cluster messaging based Copycat protocol server.
 */
public class ClusterMessagingProtocolServer implements ProtocolServer {

    private final Logger log = getLogger(getClass());

    private final ClusterCommunicationService clusterCommunicator;

    private volatile RequestHandler handler;

    private ExecutorService pool;

    public ClusterMessagingProtocolServer(ClusterCommunicationService clusterCommunicator) {
        this.clusterCommunicator = clusterCommunicator;
    }

    @Override
    public void requestHandler(RequestHandler handler) {
        this.handler = handler;
    }

    @Override
    public CompletableFuture<Void> listen() {
        if (pool == null || pool.isShutdown()) {
            pool = newCachedThreadPool(namedThreads("copycat-netty-messaging-server-%d"));
        }

        clusterCommunicator.addSubscriber(COPYCAT_PING, new PingHandler());
        clusterCommunicator.addSubscriber(COPYCAT_SYNC, new SyncHandler());
        clusterCommunicator.addSubscriber(COPYCAT_POLL, new PollHandler());
        clusterCommunicator.addSubscriber(COPYCAT_SUBMIT, new SubmitHandler());
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> close() {
        clusterCommunicator.removeSubscriber(COPYCAT_PING);
        clusterCommunicator.removeSubscriber(COPYCAT_SYNC);
        clusterCommunicator.removeSubscriber(COPYCAT_POLL);
        clusterCommunicator.removeSubscriber(COPYCAT_SUBMIT);
        if (pool != null) {
            pool.shutdownNow();
            pool = null;
        }
        return CompletableFuture.completedFuture(null);
    }

    private final class PingHandler extends CopycatMessageHandler<PingRequest> {

        @Override
        public void raftHandle(PingRequest request, ClusterMessage message) {
            pool.submit(new Runnable() {

                @Override
                public void run() {
                    currentHandler().ping(request)
                        .whenComplete(new PostExecutionTask<>(message));
                }
            });
        }
    }

    private final class SyncHandler extends CopycatMessageHandler<SyncRequest> {

        @Override
        public void raftHandle(SyncRequest request, ClusterMessage message) {
            pool.submit(new Runnable() {

                @Override
                public void run() {
                    currentHandler().sync(request)
                        .whenComplete(new PostExecutionTask<>(message));
                }
            });
        }
    }

    private final class PollHandler extends CopycatMessageHandler<PollRequest> {

        @Override
        public void raftHandle(PollRequest request, ClusterMessage message) {
            pool.submit(new Runnable() {

                @Override
                public void run() {
                    currentHandler().poll(request)
                    .whenComplete(new PostExecutionTask<>(message));
                }
            });
        }
    }

    private final class SubmitHandler extends CopycatMessageHandler<SubmitRequest> {

        @Override
        public void raftHandle(SubmitRequest request, ClusterMessage message) {
            pool.submit(new Runnable() {

                @Override
                public void run() {
                    currentHandler().submit(request)
                    .whenComplete(new PostExecutionTask<>(message));
                }
            });
        }
    }

    private abstract class CopycatMessageHandler<T> implements ClusterMessageHandler {

        public abstract void raftHandle(T request, ClusterMessage message);

        @Override
        public void handle(ClusterMessage message) {
            T request = DB_SERIALIZER.decode(message.payload());
            raftHandle(request, message);
        }

        RequestHandler currentHandler() {
            RequestHandler currentHandler = handler;
            if (currentHandler == null) {
                // there is a slight window of time during state transition,
                // where handler becomes null
                long sleepMs = 1;
                for (int i = 0; i < 10; ++i) {
                    currentHandler = handler;
                    if (currentHandler != null) {
                        break;
                    }
                    try {
                        sleepMs <<= 1;
                        Thread.sleep(sleepMs);
                    } catch (InterruptedException e) {
                        log.error("Interrupted", e);
                        return handler;
                    }
                }
                if (currentHandler == null) {
                    log.error("There was no handler registered!");
                    return handler;
                }
            }
            return currentHandler;
        }

        final class PostExecutionTask<R> implements BiConsumer<R, Throwable> {

            private final ClusterMessage message;

            public PostExecutionTask(ClusterMessage message) {
                this.message = message;
            }

            @Override
            public void accept(R response, Throwable error) {
                if (error != null) {
                    log.error("Processing {} failed.", message.subject(),  error);
                } else {
                    try {
                        log.trace("responding to {}", message.subject());
                        message.respond(DB_SERIALIZER.encode(response));
                    } catch (Exception e) {
                        log.error("Failed responding with {}", response.getClass().getName(), e);
                    }
                }
            }
        }
    }
}
