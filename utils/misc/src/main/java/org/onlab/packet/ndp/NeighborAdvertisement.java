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
package org.onlab.packet.ndp;

import org.onlab.packet.BasePacket;
import org.onlab.packet.Deserializer;
import org.onlab.packet.Ethernet;
import org.onlab.packet.ICMP6;
import org.onlab.packet.IPacket;
import org.onlab.packet.IPv6;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.MacAddress;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onlab.packet.PacketUtils.checkInput;

/**
 * Implements ICMPv6 Neighbor Advertisement packet format (RFC 4861).
 */
public class NeighborAdvertisement extends BasePacket {
    public static final byte HEADER_LENGTH = 20; // bytes

    protected byte routerFlag;
    protected byte solicitedFlag;
    protected byte overrideFlag;
    protected byte[] targetAddress = new byte[Ip6Address.BYTE_LENGTH];

    private final NeighborDiscoveryOptions options =
        new NeighborDiscoveryOptions();

    /**
     * Gets router flag.
     *
     * @return the router flag
     */
    public byte getRouterFlag() {
        return this.routerFlag;
    }

    /**
     * Sets router flag.
     *
     * @param routerFlag the router flag to set
     * @return this
     */
    public NeighborAdvertisement setRouterFlag(final byte routerFlag) {
        this.routerFlag = routerFlag;
        return this;
    }

    /**
     * Gets solicited flag.
     *
     * @return the solicited flag
     */
    public byte getSolicitedFlag() {
        return this.solicitedFlag;
    }

    /**
     * Sets solicited flag.
     *
     * @param solicitedFlag the solicited flag to set
     * @return this
     */
    public NeighborAdvertisement setSolicitedFlag(final byte solicitedFlag) {
        this.solicitedFlag = solicitedFlag;
        return this;
    }

    /**
     * Gets override flag.
     *
     * @return the override flag
     */
    public byte getOverrideFlag() {
        return this.overrideFlag;
    }

    /**
     * Sets override flag.
     *
     * @param overrideFlag the override flag to set
     * @return this
     */
    public NeighborAdvertisement setOverrideFlag(final byte overrideFlag) {
        this.overrideFlag = overrideFlag;
        return this;
    }

    /**
     * Gets target address.
     *
     * @return the target IPv6 address
     */
    public byte[] getTargetAddress() {
        return this.targetAddress;
    }

    /**
     * Sets target address.
     *
     * @param targetAddress the target IPv6 address to set
     * @return this
     */
    public NeighborAdvertisement setTargetAddress(final byte[] targetAddress) {
        this.targetAddress =
            Arrays.copyOfRange(targetAddress, 0, Ip6Address.BYTE_LENGTH);
        return this;
    }

    /**
     * Gets the Neighbor Discovery Protocol packet options.
     *
     * @return the Neighbor Discovery Protocol packet options
     */
    public List<NeighborDiscoveryOptions.Option> getOptions() {
        return this.options.options();
    }

    /**
     * Adds a Neighbor Discovery Protocol packet option.
     *
     * @param type the option type
     * @param data the option data
     * @return this
     */
    public NeighborAdvertisement addOption(final byte type,
                                           final byte[] data) {
        this.options.addOption(type, data);
        return this;
    }

    @Override
    public byte[] serialize() {
        byte[] optionsData = null;
        if (this.options.hasOptions()) {
            optionsData = this.options.serialize();
        }

        int optionsLength = 0;
        if (optionsData != null) {
            optionsLength = optionsData.length;
        }

        final byte[] data = new byte[HEADER_LENGTH + optionsLength];
        final ByteBuffer bb = ByteBuffer.wrap(data);

        bb.putInt((this.routerFlag & 0x1) << 31 |
                  (this.solicitedFlag & 0x1) << 30 |
                  (this.overrideFlag & 0x1) << 29);
        bb.put(this.targetAddress, 0, Ip6Address.BYTE_LENGTH);
        if (optionsData != null) {
            bb.put(optionsData);
        }

        return data;
    }

