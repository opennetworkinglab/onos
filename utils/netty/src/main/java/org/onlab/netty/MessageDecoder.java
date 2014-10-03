package org.onlab.netty;

import static com.google.common.base.Preconditions.checkState;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.util.Arrays;
import java.util.List;

// TODO: Implement performance enchancements such as those described in the javadoc for ReplayingDecoder.
public class MessageDecoder extends ReplayingDecoder<InternalMessage> {

    private final NettyMessagingService messagingService;
    private final Serializer serializer;

    public MessageDecoder(NettyMessagingService messagingService, Serializer serializer) {
        this.messagingService = messagingService;
        this.serializer = serializer;
    }

    @Override
    protected void decode(
            ChannelHandlerContext context,
            ByteBuf buffer,
            List<Object> out) throws Exception {

        byte[] preamble = new byte[MessageEncoder.PREAMBLE.length];
        buffer.readBytes(preamble);
        checkState(Arrays.equals(MessageEncoder.PREAMBLE, preamble), "Message has wrong preamble");

        int bodySize = buffer.readInt();
        byte[] body = new byte[bodySize];
        buffer.readBytes(body);

        InternalMessage message = serializer.decode(body);
        message.setMessagingService(messagingService);
        out.add(message);
    }
}
