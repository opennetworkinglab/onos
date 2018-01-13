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

package org.onosproject.xmpp.core.ctl.handlers;

import com.fasterxml.aalto.WFCException;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.CombinedChannelDuplexHandler;
import org.dom4j.Element;
import org.onosproject.xmpp.core.XmppDevice;
import org.onosproject.xmpp.core.XmppDeviceFactory;
import org.onosproject.xmpp.core.XmppSession;
import org.onosproject.xmpp.core.ctl.exception.UnsupportedStanzaTypeException;
import org.onosproject.xmpp.core.ctl.exception.XmppValidationException;

import org.onosproject.xmpp.core.stream.XmppStreamClose;
import org.onosproject.xmpp.core.stream.XmppStreamError;
import org.onosproject.xmpp.core.stream.XmppStreamOpen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.Packet;


import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.onlab.util.Tools.groupedThreads;

/**
 * Handles a XMPP channel related events and implements XMPP state machine.
 */
public class XmppChannelHandler extends CombinedChannelDuplexHandler implements XmppSession {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected ExecutorService executorService =
            Executors.newFixedThreadPool(32, groupedThreads("onos/xmpp", "message-stats-%d", logger));

    protected volatile ChannelState state;
    protected Channel channel;

    protected XmppDevice xmppDevice;
    private XmppDeviceFactory deviceFactory;

    public XmppChannelHandler(XmppDeviceFactory deviceFactory) {
        ChannelInboundHandlerAdapter inboundHandlerAdapter = new ChannelInboundHandlerAdapter();
        ChannelOutboundHandlerAdapter outboundHandlerAdapter = new ChannelOutboundHandlerAdapter();
        this.init(inboundHandlerAdapter, outboundHandlerAdapter);
        this.state = ChannelState.IDLE;
        this.deviceFactory = deviceFactory;
    }

    @Override
    public boolean isActive() {
        return channel.isActive();
    }

    @Override
    public InetSocketAddress remoteAddress() {
        return (InetSocketAddress) channel.remoteAddress();
    }

    @Override
    public void closeSession() {
        sendStreamCloseReply();
    }

    @Override
    public boolean sendPacket(Packet xmppPacket) {
        if (channel.isActive()) {
            channel.writeAndFlush(xmppPacket, channel.voidPromise());
            return true;
        } else {
            logger.warn("Dropping messages for device {} because channel is not connected: {}",
                     xmppDevice.getIpAddress(), xmppPacket);
            return false;
        }
    }

    enum XmppEvent {
        XmppStreamClose, XmppStreamOpen, XmppStreamError, IQ, Message, Presence
    }

    enum ChannelState {

        IDLE() {
            @Override
            void processStreamClose(XmppChannelHandler handler, ChannelHandlerContext ctx, XmppStreamClose msg) {
                // ignore
            }

            @Override
            void processStreamError(XmppChannelHandler handler, ChannelHandlerContext ctx, XmppStreamError error) {
                // ignore
            }

            @Override
            void processUpstreamXmppPacket(XmppChannelHandler handler, ChannelHandlerContext ctx,  Object msg) {
                // ignore
                handler.logger.info("XMPP Packet in state IDLE received. Packet ignored..");
            }
        },

        WAIT_STREAM_CLOSE() {
            @Override
            void processDownstreamXmppEvent(XmppChannelHandler handler, ChannelHandlerContext ctx,  Object msg) {
                /**
                 * Block all downstream events during WAIT_STREAM_CLOSE.
                 *
                 * RFC 6120
                 * 4.4 Closing a Stream
                 * "2. Refrain from sending any further data over its outbound stream to the other entity,
                 * but continue to process data received from the other entity (and, if necessary, process such data)."
                 */
            }

            @Override
            void processStreamClose(XmppChannelHandler handler, ChannelHandlerContext ctx, XmppStreamClose msg) {
                handler.xmppDevice.disconnectDevice();
                handler.closeChannel();
                handler.setState(IDLE);
            }

            @Override
            void processStreamOpen(XmppChannelHandler handler, ChannelHandlerContext ctx, XmppStreamOpen streamOpen) {
                // ignore
            }
        },

        STREAM_OPEN() {
            @Override
            void processStreamOpen(XmppChannelHandler handler, ChannelHandlerContext ctx, XmppStreamOpen streamOpen) {
                // ignore
            }

            @Override
            void processDownstreamXmppEvent(XmppChannelHandler handler, ChannelHandlerContext ctx, Object msg) {
                if (msg instanceof XmppStreamClose) {
                    handler.setState(ChannelState.WAIT_STREAM_CLOSE);
                }
                ctx.writeAndFlush(msg);
            }
        };

        void processStreamError(XmppChannelHandler handler, ChannelHandlerContext ctx, XmppStreamError streamError) {
            handler.handleStreamError(streamError);
        }

