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
package org.onosproject.pim.impl;

import org.onlab.packet.Ethernet;
import org.onlab.packet.IPacket;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onlab.packet.PIM;

public class PimPacket {

    // Ethernet header
    private Ethernet ethHeader = new Ethernet();

    // IP header
    private IPv4 ipHeader = new IPv4();

    // PIM Header
    private PIM pimHeader = new PIM();

    // The pim type
    private byte pimType;

    // PIM MAC address
    private MacAddress pimDestinationMac = MacAddress.valueOf("01:00:5E:00:00:0d");

    /**
     * Create a PIM packet for a given PIM type.
     *
     * The resulting packet will have Ethernet and IPv4 headers with all defaults filled in.
     * The final packet will require a PIM header that corresponds to the PIM type set as
     * a payload.
     *
     * Additionally the source MAC and IPv4 address will need to be filled in for the
     * packet to be ready to serialize in most cases.
     *
     * @param type PIM.TYPE_XXXX where XXX is the PIM message type
     */
    public PimPacket(byte type) {
        pimType = type;
        initDefaults();
    }

    /**
     * Fill in defaults for the Ethernet, IPv4 and PIM headers, then associate each
     * of these headers as payload and parent accordingly.
     */
    public void initDefaults() {
        // Prepopulate dst MACAddress and Ethernet Types. The Source MAC needs to be filled in.
        ethHeader.setDestinationMACAddress(pimDestinationMac);
        ethHeader.setEtherType(Ethernet.TYPE_IPV4);

        // Prepopulate the IP Type and Dest address. The Source IP address needs to be filled in.
        ipHeader.setDestinationAddress(PIM.PIM_ADDRESS.getIp4Address().toInt());
        ipHeader.setTtl((byte) 1);
        ipHeader.setProtocol(IPv4.PROTOCOL_PIM);

        // Establish the order between Ethernet and IP headers
        ethHeader.setPayload(ipHeader);
        ipHeader.setParent(ethHeader);

        // Prepopulate the PIM packet
        pimHeader.setPIMType(pimType);

        // Establish the order between IP and PIM headers
        ipHeader.setPayload(pimHeader);
        pimHeader.setParent(ipHeader);
    }

    /**
     * Set the source MAC address.
     *
     * @param src source MAC address
     */
    public void setSrcMacAddr(MacAddress src) {
        ethHeader.setSourceMACAddress(src);
    }

    /**
     * Set the source IPv4 address.
     *
     * @param ipSrcAddress the source IPv4 address
     */
    public void setSrcIpAddress(Ip4Address ipSrcAddress) {
        ipHeader.setSourceAddress(ipSrcAddress.toInt());
    }

    /**
     * Set the PIM payload.
     *
     * @param payload the PIM payload
     */
    public void setPimPayload(IPacket payload) {
        pimHeader.setPayload(payload);
        payload.setParent(pimHeader);
    }

    /**
     * Get the ethernet header.
     *
     * @return the Ethernet header
     */
    public Ethernet getEthernet() {
        return ethHeader;
    }

    /**
     * Get the IPv4 header.
     *
     * @return the IPv4 header
     */
    public IPv4 getIpv4() {
        return ipHeader;
    }
}
