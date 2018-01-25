/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.artemis.impl.moas;

import com.google.common.annotations.Beta;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.onosproject.artemis.ArtemisMoasAgent;
import org.onosproject.artemis.ArtemisPacketProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * MOAS Server Controller.
 */
@Beta
public class MoasServerController {
    private final Logger log = LoggerFactory.getLogger(getClass());

    protected ArtemisMoasAgent deviceAgent;
    protected ArtemisPacketProcessor packetAgent;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ChannelFuture channel;
    private int port = 32323;

    private boolean isRunning = false;

    /**
     * Run the MOAS Servcer.
     */
    private void run() {
        final MoasServerController ctrl = this;
        try {
            final ServerBootstrap bootstrap = createServerBootStrap();

            bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(
                            new MoasServerHandler(ctrl)
                    );
                }
            });

            channel = bootstrap.bind(port).sync();
            isRunning = true;
        } catch (Exception e) {
            log.warn(ExceptionUtils.getFullStackTrace(e));
        }
    }

    /**
     * Create netty server bootstrap.
     *
     * @return bootstrap
     */
    private ServerBootstrap createServerBootStrap() {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();

        return new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true);
    }

    /**
     * Start Server Controller and initialize agents.
     *
     * @param deviceAgent device agent
     * @param packetAgent packet agen
     */
    public void start(ArtemisMoasAgent deviceAgent, ArtemisPacketProcessor packetAgent) {
        if (isRunning) {
            stop();
            this.deviceAgent = deviceAgent;
            this.packetAgent = packetAgent;
            run();
        } else {
            this.deviceAgent = deviceAgent;
            this.packetAgent = packetAgent;
            run();
        }
        isRunning = true;
    }

    /**
     * Stop Server Controller.
     */
    public void stop() {
        if (isRunning) {
            channel.channel().close();
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            isRunning = false;
        }
    }
}
