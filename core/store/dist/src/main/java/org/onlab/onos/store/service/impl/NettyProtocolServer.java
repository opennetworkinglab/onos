package org.onlab.onos.store.service.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import net.kuujo.copycat.cluster.TcpMember;
import net.kuujo.copycat.protocol.PingRequest;
import net.kuujo.copycat.protocol.PollRequest;
import net.kuujo.copycat.protocol.RequestHandler;
import net.kuujo.copycat.protocol.SubmitRequest;
import net.kuujo.copycat.protocol.SyncRequest;
import net.kuujo.copycat.spi.protocol.ProtocolServer;

import org.onlab.netty.Message;
import org.onlab.netty.MessageHandler;
import org.onlab.netty.NettyMessagingService;
import org.slf4j.Logger;

/**
 * {@link NettyMessagingService} based Copycat protocol server.
 */
public class NettyProtocolServer implements ProtocolServer {

    private final Logger log = getLogger(getClass());

    private final NettyMessagingService messagingService;
    private RequestHandler handler;


    public NettyProtocolServer(TcpMember member) {
        messagingService = new NettyMessagingService(member.host(), member.port());

        messagingService.registerHandler(NettyProtocol.COPYCAT_PING, new CopycatMessageHandler<PingRequest>());
        messagingService.registerHandler(NettyProtocol.COPYCAT_SYNC, new CopycatMessageHandler<SyncRequest>());
        messagingService.registerHandler(NettyProtocol.COPYCAT_POLL, new CopycatMessageHandler<PollRequest>());
        messagingService.registerHandler(NettyProtocol.COPYCAT_SUBMIT, new CopycatMessageHandler<SubmitRequest>());
    }

    protected NettyMessagingService getNettyMessagingService() {
        return messagingService;
    }

    @Override
    public void requestHandler(RequestHandler handler) {
        this.handler = handler;
    }

    @Override
    public CompletableFuture<Void> listen() {
        try {
            messagingService.activate();
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    @Override
    public CompletableFuture<Void> close() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            messagingService.deactivate();
            future.complete(null);
            return future;
        } catch (Exception e) {
            future.completeExceptionally(e);
            return future;
        }
    }

    private class CopycatMessageHandler<T> implements MessageHandler {

        @Override
        public void handle(Message message) throws IOException {
            T request = NettyProtocol.SERIALIZER.decode(message.payload());
            if (request.getClass().equals(PingRequest.class)) {
                handler.ping((PingRequest) request).whenComplete((response, error) -> {
                    try {
                        message.respond(NettyProtocol.SERIALIZER.encode(response));
                    } catch (Exception e) {
                        log.error("Failed to respond to ping request", e);
                    }
                });
            } else if (request.getClass().equals(PollRequest.class)) {
                handler.poll((PollRequest) request).whenComplete((response, error) -> {
                    try {
                        message.respond(NettyProtocol.SERIALIZER.encode(response));
                    } catch (Exception e) {
                        log.error("Failed to respond to poll request", e);
                    }
                });
            } else if (request.getClass().equals(SyncRequest.class)) {
                handler.sync((SyncRequest) request).whenComplete((response, error) -> {
                    try {
                        message.respond(NettyProtocol.SERIALIZER.encode(response));
                    } catch (Exception e) {
                        log.error("Failed to respond to sync request", e);
                    }
                });
            } else if (request.getClass().equals(SubmitRequest.class)) {
                handler.submit((SubmitRequest) request).whenComplete((response, error) -> {
                    try {
                        message.respond(NettyProtocol.SERIALIZER.encode(response));
                    } catch (Exception e) {
                        log.error("Failed to respond to submit request", e);
                    }
                });
            }
        }
    }
}
