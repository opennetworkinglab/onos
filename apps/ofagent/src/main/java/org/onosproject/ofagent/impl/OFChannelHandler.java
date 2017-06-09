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
import io.netty.handler.timeout.ReadTimeoutException;
import org.onosproject.ofagent.api.OFSwitch;
import org.projectfloodlight.openflow.protocol.OFErrorMsg;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.RejectedExecutionException;

import static org.onosproject.ofagent.impl.OFChannelHandler.ChannelState.INIT;

/**
 * Implementation of OpenFlow channel handler.
 * It processes OpenFlow message according to the channel state.
 */
public final class OFChannelHandler extends ChannelDuplexHandler {

    private static final String MSG_CHANNEL_STATE = "Set channel(%s) state: %s";

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final OFSwitch ofSwitch;

    private Channel channel;
    private ChannelState state;

    enum ChannelState {

        INIT {
            @Override
            void processOFMessage(final OFChannelHandler handler,
                                  final OFMessage msg) {
                logProcessOFMessageDetails(handler, msg, this);
                // TODO implement
            }
        },
        WAIT_HELLO {
            @Override
            void processOFMessage(final OFChannelHandler handler,
                                  final OFMessage msg) {
                logProcessOFMessageDetails(handler, msg, this);
                switch (msg.getType()) {
                    case HELLO:
                        handler.setState(ChannelState.WAIT_FEATURE_REQUEST);
                        break;
                    default:
                        handler.illegalMessageReceived(msg);
                        break;
                }
            }
        },
        WAIT_FEATURE_REQUEST {
            @Override
            void processOFMessage(final OFChannelHandler handler,
                                  final OFMessage msg) {
                logProcessOFMessageDetails(handler, msg, this);
                switch (msg.getType()) {
                    case FEATURES_REQUEST:
                        handler.ofSwitch.processFeaturesRequest(handler.channel, msg);
                        handler.setState(ChannelState.ESTABLISHED);
                        break;
                    case ECHO_REQUEST:
                        handler.ofSwitch.processEchoRequest(handler.channel, msg);
                        break;
                    case ERROR:
                        handler.logErrorClose((OFErrorMsg) msg);
                        break;
                    default:
                        handler.illegalMessageReceived(msg);
                        break;

                }
            }
        },
        ESTABLISHED {
            @Override
            void processOFMessage(final OFChannelHandler handler,
                                  final OFMessage msg) {
                logProcessOFMessageDetails(handler, msg, this);
                // TODO implement
                // TODO add this channel to ofSwitch role service
                switch (msg.getType()) {
                    case STATS_REQUEST:
                        //TODO implement
                        //TODO: use vNetService to build OFPortDesc.
                        handler.ofSwitch.processStatsRequest(handler.channel, msg);
                        break;
                    case SET_CONFIG:
                        //TODO implement
                        handler.ofSwitch.processSetConfigMessage(handler.channel, msg);
                        break;
                    case GET_CONFIG_REQUEST:
                        //TODO implement
                        handler.ofSwitch.processGetConfigRequest(handler.channel, msg);
                        break;
                    case BARRIER_REQUEST:
                        //TODO implement
                        handler.ofSwitch.processBarrierRequest(handler.channel, msg);
                        break;
                    case ECHO_REQUEST:
                        handler.ofSwitch.processEchoRequest(handler.channel, msg);
                        break;
                    case ERROR:
                        handler.logErrorClose((OFErrorMsg) msg);
                        break;
                    default:
                        handler.unhandledMessageReceived(msg);
                        break;
                }
            }
        };

        abstract void processOFMessage(final OFChannelHandler handler, final OFMessage msg);

        private static void logProcessOFMessageDetails(final OFChannelHandler handler,
                                            final OFMessage msg, ChannelState chnState) {
            handler.log.trace("Channel state: {} dpid: {} processOFMessage type: {} nsg: {}",
                              chnState, handler.ofSwitch.dpid(), msg.getType(), msg);
        }
    }

    /**
     * Default constructor.
     *
     * @param ofSwitch openflow switch that owns this channel
     */
    public OFChannelHandler(OFSwitch ofSwitch) {
        super();
        this.ofSwitch = ofSwitch;
        setState(INIT);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.channel = ctx.channel();
        // FIXME move this to channel handler and add channel when OF handshake is done
        ofSwitch.addControllerChannel(channel);
        try {
            ofSwitch.sendOfHello(channel);
            log.trace("Send OF_13 Hello to {}", channel.remoteAddress());
            setState(ChannelState.WAIT_HELLO);
        } catch (Exception ex) {
            log.error("Failed sending OF_13 Hello to {} for {}", channel.remoteAddress(), ex.getMessage());
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        ofSwitch.deleteControllerChannel(channel);
        log.info("Device {} disconnected from controller {}", ofSwitch.dpid(), channel.remoteAddress());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            state.processOFMessage(this, (OFMessage) msg);
        } catch (Exception ex) {
            ctx.fireExceptionCaught(ex);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof ReadTimeoutException) {
            log.error("Connection closed because of ReadTimeoutException {}", cause.getMessage());
        } else if (cause instanceof ClosedChannelException) {
            log.error("ClosedChannelException occurred");
            return;
        } else if (cause instanceof RejectedExecutionException) {
            log.error("Could not process message: queue full");
        } else if (cause instanceof IOException) {
            log.error("IOException occurred");
        } else {
            log.error("Error while processing message from switch {}", cause.getMessage());
        }
        channel.close();
    }

    private void setState(ChannelState state) {
        this.state = state;
        if (state != INIT) {
            log.debug(String.format(MSG_CHANNEL_STATE, channel.remoteAddress(), state.name()));
        }
    }

    private void logErrorClose(OFErrorMsg errorMsg) {
        log.error("{} from switch {} in state {}",
                errorMsg,
                ofSwitch.dpid(),
                state);
        channel.close();
    }

    private void illegalMessageReceived(OFMessage ofMessage) {
        log.warn("Controller should never send message {} to switch {} in current state {}",
                ofMessage.getType(), ofSwitch.dpid(), state);
    }

    private void unhandledMessageReceived(OFMessage ofMessage) {
        log.warn("Unexpected message {} received for switch {} in state {}",
                ofMessage.getType(), ofSwitch.dpid(), state);
    }
}
