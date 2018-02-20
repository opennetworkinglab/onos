/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.netconf;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * Representation of rpc-reply.
 *
 * @see NetconfRpcParserUtil
 */
@Beta
public class NetconfRpcReply {

    private static final Logger log = getLogger(NetconfRpcReply.class);

    public enum Type {
        OK,
        ERROR,
        RESPONSE
    }

    private final Set<NetconfRpcReply.Type> replies;

    private final String messageId;

    // rpc-reply is ok or (0+ rpc-error and/or 0+ rpc-response)

    private final List<NetconfRpcError> errors;
    private final List<String> responses;

    protected NetconfRpcReply(NetconfRpcReply.Builder builder) {
        this.messageId = checkNotNull(builder.messageId);
        this.replies = ImmutableSet.copyOf(builder.replies);
        this.errors = ImmutableList.copyOf(builder.errors);
        this.responses = ImmutableList.copyOf(builder.responses);
    }

    /**
     * Returns message-id of this message.
     *
     * @return message-id
     */
    //@Nonnull
    public String messageId() {
        return messageId;
    }

    /**
     * Returns true if ok reply.
     *
     * @return true if ok reply.
     */
    public boolean isOk() {
        return replies.contains(Type.OK);
    }

    /**
     * Returns true if reply contains rpc-error.
     *
     * @return true if reply contains rpc-error
     */
    public boolean hasError() {
        return replies.contains(Type.ERROR);
    }

    /**
     * Returns list of rpc-errors in rpc-reply.
     *
     * @return list of rpc-errors in rpc-reply.
     */
    public List<NetconfRpcError> errors() {
        return errors;
    }

    /**
     * Returns list of rpc responses in rpc-reply.
     *
     * @return list of rpc responses in rpc-reply.
     */
    public List<String> responses() {
        return responses;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("messageId", messageId)
                .add("replies", replies)
                .add("errors", errors)
                .add("responses", responses)
                .toString();
    }

    /**
     * Creates builder to build {@link NetconfRpcReply}.
     * @return created builder
     */
    public static NetconfRpcReply.Builder builder() {
        return new Builder();
    }

    /**
     * Builder to build {@link NetconfRpcReply}.
     */
    public static final class Builder {
        private String messageId;
        private Set<NetconfRpcReply.Type> replies = EnumSet.noneOf(NetconfRpcReply.Type.class);
        private List<NetconfRpcError> errors = new ArrayList<>();
        private List<String> responses = new ArrayList<>();

        private Builder() {}

        /**
         * Builder method to set message-id.
         *
         * @param messageId field to set
         * @return builder
         */
        public NetconfRpcReply.Builder withMessageId(String messageId) {
            this.messageId = messageId;
            return this;
        }

        /**
         * Builder method to adding error parameter.
         *
         * @param error field to add
         * @return builder
         */
        public NetconfRpcReply.Builder addError(NetconfRpcError error) {
            this.replies.add(Type.ERROR);
            this.errors.add(error);
            return this;
        }

        /**
         * Builder method for adding response parameter.
         *
         * @param response field to add
         * @return builder
         */
        public NetconfRpcReply.Builder addResponses(String response) {
            this.replies.add(Type.RESPONSE);
            this.responses.add(response);
            return this;
        }

        /**
         * Builds ok reply message.
         * @return ok
         */
        public NetconfRpcReply buildOk() {
            if (!replies.isEmpty()) {
                log.warn("Unexpected item in replies area: {}", replies);
            }
            this.replies.add(Type.OK);
            return build();
        }

        /**
         * Builder method of the builder.
         *
         * @return built class
         */
        public NetconfRpcReply build() {
            if (replies.isEmpty()) {
                log.warn("Empty rpc-reply?");
            }
            return new NetconfRpcReply(this);
        }
    }

}
