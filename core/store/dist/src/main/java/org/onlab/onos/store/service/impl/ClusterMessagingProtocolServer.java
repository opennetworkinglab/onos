package org.onlab.onos.store.service.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.concurrent.CompletableFuture;

import net.kuujo.copycat.protocol.PingRequest;
import net.kuujo.copycat.protocol.PollRequest;
import net.kuujo.copycat.protocol.RequestHandler;
import net.kuujo.copycat.protocol.SubmitRequest;
import net.kuujo.copycat.protocol.SyncRequest;
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
    private RequestHandler handler;

    public ClusterMessagingProtocolServer(ClusterCommunicationService clusterCommunicator) {

        clusterCommunicator.addSubscriber(
                ClusterMessagingProtocol.COPYCAT_PING, new CopycatMessageHandler<PingRequest>());
        clusterCommunicator.addSubscriber(
                ClusterMessagingProtocol.COPYCAT_SYNC, new CopycatMessageHandler<SyncRequest>());
        clusterCommunicator.addSubscriber(
                ClusterMessagingProtocol.COPYCAT_POLL, new CopycatMessageHandler<PollRequest>());
        clusterCommunicator.addSubscriber(
                ClusterMessagingProtocol.COPYCAT_SUBMIT, new CopycatMessageHandler<SubmitRequest>());
    }

    @Override
    public void requestHandler(RequestHandler handler) {
        this.handler = handler;
    }

    @Override
    public CompletableFuture<Void> listen() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> close() {
        return CompletableFuture.completedFuture(null);
    }

    private class CopycatMessageHandler<T> implements ClusterMessageHandler {

        @Override
        public void handle(ClusterMessage message) {
            T request = ClusterMessagingProtocol.SERIALIZER.decode(message.payload());
            if (request.getClass().equals(PingRequest.class)) {
                handler.ping((PingRequest) request).whenComplete((response, error) -> {
                    try {
                        message.respond(ClusterMessagingProtocol.SERIALIZER.encode(response));
                    } catch (Exception e) {
                        log.error("Failed to respond to ping request", e);
                    }
                });
            } else if (request.getClass().equals(PollRequest.class)) {
                handler.poll((PollRequest) request).whenComplete((response, error) -> {
                    try {
                        message.respond(ClusterMessagingProtocol.SERIALIZER.encode(response));
                    } catch (Exception e) {
                        log.error("Failed to respond to poll request", e);
                    }
                });
            } else if (request.getClass().equals(SyncRequest.class)) {
                handler.sync((SyncRequest) request).whenComplete((response, error) -> {
                    try {
                        message.respond(ClusterMessagingProtocol.SERIALIZER.encode(response));
                    } catch (Exception e) {
                        log.error("Failed to respond to sync request", e);
                    }
                });
            } else if (request.getClass().equals(SubmitRequest.class)) {
                handler.submit((SubmitRequest) request).whenComplete((response, error) -> {
                    try {
                        message.respond(ClusterMessagingProtocol.SERIALIZER.encode(response));
                    } catch (Exception e) {
                        log.error("Failed to respond to submit request", e);
                    }
                });
            }
        }
    }
}