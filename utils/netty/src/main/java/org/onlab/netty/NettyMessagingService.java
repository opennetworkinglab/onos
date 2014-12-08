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
package org.onlab.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
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

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

/**
 * A Netty based implementation of MessagingService.
 */
public class NettyMessagingService implements MessagingService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Endpoint localEp;
    private final ConcurrentMap<String, MessageHandler> handlers = new ConcurrentHashMap<>();
    private final AtomicLong messageIdGenerator = new AtomicLong(0);
    private final Cache<Long, SettableFuture<byte[]>> responseFutures = CacheBuilder.newBuilder()
            .maximumSize(100000)
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .removalListener(new RemovalListener<Long, SettableFuture<byte[]>>() {
                @Override
                public void onRemoval(RemovalNotification<Long, SettableFuture<byte[]>> entry) {
                    entry.getValue().setException(new TimeoutException("Timedout waiting for reply"));
                }
            })
            .build();
    private final GenericKeyedObjectPool<Endpoint, Channel> channels
            = new GenericKeyedObjectPool<Endpoint, Channel>(new OnosCommunicationChannelFactory());

    private EventLoopGroup serverGroup;
    private EventLoopGroup clientGroup;
    private Class<? extends ServerChannel> serverChannelClass;
    private Class<? extends Channel> clientChannelClass;

    private void initEventLoopGroup() {
        // try Epoll first and if that does work, use nio.
        try {
            clientGroup = new EpollEventLoopGroup();
            serverGroup = new EpollEventLoopGroup();
            serverChannelClass = EpollServerSocketChannel.class;
            clientChannelClass = EpollSocketChannel.class;
            return;
        } catch (Throwable t) {
            log.warn("Failed to initialize native (epoll) transport. Reason: {}. Proceeding with nio.", t.getMessage());
        }
        clientGroup = new NioEventLoopGroup();
        serverGroup = new NioEventLoopGroup();
        serverChannelClass = NioServerSocketChannel.class;
        clientChannelClass = NioSocketChannel.class;
    }

    public NettyMessagingService(String ip, int port) {
        localEp = new Endpoint(ip, port);
    }

    public NettyMessagingService() {
        this(8080);
    }

    public NettyMessagingService(int port) {
        try {
            localEp = new Endpoint(java.net.InetAddress.getLocalHost().getHostName(), port);
        } catch (UnknownHostException e) {
            // Cannot resolve the local host, something is very wrong. Bailing out.
            throw new IllegalStateException("Cannot resolve local host", e);
        }
    }

    public void activate() throws InterruptedException {
        channels.setTestOnBorrow(true);
        channels.setTestOnReturn(true);
        initEventLoopGroup();
        startAcceptingConnections();
    }

    public void deactivate() throws Exception {
        channels.close();
        serverGroup.shutdownGracefully();
        clientGroup.shutdownGracefully();
    }

    /**
     * Returns the local endpoint for this instance.
     * @return local end point.
     */
    public Endpoint localEp() {
        return localEp;
    }

    @Override
    public void sendAsync(Endpoint ep, String type, byte[] payload) throws IOException {
        InternalMessage message = new InternalMessage.Builder(this)
            .withId(messageIdGenerator.incrementAndGet())
            .withSender(localEp)
            .withType(type)
            .withPayload(payload)
            .build();
        sendAsync(ep, message);
    }

    protected void sendAsync(Endpoint ep, InternalMessage message) throws IOException {
        Channel channel = null;
        try {
            try {
                channel = channels.borrowObject(ep);
                channel.eventLoop().execute(new WriteTask(channel, message));
            } finally {
                channels.returnObject(ep, channel);
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }

    }

    @Override
    public ListenableFuture<byte[]> sendAndReceive(Endpoint ep, String type, byte[] payload)
            throws IOException {
        SettableFuture<byte[]> futureResponse = SettableFuture.create();
        Long messageId = messageIdGenerator.incrementAndGet();
        responseFutures.put(messageId, futureResponse);
        InternalMessage message = new InternalMessage.Builder(this)
            .withId(messageId)
            .withSender(localEp)
            .withType(type)
            .withPayload(payload)
            .build();
        try {
            sendAsync(ep, message);
        } catch (Exception e) {
            responseFutures.invalidate(messageId);
            throw e;
        }
        return futureResponse;
    }

    @Override
    public void registerHandler(String type, MessageHandler handler) {
        handlers.putIfAbsent(type, handler);
    }

    @Override
    public void unregisterHandler(String type) {
        handlers.remove(type);
    }

    private MessageHandler getMessageHandler(String type) {
        return handlers.get(type);
    }

    private void startAcceptingConnections() throws InterruptedException {
        ServerBootstrap b = new ServerBootstrap();
        b.option(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 32 * 1024);
        b.option(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 8 * 1024);
        b.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        b.group(serverGroup, clientGroup)
            .channel(serverChannelClass)
            .childHandler(new OnosCommunicationChannelInitializer())
            .option(ChannelOption.SO_BACKLOG, 128)
            .childOption(ChannelOption.SO_KEEPALIVE, true);

        // Bind and start to accept incoming connections.
        b.bind(localEp.port()).sync();
    }

    private class OnosCommunicationChannelFactory
        implements KeyedPoolableObjectFactory<Endpoint, Channel> {

        @Override
        public void activateObject(Endpoint endpoint, Channel channel)
                throws Exception {
        }

        @Override
        public void destroyObject(Endpoint ep, Channel channel) throws Exception {
            channel.close();
        }

        @Override
        public Channel makeObject(Endpoint ep) throws Exception {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
            bootstrap.option(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 32 * 1024);
            bootstrap.option(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 8 * 1024);
            bootstrap.group(clientGroup);
            // TODO: Make this faster:
            // http://normanmaurer.me/presentations/2014-facebook-eng-netty/slides.html#37.0
            bootstrap.channel(clientChannelClass);
            bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
            bootstrap.handler(new OnosCommunicationChannelInitializer());
            // Start the client.
            ChannelFuture f = bootstrap.connect(ep.host(), ep.port()).sync();
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

    private class OnosCommunicationChannelInitializer extends ChannelInitializer<SocketChannel> {

        private final ChannelHandler dispatcher = new InboundMessageDispatcher();
        private final ChannelHandler encoder = new MessageEncoder();

        @Override
        protected void initChannel(SocketChannel channel) throws Exception {
            channel.pipeline()
                .addLast("encoder", encoder)
                .addLast("decoder", new MessageDecoder(NettyMessagingService.this))
                .addLast("handler", dispatcher);
        }
    }

    private static class WriteTask implements Runnable {

        private final InternalMessage message;
        private final Channel channel;

        public WriteTask(Channel channel, InternalMessage message) {
            this.channel = channel;
            this.message = message;
        }

        @Override
        public void run() {
            channel.writeAndFlush(message).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
        }
    }

    @ChannelHandler.Sharable
    private class InboundMessageDispatcher extends SimpleChannelInboundHandler<InternalMessage> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, InternalMessage message) throws Exception {
            String type = message.type();
            if (type.equals(InternalMessage.REPLY_MESSAGE_TYPE)) {
                try {
                    SettableFuture<byte[]> futureResponse =
                        NettyMessagingService.this.responseFutures.getIfPresent(message.id());
                    if (futureResponse != null) {
                        futureResponse.set(message.payload());
                    } else {
                        log.warn("Received a reply for message id:[{}]. "
                                + " from {}. But was unable to locate the"
                                + " request handle", message.id(), message.sender());
                    }
                } finally {
                    NettyMessagingService.this.responseFutures.invalidate(message.id());
                }
                return;
            }
            MessageHandler handler = NettyMessagingService.this.getMessageHandler(type);
            if (handler != null) {
                handler.handle(message);
            } else {
                log.debug("No handler registered for {}", type);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
            log.error("Exception inside channel handling pipeline.", cause);
            context.close();
        }
    }
}
