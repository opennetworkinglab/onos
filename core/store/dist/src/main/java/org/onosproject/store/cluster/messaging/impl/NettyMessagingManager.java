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
package org.onosproject.store.cluster.messaging.impl;

import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.util.concurrent.MoreExecutors;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.Tools;
import org.onosproject.cluster.ClusterMetadataService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.core.HybridLogicalClockService;
import org.onosproject.store.cluster.messaging.Endpoint;
import org.onosproject.store.cluster.messaging.MessagingException;
import org.onosproject.store.cluster.messaging.MessagingService;
import org.onosproject.store.cluster.messaging.impl.InternalMessage.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.security.AppGuard.checkPermission;
import static org.onosproject.security.AppPermission.Type.CLUSTER_WRITE;

/**
 * Netty based MessagingService.
 */
@Component(immediate = true)
@Service
public class NettyMessagingManager implements MessagingService {

    private static final int REPLY_TIME_OUT_MILLIS = 250;
    private static final short MIN_KS_LENGTH = 6;

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String REPLY_MESSAGE_TYPE = "NETTY_MESSAGING_REQUEST_REPLY";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HybridLogicalClockService clockService;

    private Endpoint localEp;
    private int preamble;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final Map<String, Consumer<InternalMessage>> handlers = new ConcurrentHashMap<>();
    private final AtomicLong messageIdGenerator = new AtomicLong(0);
    private final Cache<Long, Callback> callbacks = CacheBuilder.newBuilder()
            .expireAfterWrite(REPLY_TIME_OUT_MILLIS, TimeUnit.MILLISECONDS)
            .removalListener(new RemovalListener<Long, Callback>() {
                @Override
                public void onRemoval(RemovalNotification<Long, Callback> entry) {
                    if (entry.wasEvicted()) {
                        entry.getValue().completeExceptionally(new TimeoutException("Timedout waiting for reply"));
                    }
                }
            })
            .build();

    private final GenericKeyedObjectPool<Endpoint, Connection> channels
            = new GenericKeyedObjectPool<>(new OnosCommunicationChannelFactory());

    private EventLoopGroup serverGroup;
    private EventLoopGroup clientGroup;
    private Class<? extends ServerChannel> serverChannelClass;
    private Class<? extends Channel> clientChannelClass;

    protected static final boolean TLS_DISABLED = false;
    protected boolean enableNettyTls = TLS_DISABLED;

    protected String ksLocation;
    protected String tsLocation;
    protected char[] ksPwd;
    protected char[] tsPwd;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterMetadataService clusterMetadataService;

