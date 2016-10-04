/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.lisp.ctl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * The main LISP controller class.
 * Bootstraps LISP netty channel, handles all setup and network listeners.
 */
public class LispController {

    protected static final Logger log = LoggerFactory.getLogger(LispController.class);

    private static final int LISP_DATA_PORT = 4341;
    private static final int LISP_CONTROL_PORT = 4342;

    // Configuration options
    protected List<Integer> lispPorts = ImmutableList.of(LISP_DATA_PORT, LISP_CONTROL_PORT);

    protected static final int SEND_BUFFER_SIZE = 4 * 1024 * 1024;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    /**
     * Stitches all channel handlers into server bootstrap.
     */
    public void run() {

        try {
            final ServerBootstrap bootstrap = createServerBootstrap();

            configBootstrapOptions(bootstrap);

            List<ChannelFuture> channelFutures = Lists.newArrayList();

            lispPorts.forEach(p -> {
                InetSocketAddress sa = new InetSocketAddress(p);
                channelFutures.add(bootstrap.bind(sa));
                log.info("Listening for LISP router connections on {}", sa);
            });

            for (ChannelFuture f : channelFutures) {
                f.sync();
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Initializes server bootstrap with given LISP channel initializer.
     *
     * @return initialized server bootstrap
     */
    private ServerBootstrap createServerBootstrap() {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new LispChannelInitializer());

        return bootstrap;
    }

    /**
     * Configures bootstrap options to tune the communication performance.
     *
     * @param bootstrap LISP server bootstrap
     */
    private void configBootstrapOptions(ServerBootstrap bootstrap) {
        bootstrap.option(ChannelOption.SO_REUSEADDR, true);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.option(ChannelOption.SO_SNDBUF, SEND_BUFFER_SIZE);
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
            e.printStackTrace();
        }
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
            bossGroup.shutdownGracefully().sync();
            workerGroup.shutdownGracefully().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
