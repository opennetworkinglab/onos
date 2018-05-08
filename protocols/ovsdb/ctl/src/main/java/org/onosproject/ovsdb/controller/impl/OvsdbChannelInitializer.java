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

package org.onosproject.ovsdb.controller.impl;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

/**
 * Creates a ChannelInitializer for a server-side OVSDB channel.
 */
public class OvsdbChannelInitializer
        extends ChannelInitializer<SocketChannel> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final SSLContext sslContext;
    protected Controller controller;

    private static final int READER_IDLE_TIME = 20;
    private static final int WRITER_IDLE_TIME = 25;
    private static final int ALL_IDLE_TIME = 0;
    private static final int TIMEOUT = 180;

    public OvsdbChannelInitializer(Controller controller, SSLContext sslContext) {
        super();
        this.controller = controller;
        this.sslContext = sslContext;
    }

    @Override
    protected void initChannel(SocketChannel channel) throws Exception {

        ChannelPipeline pipeline = channel.pipeline();
        if (sslContext != null) {
            log.info("OVSDB SSL enabled.");
            SSLEngine sslEngine = sslContext.createSSLEngine();

            sslEngine.setNeedClientAuth(true);
            sslEngine.setUseClientMode(false);
            sslEngine.setEnabledProtocols(sslEngine.getSupportedProtocols());
            sslEngine.setEnabledCipherSuites(sslEngine.getSupportedCipherSuites());
            sslEngine.setEnableSessionCreation(true);

            SslHandler sslHandler = new SslHandler(sslEngine);
            pipeline.addLast("ssl", sslHandler);
        } else {
            log.info("OVSDB SSL disabled.");
        }
        pipeline.addLast(new StringEncoder(CharsetUtil.UTF_8));
        pipeline.addLast(new MessageDecoder());

        pipeline.addLast(new IdleStateHandler(READER_IDLE_TIME, WRITER_IDLE_TIME, ALL_IDLE_TIME));
        pipeline.addLast(new ReadTimeoutHandler(TIMEOUT));
        controller.handleNewNodeConnection(channel);
    }
}

