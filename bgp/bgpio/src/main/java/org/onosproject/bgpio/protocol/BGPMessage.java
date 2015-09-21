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
import org.onosproject.bgpio.exceptions.BGPParseException;
import org.onosproject.bgpio.types.BGPHeader;

/**
 * Abstraction of an entity providing BGP Messages.
 */
public interface BGPMessage extends Writeable {
    /**
     * Returns BGP Header of BGP Message.
     *
     * @return BGP Header of BGP Message
     */
    BGPHeader getHeader();

    /**
     * Returns version of BGP Message.
     *
     * @return version of BGP Message
     */
    BGPVersion getVersion();

    /**
     * Returns BGP Type of BGP Message.
     *
     * @return BGP Type of BGP Message
     */
    BGPType getType();

    @Override
    void writeTo(ChannelBuffer cb) throws BGPParseException;

    /**
     * Builder interface with get and set functions to build BGP Message.
     */
    interface Builder {
        /**
         * Builds BGP Message.
         *
         * @return BGP Message
         * @throws BGPParseException while building bgp message
         */
        BGPMessage build() throws BGPParseException;

        /**
         * Returns BGP Version of BGP Message.
         *
         * @return BGP Version of BGP Message
         */
        BGPVersion getVersion();

        /**
         * Returns BGP Type of BGP Message.
         *
         * @return BGP Type of BGP Message
         */
        BGPType getType();

        /**
         * Returns BGP Header of BGP Message.
         *
         * @return BGP Header of BGP Message
         */
        BGPHeader getHeader();

        /**
         * Sets BgpHeader and return its builder.
         *
         * @param bgpMsgHeader BGP Message Header
         * @return builder by setting BGP message header
         */
        Builder setHeader(BGPHeader bgpMsgHeader);
    }
}