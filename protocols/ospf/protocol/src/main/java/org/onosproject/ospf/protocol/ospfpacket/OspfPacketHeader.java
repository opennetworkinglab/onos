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
package org.onosproject.ospf.protocol.ospfpacket;

import com.google.common.base.MoreObjects;
import org.jboss.netty.buffer.ChannelBuffer;
import org.onlab.packet.Ip4Address;
import org.onosproject.ospf.controller.OspfMessage;
import org.onosproject.ospf.controller.OspfPacketType;
import org.onosproject.ospf.exceptions.OspfParseException;

/**
 * Defines the OSPF Packet Header, fields and access methods.
 * Every OSPF packet starts with a standard 24 byte header.
 * This header contains all the information necessary to determine whether
 * the packet should be accepted for further processing
 */
public class OspfPacketHeader implements OspfMessage {

    /*
        0                   1                   2                   3
        0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |   Version #   |     Type      |         Packet length         |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                          Router ID                            |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                           Area ID                             |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |           Checksum            |             AuType            |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                       Authentication                          |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                       Authentication                          |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */

    private int ospfVer;
    private int ospfType;
    private int ospfPackLength;
    private Ip4Address routerId;
    private Ip4Address areaId;
    private int checkSum;
    private int auType;
    private int authentication;
    private Ip4Address destinationIp;
    private Ip4Address sourceIp;
    private int interfaceIndex;

    /**
     * Gets the source IP.
     *
     * @return source IP address
     */
    public Ip4Address sourceIp() {
        return sourceIp;
    }

    /**
     * Sets the source IP address.
     *
     * @param sourceIp source IP address
     */
    public void setSourceIp(Ip4Address sourceIp) {
        this.sourceIp = sourceIp;
    }

    @Override
    public OspfPacketType ospfMessageType() {
        //default impl
        return null;
    }

    @Override
    public void readFrom(ChannelBuffer channelBuffer) throws OspfParseException {
        //default impl
    }

    @Override
    public byte[] asBytes() {
        //default impl
        return new byte[0];
    }

    /**
     * Gets OSPF version.
     *
     * @return OSPF version
     */
    public int ospfVersion() {
        return ospfVer;
    }

    /**
     * Sets OSPF version.
     *
     * @param ospfVer OSPF version
     */
    public void setOspfVer(int ospfVer) {
        this.ospfVer = ospfVer;
    }

    /**
     * Gets OSPF packet type.
     *
     * @return OSPF packet type
     */
    public int ospfType() {
        return ospfType;
    }

    /**
     * Sets OSPF packet type.
     *
     * @param ospfType packet type
     */
    public void setOspftype(int ospfType) {
        this.ospfType = ospfType;
    }

    /**
     * Gets ospf packet length.
     *
     * @return OSPF packet length
     */
    public int ospfPacLength() {
        return ospfPackLength;
    }

    /**
     * Sets OSPF packet length.
     *
     * @param ospfPacLength packet length
     */
    public void setOspfPacLength(int ospfPacLength) {
        this.ospfPackLength = ospfPacLength;
    }

    /**
     * Gets router id.
     *
     * @return routerId
     */
    public Ip4Address routerId() {
        return routerId;
    }

    /**
     * Sets router id.
     *
     * @param routerId router id
     */
    public void setRouterId(Ip4Address routerId) {
        this.routerId = routerId;
    }

    /**
     * Gets area id.
     *
     * @return areaId area id
     */
    public Ip4Address areaId() {
        return areaId;
    }

    /**
     * Sets area id.
     *
     * @param areaId area id
     */
    public void setAreaId(Ip4Address areaId) {
        this.areaId = areaId;
    }

    /**
     * Gets checksum value.
     *
     * @return checkSum check sum value
     */
    public int checksum() {
        return checkSum;
    }

    /**
     * Sets checksum.
     *
     * @param checkSum check sum value
     */
    public void setChecksum(int checkSum) {
        this.checkSum = checkSum;
    }

    /**
     * Gets auth type.
     *
     * @return authType authentication type
     */
    public int authType() {
        return auType;
    }

    /**
     * Sets auth Type.
     *
     * @param auType authentication type
     */
    public void setAuthType(int auType) {
        this.auType = auType;
    }

    /**
     * Gets authentication.
     *
     * @return authentication
     */
    public int authentication() {
        return authentication;
    }

    /**
     * Sets authentication.
     *
     * @param authentication authentication
     */
    public void setAuthentication(int authentication) {
        this.authentication = authentication;
    }

    /**
     * Gets destination IP.
     *
     * @return destination IP
     */
    public Ip4Address destinationIp() {
        return destinationIp;
    }

    /**
     * Sets destination IP.
     *
     * @param destinationIp destination IP
     */
    public void setDestinationIp(Ip4Address destinationIp) {
        this.destinationIp = destinationIp;
    }

    /**
     * Returns the interface index on which the message received.
     *
     * @return interface index on which the message received
     */
    public int interfaceIndex() {
        return interfaceIndex;
    }

    /**
     * Sets the interface index on which the message received.
     *
     * @param interfaceIndex interface index on which the message received
     */
    public void setInterfaceIndex(int interfaceIndex) {
        this.interfaceIndex = interfaceIndex;
    }

    /**
     * Populates the header from the packetHeader instance.
     *
     * @param ospfPacketHeader packet header instance.
     */
    public void populateHeader(OspfPacketHeader ospfPacketHeader) {
        this.setInterfaceIndex(ospfPacketHeader.interfaceIndex());
        this.setSourceIp(ospfPacketHeader.sourceIp());
        this.setOspfVer(ospfPacketHeader.ospfVersion());
        this.setOspftype(ospfPacketHeader.ospfType());
        this.setOspfPacLength(ospfPacketHeader.ospfPacLength());
        this.setRouterId(ospfPacketHeader.routerId());
        this.setAreaId(ospfPacketHeader.areaId());
        this.setChecksum(ospfPacketHeader.checksum());
        this.setAuthType(ospfPacketHeader.authType());
        this.setAuthentication(ospfPacketHeader.authentication());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("ospfVersion", ospfVer)
                .add("ospfType", ospfType)
                .add("ospfPackLength", ospfPackLength)
                .add("routerId", routerId)
                .add("areaId", areaId)
                .add("checkSum", checkSum)
                .add("auType", auType)
                .add("authentication", authentication)
                .add("destinationIP", destinationIp)
                .toString();
    }
}