/*
 * Copyright 2014 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onlab.netty;

import java.io.IOException;

/**
 * Internal message representation with additional attributes
 * for supporting, synchronous request/reply behavior.
 */
public final class InternalMessage implements Message {

    public static final String REPLY_MESSAGE_TYPE = "NETTY_MESSAGING_REQUEST_REPLY";

    private long id;
    private Endpoint sender;
    private String type;
    private byte[] payload;
    private transient NettyMessagingService messagingService;

    // Must be created using the Builder.
    private InternalMessage() {}

    InternalMessage(long id, Endpoint sender, String type, byte[] payload) {
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
            .withSender(messagingService.localEp())
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
