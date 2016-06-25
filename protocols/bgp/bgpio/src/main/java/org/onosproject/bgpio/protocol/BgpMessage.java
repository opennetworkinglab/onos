/*
 * Copyright 2015-present Open Networking Laboratory
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
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.types.BgpHeader;

/**
 * Abstraction of an entity providing BGP Messages.
 */
public interface BgpMessage extends Writeable {
    /**
     * Returns BGP Header of BGP Message.
     *
     * @return BGP Header of BGP Message
     */
    BgpHeader getHeader();

    /**
     * Returns version of BGP Message.
     *
     * @return version of BGP Message
     */
    BgpVersion getVersion();

    /**
     * Returns BGP Type of BGP Message.
     *
     * @return BGP Type of BGP Message
     */
    BgpType getType();

    @Override
    void writeTo(ChannelBuffer cb) throws BgpParseException;

    /**
     * Builder interface with get and set functions to build BGP Message.
     */
    interface Builder {
        /**
         * Builds BGP Message.
         *
         * @return BGP Message
         * @throws BgpParseException while building bgp message
         */
        BgpMessage build() throws BgpParseException;

        /**
         * Sets BgpHeader and return its builder.
         *
         * @param bgpMsgHeader BGP Message Header
         * @return builder by setting BGP message header
         */
        Builder setHeader(BgpHeader bgpMsgHeader);
    }
}