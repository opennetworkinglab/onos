package org.onlab.netty;

import java.io.IOException;

/**
 * Internal message representation with additional attributes
 * for supporting, synchronous request/reply behavior.
 */
public final class InternalMessage implements Message {

    public static final String REPLY_MESSAGE_TYPE = "NETTY_MESSAGIG_REQUEST_REPLY";

    private long id;
    private Endpoint sender;
    private String type;
    private byte[] payload;
    private transient NettyMessagingService messagingService;

    // Must be created using the Builder.
    private InternalMessage() {}

    public long id() {
        return id;
    }

    public String type() {
        return type;
    }

    public Endpoint sender() {
        return sender;
    }

    @Override
    public byte[] payload() {
        return payload;
    }

    protected void setMessagingService(NettyMessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @Override
    public void respond(byte[] data) throws IOException {
        Builder builder = new Builder(messagingService);
        InternalMessage message = builder.withId(this.id)
             // FIXME: Sender should be messagingService.localEp.
            .withSender(this.sender)
            .withPayload(data)
            .withType(REPLY_MESSAGE_TYPE)
            .build();
        messagingService.sendAsync(sender, message);
    }


    /**
     * Builder for InternalMessages.
     */
    public static final class Builder {
        private InternalMessage message;

        public Builder(NettyMessagingService messagingService) {
            message = new InternalMessage();
            message.messagingService = messagingService;
        }

        public Builder withId(long id) {
            message.id = id;
            return this;
        }

        public Builder withType(String type) {
            message.type = type;
            return this;
        }

        public Builder withSender(Endpoint sender) {
            message.sender = sender;
            return this;
        }
        public Builder withPayload(byte[] payload) {
            message.payload = payload;
            return this;
        }

        public InternalMessage build() {
            return message;
        }
    }
}
