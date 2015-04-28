package org.onosproject.ovsdb.lib.impl;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.CharsetUtil;

import org.onlab.packet.IpAddress;
import org.onosproject.ovsdb.lib.OvsdbClient;
import org.onosproject.ovsdb.lib.OvsdbConnection;
import org.onosproject.ovsdb.lib.jsonrpc.ExceptionHandler;
import org.onosproject.ovsdb.lib.jsonrpc.OvsdbRpcDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsdbConnectionImpl implements OvsdbConnection {
    private static final Logger log = LoggerFactory
            .getLogger(OvsdbConnectionImpl.class);
    boolean singletonCreated = false;

    @Override
    public OvsdbClient connect(IpAddress address, int port) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void disconnect(OvsdbClient client) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean startOvsdbManager(int ovsdbListenPort) {

        if (!singletonCreated) {
            new Thread() {
                @Override
                public void run() {
                    ovsdbManager(ovsdbListenPort);
                }

            }.start();
            singletonCreated = true;
            return true;
        } else {
            return false;
        }
    }

    private void ovsdbManager(int ovsdbListenPort) {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 100)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel channel)
                                throws Exception {
                            log.debug("New Passive channel created : "
                                    + channel.toString());
                            channel.pipeline()
                                    .addLast(new OvsdbRpcDecoder(100000),
                                             new StringEncoder(
                                                               CharsetUtil.UTF_8),
                                             new ExceptionHandler());

                        }
                    });
            b.option(ChannelOption.TCP_NODELAY, true);
            b.option(ChannelOption.RCVBUF_ALLOCATOR,
                     new AdaptiveRecvByteBufAllocator(65535, 65535, 65535));
            // Start the server.
            ChannelFuture f = b.bind(ovsdbListenPort).sync();
            Channel serverListenChannel = f.channel();
            // Wait until the server socket is closed.
            serverListenChannel.closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("Thread interrupted", e);
        } finally {
            // Shut down all event loops to terminate all threads.
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }

    }
}
