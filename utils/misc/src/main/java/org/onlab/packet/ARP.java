/*
 * Copyright 2014-present Open Networking Foundation
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

package org.onlab.packet;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onlab.packet.PacketUtils.checkHeaderLength;
import static org.onlab.packet.PacketUtils.checkInput;

/**
 * Representation of an ARP Packet.
 */
public class ARP extends BasePacket {
    public static final short HW_TYPE_ETHERNET = 0x1;

    public static final short PROTO_TYPE_IP = 0x800;

    public static final short OP_REQUEST = 0x1;
    public static final short OP_REPLY = 0x2;
    public static final short OP_RARP_REQUEST = 0x3;
    public static final short OP_RARP_REPLY = 0x4;

    public static final short INITIAL_HEADER_LENGTH = 8;

    protected short hardwareType;
    protected short protocolType;
    protected byte hardwareAddressLength;
    protected byte protocolAddressLength;
    protected short opCode;
    protected byte[] senderHardwareAddress;
    protected byte[] senderProtocolAddress;
    protected byte[] targetHardwareAddress;
    protected byte[] targetProtocolAddress;

    /**
     * @return the hardwareType
     */
    public short getHardwareType() {
        return this.hardwareType;
    }

    /**
     * @param hwType
     *            the hardwareType to set
     * @return this
     */
    public ARP setHardwareType(final short hwType) {
        this.hardwareType = hwType;
        return this;
    }

    /**
     * @return the protocolType
     */
    public short getProtocolType() {
        return this.protocolType;
    }

    /**
     * @param protoType
     *            the protocolType to set
     * @return this
     */
    public ARP setProtocolType(final short protoType) {
        this.protocolType = protoType;
        return this;
    }

    /**
     * @return the hardwareAddressLength
     */
    public byte getHardwareAddressLength() {
        return this.hardwareAddressLength;
    }

    /**
     * @param hwAddressLength
     *            the hardwareAddressLength to set
     * @return this
     */
    public ARP setHardwareAddressLength(final byte hwAddressLength) {
        this.hardwareAddressLength = hwAddressLength;
        return this;
    }

    /**
     * @return the protocolAddressLength
     */
    public byte getProtocolAddressLength() {
        return this.protocolAddressLength;
    }

    /**
     * @param protoAddressLength
     *            the protocolAddressLength to set
     * @return this
     */
    public ARP setProtocolAddressLength(final byte protoAddressLength) {
        this.protocolAddressLength = protoAddressLength;
        return this;
    }

    /**
     * @return the opCode
     */
    public short getOpCode() {
        return this.opCode;
    }

    /**
     * @param op
     *            the opCode to set
     * @return this
     */
    public ARP setOpCode(final short op) {
        this.opCode = op;
        return this;
    }

    /**
     * @return the senderHardwareAddress
     */
    public byte[] getSenderHardwareAddress() {
        return this.senderHardwareAddress;
    }

    /**
     * @param senderHWAddress
     *            the senderHardwareAddress to set
     * @return this
     */
    public ARP setSenderHardwareAddress(final byte[] senderHWAddress) {
        this.senderHardwareAddress = senderHWAddress;
        return this;
    }

    /**
     * @return the senderProtocolAddress
     */
    public byte[] getSenderProtocolAddress() {
        return this.senderProtocolAddress;
    }

    /**
     * @param senderProtoAddress
     *            the senderProtocolAddress to set
     * @return this
     */
    public ARP setSenderProtocolAddress(final byte[] senderProtoAddress) {
        this.senderProtocolAddress = senderProtoAddress;
        return this;
    }

    public ARP setSenderProtocolAddress(final int address) {
        this.senderProtocolAddress = ByteBuffer.allocate(4).putInt(address)
                .array();
        return this;
    }

    /**
     * @return the targetHardwareAddress
     */
    public byte[] getTargetHardwareAddress() {
        return this.targetHardwareAddress;
    }

    /**
     * @param targetHWAddress
     *            the targetHardwareAddress to set
     * @return this
     */
    public ARP setTargetHardwareAddress(final byte[] targetHWAddress) {
        this.targetHardwareAddress = targetHWAddress;
        return this;
    }

    /**
     * @return the targetProtocolAddress
     */
    public byte[] getTargetProtocolAddress() {
        return this.targetProtocolAddress;
    }

    /**
     * @return True if gratuitous ARP (SPA = TPA), false otherwise
     */
    public boolean isGratuitous() {
        assert this.senderProtocolAddress.length == this.targetProtocolAddress.length;

        int indx = 0;
        while (indx < this.senderProtocolAddress.length) {
            if (this.senderProtocolAddress[indx] != this.targetProtocolAddress[indx]) {
                return false;
            }
            indx++;
        }

        return true;
    }

    /**
     * @param targetProtoAddress
     *            the targetProtocolAddress to set
     * @return this
     */
    public ARP setTargetProtocolAddress(final byte[] targetProtoAddress) {
        this.targetProtocolAddress = targetProtoAddress;
        return this;
    }

