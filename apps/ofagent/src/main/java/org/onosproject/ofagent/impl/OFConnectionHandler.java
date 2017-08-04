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
package org.onosproject.ofagent.impl;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.onosproject.ofagent.api.OFController;
import org.onosproject.ofagent.api.OFSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of OpenFlow connection handler.
 * It retries a connection for a certain amount of time and then give up.
 */
public final class OFConnectionHandler implements ChannelFutureListener {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String MSG_STATE = "Device %s %s to controller %s:%s";
    private static final String MSG_CONNECTING = "connecting";
    private static final String MSG_CONNECTED = "connected";
    private static final String MSG_FAILED = "failed to connect";

    private final AtomicInteger retryCount;
    private final OFSwitch ofSwitch;
    private final OFController controller;
    private final EventLoopGroup workGroup;

    // TODO make this value configurable
    private static final int MAX_RETRY = 3;

    /**
     * Default constructor.
     *
     * @param ofSwitch   openflow switch that initiates this connection
     * @param controller controller to connect
     * @param workGroup  work group for connection
     */
    public OFConnectionHandler(OFSwitch ofSwitch, OFController controller,
                               EventLoopGroup workGroup) {
        this.ofSwitch = ofSwitch;
        this.controller = controller;
        this.workGroup = workGroup;
        this.retryCount = new AtomicInteger();
    }

    /**
     * Creates a connection to the supplied controller.
     */
    public void connect() {
        SocketAddress remoteAddr = new InetSocketAddress(
                controller.ip().toInetAddress(), controller.port().toInt());
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(workGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new OFChannelInitializer(ofSwitch));

        log.debug(String.format(MSG_STATE,
                ofSwitch.dpid(),
                MSG_CONNECTING,
                controller.ip(),
                controller.port()));
        bootstrap.connect(remoteAddr).addListener(this);
    }

    @Override
    public void operationComplete(ChannelFuture future) throws Exception {
        if (future.isSuccess()) {
            log.info(String.format(MSG_STATE,
                    ofSwitch.dpid(),
                    MSG_CONNECTED,
                    controller.ip(),
                    controller.port()));
            // FIXME add close future listener to handle connection lost
        } else {
            if (retryCount.getAndIncrement() > MAX_RETRY) {
                log.warn(String.format(MSG_STATE,
                        ofSwitch.dpid(),
                        MSG_FAILED,
                        controller.ip(),
                        controller.port()));
            } else {
                final EventLoop loop = future.channel().eventLoop();
                loop.schedule(this::connect, 1L, TimeUnit.SECONDS);
            }
        }
    }
}
