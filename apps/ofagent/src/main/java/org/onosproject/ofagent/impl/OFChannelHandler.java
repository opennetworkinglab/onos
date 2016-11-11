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

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.ReadTimeoutException;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceDirectory;
import org.onosproject.incubator.net.virtual.VirtualNetworkService;
import org.onosproject.ofagent.api.OFSwitch;
import org.projectfloodlight.openflow.protocol.OFErrorMsg;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

/**
 * Implementation of OpenFlow channel handler.
 * It processes OpenFlow message according to the channel state.
 */
public final class OFChannelHandler extends ChannelDuplexHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final OFSwitch ofSwitch;

    private ChannelHandlerContext ctx;
    private ChannelState state;
    protected static final OFFactory FACTORY = OFFactories.getFactory(OFVersion.OF_13);
    protected VirtualNetworkService vNetService;

    enum ChannelState {

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

                switch (msg.getType()) {
                    case FEATURES_REQUEST:
                        handler.ofSwitch.processFeaturesRequest(handler.ctx.channel(), msg);
                        handler.setState(ChannelState.ESTABLISHED);
                        break;
                    case ECHO_REQUEST:
                        handler.ofSwitch.processEchoRequest(handler.ctx.channel(), msg);
                        break;
                    case ERROR:
                        handler.logErrorClose(handler.ctx, (OFErrorMsg) msg);
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
                // TODO implement
                // TODO add this channel to ofSwitch role service
                switch (msg.getType()) {
                    case STATS_REQUEST:
                        //TODO implement
                        //TODO: use vNetService to build OFPortDesc.
                        break;
                    case SET_CONFIG:
                        //TODO implement
                        break;
                    case GET_CONFIG_REQUEST:
                        //TODO implement
                        break;
                    case BARRIER_REQUEST:
                        //TODO implement
                        break;
                    case ECHO_REQUEST:
                        handler.ofSwitch.processEchoRequest(handler.ctx.channel(), msg);
                        break;
                    case ERROR:
                        handler.logErrorClose(handler.ctx, (OFErrorMsg) msg);
                        break;
                    default:
                        handler.unhandledMessageReceived(msg);
                        break;
                }
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
        super();
        this.ofSwitch = ofSwitch;

        setState(ChannelState.INIT);

        ServiceDirectory services = new DefaultServiceDirectory();
        vNetService = services.get(VirtualNetworkService.class);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
        log.debug("Channel Active. Send OF_13 Hello to {}", ctx.channel().remoteAddress());

        try {
            ofSwitch.sendOfHello(ctx.channel());
            setState(ChannelState.WAIT_HELLO);
        } catch (Throwable cause) {
            log.error("Exception occured because of{}", cause.getMessage());
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {

        try {
            if (msg instanceof List) {
                ((List) msg).forEach(ofm -> {
                    state.processOFMessage(this, (OFMessage) ofm);
                });
            } else {
                state.processOFMessage(this, (OFMessage) msg);
            }
        } catch (Throwable cause) {
            log.error("Exception occured {}", cause.getMessage());
        }

    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx)
            throws Exception {
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof ReadTimeoutException) {
            log.error("Connection closed because of ReadTimeoutException {}", cause.getMessage());
        } else if (cause instanceof ClosedChannelException) {
            log.error("ClosedChannelException occured");
            return;
        } else if (cause instanceof RejectedExecutionException) {
            log.error("Could not process message: queue full");
        } else if (cause instanceof IOException) {
            log.error("IOException occured");
        } else {
            log.error("Error while processing message from switch {}", cause.getMessage());
        }
        ctx.close();
    }

    private void setState(ChannelState state) {
        this.state = state;
    }

    private void logErrorClose(ChannelHandlerContext ctx, OFErrorMsg errorMsg) {
        log.error("{} from switch {} in state {}",
                errorMsg,
                ofSwitch.device().id().toString(),
                state);

        log.error("Disconnecting...");
        ctx.close();
    }

    private void illegalMessageReceived(OFMessage ofMessage) {
        log.warn("Controller should never send this message {} in current state {}",
                ofMessage.getType().toString(),
                state);
    }

    private void unhandledMessageReceived(OFMessage ofMessage) {
        log.warn("Unhandled message {} received in state {}. Ignored",
                ofMessage.getType().toString(),
                state);
    }
}
