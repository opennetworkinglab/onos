package org.onlab.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Encode InternalMessage out into a byte buffer.
 */
public class MessageEncoder extends MessageToByteEncoder<InternalMessage> {

    // onosiscool in ascii
    public static final byte[] PREAMBLE = "onosiscool".getBytes();

    private final Serializer serializer;

    public MessageEncoder(Serializer serializer) {
        this.serializer = serializer;
    }

    @Override
    protected void encode(
            ChannelHandlerContext context,
            InternalMessage message,
            ByteBuf out) throws Exception {

        // write preamble
        out.writeBytes(PREAMBLE);

        byte[] payload = serializer.encode(message);

        // write payload length
        out.writeInt(payload.length);

        // write payload.
        out.writeBytes(payload);
    }
}
