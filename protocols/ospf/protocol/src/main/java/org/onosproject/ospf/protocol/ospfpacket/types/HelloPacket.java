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
package org.onosproject.ospf.protocol.ospfpacket.types;

import com.google.common.base.MoreObjects;
import com.google.common.primitives.Bytes;
import org.jboss.netty.buffer.ChannelBuffer;
import org.onlab.packet.Ip4Address;
import org.onosproject.ospf.exceptions.OspfErrorType;
import org.onosproject.ospf.exceptions.OspfParseException;
import org.onosproject.ospf.protocol.ospfpacket.OspfPacketHeader;
import org.onosproject.ospf.controller.OspfPacketType;
import org.onosproject.ospf.protocol.util.OspfUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines an OSPF Hello Message, and the fields and methods to access it.
 * Hello packets are OSPF packet type 1. These packets are sent
 * periodically on all interfaces in order to establish and
 * maintain neighbor relationships.
 */
public class HelloPacket extends OspfPacketHeader {

    /*
              0                   1                   2                   3
            0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
            +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
            |   Version #   |       1       |         Packet length         |
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
            |                        Network Mask                           |
            +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
            |         HelloInterval         |    Options    |    Rtr Pri    |
            +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
            |                     RouterDeadInterval                        |
            +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
            |                      Designated Router                        |
            +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
            |                   Backup Designated Router                    |
            +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
            |                          Neighbor                             |
            +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
            |                              ...                              |

            Hello Message Format
            REFERENCE : RFC 2328
    */

    private static final Logger log = LoggerFactory.getLogger(HelloPacket.class);
    private Ip4Address networkMask;
    private int options;
    private int helloInterval;
    private int routerPriority;
    private int routerDeadInterval;
    private Ip4Address bdr;
    private Ip4Address dr;
    private List<Ip4Address> neighborAddress = new ArrayList<>();

    /**
     * Creates an instance of Hello packet.
     */
    public HelloPacket() {
    }

    /**
     * Creates an instance of Hello packet.
     *
     * @param ospfHeader OSPF header instance.
     */
    public HelloPacket(OspfPacketHeader ospfHeader) {
        populateHeader(ospfHeader);
    }

    /**
     * Gets network mask.
     *
     * @return network mask
     */
    public Ip4Address networkMask() {
        return networkMask;
    }

    /**
     * Sets network mask.
     *
     * @param networkMask network mask
     */
    public void setNetworkMask(Ip4Address networkMask) {
        this.networkMask = networkMask;
    }

    /**
     * Gets BDRs IP address.
     *
     * @return BDRs IP address
     */
    public Ip4Address bdr() {
        return bdr;
    }

    /**
     * Sets BDR IP address.
     *
     * @param bdr BDR IP address
     */
    public void setBdr(Ip4Address bdr) {
        this.bdr = bdr;
    }

    /**
     * Gets DRs IP address.
     *
     * @return DRs IP address
     */
    public Ip4Address dr() {
        return dr;
    }

    /**
     * Sets DRs IP address.
     *
     * @param dr DRs IP address
     */
    public void setDr(Ip4Address dr) {
        this.dr = dr;
    }

    /**
     * Adds neighbor to map.
     *
     * @param neighborID neighbors id
     */
    public void addNeighbor(Ip4Address neighborID) {
        if (!neighborAddress.contains(neighborID)) {
            neighborAddress.add(neighborID);
        }
    }

    /**
     * Checks neighbor is in map or not.
     *
     * @param neighborID neighbors id
     * @return true if neighbor exist else false
     */
    public boolean containsNeighbour(Ip4Address neighborID) {
        return (neighborAddress.contains(neighborID)) ? true : false;
    }

    /**
     * Gets options value.
     *
     * @return options value
     */
    public int options() {
        return options;
    }

    /**
     * Sets options value.
     *
     * @param options options value
     */
    public void setOptions(int options) {
        this.options = options;
    }

    /**
     * Gets router priority.
     *
     * @return routerPriority
     */
    public int routerPriority() {
        return routerPriority;
    }

    /**
     * Sets router priority.
     *
     * @param routerPriority router priority
     */
    public void setRouterPriority(int routerPriority) {
        this.routerPriority = routerPriority;
    }

    /**
     * Gets hello interval.
     *
     * @return hello Interval
     */
    public int helloInterval() {
        return helloInterval;
    }

    /**
     * Sets hello Interval.
     *
     * @param helloInterval hello Interval
     */
    public void setHelloInterval(int helloInterval) {
        this.helloInterval = helloInterval;
    }

    /**
     * Gets router dead interval.
     *
     * @return router dead interval
     */
    public int routerDeadInterval() {
        return routerDeadInterval;
    }

    /**
     * Sets router dead interval.
     *
     * @param routerDeadInterval router dead interval
     */
    public void setRouterDeadInterval(int routerDeadInterval) {
        this.routerDeadInterval = routerDeadInterval;
    }

