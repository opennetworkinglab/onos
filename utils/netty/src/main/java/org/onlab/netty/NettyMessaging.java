/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onlab.netty;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
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
import org.onosproject.store.cluster.messaging.Endpoint;
import org.onosproject.store.cluster.messaging.MessagingService;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Implementation of MessagingService based on <a href="http://netty.io/">Netty</a> framework.
 */
public class NettyMessaging implements MessagingService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String REPLY_MESSAGE_TYPE = "NETTY_MESSAGING_REQUEST_REPLY";

    private Endpoint localEp;
    private int preamble;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final Map<String, Consumer<InternalMessage>> handlers = new ConcurrentHashMap<>();
    private final AtomicLong messageIdGenerator = new AtomicLong(0);
    private final Cache<Long, CompletableFuture<byte[]>> responseFutures = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .removalListener(new RemovalListener<Long, CompletableFuture<byte[]>>() {
                @Override
                public void onRemoval(RemovalNotification<Long, CompletableFuture<byte[]>> entry) {
                    if (entry.wasEvicted()) {
                        entry.getValue().completeExceptionally(new TimeoutException("Timedout waiting for reply"));
                    }
                }
            })
            .build();

    private final GenericKeyedObjectPool<Endpoint, Channel> channels
            = new GenericKeyedObjectPool<Endpoint, Channel>(new OnosCommunicationChannelFactory());

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

    private void initEventLoopGroup() {
        // try Epoll first and if that does work, use nio.
        try {
            clientGroup = new EpollEventLoopGroup();
            serverGroup = new EpollEventLoopGroup();
            serverChannelClass = EpollServerSocketChannel.class;
            clientChannelClass = EpollSocketChannel.class;
            return;
        } catch (Throwable e) {
            log.debug("Failed to initialize native (epoll) transport. "
                    + "Reason: {}. Proceeding with nio.", e.getMessage());
        }
        clientGroup = new NioEventLoopGroup();
        serverGroup = new NioEventLoopGroup();
        serverChannelClass = NioServerSocketChannel.class;
        clientChannelClass = NioSocketChannel.class;
    }

    public void start(int preamble, Endpoint localEp) throws Exception {
        if (started.get()) {
            log.warn("Already running at local endpoint: {}", localEp);
            return;
        }
        this.preamble = preamble;
        this.localEp = localEp;
        channels.setLifo(true);
        channels.setTestOnBorrow(true);
        channels.setTestOnReturn(true);
        channels.setMinEvictableIdleTimeMillis(60_000L);
        channels.setTimeBetweenEvictionRunsMillis(30_000L);
        initEventLoopGroup();
        startAcceptingConnections();
        started.set(true);
    }

    public void stop() throws Exception {
        if (started.get()) {
            channels.close();
            serverGroup.shutdownGracefully();
            clientGroup.shutdownGracefully();
            started.set(false);
        }
    }

    @Override
    public CompletableFuture<Void> sendAsync(Endpoint ep, String type, byte[] payload) {
        InternalMessage message = new InternalMessage(messageIdGenerator.incrementAndGet(),
                                                      localEp,
                                                      type,
                                                      payload);
        return sendAsync(ep, message);
    }

    protected CompletableFuture<Void> sendAsync(Endpoint ep, InternalMessage message) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            if (ep.equals(localEp)) {
                dispatchLocally(message);
                future.complete(null);
            } else {
                Channel channel = null;
                try {
                    channel = channels.borrowObject(ep);
                    channel.writeAndFlush(message).addListener(channelFuture -> {
                        if (!channelFuture.isSuccess()) {
                            future.completeExceptionally(channelFuture.cause());
                        } else {
                            future.complete(null);
                        }
                    });
                } finally {
                    channels.returnObject(ep, channel);
                }
            }
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    @Override
    public CompletableFuture<byte[]> sendAndReceive(Endpoint ep, String type, byte[] payload) {
        CompletableFuture<byte[]> response = new CompletableFuture<>();
        Long messageId = messageIdGenerator.incrementAndGet();
        responseFutures.put(messageId, response);
        InternalMessage message = new InternalMessage(messageId, localEp, type, payload);
        try {
            sendAsync(ep, message);
        } catch (Exception e) {
            responseFutures.invalidate(messageId);
            response.completeExceptionally(e);
        }
        return response;
    }

    @Override
    public void registerHandler(String type, Consumer<byte[]> handler, Executor executor) {
        handlers.put(type, message -> executor.execute(() -> handler.accept(message.payload())));
    }

    @Override
    public void registerHandler(String type, Function<byte[], byte[]> handler, Executor executor) {
        handlers.put(type, message -> executor.execute(() -> {
            byte[] responsePayload = handler.apply(message.payload());
            if (responsePayload != null) {
                InternalMessage response = new InternalMessage(message.id(),
                        localEp,
                        REPLY_MESSAGE_TYPE,
                        responsePayload);
                sendAsync(message.sender(), response).whenComplete((result, error) -> {
                    if (error != null) {
                        log.debug("Failed to respond", error);
                    }
                });
            }
        }));
    }

    @Override
    public void registerHandler(String type, Function<byte[], CompletableFuture<byte[]>> handler) {
        handlers.put(type, message -> {
            handler.apply(message.payload()).whenComplete((result, error) -> {
                if (error == null) {
                    InternalMessage response = new InternalMessage(message.id(),
                                                                   localEp,
                                                                   REPLY_MESSAGE_TYPE,
                                                                   result);
                    sendAsync(message.sender(), response).whenComplete((r, e) -> {
                        if (e != null) {
                            log.debug("Failed to respond", e);
                        }
                    });
                }
            });
        });
    }

    @Override
    public void unregisterHandler(String type) {
        handlers.remove(type);
    }

    private void startAcceptingConnections() throws InterruptedException {
        ServerBootstrap b = new ServerBootstrap();
        b.option(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 32 * 1024);
        b.option(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 8 * 1024);
        b.option(ChannelOption.SO_RCVBUF, 1048576);
        b.option(ChannelOption.TCP_NODELAY, true);
        b.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
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
        implements KeyedPoolableObjectFactory<Endpoint, Channel> {

        @Override
        public void activateObject(Endpoint endpoint, Channel channel)
                throws Exception {
        }

        @Override
        public void destroyObject(Endpoint ep, Channel channel) throws Exception {
            log.debug("Closing connection to {}", ep);
            channel.close();
        }

        @Override
        public Channel makeObject(Endpoint ep) throws Exception {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
            bootstrap.option(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 10 * 64 * 1024);
            bootstrap.option(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 10 * 32 * 1024);
            bootstrap.option(ChannelOption.SO_SNDBUF, 1048576);
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
            ChannelFuture f = bootstrap.connect(ep.host().toString(), ep.port()).sync();
            log.debug("Established a new connection to {}", ep);
            return f.channel();
        }

        @Override
        public void passivateObject(Endpoint ep, Channel channel)
                throws Exception {
        }

        @Override
        public boolean validateObject(Endpoint ep, Channel channel) {
            return channel.isOpen();
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
                    .addLast("decoder", new MessageDecoder(preamble))
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
                    .addLast("decoder", new MessageDecoder(preamble))
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
                        .addLast("decoder", new MessageDecoder(preamble))
                        .addLast("handler", dispatcher);
        }
    }

    @ChannelHandler.Sharable
    private class InboundMessageDispatcher extends SimpleChannelInboundHandler<InternalMessage> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, InternalMessage message) throws Exception {
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
    }
    private void dispatchLocally(InternalMessage message) throws IOException {
        String type = message.type();
        if (REPLY_MESSAGE_TYPE.equals(type)) {
            try {
                CompletableFuture<byte[]> futureResponse =
                    responseFutures.getIfPresent(message.id());
                if (futureResponse != null) {
                    futureResponse.complete(message.payload());
                } else {
                    log.warn("Received a reply for message id:[{}]. "
                            + " from {}. But was unable to locate the"
                            + " request handle", message.id(), message.sender());
                }
            } finally {
                responseFutures.invalidate(message.id());
            }
            return;
        }
        Consumer<InternalMessage> handler = handlers.get(type);
        if (handler != null) {
            handler.accept(message);
        } else {
            log.debug("No handler registered for {}", type);
        }
    }
}
