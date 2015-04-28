package org.onosproject.ovsdb.lib.jsonrpc;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.TooLongFrameException;

import java.io.IOException;

import org.onosproject.ovsdb.lib.error.InvalidEncodingException;

public class ExceptionHandler extends ChannelHandlerAdapter {

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        if ((cause instanceof InvalidEncodingException)
                || (cause instanceof TooLongFrameException)) {
            ctx.channel().disconnect();
        }
        /*
         * In cases where a connection is quickly established and the closed
         * Catch the IOException and close the channel
         */
        if (cause instanceof IOException) {
            ctx.channel().close();
        }
    }
}