    @Override
    public OspfPacketType ospfMessageType() {
        return OspfPacketType.HELLO;
    }

    @Override
    public void readFrom(ChannelBuffer channelBuffer) throws OspfParseException {

        try {
            byte[] tempByteArray = new byte[OspfUtil.FOUR_BYTES];
            channelBuffer.readBytes(tempByteArray, 0, OspfUtil.FOUR_BYTES);
            this.setNetworkMask(Ip4Address.valueOf(tempByteArray));
            this.setHelloInterval(channelBuffer.readShort());
            this.setOptions(channelBuffer.readByte());
            this.setRouterPriority(channelBuffer.readByte() & 0xff);
            this.setRouterDeadInterval(channelBuffer.readInt());
            tempByteArray = new byte[OspfUtil.FOUR_BYTES];
            channelBuffer.readBytes(tempByteArray, 0, OspfUtil.FOUR_BYTES);
            this.setDr(Ip4Address.valueOf(tempByteArray));
            tempByteArray = new byte[OspfUtil.FOUR_BYTES];
            channelBuffer.readBytes(tempByteArray, 0, OspfUtil.FOUR_BYTES);
            this.setBdr(Ip4Address.valueOf(tempByteArray));

            while (channelBuffer.readableBytes() > 0) {
                tempByteArray = new byte[OspfUtil.FOUR_BYTES];
                channelBuffer.readBytes(tempByteArray, 0, OspfUtil.FOUR_BYTES);
                this.addNeighbor(Ip4Address.valueOf(tempByteArray));
            }

        } catch (Exception e) {
            log.debug("Error::HelloPacket:: {}", e.getMessage());
            throw new OspfParseException(OspfErrorType.MESSAGE_HEADER_ERROR, OspfErrorType.BAD_MESSAGE_LENGTH);
        }
    }

    @Override
    public byte[] asBytes() {

        byte[] helloMessage = null;
        byte[] helloHeader = getHelloHeaderAsByteArray();
        byte[] helloBody = getHelloBodyAsByteArray();
        helloMessage = Bytes.concat(helloHeader, helloBody);

        log.debug("HelloPacket::asBytes::Hello asBytes:: {}", helloMessage);

        return helloMessage;
    }

    /**
     * Gets hello header as byte array.
     *
     * @return hello header
     */
    public byte[] getHelloHeaderAsByteArray() {
        List<Byte> headerLst = new ArrayList<>();
        try {
            headerLst.add((byte) this.ospfVersion());
            headerLst.add((byte) this.ospfType());
            headerLst.addAll(Bytes.asList(OspfUtil.convertToTwoBytes(this.ospfPacLength())));
            headerLst.addAll(Bytes.asList(this.routerId().toOctets()));
            headerLst.addAll(Bytes.asList(this.areaId().toOctets()));
            headerLst.addAll(Bytes.asList(OspfUtil.convertToTwoBytes(this.checksum())));
            headerLst.addAll(Bytes.asList(OspfUtil.convertToTwoBytes(this.authType())));
            //Authentication is 0 always. Total 8 bytes consist of zero
            byte[] auth = new byte[OspfUtil.EIGHT_BYTES];
            headerLst.addAll(Bytes.asList(auth));
        } catch (Exception e) {
            log.debug("Error::getHelloHeaderAsByteArray {}", e.getMessage());
            return Bytes.toArray(headerLst);
        }

        return Bytes.toArray(headerLst);
    }

    /**
     * Gets hello body as byte array.
     *
     * @return hello body as byte array
     */
    public byte[] getHelloBodyAsByteArray() {
        List<Byte> bodyLst = new ArrayList<>();

        try {
            bodyLst.addAll(Bytes.asList(this.networkMask().toOctets()));
            bodyLst.addAll(Bytes.asList(OspfUtil.convertToTwoBytes(this.helloInterval())));
            bodyLst.add((byte) this.options());
            bodyLst.add((byte) this.routerPriority());
            bodyLst.addAll(Bytes.asList(OspfUtil.convertToFourBytes(this.routerDeadInterval())));
            bodyLst.addAll(Bytes.asList(this.dr().toOctets()));
            bodyLst.addAll(Bytes.asList(this.bdr().toOctets()));
            for (Ip4Address neighbour : neighborAddress) {
                bodyLst.addAll(Bytes.asList(neighbour.toOctets()));
            }

        } catch (Exception e) {
            log.debug("Error::getHelloBodyAsByteArray {}", e.getMessage());
            return Bytes.toArray(bodyLst);
        }

        return Bytes.toArray(bodyLst);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("networkMask", networkMask)
                .add("options", options)
                .add("helloInterval", helloInterval)
                .add("routerPriority", routerPriority)
                .add("routerDeadInterval", routerDeadInterval)
                .add("bdr", bdr)
                .add("dr", dr)
                .toString();
    }
}