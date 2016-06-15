/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.vtnrsc;

import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.net.PortNumber;

/**
 * Abstraction of an entity to provide five tuple information from packet.
 * Five tuple means source ip address, destination ip address, source port number,
 * destination port number and protocol of the packet.
 */
public interface FiveTuple {

    /**
     * Returns the protocol value.
     *
     * @return protocol value IPv4.PROTOCOL_TCP(0x06), IPv4.PROTOCOL_UDP(0x11), IPv4.PROTOCOL_ICMP(0x01)
     */
    byte protocol();

    /**
     * Returns source ip address.
     *
     * @return ipSrc
     */
    IpAddress ipSrc();

    /**
     * Returns destination ip address.
     *
     * @return ipDst
     */
    IpAddress ipDst();

    /**
     * Returns source port.
     *
     * @return portSrc
     */
    PortNumber portSrc();

    /**
     * Returns destination port.
     *
     * @return portDst
     */
    PortNumber portDst();

    /**
     * Returns source mac.
     *
     * @return srcMac
     */
    MacAddress macSrc();

    /**
     * Returns destination mac.
     *
     * @return dstMac
     */
    MacAddress macDst();

    /**
     * Returns the tenant id.
     *
     * @return tenantId
     */
    TenantId tenantId();

    /**
     * Builder class for Five tuple info.
     */
    interface Builder {

        /**
         * Assign the source ip address to this object.
         *
         * @param ipSrc source ip address
         * @return this the builder object
         */
        Builder setIpSrc(IpAddress ipSrc);

        /**
         * Assign the destination ip address to this object.
         *
         * @param ipDst destination ip address
         * @return this the builder object
         */
        Builder setIpDst(IpAddress ipDst);

        /**
         * Assign the source port to this object.
         *
         * @param portSrc source port
         * @return this the builder object
         */
        Builder setPortSrc(PortNumber portSrc);

        /**
         * Assign the destination port to this object.
         *
         * @param portDst destination port
         * @return this the builder object
         */
        Builder setPortDst(PortNumber portDst);

        /**
         * Assign the source mac address to this object.
         *
         * @param macSrc source mac address
         * @return this the builder object
         */
        Builder setMacSrc(MacAddress macSrc);

        /**
         * Assign the destination mac address to this object.
         *
         * @param macDst destination mac address
         * @return this the builder object
         */
        Builder setMacDst(MacAddress macDst);

        /**
         * Assign the protocol to this object.
         *
         * @param protocol packet protocol
         * @return this the builder object
         */
        Builder setProtocol(byte protocol);

        /**
         * Assign the tenant id to this object.
         *
         * @param tenantId tenant id
         * @return this the builder object
         */
        Builder setTenantId(TenantId tenantId);

        /**
         * Builds a FiveTuple object.
         *
         * @return instance of FiveTuple
         */
        FiveTuple build();
    }
}
