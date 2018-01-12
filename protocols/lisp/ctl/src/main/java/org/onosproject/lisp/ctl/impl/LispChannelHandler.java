/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.lisp.ctl.impl;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import org.onlab.packet.IpAddress;
import org.onosproject.lisp.ctl.LispRouter;
import org.onosproject.lisp.ctl.LispRouterFactory;
import org.onosproject.lisp.msg.protocols.LispEncapsulatedControl;
import org.onosproject.lisp.msg.protocols.LispInfoReply;
import org.onosproject.lisp.msg.protocols.LispInfoRequest;
import org.onosproject.lisp.msg.protocols.LispMapNotify;
import org.onosproject.lisp.msg.protocols.LispMapRegister;
import org.onosproject.lisp.msg.protocols.LispMapRequest;
import org.onosproject.lisp.msg.protocols.LispMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.onlab.packet.IpAddress.valueOf;
import static org.onlab.util.Tools.groupedThreads;

/**
 * Channel handler deals with the xTR connection and dispatches xTR messages
 * to the appropriate locations.
 */
public class LispChannelHandler extends ChannelInboundHandlerAdapter {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final LispRouterFactory routerFactory = LispRouterFactory.getInstance();

    ExecutorService executorMessages =
            Executors.newFixedThreadPool(32, groupedThreads("onos/lisp", "message-stats-%d", log));

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        executorMessages.execute(new LispMessageHandler(ctx, (LispMessage) msg));
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt)
            throws Exception {

        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                log.info("READER_IDLE read timeout");
                ctx.disconnect();
            } else if (event.state() == IdleState.WRITER_IDLE) {
                log.info("WRITER_IDLE write timeout");
                ctx.disconnect();
            } else if (event.state() == IdleState.ALL_IDLE) {
                log.info("ALL_IDLE total timeout");
                ctx.disconnect();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        log.warn(cause.getMessage());

        //TODO: add error handle mechanisms for each cases
    }

    /**
     * Extracts LISP message from encapsulated control message.
     *
     * @param ecm Encapsulated Control Message
     * @return extracted LISP message
     */
    private LispMessage extractLispMessage(LispEncapsulatedControl ecm) {
        LispMessage message = ecm.getControlMessage();
        message.configSender(ecm.getSender());
        return message;
    }

    /**
     * LISP message handler.
     */
    private final class LispMessageHandler implements Runnable {

        protected final ChannelHandlerContext ctx;
        protected final LispMessage msg;
        private LispRouter router;

        public LispMessageHandler(ChannelHandlerContext ctx, LispMessage msg) {
            this.ctx = ctx;
            this.msg = msg;
        }

        @Override
        public void run() {

            try {
                // process map-request message that is encapsulated in ECM
                if (msg instanceof LispEncapsulatedControl) {
                    LispMessage innerMsg = extractLispMessage((LispEncapsulatedControl) msg);
                    if (innerMsg instanceof LispMapRequest) {

                        IpAddress xtrAddress = valueOf(innerMsg.getSender().getAddress());

                        router = routerFactory.getRouterInstance(xtrAddress);

                        if (!router.isConnected()) {
                            router.setChannel(ctx.channel());
                            router.connectRouter();
                        }

                        router.setEidRecords(((LispMapRequest) innerMsg).getEids());
                        router.setSubscribed(true);
                        router.handleMessage(innerMsg);

                        LispMapResolver mapResolver = LispMapResolver.getInstance();
                        List<LispMessage> lispMessages =
                                mapResolver.processMapRequest(msg);

                        if (lispMessages != null) {
                            lispMessages.forEach(ctx::writeAndFlush);
                        }
                    }
                }

                // process map-register message
                if (msg instanceof LispMapRegister) {

                    LispMapRegister register = (LispMapRegister) msg;
                    IpAddress xtrAddress = valueOf(register.getSender().getAddress());
                    router = routerFactory.getRouterInstance(xtrAddress);

                    if (!router.isConnected()) {
                        router.setChannel(ctx.channel());
                        router.connectRouter();
                    }

                    router.handleMessage(register);

                    LispMapServer mapServer = LispMapServer.getInstance();
                    LispMapNotify mapNotify = mapServer.processMapRegister(register);

                    if (mapNotify != null) {
                        ctx.writeAndFlush(mapNotify);
                    }
                }

                // process info-request message
                if (msg instanceof LispInfoRequest) {
                    LispMapServer mapServer = LispMapServer.getInstance();
                    LispInfoReply infoReply = mapServer.processInfoRequest(msg);

                    if (infoReply != null) {
                        ctx.writeAndFlush(infoReply);
                    }
                }
            } finally {
                // try to remove the received message form the buffer
                ReferenceCountUtil.release(msg);
            }
        }
    }
}