    @Activate
    public void activate() throws Exception {
        ControllerNode localNode = clusterMetadataService.getLocalNode();
        getTlsParameters();

        if (started.get()) {
            log.warn("Already running at local endpoint: {}", localEp);
            return;
        }
        this.preamble = clusterMetadataService.getClusterMetadata().getName().hashCode();
        this.localEp = new Endpoint(localNode.ip(), localNode.tcpPort());
        channels.setLifo(true);
        channels.setTestOnBorrow(true);
        channels.setTestOnReturn(true);
        channels.setMinEvictableIdleTimeMillis(60_000L);
        channels.setTimeBetweenEvictionRunsMillis(30_000L);
        initEventLoopGroup();
        startAcceptingConnections();
        started.set(true);
        serverGroup.scheduleWithFixedDelay(callbacks::cleanUp, 0, REPLY_TIME_OUT_MILLIS, TimeUnit.MILLISECONDS);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() throws Exception {
        if (started.get()) {
            channels.close();
            serverGroup.shutdownGracefully();
            clientGroup.shutdownGracefully();
            started.set(false);
        }
        log.info("Stopped");
    }

    private void getTlsParameters() {
        String tempString = System.getProperty("enableNettyTLS");
        enableNettyTls = Strings.isNullOrEmpty(tempString) ? TLS_DISABLED : Boolean.parseBoolean(tempString);
        log.info("enableNettyTLS = {}", enableNettyTls);
        if (enableNettyTls) {
            ksLocation = System.getProperty("javax.net.ssl.keyStore");
            if (Strings.isNullOrEmpty(ksLocation)) {
                enableNettyTls = TLS_DISABLED;
                return;
            }
            tsLocation = System.getProperty("javax.net.ssl.trustStore");
            if (Strings.isNullOrEmpty(tsLocation)) {
                enableNettyTls = TLS_DISABLED;
                return;
            }
            ksPwd = System.getProperty("javax.net.ssl.keyStorePassword").toCharArray();
            if (MIN_KS_LENGTH > ksPwd.length) {
                enableNettyTls = TLS_DISABLED;
                return;
            }
            tsPwd = System.getProperty("javax.net.ssl.trustStorePassword").toCharArray();
            if (MIN_KS_LENGTH > tsPwd.length) {
                enableNettyTls = TLS_DISABLED;
                return;
            }
        }
    }
    private void initEventLoopGroup() {
        // try Epoll first and if that does work, use nio.
        try {
            clientGroup = new EpollEventLoopGroup(0, groupedThreads("NettyMessagingEvt", "epollC-%d", log));
            serverGroup = new EpollEventLoopGroup(0, groupedThreads("NettyMessagingEvt", "epollS-%d", log));
            serverChannelClass = EpollServerSocketChannel.class;
            clientChannelClass = EpollSocketChannel.class;
            return;
        } catch (Throwable e) {
            log.debug("Failed to initialize native (epoll) transport. "
                              + "Reason: {}. Proceeding with nio.", e.getMessage());
        }
        clientGroup = new NioEventLoopGroup(0, groupedThreads("NettyMessagingEvt", "nioC-%d", log));
        serverGroup = new NioEventLoopGroup(0, groupedThreads("NettyMessagingEvt", "nioS-%d", log));
        serverChannelClass = NioServerSocketChannel.class;
        clientChannelClass = NioSocketChannel.class;
    }

    @Override
    public CompletableFuture<Void> sendAsync(Endpoint ep, String type, byte[] payload) {
        checkPermission(CLUSTER_WRITE);
        InternalMessage message = new InternalMessage(preamble,
                                                      clockService.timeNow(),
                                                      messageIdGenerator.incrementAndGet(),
                                                      localEp,
                                                      type,
                                                      payload);
        return sendAsync(ep, message);
    }

    protected CompletableFuture<Void> sendAsync(Endpoint ep, InternalMessage message) {
        checkPermission(CLUSTER_WRITE);
        if (ep.equals(localEp)) {
            try {
                dispatchLocally(message);
            } catch (IOException e) {
                return Tools.exceptionalFuture(e);
            }
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            Connection connection = null;
            try {
                connection = channels.borrowObject(ep);
                connection.send(message, future);
            } finally {
                if (connection != null) {
                    channels.returnObject(ep, connection);
                }
            }
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    @Override
    public CompletableFuture<byte[]> sendAndReceive(Endpoint ep, String type, byte[] payload) {
        checkPermission(CLUSTER_WRITE);
        return sendAndReceive(ep, type, payload, MoreExecutors.directExecutor());
    }

    @Override
    public CompletableFuture<byte[]> sendAndReceive(Endpoint ep, String type, byte[] payload, Executor executor) {
        checkPermission(CLUSTER_WRITE);
        CompletableFuture<byte[]> response = new CompletableFuture<>();
        Callback callback = new Callback(response, executor);
        Long messageId = messageIdGenerator.incrementAndGet();
        callbacks.put(messageId, callback);
        InternalMessage message = new InternalMessage(preamble,
                                                      clockService.timeNow(),
                                                      messageId,
                                                      localEp,
                                                      type,
                                                      payload);
        return sendAsync(ep, message).whenComplete((r, e) -> {
            if (e != null) {
                callbacks.invalidate(messageId);
            }
        }).thenComposeAsync(v -> response, executor);
    }

    @Override
    public void registerHandler(String type, BiConsumer<Endpoint, byte[]> handler, Executor executor) {
        checkPermission(CLUSTER_WRITE);
        handlers.put(type, message -> executor.execute(() -> handler.accept(message.sender(), message.payload())));
    }

    @Override
    public void registerHandler(String type, BiFunction<Endpoint, byte[], byte[]> handler, Executor executor) {
        checkPermission(CLUSTER_WRITE);
        handlers.put(type, message -> executor.execute(() -> {
            byte[] responsePayload = null;
            Status status = Status.OK;
            try {
                responsePayload = handler.apply(message.sender(), message.payload());
            } catch (Exception e) {
                status = Status.ERROR_HANDLER_EXCEPTION;
            }
            sendReply(message, status, Optional.ofNullable(responsePayload));
        }));
    }

    @Override
    public void registerHandler(String type, BiFunction<Endpoint, byte[], CompletableFuture<byte[]>> handler) {
        checkPermission(CLUSTER_WRITE);
        handlers.put(type, message -> {
            handler.apply(message.sender(), message.payload()).whenComplete((result, error) -> {
                Status status = error == null ? Status.OK : Status.ERROR_HANDLER_EXCEPTION;
                sendReply(message, status, Optional.ofNullable(result));
            });
        });
    }

    @Override
    public void unregisterHandler(String type) {
        checkPermission(CLUSTER_WRITE);
        handlers.remove(type);
    }

    private void startAcceptingConnections() throws InterruptedException {
        ServerBootstrap b = new ServerBootstrap();
        b.childOption(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 32 * 1024);
        b.childOption(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 8 * 1024);
        b.option(ChannelOption.SO_RCVBUF, 1048576);
        b.option(ChannelOption.TCP_NODELAY, true);
        b.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        b.group(serverGroup, clientGroup);
        b.channel(serverChannelClass);
        if (enableNettyTls) {
            b.childHandler(new SslServerCommunicationChannelInitializer());
        } else {
            b.childHandler(new OnosCommunicationChannelInitializer());
        }
        b.option(ChannelOption.SO_BACKLOG, 128);
        b.childOption(ChannelOption.SO_KEEPALIVE, true);

        // Bind and start to accept incoming connections.
        b.bind(localEp.port()).sync().addListener(future -> {
            if (future.isSuccess()) {
                log.info("{} accepting incoming connections on port {}", localEp.host(), localEp.port());
            } else {
                log.warn("{} failed to bind to port {}", localEp.host(), localEp.port(), future.cause());
            }
        });
    }

    private class OnosCommunicationChannelFactory
            implements KeyedPoolableObjectFactory<Endpoint, Connection> {

        @Override
        public void activateObject(Endpoint endpoint,  Connection connection)
                throws Exception {
        }

        @Override
        public void destroyObject(Endpoint ep, Connection connection) throws Exception {
            log.debug("Closing connection to {}", ep);
            //Is this the right way to destroy?
            connection.destroy();
        }

        @Override
        public Connection makeObject(Endpoint ep) throws Exception {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
            bootstrap.option(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 10 * 64 * 1024);
            bootstrap.option(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 10 * 32 * 1024);
            bootstrap.option(ChannelOption.SO_SNDBUF, 1048576);
            bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000);
            bootstrap.group(clientGroup);
            // TODO: Make this faster:
            // http://normanmaurer.me/presentations/2014-facebook-eng-netty/slides.html#37.0
            bootstrap.channel(clientChannelClass);
            bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
            if (enableNettyTls) {
                bootstrap.handler(new SslClientCommunicationChannelInitializer());
            } else {
                bootstrap.handler(new OnosCommunicationChannelInitializer());
            }
            // Start the client.
            CompletableFuture<Channel> retFuture = new CompletableFuture<>();
            ChannelFuture f = bootstrap.connect(ep.host().toInetAddress(), ep.port());

            f.addListener(future -> {
                if (future.isSuccess()) {
                    retFuture.complete(f.channel());
                } else {
                    retFuture.completeExceptionally(future.cause());
                }
            });
            log.debug("Established a new connection to {}", ep);
            return new Connection(retFuture);
        }

        @Override
        public void passivateObject(Endpoint ep, Connection connection)
                throws Exception {
        }

        @Override
        public boolean validateObject(Endpoint ep, Connection connection) {
            return connection.validate();
        }
    }

    private class SslServerCommunicationChannelInitializer extends ChannelInitializer<SocketChannel> {

        private final ChannelHandler dispatcher = new InboundMessageDispatcher();
        private final ChannelHandler encoder = new MessageEncoder(preamble);

        @Override
        protected void initChannel(SocketChannel channel) throws Exception {
            TrustManagerFactory tmFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            KeyStore ts = KeyStore.getInstance("JKS");
            ts.load(new FileInputStream(tsLocation), tsPwd);
            tmFactory.init(ts);

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(new FileInputStream(ksLocation), ksPwd);
            kmf.init(ks, ksPwd);

            SSLContext serverContext = SSLContext.getInstance("TLS");
            serverContext.init(kmf.getKeyManagers(), tmFactory.getTrustManagers(), null);

            SSLEngine serverSslEngine = serverContext.createSSLEngine();

            serverSslEngine.setNeedClientAuth(true);
            serverSslEngine.setUseClientMode(false);
            serverSslEngine.setEnabledProtocols(serverSslEngine.getSupportedProtocols());
            serverSslEngine.setEnabledCipherSuites(serverSslEngine.getSupportedCipherSuites());
            serverSslEngine.setEnableSessionCreation(true);

            channel.pipeline().addLast("ssl", new io.netty.handler.ssl.SslHandler(serverSslEngine))
                    .addLast("encoder", encoder)
                    .addLast("decoder", new MessageDecoder())
                    .addLast("handler", dispatcher);
        }
    }

    private class SslClientCommunicationChannelInitializer extends ChannelInitializer<SocketChannel> {

        private final ChannelHandler dispatcher = new InboundMessageDispatcher();
        private final ChannelHandler encoder = new MessageEncoder(preamble);

        @Override
        protected void initChannel(SocketChannel channel) throws Exception {
            TrustManagerFactory tmFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            KeyStore ts = KeyStore.getInstance("JKS");
            ts.load(new FileInputStream(tsLocation), tsPwd);
            tmFactory.init(ts);

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(new FileInputStream(ksLocation), ksPwd);
            kmf.init(ks, ksPwd);

            SSLContext clientContext = SSLContext.getInstance("TLS");
            clientContext.init(kmf.getKeyManagers(), tmFactory.getTrustManagers(), null);

            SSLEngine clientSslEngine = clientContext.createSSLEngine();

            clientSslEngine.setUseClientMode(true);
            clientSslEngine.setEnabledProtocols(clientSslEngine.getSupportedProtocols());
            clientSslEngine.setEnabledCipherSuites(clientSslEngine.getSupportedCipherSuites());
            clientSslEngine.setEnableSessionCreation(true);

            channel.pipeline().addLast("ssl", new io.netty.handler.ssl.SslHandler(clientSslEngine))
                    .addLast("encoder", encoder)
                    .addLast("decoder", new MessageDecoder())
                    .addLast("handler", dispatcher);
        }
    }

    private class OnosCommunicationChannelInitializer extends ChannelInitializer<SocketChannel> {

        private final ChannelHandler dispatcher = new InboundMessageDispatcher();
        private final ChannelHandler encoder = new MessageEncoder(preamble);

        @Override
        protected void initChannel(SocketChannel channel) throws Exception {
            channel.pipeline()
                    .addLast("encoder", encoder)
                    .addLast("decoder", new MessageDecoder())
                    .addLast("handler", dispatcher);
        }
    }

    @ChannelHandler.Sharable
    private class InboundMessageDispatcher extends SimpleChannelInboundHandler<Object> {
     // Effectively SimpleChannelInboundHandler<InternalMessage>,
     // had to specify <Object> to avoid Class Loader not being able to find some classes.

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object rawMessage) throws Exception {
            InternalMessage message = (InternalMessage) rawMessage;
            try {
                dispatchLocally(message);
            } catch (RejectedExecutionException e) {
                log.warn("Unable to dispatch message due to {}", e.getMessage());
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
            log.error("Exception inside channel handling pipeline.", cause);
            context.close();
        }

        /**
         * Returns true if the given message should be handled.
         *
         * @param msg inbound message
         * @return true if {@code msg} is {@link InternalMessage} instance.
         *
         * @see SimpleChannelInboundHandler#acceptInboundMessage(Object)
         */
        @Override
        public final boolean acceptInboundMessage(Object msg) {
            return msg instanceof InternalMessage;
        }
    }

    private void dispatchLocally(InternalMessage message) throws IOException {
        if (message.preamble() != preamble) {
            log.debug("Received {} with invalid preamble from {}", message.type(), message.sender());
            sendReply(message, Status.PROTOCOL_EXCEPTION, Optional.empty());
        }
        clockService.recordEventTime(message.time());
        String type = message.type();
        if (REPLY_MESSAGE_TYPE.equals(type)) {
            try {
                Callback callback =
                        callbacks.getIfPresent(message.id());
                if (callback != null) {
                    if (message.status() == Status.OK) {
                        callback.complete(message.payload());
                    } else if (message.status() == Status.ERROR_NO_HANDLER) {
                        callback.completeExceptionally(new MessagingException.NoRemoteHandler());
                    } else if (message.status() == Status.ERROR_HANDLER_EXCEPTION) {
                        callback.completeExceptionally(new MessagingException.RemoteHandlerFailure());
                    } else if (message.status() == Status.PROTOCOL_EXCEPTION) {
                        callback.completeExceptionally(new MessagingException.ProtocolException());
                    }
                } else {
                    log.debug("Received a reply for message id:[{}]. "
                                     + " from {}. But was unable to locate the"
                                     + " request handle", message.id(), message.sender());
                }
            } finally {
                callbacks.invalidate(message.id());
            }
            return;
        }
        Consumer<InternalMessage> handler = handlers.get(type);
        if (handler != null) {
            handler.accept(message);
        } else {
            log.debug("No handler for message type {}", message.type(), message.sender());
            sendReply(message, Status.ERROR_NO_HANDLER, Optional.empty());
        }
    }

    private void sendReply(InternalMessage message, Status status, Optional<byte[]> responsePayload) {
        InternalMessage response = new InternalMessage(preamble,
                clockService.timeNow(),
                message.id(),
                localEp,
                REPLY_MESSAGE_TYPE,
                responsePayload.orElse(new byte[0]),
                status);
        sendAsync(message.sender(), response).whenComplete((result, error) -> {
            if (error != null) {
                log.debug("Failed to respond", error);
            }
        });
    }

    private final class Callback {
        private final CompletableFuture<byte[]> future;
        private final Executor executor;

        public Callback(CompletableFuture<byte[]> future, Executor executor) {
            this.future = future;
            this.executor = executor;
        }

        public void complete(byte[] value) {
            executor.execute(() -> future.complete(value));
        }

        public void completeExceptionally(Throwable error) {
            executor.execute(() -> future.completeExceptionally(error));
        }
    }
    private final class Connection {
        private final CompletableFuture<Channel> internalFuture;

        public Connection(CompletableFuture<Channel> internalFuture) {
            this.internalFuture = internalFuture;
        }

        /**
         * Sends a message out on its channel and associated the message with a
         * completable future used for signaling.
         * @param message the message to be sent
         * @param future a future that is completed normally or exceptionally if
         *               message sending succeeds or fails respectively
         */
        public void send(Object message, CompletableFuture<Void> future) {
            internalFuture.whenComplete((channel, throwable) -> {
                if (throwable == null) {
                    channel.writeAndFlush(message).addListener(channelFuture -> {
                        if (!channelFuture.isSuccess()) {
                            future.completeExceptionally(channelFuture.cause());
                        } else {
                            future.complete(null);
                        }
                    });
                } else {
                    future.completeExceptionally(throwable);
                }
            });
        }

        /**
         * Destroys a channel by closing its channel (if it exists) and
         * cancelling its future.
         */
        public void destroy() {
            Channel channel = internalFuture.getNow(null);
            if (channel != null) {
                channel.close();
            }
            internalFuture.cancel(false);
        }

        /**
         * Determines whether the connection is valid meaning it is either
         * complete with and active channel
         * or it has not yet completed.
         * @return true if the channel has an active connection or has not
         * yet completed
         */
        public boolean validate() {
            if (internalFuture.isCompletedExceptionally()) {
                return false;
            }
            Channel channel = internalFuture.getNow(null);
            return channel == null || channel.isActive();
        }
    }
}
