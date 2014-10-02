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
    protected void encode(ChannelHandlerContext context, InternalMessage message,
            ByteBuf out) throws Exception {

        // write preamble
        out.writeBytes(PREAMBLE);

        // write id
        out.writeLong(message.id());

        // write type length
        out.writeInt(message.type().length());

        // write type
        out.writeBytes(message.type().getBytes());

        // write sender host name size
        out.writeInt(message.sender().host().length());

        // write sender host name.
        out.writeBytes(message.sender().host().getBytes());

        // write port
        out.writeInt(message.sender().port());

        try {
            serializer.encode(message.payload());
        } catch (Exception e) {
            e.printStackTrace();
        }

        byte[] payload = serializer.encode(message.payload());

        // write payload length.
        out.writeInt(payload.length);

        // write payload bytes
        out.writeBytes(payload);
    }
}
