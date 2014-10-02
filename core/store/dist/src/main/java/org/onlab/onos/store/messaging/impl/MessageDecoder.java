package org.onlab.onos.store.messaging.impl;

import java.util.Arrays;
import java.util.List;

import static com.google.common.base.Preconditions.checkState;

import org.onlab.onos.store.messaging.Endpoint;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

/**
 * Decode bytes into a InrenalMessage.
 */
public class MessageDecoder extends ByteToMessageDecoder {

    private final NettyMessagingService messagingService;
    private final Serializer serializer;

    public MessageDecoder(NettyMessagingService messagingService, Serializer serializer) {
        this.messagingService = messagingService;
        this.serializer = serializer;
    }

    @Override
    protected void decode(ChannelHandlerContext context, ByteBuf in,
            List<Object> messages) throws Exception {

        byte[] preamble = in.readBytes(MessageEncoder.PREAMBLE.length).array();
        checkState(Arrays.equals(MessageEncoder.PREAMBLE, preamble), "Message has wrong preamble");

        // read message Id.
        long id = in.readLong();

        // read message type; first read size and then bytes.
        String type = new String(in.readBytes(in.readInt()).array());

        // read sender host name; first read size and then bytes.
        String host = new String(in.readBytes(in.readInt()).array());

        // read sender port.
        int port = in.readInt();

        Endpoint sender = new Endpoint(host, port);

        // read message payload; first read size and then bytes.
        Object payload = serializer.decode(in.readBytes(in.readInt()).array());

        InternalMessage message = new InternalMessage.Builder(messagingService)
                .withId(id)
                .withSender(sender)
                .withType(type)
                .withPayload(payload)
                .build();

        messages.add(message);
    }
}
