/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.xmpp.core.ctl;

import com.google.common.base.Strings;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.AbstractChannel;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.onosproject.xmpp.core.XmppDeviceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Dictionary;

import static org.onlab.util.Tools.get;
import static org.onosproject.xmpp.core.ctl.OsgiPropertyConstants.XMPP_PORT;

/**
 *  The XMPP server class. Starts XMPP server and listens to new XMPP device TCP connections.
 */
public class XmppServer {

    protected static final Logger log = LoggerFactory.getLogger(XmppServer.class);

    protected Integer port = 5259;

    protected Channel channel;
    protected EventLoopGroup eventLoopGroup;
    protected Class<? extends AbstractChannel> channelClass;


    /**
     * Initializes XMPP server.
     */
    public void init() {

    }

    /**
     * Runs XMPP server thread.
     * @param deviceFactory XMPP devices factory
     */
    public void run(XmppDeviceFactory deviceFactory) {
        try {
            final ServerBootstrap bootstrap = createServerBootStrap(deviceFactory);

            InetSocketAddress socketAddress = new InetSocketAddress(port);
            channel = bootstrap.bind(socketAddress).sync().channel().closeFuture().channel();

            log.info("Listening for device connections on {}", socketAddress);

        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private ServerBootstrap createServerBootStrap(XmppDeviceFactory deviceFactory) {

        ServerBootstrap bootstrap = new ServerBootstrap();
        configureBootstrap(bootstrap);
        initEventLoopGroup();

        bootstrap.group(eventLoopGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new XmppChannelInitializer(deviceFactory));

        return bootstrap;
    }

    /**
     * Initializes event loop group.
     */
    private void initEventLoopGroup() {

        // try to use EpollEventLoopGroup if possible,
        // if OS does not support native Epoll, fallback to use netty NIO
        try {
            eventLoopGroup = new EpollEventLoopGroup();
            channelClass = EpollSocketChannel.class;
            return;
        } catch (Error e) {
            log.debug("Failed to initialize native (epoll) transport. "
                    + "Reason: {}. Proceeding with NIO event group.", e);
        }
        eventLoopGroup = new NioEventLoopGroup();
        channelClass = NioServerSocketChannel.class;
    }

    private void configureBootstrap(ServerBootstrap bootstrap) {
        bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        bootstrap.option(ChannelOption.SO_RCVBUF, 2048);
    }

    /**
     * TLS/SSL setup. If needed.
     */
    private void initTls() {
        // TODO: add support for TLS/SSL
    }

    /**
     * Sets configuration parameters defined via ComponentConfiguration subsystem.
     * @param properties properties to be set
     */
    public void setConfiguration(Dictionary<?, ?> properties) {
        String port = get(properties, XMPP_PORT);
        if (!Strings.isNullOrEmpty(port)) {
            this.port = Integer.parseInt(port);
        }
        log.debug("XMPP port set to {}", this.port);
    }

    /**
     * Starts XMPP server.
     *
     * @param deviceFactory XMPP devices factory
     */
    public void start(XmppDeviceFactory deviceFactory) {
        log.info("XMPP Server has started.");
        this.run(deviceFactory);
    }

    /**
     * Stops XMPP server.
     *
     */
    public void stop() {
        log.info("Stopping XMPP I/O");
        channel.close();
        eventLoopGroup.shutdownGracefully();
    }
}
