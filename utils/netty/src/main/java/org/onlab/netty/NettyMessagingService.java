package org.onlab.netty;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

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
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * A Netty based implementation of MessagingService.
 */
public class NettyMessagingService implements MessagingService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final int port;
    private final Endpoint localEp;
    private final EventLoopGroup bossGroup = new NioEventLoopGroup();
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();
    private final ConcurrentMap<String, MessageHandler> handlers = new ConcurrentHashMap<>();
    private final Cache<Long, AsyncResponse<?>> responseFutures = CacheBuilder.newBuilder()
            .maximumSize(100000)
            .weakValues()
            // TODO: Once the entry expires, notify blocking threads (if any).
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();
    private final GenericKeyedObjectPool<Endpoint, Channel> channels
            = new GenericKeyedObjectPool<Endpoint, Channel>(new OnosCommunicationChannelFactory());

    protected PayloadSerializer payloadSerializer;

    public NettyMessagingService() {
        // TODO: Default port should be configurable.
        this(8080);
    }

    // FIXME: Constructor should not throw exceptions.
    public NettyMessagingService(int port) {
        this.port = port;
        try {
            localEp = new Endpoint(java.net.InetAddress.getLocalHost().getHostName(), port);
        } catch (UnknownHostException e) {
            // bailing out.
            throw new RuntimeException(e);
        }
    }

    public void activate() throws Exception {
        channels.setTestOnBorrow(true);
        channels.setTestOnReturn(true);
        startAcceptingConnections();
    }

    public void deactivate() throws Exception {
        channels.close();
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

    @Override
    public void sendAsync(Endpoint ep, String type, Object payload) throws IOException {
        InternalMessage message = new InternalMessage.Builder(this)
            .withId(RandomUtils.nextLong())
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
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public <T> Response<T> sendAndReceive(Endpoint ep, String type, Object payload)
            throws IOException {
        AsyncResponse<T> futureResponse = new AsyncResponse<T>();
        Long messageId = RandomUtils.nextLong();
        responseFutures.put(messageId, futureResponse);
        InternalMessage message = new InternalMessage.Builder(this)
            .withId(messageId)
            .withSender(localEp)
            .withType(type)
            .withPayload(payload)
            .build();
        sendAsync(ep, message);
        return futureResponse;
    }

    @Override
    public void registerHandler(String type, MessageHandler handler) {
        // TODO: Is this the right semantics for handler registration?
        handlers.putIfAbsent(type, handler);
    }

    public void unregisterHandler(String type) {
        handlers.remove(type);
    }

    @Override
    public void setPayloadSerializer(PayloadSerializer payloadSerializer) {
        this.payloadSerializer = payloadSerializer;
    }

    private MessageHandler getMessageHandler(String type) {
        return handlers.get(type);
    }

    private void startAcceptingConnections() throws InterruptedException {
        ServerBootstrap b = new ServerBootstrap();
        b.option(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 32 * 1024);
        b.option(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 8 * 1024);
        // TODO: Need JVM options to configure PooledByteBufAllocator.
        b.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        b.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .childHandler(new OnosCommunicationChannelInitializer())
            .option(ChannelOption.SO_BACKLOG, 128)
            .childOption(ChannelOption.SO_KEEPALIVE, true);

        // Bind and start to accept incoming connections.
        b.bind(port).sync();
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
            bootstrap.group(workerGroup);
            // TODO: Make this faster:
            // http://normanmaurer.me/presentations/2014-facebook-eng-netty/slides.html#37.0
            bootstrap.channel(NioSocketChannel.class);
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
        private final ChannelHandler encoder = new MessageEncoder(payloadSerializer);

        @Override
        protected void initChannel(SocketChannel channel) throws Exception {
            channel.pipeline()
                .addLast("encoder", encoder)
                .addLast("decoder", new MessageDecoder(NettyMessagingService.this, payloadSerializer))
                .addLast("handler", dispatcher);
        }
    }

    private class WriteTask implements Runnable {

        private final InternalMessage message;
        private final Channel channel;

        public WriteTask(Channel channel, InternalMessage message) {
            this.channel = channel;
            this.message = message;
        }

        @Override
        public void run() {
            channel.writeAndFlush(message, channel.voidPromise());
        }
    }

    @ChannelHandler.Sharable
    private class InboundMessageDispatcher extends SimpleChannelInboundHandler<InternalMessage> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, InternalMessage message) throws Exception {
            String type = message.type();
            if (type.equals(InternalMessage.REPLY_MESSAGE_TYPE)) {
                try {
                    AsyncResponse<?> futureResponse =
                        NettyMessagingService.this.responseFutures.getIfPresent(message.id());
                    if (futureResponse != null) {
                        futureResponse.setResponse(message.payload());
                    } else {
                        log.warn("Received a reply. But was unable to locate the request handle");
                    }
                } finally {
                    NettyMessagingService.this.responseFutures.invalidate(message.id());
                }
                return;
            }
            MessageHandler handler = NettyMessagingService.this.getMessageHandler(type);
            handler.handle(message);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
            context.close();
        }
    }
}
