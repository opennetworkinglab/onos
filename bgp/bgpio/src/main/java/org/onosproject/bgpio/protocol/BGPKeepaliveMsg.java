/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.bgpio.protocol;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.bgpio.types.BGPHeader;

/**
 * Abstraction of an entity providing BGP Keepalive Message.
 */
public interface BGPKeepaliveMsg extends BGPMessage {

    @Override
    BGPVersion getVersion();

    @Override
    BGPType getType();

    @Override
    void writeTo(ChannelBuffer channelBuffer);

    @Override
    BGPHeader getHeader();

    /**
     * Builder interface with get and set functions to build Keepalive message.
     */
    interface Builder extends BGPMessage.Builder {

        @Override
        BGPKeepaliveMsg build();

        @Override
        BGPVersion getVersion();

        @Override
        BGPType getType();

        @Override
        Builder setHeader(BGPHeader bgpMsgHeader);

        @Override
        BGPHeader getHeader();
    }
}