        void processStreamOpen(XmppChannelHandler handler, ChannelHandlerContext ctx, XmppStreamOpen xmppStreamOpen) {
            handler.xmppDevice = handler.deviceFactory.getXmppDevice(xmppStreamOpen.getFromJid(), handler);
            handler.sendStreamOpenReply(xmppStreamOpen);
            handler.xmppDevice.registerConnectedDevice();
            handler.setState(STREAM_OPEN);
        }

        void processStreamClose(XmppChannelHandler handler, ChannelHandlerContext ctx, XmppStreamClose msg) {
            handler.sendStreamCloseReply();
            handler.xmppDevice.disconnectDevice();
        }

        void processUpstreamXmppPacket(XmppChannelHandler handler, ChannelHandlerContext ctx,  Object msg) {
            handler.executorService.execute(new XmppPacketHandler(handler.xmppDevice, ctx, (Packet) msg));
        }

        void processDownstreamXmppEvent(XmppChannelHandler handler, ChannelHandlerContext ctx,  Object msg) {
            ctx.writeAndFlush(msg);
        }

        void processUpstreamXmppEvent(XmppChannelHandler handler,  ChannelHandlerContext ctx,  Object msg) {
            XmppEvent event = XmppEvent.valueOf(msg.getClass().getSimpleName());
            handler.logger.info("XMPP event {} received in STATE={} for device: {}",
                                event, handler.state, ctx.channel().remoteAddress());
            switch (event) {
                case XmppStreamOpen:
                    handler.state.processStreamOpen(handler, ctx, (XmppStreamOpen) msg);
                    break;
                case XmppStreamClose:
                    handler.state.processStreamClose(handler, ctx, (XmppStreamClose) msg);
                    break;
                case XmppStreamError:
                    handler.state.processStreamError(handler, ctx, (XmppStreamError) msg);
                    break;
                case IQ:
                case Message:
                case Presence:
                    handler.state.processUpstreamXmppPacket(handler, ctx, msg);
                    break;
                default:
                    break;
            }
        }
    }

    private void closeChannel() {
        if (channel != null) {
            channel.close();
        }
    }

    private void handleStreamError(XmppStreamError streamError) {
        // TODO: handle stream errors
    }

    private void sendStreamCloseReply() {
        XmppStreamClose streamClose = new XmppStreamClose();
        channel.writeAndFlush(streamClose);
    }

    private void sendStreamOpenReply(XmppStreamOpen xmppStreamOpen) {
        Element element = xmppStreamOpen.getElement().createCopy();
        element.addAttribute("from", xmppStreamOpen.getToJid().toString());
        element.addAttribute("to", xmppStreamOpen.getFromJid().toString());
        XmppStreamOpen xmppStreamOpenReply = new XmppStreamOpen(element);
        channel.writeAndFlush(xmppStreamOpenReply);
    }

    private void sendStreamError(XmppStreamError.Condition condition) {
        XmppStreamError error = new XmppStreamError(condition);
        channel.writeAndFlush(error);
    }

    private void handleChannelException(Throwable cause) {
        XmppStreamError.Condition condition = getStreamErrorCondition(cause.getCause());
        sendStreamError(condition);
        sendStreamCloseReply();
    }

    protected void setState(ChannelState state) {
        logger.info("Transition from state {} to {}", this.state, state);
        this.state = state;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        channel = ctx.channel();
        logger.info("New device connection from {}",
                 channel.remoteAddress());
        this.state = ChannelState.IDLE;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        this.state.processUpstreamXmppEvent(this, ctx, msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        logger.info("Exception caught: {}", cause.getMessage());
        handleChannelException(cause.getCause());
    }

    private XmppStreamError.Condition getStreamErrorCondition(Throwable cause) {
        //TODO: add error handle mechanisms for each cases
        if (cause instanceof UnsupportedStanzaTypeException) {
            return XmppStreamError.Condition.unsupported_stanza_type;
        } else if (cause instanceof WFCException) {
            return XmppStreamError.Condition.bad_format;
        } else if (cause instanceof XmppValidationException) {
            return XmppStreamError.Condition.bad_format;
        } else {
            return XmppStreamError.Condition.internal_server_error;
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        this.state.processDownstreamXmppEvent(this, ctx,  msg);
        logger.info("Writing packet... Current State " + this.state.toString());
    }

    /**
     * XMPP message handler.
     */
    private static final class XmppPacketHandler implements Runnable {

        protected final ChannelHandlerContext ctx;
        protected final Packet packet;
        protected final XmppDevice xmppDevice;

        public XmppPacketHandler(XmppDevice xmppDevice, ChannelHandlerContext ctx, Packet packet) {
            this.ctx = ctx;
            this.packet = packet;
            this.xmppDevice = xmppDevice;
        }

        @Override
        public void run() {
            xmppDevice.handlePacket(packet);
        }
    }
}
