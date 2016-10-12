/*
 * Copyright 2017-present Open Networking Laboratory
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

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.nio.NioEventLoopGroup;
import org.onosproject.ofagent.api.OFController;
import org.onosproject.ofagent.api.OFSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of OpenFlow connection handler.
 * It retries a connection for a certain amount of time and then give up.
 */
public final class OFConnectionHandler implements ChannelFutureListener {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final int MAX_RETRY = 10;

    private final AtomicInteger retryCount = new AtomicInteger();
    private final OFSwitch ofSwitch;
    private final OFController controller;
    private final NioEventLoopGroup workGroup;

    /**
     * Default constructor.
     *
     * @param ofSwitch   openflow switch that initiates this connection
     * @param controller controller to connect
     * @param workGroup  work group for connection
     */
    public OFConnectionHandler(OFSwitch ofSwitch, OFController controller,
                               NioEventLoopGroup workGroup) {
        this.ofSwitch = ofSwitch;
        this.controller = controller;
        this.workGroup = workGroup;
    }

    /**
     * Creates a connection to the supplied controller.
     */
    public void connect() {
        // TODO initiates a connection to the controller
    }

    @Override
    public void operationComplete(ChannelFuture future) throws Exception {

        if (future.isSuccess()) {
            log.debug("{} is connected to controller {}", ofSwitch.device().id(), controller);
            // TODO do something for a new connection if there's any
        } else {
            log.debug("{} failed to connect {}, retry..", ofSwitch.device().id(), controller);
            // TODO retry connect if retry count is less than MAX
        }
    }
}
