/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.lisp.ctl;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import org.onosproject.lisp.msg.protocols.LispEncapsulatedControl;
import org.onosproject.lisp.msg.protocols.LispInfoReply;
import org.onosproject.lisp.msg.protocols.LispInfoRequest;
import org.onosproject.lisp.msg.protocols.LispMapNotify;
import org.onosproject.lisp.msg.protocols.LispMapRegister;
import org.onosproject.lisp.msg.protocols.LispMapRequest;
import org.onosproject.lisp.msg.protocols.LispMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Channel handler deals with the xTR connection and dispatches xTR messages
 * to the appropriate locations.
 */
public class LispChannelHandler extends ChannelInboundHandlerAdapter {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        try {
            // first we need to check whether this is an ECM
            if (msg instanceof LispEncapsulatedControl) {
                LispMessage innerMsg =
                        extractLispMessage((LispEncapsulatedControl) msg);
                if (innerMsg instanceof LispMapRequest) {
                    LispMapResolver mapResolver = new LispMapResolver();
                    LispMessage lispMessage =
                            mapResolver.processMapRequest(
                                    (LispEncapsulatedControl) msg);

                    ctx.writeAndFlush(lispMessage);
                }
            }

            if (msg instanceof LispMapRegister) {
                LispMapServer mapServer = LispMapServer.getInstance();
                LispMapNotify mapNotify =
                        mapServer.processMapRegister((LispMapRegister) msg);

                if (mapNotify != null) {
                    ctx.writeAndFlush(mapNotify);
                }
            }

            if (msg instanceof LispInfoRequest) {
                LispMapServer mapServer = LispMapServer.getInstance();
                LispInfoReply infoReply = mapServer.processInfoRequest((LispInfoRequest) msg);

                ctx.writeAndFlush(infoReply);
            }
        } finally {
            // try to remove the received message form the buffer
            ReferenceCountUtil.release(msg);
        }
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
}
