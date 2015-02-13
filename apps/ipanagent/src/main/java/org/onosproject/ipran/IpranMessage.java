package org.onosproject.ipran;

import org.onlab.netty.Endpoint;



/**
 * Internal message representation with additional attributes
 * for supporting, synchronous request/reply behavior.
 */
public class IpranMessage {
    private long id;
    private Endpoint sender;
    private String type;
    private byte[] payload;

    // Must be created using the Builder.
    private IpranMessage() {}

    public IpranMessage(long id, Endpoint sender, String type, byte[] payload) {
        this.id = id;
        this.sender = sender;
        this.type = type;
        this.payload = payload;
    }

    public long id() {
        return id;
    }

    public String type() {
        return type;
    }

    public Endpoint sender() {
        return sender;
    }

    public byte[] payload() {
        return payload;
    }
    /**
     * Builder for InternalMessages.
     */
    public static final class Builder {
        private IpranMessage message;

        public Builder() {
            message = new IpranMessage();
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

        public IpranMessage build() {
            return message;
        }
    }
}
