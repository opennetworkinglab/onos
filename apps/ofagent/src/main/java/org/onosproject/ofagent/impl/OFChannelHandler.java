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

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import org.onosproject.ofagent.api.OFSwitch;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of OpenFlow channel handler.
 * It processes OpenFlow message according to the channel state.
 */
public final class OFChannelHandler extends ChannelDuplexHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final OFSwitch ofSwitch;

    private Channel channel;
    private ChannelState state;

    private enum ChannelState {

        INIT {
            @Override
            void processOFMessage(final OFChannelHandler handler,
                                  final OFMessage msg) {
                // TODO implement
            }
        },
        WAIT_HELLO {
            @Override
            void processOFMessage(final OFChannelHandler handler,
                                  final OFMessage msg) {
                // TODO implement
            }
        },
        WAIT_FEATURE_REQUEST {
            @Override
            void processOFMessage(final OFChannelHandler handler,
                                  final OFMessage msg) {
                // TODO implement
            }
        },
        ESTABLISHED {
            @Override
            void processOFMessage(final OFChannelHandler handler,
                                  final OFMessage msg) {
                // TODO implement
                // TODO add this channel to ofSwitch role service
            }
        };

        abstract void processOFMessage(final OFChannelHandler handler,
                                       final OFMessage msg);
    }

    /**
     * Default constructor.
     *
     * @param ofSwitch openflow switch that owns this channel
     */
    public OFChannelHandler(OFSwitch ofSwitch) {
        this.ofSwitch = ofSwitch;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        this.channel = ctx.channel();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {
        try {
            OFMessage ofMsg = (OFMessage) msg;
            // TODO process OF message

        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx)
            throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }

    private void setState(ChannelState state) {
        this.state = state;
    }
}
