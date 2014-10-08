package org.onlab.netty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Encode InternalMessage out into a byte buffer.
 */
@Sharable
public class MessageEncoder extends MessageToByteEncoder<InternalMessage> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    // onosiscool in ascii
    public static final byte[] PREAMBLE = "onosiscool".getBytes();
    public static final int HEADER_VERSION = 1;
    public static final int SERIALIZER_VERSION = 1;


    private static final KryoSerializer SERIALIZER = new KryoSerializer();

    @Override
    protected void encode(
            ChannelHandlerContext context,
            InternalMessage message,
            ByteBuf out) throws Exception {

        // write version
        out.writeInt(HEADER_VERSION);

        // write preamble
        out.writeBytes(PREAMBLE);

        try {
            SERIALIZER.encode(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
        byte[] payload = SERIALIZER.encode(message);

        // write payload length
        out.writeInt(payload.length);

        // write payloadSerializer version
        out.writeInt(SERIALIZER_VERSION);

        // write payload.
        out.writeBytes(payload);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        log.error("Exception inside channel handling pipeline.", cause);
        context.close();
    }
}
