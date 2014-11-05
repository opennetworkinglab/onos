package org.onlab.onos.store.service.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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

import org.onlab.netty.Endpoint;
import org.onlab.netty.NettyMessagingService;
import org.slf4j.Logger;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * {@link NettyMessagingService} based Copycat protocol client.
 */
public class NettyProtocolClient implements ProtocolClient {

    private final Logger log = getLogger(getClass());
    private static final ThreadFactory THREAD_FACTORY =
            new ThreadFactoryBuilder().setNameFormat("copycat-netty-messaging-%d").build();

    // Remote endpoint, this client instance is used
    // for communicating with.
    private final Endpoint remoteEp;
    private final NettyMessagingService messagingService;

    // TODO: Is 10 the right number of threads?
    private static final ScheduledExecutorService THREAD_POOL =
            new ScheduledThreadPoolExecutor(10, THREAD_FACTORY);

    public NettyProtocolClient(NettyProtocol protocol, TcpMember member) {
        this(new Endpoint(member.host(), member.port()), protocol.getServer().getNettyMessagingService());
    }

    public NettyProtocolClient(Endpoint remoteEp, NettyMessagingService messagingService) {
        this.remoteEp = remoteEp;
        this.messagingService = messagingService;
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
    public CompletableFuture<Void> connect() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> close() {
        return CompletableFuture.completedFuture(null);
    }

    public <I> String messageType(I input) {
        Class<?> clazz = input.getClass();
        if (clazz.equals(PollRequest.class)) {
            return NettyProtocol.COPYCAT_POLL;
        } else if (clazz.equals(SyncRequest.class)) {
            return NettyProtocol.COPYCAT_SYNC;
        } else if (clazz.equals(SubmitRequest.class)) {
            return NettyProtocol.COPYCAT_SUBMIT;
        } else if (clazz.equals(PingRequest.class)) {
            return NettyProtocol.COPYCAT_PING;
        } else {
            throw new IllegalArgumentException("Unknown class " + clazz.getName());
        }

    }

    private <I, O> CompletableFuture<O> requestReply(I request) {
        CompletableFuture<O> future = new CompletableFuture<>();
        THREAD_POOL.schedule(new RPCTask<I, O>(request, future), 0, TimeUnit.MILLISECONDS);
        return future;
    }

    private class RPCTask<I, O> implements Runnable {

        private final String messageType;
        private final byte[] payload;

        private final CompletableFuture<O> future;

        public RPCTask(I request, CompletableFuture<O> future) {
            this.messageType = messageType(request);
            this.payload = NettyProtocol.SERIALIZER.encode(request);
            this.future = future;
        }

        @Override
        public void run() {
            try {
                byte[] response = messagingService
                    .sendAndReceive(remoteEp, messageType, payload)
                    .get(NettyProtocol.RETRY_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
                future.complete(NettyProtocol.SERIALIZER.decode(response));

            } catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
                if (messageType.equals(NettyProtocol.COPYCAT_SYNC) ||
                        messageType.equals(NettyProtocol.COPYCAT_PING)) {
                    log.warn("Request to {} failed. Will retry "
                            + "in {} ms", remoteEp, NettyProtocol.RETRY_INTERVAL_MILLIS);
                    THREAD_POOL.schedule(
                            this,
                            NettyProtocol.RETRY_INTERVAL_MILLIS,
                            TimeUnit.MILLISECONDS);
                } else {
                    future.completeExceptionally(e);
                }
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        }
    }
}