    @Override
    public IPacket deserialize(byte[] data, int offset, int length) {
        final ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
        int iscratch;

        iscratch = bb.getInt();
        this.routerFlag = (byte) (iscratch >> 31 & 0x1);
        this.solicitedFlag = (byte) (iscratch >> 30 & 0x1);
        this.overrideFlag = (byte) (iscratch >> 29 & 0x1);
        bb.get(this.targetAddress, 0, Ip6Address.BYTE_LENGTH);

        this.options.deserialize(data, bb.position(),
                                 bb.limit() - bb.position());

        return this;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 5807;
        int result = super.hashCode();
        ByteBuffer bb;
        result = prime * result + this.routerFlag;
        result = prime * result + this.solicitedFlag;
        result = prime * result + this.overrideFlag;
        bb = ByteBuffer.wrap(this.targetAddress);
        for (int i = 0; i < this.targetAddress.length / 4; i++) {
            result = prime * result + bb.getInt();
        }
        result = prime * result + this.options.hashCode();
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof NeighborAdvertisement)) {
            return false;
        }
        final NeighborAdvertisement other = (NeighborAdvertisement) obj;
        if (this.routerFlag != other.routerFlag) {
            return false;
        }
        if (this.solicitedFlag != other.solicitedFlag) {
            return false;
        }
        if (this.overrideFlag != other.overrideFlag) {
            return false;
        }
        if (!Arrays.equals(this.targetAddress, other.targetAddress)) {
            return false;
        }
        if (!this.options.equals(other.options)) {
            return false;
        }
        return true;
    }

    /**
     * Deserializer function for neighbor advertisement packets.
     *
     * @return deserializer function
     */
    public static Deserializer<NeighborAdvertisement> deserializer() {
        return (data, offset, length) -> {
            checkInput(data, offset, length, HEADER_LENGTH);

            NeighborAdvertisement neighborAdvertisement = new NeighborAdvertisement();

            ByteBuffer bb = ByteBuffer.wrap(data, offset, length);

            int iscratch;

            iscratch = bb.getInt();
            neighborAdvertisement.routerFlag = (byte) (iscratch >> 31 & 0x1);
            neighborAdvertisement.solicitedFlag = (byte) (iscratch >> 30 & 0x1);
            neighborAdvertisement.overrideFlag = (byte) (iscratch >> 29 & 0x1);
            bb.get(neighborAdvertisement.targetAddress, 0, Ip6Address.BYTE_LENGTH);

            if (bb.limit() - bb.position() > 0) {
                NeighborDiscoveryOptions options = NeighborDiscoveryOptions.deserializer()
                        .deserialize(data, bb.position(), bb.limit() - bb.position());

                for (NeighborDiscoveryOptions.Option option : options.options()) {
                    neighborAdvertisement.addOption(option.type(), option.data());
                }
            }

            return neighborAdvertisement;
        };
    }

    @Override
    public String toString() {
        return toStringHelper(getClass())
                .add("routerFlag", Byte.toString(routerFlag))
                .add("solicitedFlag", Byte.toString(solicitedFlag))
                .add("overrideFlag", Byte.toString(overrideFlag))
                .add("targetAddress", Arrays.toString(targetAddress))
                .toString();
    }

    /**
     * Builds an NDP reply based on a request.
     *
     * @param srcIp   the IP address to use as the reply source
     * @param srcMac  the MAC address to use as the reply source
     * @param request the Neighbor Solicitation request we got
     * @return an Ethernet frame containing the Neighbor Advertisement reply
     */
    public static Ethernet buildNdpAdv(byte[] srcIp,
                                   byte[] srcMac,
                                   Ethernet request) {

        if (srcIp.length != Ip6Address.BYTE_LENGTH ||
                srcMac.length != MacAddress.MAC_ADDRESS_LENGTH) {
            return null;
        }

        if (request.getEtherType() != Ethernet.TYPE_IPV6) {
            return null;
        }

        IPv6 ipv6Request = (IPv6) request.getPayload();

        if (ipv6Request.getNextHeader() != IPv6.PROTOCOL_ICMP6) {
            return null;
        }

        ICMP6 icmpv6 = (ICMP6) ipv6Request.getPayload();

        if (icmpv6.getIcmpType() != ICMP6.NEIGHBOR_SOLICITATION) {
            return null;
        }

        Ethernet eth = new Ethernet();
        eth.setDestinationMACAddress(request.getSourceMAC());
        eth.setSourceMACAddress(srcMac);
        eth.setEtherType(Ethernet.TYPE_IPV6);
        eth.setVlanID(request.getVlanID());

        IPv6 ipv6 = new IPv6();
        ipv6.setSourceAddress(srcIp);
        ipv6.setDestinationAddress(ipv6Request.getSourceAddress());
        ipv6.setHopLimit((byte) 255);

        ICMP6 icmp6 = new ICMP6();
        icmp6.setIcmpType(ICMP6.NEIGHBOR_ADVERTISEMENT);
        icmp6.setIcmpCode((byte) 0);

        NeighborAdvertisement nadv = new NeighborAdvertisement();
        nadv.setTargetAddress(srcIp);
        nadv.setSolicitedFlag((byte) 1);
        nadv.setOverrideFlag((byte) 1);
        nadv.addOption(NeighborDiscoveryOptions.TYPE_TARGET_LL_ADDRESS,
                       srcMac);

        icmp6.setPayload(nadv);
        ipv6.setPayload(icmp6);
        eth.setPayload(ipv6);
        return eth;
    }

}
