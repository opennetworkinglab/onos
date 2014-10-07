package org.onlab.netty;

import static com.google.common.base.Preconditions.checkState;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.util.Arrays;
import java.util.List;

/**
 * Decoder for inbound messages.
 */
public class MessageDecoder extends ReplayingDecoder<DecoderState> {

    private final NettyMessagingService messagingService;
    private final PayloadSerializer payloadSerializer;

    private int contentLength;

    public MessageDecoder(NettyMessagingService messagingService, PayloadSerializer payloadSerializer) {
        super(DecoderState.READ_HEADER_VERSION);
        this.messagingService = messagingService;
        this.payloadSerializer = payloadSerializer;
    }

    @Override
    protected void decode(
            ChannelHandlerContext context,
            ByteBuf buffer,
            List<Object> out) throws Exception {

        switch(state()) {
        case READ_HEADER_VERSION:
            int headerVersion = buffer.readInt();
            checkState(headerVersion == MessageEncoder.HEADER_VERSION, "Unexpected header version");
            checkpoint(DecoderState.READ_PREAMBLE);
        case READ_PREAMBLE:
            byte[] preamble = new byte[MessageEncoder.PREAMBLE.length];
            buffer.readBytes(preamble);
            checkState(Arrays.equals(MessageEncoder.PREAMBLE, preamble), "Message has wrong preamble");
            checkpoint(DecoderState.READ_CONTENT_LENGTH);
        case READ_CONTENT_LENGTH:
            contentLength = buffer.readInt();
            checkpoint(DecoderState.READ_SERIALIZER_VERSION);
        case READ_SERIALIZER_VERSION:
            int serializerVersion = buffer.readInt();
            checkState(serializerVersion == MessageEncoder.SERIALIZER_VERSION, "Unexpected serializer version");
            checkpoint(DecoderState.READ_CONTENT);
        case READ_CONTENT:
            InternalMessage message = payloadSerializer.decode(buffer.readBytes(contentLength).nioBuffer());
            message.setMessagingService(messagingService);
            out.add(message);
            checkpoint(DecoderState.READ_HEADER_VERSION);
            break;
         default:
            checkState(false, "Must not be here");
        }
    }
}
