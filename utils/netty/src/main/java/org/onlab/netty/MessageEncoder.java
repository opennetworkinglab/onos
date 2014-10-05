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
    public static final int HEADER_VERSION = 1;
    public static final int SERIALIZER_VERSION = 1;


    private final Serializer serializer;

    public MessageEncoder(Serializer serializer) {
        this.serializer = serializer;
    }

    @Override
    protected void encode(
            ChannelHandlerContext context,
            InternalMessage message,
            ByteBuf out) throws Exception {

        // write version
        out.writeInt(HEADER_VERSION);

        // write preamble
        out.writeBytes(PREAMBLE);

        byte[] payload = serializer.encode(message);

        // write payload length
        out.writeInt(payload.length);

        // write serializer version
        out.writeInt(SERIALIZER_VERSION);

        // write payload.
        out.writeBytes(payload);
    }
}
