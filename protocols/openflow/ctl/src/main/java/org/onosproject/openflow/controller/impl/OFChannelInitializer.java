/*
 * Copyright 2015-present Open Networking Laboratory
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

package org.onosproject.openflow.controller.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.concurrent.EventExecutorGroup;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

/**
 * Creates a ChannelInitializer for a server-side openflow channel.
 */
public class OFChannelInitializer
    extends ChannelInitializer<SocketChannel> {

    private final Logger log = LoggerFactory.getLogger(getClass());


    private final SSLContext sslContext;
    protected Controller controller;
    protected EventExecutorGroup pipelineExecutor;

    public OFChannelInitializer(Controller controller,
                                   EventExecutorGroup pipelineExecutor,
                                   SSLContext sslContext) {
        super();
        this.controller = controller;
        this.pipelineExecutor = pipelineExecutor;
        this.sslContext = sslContext;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {

        OFChannelHandler handler = new OFChannelHandler(controller);

        ChannelPipeline pipeline = ch.pipeline();
        if (sslContext != null) {
            log.info("OpenFlow SSL enabled.");
            SSLEngine sslEngine = sslContext.createSSLEngine();

            sslEngine.setNeedClientAuth(true);
            sslEngine.setUseClientMode(false);
            sslEngine.setEnabledProtocols(sslEngine.getSupportedProtocols());
            sslEngine.setEnabledCipherSuites(sslEngine.getSupportedCipherSuites());
            sslEngine.setEnableSessionCreation(true);

            SslHandler sslHandler = new SslHandler(sslEngine);
            pipeline.addLast("ssl", sslHandler);
        } else {
            log.debug("OpenFlow SSL disabled.");
        }
        pipeline.addLast("ofmessageencoder", OFMessageEncoder.getInstance());
        pipeline.addLast("ofmessagedecoder", OFMessageDecoder.getInstance());

        pipeline.addLast("idle", new IdleStateHandler(20, 25, 0));
        pipeline.addLast("timeout", new ReadTimeoutHandler(30));

        // XXX S ONOS: was 15 increased it to fix Issue #296
        pipeline.addLast("handshaketimeout",
                         new HandshakeTimeoutHandler(handler, 60));
        // ExecutionHandler equivalent now part of Netty core
        if (pipelineExecutor != null) {
            pipeline.addLast(pipelineExecutor, "handler", handler);
        } else {
            pipeline.addLast("handler", handler);
        }
    }
}
