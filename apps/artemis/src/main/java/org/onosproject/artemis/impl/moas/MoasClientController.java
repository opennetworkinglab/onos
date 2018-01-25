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
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onosproject.artemis.ArtemisPacketProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MOAS Client Controller.
 */
@Beta
public class MoasClientController {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private IpAddress host;
    private EventLoopGroup workerGroup;
    private MoasClientHandler ach;
    private ChannelFuture channel;
    private IpAddress localIp;
    private IpPrefix localPrefix;
    private ArtemisPacketProcessor packetProcessor;

    public MoasClientController(ArtemisPacketProcessor packetProcessor,
                                IpAddress host, IpAddress localIp, IpPrefix localPrefix) {
        this.host = host;
        this.ach = null;
        this.localIp = localIp;
        this.localPrefix = localPrefix;
        this.packetProcessor = packetProcessor;
    }

    /**
     * Run the MOAS client.
     */
    public void run() {
        try {
            final Bootstrap bootstrap = createBootstrap();

            ach = new MoasClientHandler(localIp, localPrefix, packetProcessor);

            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(ach);
                }
            });

            channel = bootstrap.connect(host.toInetAddress(), 32323).sync();
        } catch (Exception e) {
            log.warn(ExceptionUtils.getFullStackTrace(e));
        }
    }

    /**
     * Bootstrap netty socket.
     *
     * @return bootstrap
     */
    private Bootstrap createBootstrap() {
        workerGroup = new NioEventLoopGroup();
        return new Bootstrap()
                .group(workerGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true);
    }

    /**
     * Stop the MOAS client.
     */
    public void stop() {
        channel.channel().close();
        workerGroup.shutdownGracefully();
    }

}
