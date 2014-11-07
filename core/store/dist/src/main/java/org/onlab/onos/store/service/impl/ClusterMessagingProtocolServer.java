package org.onlab.onos.store.service.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import net.kuujo.copycat.protocol.PingRequest;
import net.kuujo.copycat.protocol.PingResponse;
import net.kuujo.copycat.protocol.PollRequest;
import net.kuujo.copycat.protocol.PollResponse;
import net.kuujo.copycat.protocol.RequestHandler;
import net.kuujo.copycat.protocol.SubmitRequest;
import net.kuujo.copycat.protocol.SubmitResponse;
import net.kuujo.copycat.protocol.SyncRequest;
import net.kuujo.copycat.protocol.SyncResponse;
import net.kuujo.copycat.spi.protocol.ProtocolServer;

import org.onlab.onos.store.cluster.messaging.ClusterCommunicationService;
import org.onlab.onos.store.cluster.messaging.ClusterMessage;
import org.onlab.onos.store.cluster.messaging.ClusterMessageHandler;
import org.slf4j.Logger;

/**
 * ONOS Cluster messaging based Copycat protocol server.
 */
public class ClusterMessagingProtocolServer implements ProtocolServer {

    private final Logger log = getLogger(getClass());
    private volatile RequestHandler handler;
    private ClusterCommunicationService clusterCommunicator;

    public ClusterMessagingProtocolServer(ClusterCommunicationService clusterCommunicator) {
        this.clusterCommunicator = clusterCommunicator;

    }

    @Override
    public void requestHandler(RequestHandler handler) {
        this.handler = handler;
    }

    @Override
    public CompletableFuture<Void> listen() {
        clusterCommunicator.addSubscriber(ClusterMessagingProtocol.COPYCAT_PING,
                                          new CopycatMessageHandler<PingRequest>());
        clusterCommunicator.addSubscriber(ClusterMessagingProtocol.COPYCAT_SYNC,
                                          new CopycatMessageHandler<SyncRequest>());
        clusterCommunicator.addSubscriber(ClusterMessagingProtocol.COPYCAT_POLL,
                                          new CopycatMessageHandler<PollRequest>());
        clusterCommunicator.addSubscriber(ClusterMessagingProtocol.COPYCAT_SUBMIT,
                                          new CopycatMessageHandler<SubmitRequest>());
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> close() {
        clusterCommunicator.removeSubscriber(ClusterMessagingProtocol.COPYCAT_PING);
        clusterCommunicator.removeSubscriber(ClusterMessagingProtocol.COPYCAT_SYNC);
        clusterCommunicator.removeSubscriber(ClusterMessagingProtocol.COPYCAT_POLL);
        clusterCommunicator.removeSubscriber(ClusterMessagingProtocol.COPYCAT_SUBMIT);
        return CompletableFuture.completedFuture(null);
    }

    private class CopycatMessageHandler<T> implements ClusterMessageHandler {

        @Override
        public void handle(ClusterMessage message) {
            T request = ClusterMessagingProtocol.SERIALIZER.decode(message.payload());
            if (handler == null) {
                // there is a slight window of time during state transition,
                // where handler becomes null
                for (int i = 0; i < 10; ++i) {
                    if (handler != null) {
                        break;
                    }
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        log.trace("Exception", e);
                    }
                }
                if (handler == null) {
                    log.error("There was no handler for registered!");
                    return;
                }
            }
            if (request.getClass().equals(PingRequest.class)) {
                handler.ping((PingRequest) request).whenComplete(new PostExecutionTask<PingResponse>(message));
            } else if (request.getClass().equals(PollRequest.class)) {
                handler.poll((PollRequest) request).whenComplete(new PostExecutionTask<PollResponse>(message));
            } else if (request.getClass().equals(SyncRequest.class)) {
                handler.sync((SyncRequest) request).whenComplete(new PostExecutionTask<SyncResponse>(message));
            } else if (request.getClass().equals(SubmitRequest.class)) {
                handler.submit((SubmitRequest) request).whenComplete(new PostExecutionTask<SubmitResponse>(message));
            } else {
                throw new IllegalStateException("Unknown request type: " + request.getClass().getName());
            }
        }

        private class PostExecutionTask<R> implements BiConsumer<R, Throwable> {

            private final ClusterMessage message;

            public PostExecutionTask(ClusterMessage message) {
                this.message = message;
            }

            @Override
            public void accept(R response, Throwable t) {
                if (t != null) {
                    log.error("Processing for " + message.subject() + " failed.", t);
                } else {
                    try {
                        log.trace("responding to {}", message.subject());
                        message.respond(ClusterMessagingProtocol.SERIALIZER.encode(response));
                    } catch (Exception e) {
                        log.error("Failed to respond to " + response.getClass().getName(), e);
                    }
                }
            }
        }
    }
}