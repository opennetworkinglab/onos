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

    private GenericKeyedObjectPool<Endpoint, Channel> channels;

    private final int port;
    private final EventLoopGroup bossGroup = new NioEventLoopGroup();
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();
    private final ConcurrentMap<String, MessageHandler> handlers = new ConcurrentHashMap<>();
    private Cache<Long, AsyncResponse<?>> responseFutures;
    private final Endpoint localEp;

    protected Serializer serializer;

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
        channels = new GenericKeyedObjectPool<Endpoint, Channel>(new OnosCommunicationChannelFactory());
        channels.setTestOnBorrow(true);
        channels.setTestOnReturn(true);
        responseFutures = CacheBuilder.newBuilder()
                .maximumSize(100000)
                .weakValues()
                // TODO: Once the entry expires, notify blocking threads (if any).
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build();
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

    private MessageHandler getMessageHandler(String type) {
        return handlers.get(type);
    }

    private void startAcceptingConnections() throws InterruptedException {
        ServerBootstrap b = new ServerBootstrap();
        b.option(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 32 * 1024);
        b.option(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 32 * 1024);
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
            Bootstrap b = new Bootstrap();
            b.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
            b.option(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 32 * 1024);
            b.option(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 32 * 1024);
            b.group(workerGroup);
            // TODO: Make this faster:
            // http://normanmaurer.me/presentations/2014-facebook-eng-netty/slides.html#37.0
            b.channel(NioSocketChannel.class);
            b.option(ChannelOption.SO_KEEPALIVE, true);
            b.handler(new OnosCommunicationChannelInitializer());

            // Start the client.
            ChannelFuture f = b.connect(ep.host(), ep.port()).sync();
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

        @Override
        protected void initChannel(SocketChannel channel) throws Exception {
            channel.pipeline()
                .addLast("encoder", new MessageEncoder(serializer))
                .addLast("decoder", new MessageDecoder(NettyMessagingService.this, serializer))
                .addLast("handler", new InboundMessageDispatcher());
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
            channel.writeAndFlush(message);
        }
    }

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
                    }
                    log.warn("Received a reply. But was unable to locate the request handle");
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
