/*
 * Copyright 2015-present Open Networking Foundation
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
import org.onosproject.bgpio.types.BgpHeader;

/**
 * Abstraction of an entity providing BGP Route Refresh Message.
 */
public interface BgpRouteRefreshMsg extends BgpMessage {

    @Override
    BgpVersion getVersion();

    @Override
    BgpType getType();

    @Override
    void writeTo(ChannelBuffer channelBuffer);

    @Override
    BgpHeader getHeader();

    /**
     * Builder interface with get and set functions to build RouteRefresh message.
     */
    interface Builder extends BgpMessage.Builder {
        @Override
        BgpRouteRefreshMsg build();

        @Override
        Builder setHeader(BgpHeader bgpMsgHeader);

        /**
         * Adds the current AFI-SAFI pair and reserved bytes to be transmitted.
         * BGP Route Refresh message contains one pair of AFI-SAFI values, but routers may transmit more than
         * one AFI-SAFI pair
         *
         * @param afi AFI value to be added to AFI-SAFI pairs
         * @param reserved reserved byte to be added. By default, this should be 0x00
         * @param safi SAFI value to be added to AFI-SAFI pairs
         * @return builder by setting hold time
         */
        Builder addAfiSafiValue(short afi, byte reserved, byte safi);
    }
}