    public ARP setTargetProtocolAddress(final int address) {
        this.targetProtocolAddress = ByteBuffer.allocate(4).putInt(address)
                .array();
        return this;
    }

    @Override
    public byte[] serialize() {
        final int length = 8 + 2 * (0xff & this.hardwareAddressLength) + 2
                * (0xff & this.protocolAddressLength);
        final byte[] data = new byte[length];
        final ByteBuffer bb = ByteBuffer.wrap(data);
        bb.putShort(this.hardwareType);
        bb.putShort(this.protocolType);
        bb.put(this.hardwareAddressLength);
        bb.put(this.protocolAddressLength);
        bb.putShort(this.opCode);
        bb.put(this.senderHardwareAddress, 0, 0xff & this.hardwareAddressLength);
        bb.put(this.senderProtocolAddress, 0, 0xff & this.protocolAddressLength);
        bb.put(this.targetHardwareAddress, 0, 0xff & this.hardwareAddressLength);
        bb.put(this.targetProtocolAddress, 0, 0xff & this.protocolAddressLength);
        return data;
    }


    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 13121;
        int result = super.hashCode();
        result = prime * result + this.hardwareAddressLength;
        result = prime * result + this.hardwareType;
        result = prime * result + this.opCode;
        result = prime * result + this.protocolAddressLength;
        result = prime * result + this.protocolType;
        result = prime * result + Arrays.hashCode(this.senderHardwareAddress);
        result = prime * result + Arrays.hashCode(this.senderProtocolAddress);
        result = prime * result + Arrays.hashCode(this.targetHardwareAddress);
        result = prime * result + Arrays.hashCode(this.targetProtocolAddress);
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
        if (!(obj instanceof ARP)) {
            return false;
        }
        final ARP other = (ARP) obj;
        if (this.hardwareAddressLength != other.hardwareAddressLength) {
            return false;
        }
        if (this.hardwareType != other.hardwareType) {
            return false;
        }
        if (this.opCode != other.opCode) {
            return false;
        }
        if (this.protocolAddressLength != other.protocolAddressLength) {
            return false;
        }
        if (this.protocolType != other.protocolType) {
            return false;
        }
        if (!Arrays.equals(this.senderHardwareAddress,
                other.senderHardwareAddress)) {
            return false;
        }
        if (!Arrays.equals(this.senderProtocolAddress,
                other.senderProtocolAddress)) {
            return false;
        }
        if (!Arrays.equals(this.targetHardwareAddress,
                other.targetHardwareAddress)) {
            return false;
        }
        if (!Arrays.equals(this.targetProtocolAddress,
                other.targetProtocolAddress)) {
            return false;
        }
        return true;
    }

    /**
     * Builds an ARP request using the supplied parameters.
     *
     * @param senderMacAddress the mac address of the sender
     * @param senderIpAddress the ip address of the sender
     * @param targetMacAddress the mac address of the target
     * @param targetIpAddress the ip address to resolve
     * @param destinationMacAddress the mac address put in Ethernet header
     * @param vlanId the vlan id
     * @return the Ethernet frame containing the ARP request
     */
    public static Ethernet buildArpRequest(byte[] senderMacAddress,
                                           byte[] senderIpAddress,
                                           byte[] targetMacAddress,
                                           byte[] targetIpAddress,
                                           byte[] destinationMacAddress,
                                           short vlanId) {

        if (senderMacAddress.length != MacAddress.MAC_ADDRESS_LENGTH ||
                senderIpAddress.length != Ip4Address.BYTE_LENGTH ||
                targetIpAddress.length != Ip4Address.BYTE_LENGTH) {
            return null;
        }

        ARP arpRequest = new ARP();
        arpRequest.setHardwareType(ARP.HW_TYPE_ETHERNET)
                .setProtocolType(ARP.PROTO_TYPE_IP)
                .setHardwareAddressLength((byte) Ethernet.DATALAYER_ADDRESS_LENGTH)
                .setProtocolAddressLength((byte) Ip4Address.BYTE_LENGTH)
                .setOpCode(ARP.OP_REQUEST)
                .setSenderHardwareAddress(senderMacAddress)
                .setTargetHardwareAddress(targetMacAddress)
                .setSenderProtocolAddress(senderIpAddress)
                .setTargetProtocolAddress(targetIpAddress);

        Ethernet eth = new Ethernet();
        eth.setDestinationMACAddress(destinationMacAddress)
                .setSourceMACAddress(senderMacAddress)
                .setEtherType(Ethernet.TYPE_ARP)
                .setVlanID(vlanId)
                .setPad(true)
                .setPayload(arpRequest);
        return eth;
    }

    /**
     * Builds an ARP request using the supplied parameters.
     *
     * @param senderMacAddress the mac address of the sender
     * @param senderIpAddress the ip address of the sender
     * @param targetIpAddress the ip address to resolve
     * @param vlanId the vlan id
     * @return the Ethernet frame containing the ARP request
     */
    public static Ethernet buildArpRequest(byte[] senderMacAddress,
                                           byte[] senderIpAddress,
                                           byte[] targetIpAddress,
                                           short vlanId) {
        return buildArpRequest(senderMacAddress, senderIpAddress,
                MacAddress.ZERO.toBytes(), targetIpAddress,
                MacAddress.BROADCAST.toBytes(), vlanId);
    }

    /**
     * Builds an ARP reply based on a request.
     *
     * @param srcIp the IP address to use as the reply source
     * @param srcMac the MAC address to use as the reply source
     * @param request the ARP request we got
     * @return an Ethernet frame containing the ARP reply
     */
    public static Ethernet buildArpReply(Ip4Address srcIp, MacAddress srcMac,
            Ethernet request) {

        Ethernet eth = new Ethernet();
        eth.setDestinationMACAddress(request.getSourceMAC());
        eth.setSourceMACAddress(srcMac);
        eth.setEtherType(Ethernet.TYPE_ARP);
        eth.setQinQVID(request.getQinQVID());
        eth.setQinQTPID(request.getQinQTPID());
        eth.setVlanID(request.getVlanID());

        ARP arp = new ARP();
        arp.setOpCode(ARP.OP_REPLY);
        arp.setProtocolType(ARP.PROTO_TYPE_IP);
        arp.setHardwareType(ARP.HW_TYPE_ETHERNET);

        arp.setProtocolAddressLength((byte) Ip4Address.BYTE_LENGTH);
        arp.setHardwareAddressLength((byte) Ethernet.DATALAYER_ADDRESS_LENGTH);
        arp.setSenderHardwareAddress(srcMac.toBytes());
        arp.setTargetHardwareAddress(request.getSourceMACAddress());

        arp.setTargetProtocolAddress(((ARP) request.getPayload())
                                             .getSenderProtocolAddress());
        arp.setSenderProtocolAddress(srcIp.toInt());

        eth.setPayload(arp);
        return eth;
    }

    /**
     * Deserializer function for ARP packets.
     *
     * @return deserializer function
     */
    public static Deserializer<ARP> deserializer() {
        return (data, offset, length) -> {
            checkInput(data, offset, length, INITIAL_HEADER_LENGTH);

            ARP arp = new ARP();
            final ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
            arp.setHardwareType(bb.getShort());
            arp.setProtocolType(bb.getShort());

            byte hwAddressLength = bb.get();
            arp.setHardwareAddressLength(hwAddressLength);

            byte protocolAddressLength = bb.get();
            arp.setProtocolAddressLength(protocolAddressLength);
            arp.setOpCode(bb.getShort());

            // Check we have enough space for the addresses
            checkHeaderLength(length, INITIAL_HEADER_LENGTH +
                    2 * hwAddressLength +
                    2 * protocolAddressLength);

            arp.senderHardwareAddress = new byte[0xff & hwAddressLength];
            bb.get(arp.senderHardwareAddress, 0, arp.senderHardwareAddress.length);
            arp.senderProtocolAddress = new byte[0xff & protocolAddressLength];
            bb.get(arp.senderProtocolAddress, 0, arp.senderProtocolAddress.length);
            arp.targetHardwareAddress = new byte[0xff & hwAddressLength];
            bb.get(arp.targetHardwareAddress, 0, arp.targetHardwareAddress.length);
            arp.targetProtocolAddress = new byte[0xff & protocolAddressLength];
            bb.get(arp.targetProtocolAddress, 0, arp.targetProtocolAddress.length);

            return arp;
        };
    }

    /**
     * Make an exact copy of the ARP packet.
     *
     * @return copy of the packet
     */
    public ARP duplicate() {
        try {
            byte[] data = serialize();
            return deserializer().deserialize(data, 0, data.length);
        } catch (DeserializationException dex) {
            // If we can't make an object out of the serialized data, its a defect
            throw new IllegalStateException(dex);
        }
    }

    @Override
    public String toString() {
        return toStringHelper(getClass())
                .add("hardwareType", Short.toString(hardwareType))
                .add("protocolType", Short.toString(protocolType))
                .add("hardwareAddressLength", Byte.toString(hardwareAddressLength))
                .add("protocolAddressLength", Byte.toString(protocolAddressLength))
                .add("opCode", Short.toString(opCode))
                .add("senderHardwareAddress", MacAddress.valueOf(senderHardwareAddress))
                .add("senderProtocolAddress", Ip4Address.valueOf(senderProtocolAddress))
                .add("targetHardwareAddress", MacAddress.valueOf(targetHardwareAddress))
                .add("targetProtocolAddress", Ip4Address.valueOf(targetProtocolAddress))
                .toString();
    }
}
