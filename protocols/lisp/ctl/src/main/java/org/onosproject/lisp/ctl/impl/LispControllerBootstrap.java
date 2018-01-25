/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.lisp.ctl.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.AbstractChannel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * Bootstraps LISP netty channel, handles all setup and network listeners.
 */
public class LispControllerBootstrap {

    private static final Logger log = LoggerFactory.getLogger(LispControllerBootstrap.class);

    private static final int LISP_DATA_PORT = 4341;
    private static final int LISP_CONTROL_PORT = 4342;

    // Configuration options
    protected List<Integer> lispPorts = ImmutableList.of(LISP_DATA_PORT, LISP_CONTROL_PORT);

    private EventLoopGroup eventLoopGroup;
    private Class<? extends AbstractChannel> channelClass;
    private List<ChannelFuture> channelFutures = Lists.newArrayList();

    /**
     * Stitches all channel handlers into server bootstrap.
     */
    private void run() {

        try {
            final Bootstrap bootstrap = createServerBootstrap();

            configBootstrapOptions(bootstrap);

            lispPorts.forEach(p -> {
                InetSocketAddress sa = new InetSocketAddress(p);
                channelFutures.add(bootstrap.bind(sa));
                log.info("Listening for LISP router connections on {}", sa);
            });

            for (ChannelFuture f : channelFutures) {
                f.sync();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Initializes server bootstrap with given LISP channel initializer.
     *
     * @return initialized server bootstrap
     */
    private Bootstrap createServerBootstrap() {
        Bootstrap bootstrap = new Bootstrap();

        initEventLoopGroup();
        bootstrap.group(eventLoopGroup)
                .channel(channelClass)
                .handler(new LispChannelInitializer());

        return bootstrap;
    }

    /**
     * Configures bootstrap options to tune the communication performance.
     *
     * @param bootstrap LISP server bootstrap
     */
    private void configBootstrapOptions(Bootstrap bootstrap) {
        bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
    }

    /**
     * Closes all open channels.
     *
     * @param channelFutures a collection of channel futures
     */
    private void closeChannels(List<ChannelFuture> channelFutures) {
        try {
            for (ChannelFuture f : channelFutures) {
                f.channel().closeFuture().sync();
            }
        } catch (InterruptedException e) {
            log.warn("Failed to close channels. Reasons: {}.", e.getMessage());
        }
    }

    /**
     * Initializes event loop group.
     */
    private void initEventLoopGroup() {

        // try to use EpollEventLoopGroup if possible,
        // if OS does not support native Epoll, fallback to use netty NIO
        try {
            eventLoopGroup = new EpollEventLoopGroup();
            channelClass = EpollDatagramChannel.class;
        } catch (NoClassDefFoundError e) {
            log.debug("Failed to initialize native (epoll) transport. "
                        + "Reason: {}. Proceeding with NIO event group.", e);
        }
        eventLoopGroup = new NioEventLoopGroup();
        channelClass = NioDatagramChannel.class;
    }

    /**
     * Launches LISP controller to listen control channel.
     */
    public void start() {
        log.info("Starting LISP control I/O");

        this.run();
    }

    /**
     * Terminates LISP controller and lease all occupied resources.
     */
    public void stop() {
        log.info("Stopping LISP control I/O");

        try {
            // try to shutdown all open event groups
            eventLoopGroup.shutdownGracefully().sync();
            closeChannels(channelFutures);
        } catch (InterruptedException e) {
            log.warn("Failed to stop LISP controller. Reasons: {}.", e.getMessage());
        }
    }
}
