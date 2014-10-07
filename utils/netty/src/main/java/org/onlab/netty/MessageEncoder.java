package org.onlab.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Encode InternalMessage out into a byte buffer.
 */
@Sharable
public class MessageEncoder extends MessageToByteEncoder<InternalMessage> {

    // onosiscool in ascii
    public static final byte[] PREAMBLE = "onosiscool".getBytes();
    public static final int HEADER_VERSION = 1;
    public static final int SERIALIZER_VERSION = 1;


    private final PayloadSerializer payloadSerializer;

    public MessageEncoder(PayloadSerializer payloadSerializer) {
        this.payloadSerializer = payloadSerializer;
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

        byte[] payload = payloadSerializer.encode(message);

        // write payload length
        out.writeInt(payload.length);

        // write payloadSerializer version
        out.writeInt(SERIALIZER_VERSION);

        // write payload.
        out.writeBytes(payload);
    }
}
