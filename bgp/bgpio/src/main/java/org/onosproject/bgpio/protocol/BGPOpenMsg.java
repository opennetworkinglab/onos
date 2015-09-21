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

import java.util.LinkedList;

import org.onosproject.bgpio.exceptions.BGPParseException;
import org.onosproject.bgpio.types.BGPHeader;
import org.onosproject.bgpio.types.BGPValueType;

/**
 * Abstraction of an entity providing BGP Open Message.
 */
public interface BGPOpenMsg extends BGPMessage {

    @Override
    BGPHeader getHeader();

    @Override
    BGPVersion getVersion();

    @Override
    BGPType getType();

    /**
     * Returns hold time of Open Message.
     *
     * @return hold time of Open Message
     */
    short getHoldTime();

    /**
     * Returns AS Number of Open Message.
     *
     * @return AS Number of Open Message
     */
    short getAsNumber();

    /**
     * Returns BGP Identifier of Open Message.
     *
     * @return BGP Identifier of Open Message
     */
    int getBgpId();

    /**
     * Returns capabilities of Open Message.
     *
     * @return capabilities of Open Message
     */
    LinkedList<BGPValueType> getCapabilityTlv();

    /**
     * Builder interface with get and set functions to build Open message.
     */
    interface Builder extends BGPMessage.Builder {

        @Override
        BGPOpenMsg build() throws BGPParseException;

        @Override
        BGPHeader getHeader();

        @Override
        BGPVersion getVersion();

        @Override
        BGPType getType();

        /**
         * Returns hold time of Open Message.
         *
         * @return hold time of Open Message
         */
        short getHoldTime();

        /**
         * Sets hold time in Open Message and return its builder.
         *
         * @param holdtime
         *           hold timer value in open message
         * @return builder by setting hold time
         */
        Builder setHoldTime(short holdtime);

        /**
         * Returns as number of Open Message.
         *
         * @return as number of Open Message
         */
        short getAsNumber();

        /**
         * Sets AS number in Open Message and return its builder.
         *
         * @param asNumber
         *           as number in open message
         * @return builder by setting asNumber
         */
        Builder setAsNumber(short asNumber);

        /**
         * Returns BGP Identifier of Open Message.
         *
         * @return BGP Identifier of Open Message
         */
        int getBgpId();

        /**
         * Sets BGP Identifier in Open Message and return its builder.
         *
         * @param bgpId
         *           BGP Identifier in open message
         * @return builder by setting BGP Identifier
         */
        Builder setBgpId(int bgpId);

        /**
         * Returns capabilities of Open Message.
         *
         * @return capabilities of Open Message
         */
        LinkedList<BGPValueType> getCapabilityTlv();

        /**
         * Sets capabilities in Open Message and return its builder.
         *
         * @param capabilityTlv
         *           capabilities in open message
         * @return builder by setting capabilities
         */
        Builder setCapabilityTlv(LinkedList<BGPValueType> capabilityTlv);

        @Override
        Builder setHeader(BGPHeader bgpMsgHeader);
    }
}
