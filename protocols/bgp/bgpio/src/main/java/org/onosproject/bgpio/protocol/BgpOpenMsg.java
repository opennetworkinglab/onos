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

import java.util.LinkedList;

import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.types.BgpHeader;
import org.onosproject.bgpio.types.BgpValueType;

/**
 * Abstraction of an entity providing BGP Open Message.
 */
public interface BgpOpenMsg extends BgpMessage {

    @Override
    BgpHeader getHeader();

    @Override
    BgpVersion getVersion();

    @Override
    BgpType getType();

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
    long getAsNumber();

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
    LinkedList<BgpValueType> getCapabilityTlv();

    /**
     * Builder interface with get and set functions to build Open message.
     */
    interface Builder extends BgpMessage.Builder {

        @Override
        BgpOpenMsg build() throws BgpParseException;

        /**
         * Sets hold time in Open Message and return its builder.
         *
         * @param holdtime hold timer value in open message
         * @return builder by setting hold time
         */
        Builder setHoldTime(short holdtime);

        /**
         * Sets AS number in Open Message and return its builder.
         *
         * @param asNumber as number in open message
         * @return builder by setting asNumber
         */
        Builder setAsNumber(short asNumber);

        /**
         * Sets BGP Identifier in Open Message and return its builder.
         *
         * @param bgpId BGP Identifier in open message
         * @return builder by setting BGP Identifier
         */
        Builder setBgpId(int bgpId);

        /**
         * Sets capabilities in Open Message and return its builder.
         *
         * @param capabilityTlv capabilities in open message
         * @return builder by setting capabilities
         */
        Builder setCapabilityTlv(LinkedList<BgpValueType> capabilityTlv);

        /**
         * Sets isLargeAsCapabilityTlvSet and return its builder.
         *
         * @param isLargeAsCapabilitySet
         *           boolean value to know whether large AS capability is set or not
         * @return builder by setting capabilities
         */
        Builder setLargeAsCapabilityTlv(boolean isLargeAsCapabilitySet);

        /**
         * Sets isLsCapabilityTlvSet and return its builder.
         *
         * @param isLsCapabilitySet
         *           boolean value to know whether LS capability is set or not
         * @return builder by setting capabilities
         */
        Builder setLsCapabilityTlv(boolean isLsCapabilitySet);

        /**
         * Sets flow specification capability and return its builder.
         *
         * @param isFlowSpecCapabilitySet boolean value to know whether flow specification capability is set or not
         *
         * @return builder by setting capabilities
         */
        Builder setFlowSpecCapabilityTlv(boolean isFlowSpecCapabilitySet);

        /**
         * Sets VPN flow specification capability and return its builder.
         *
         * @param isVpnFlowSpecCapabilitySet boolean value to know whether flow spec capability is set or not
         *
         * @return builder by setting capabilities
         */
        Builder setVpnFlowSpecCapabilityTlv(boolean isVpnFlowSpecCapabilitySet);

        /**
         * Sets flow specification route distribution policy capability and return its builder.
         *
         * @param isFlowSpecRpdCapabilitySet boolean value to know whether flow spec RPD capability is set or not
         *
         * @return builder by setting capabilities
         */
        Builder setFlowSpecRpdCapabilityTlv(boolean isFlowSpecRpdCapabilitySet);

        /**
         * Sets Evpn capability and return its builder.
         *
         * @param isEvpnCapabilitySet boolean value to know whether evpn
         *            capability is set or not
         *
         * @return builder by setting capabilities
         */
        Builder setEvpnCapabilityTlv(boolean isEvpnCapabilitySet);

        /**
         * Sets if the BGP connection uses IPv4 Unicast connections.
         *
         * @param ipV4UnicastCapabilityTlvSet boolean value to know if IPv4 is used
         *
         * @return Builder by setting capabilities
         */
        Builder setIpV4UnicastCapabilityTlvSet(boolean ipV4UnicastCapabilityTlvSet);

        /**
         * Sets if the BGP connection uses IPv6 Unicast connections.
         *
         * @param ipV6UnicastCapabilityTlvSet boolean value to know if IPv6 is used
         *
         * @return Builder by setting capabilities
         */
        Builder setIpV6UnicastCapabilityTlvSet(boolean ipV6UnicastCapabilityTlvSet);

        @Override
        Builder setHeader(BgpHeader bgpMsgHeader);
    }
}
