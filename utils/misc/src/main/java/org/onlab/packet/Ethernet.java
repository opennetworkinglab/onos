/*
 * Copyright 2014-present Open Networking Laboratory
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

import org.onlab.packet.ndp.NeighborAdvertisement;
import org.onlab.packet.ndp.NeighborSolicitation;
import org.onlab.packet.ndp.Redirect;
import org.onlab.packet.ndp.RouterAdvertisement;
import org.onlab.packet.ndp.RouterSolicitation;

import com.google.common.collect.ImmutableMap;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.packet.PacketUtils.checkHeaderLength;
import static org.onlab.packet.PacketUtils.checkInput;

/**
 * Ethernet Packet.
 */
public class Ethernet extends BasePacket {
    private static final String HEXES = "0123456789ABCDEF";
    private static final String HEX_PROTO = "0x%s";

    public static final short TYPE_ARP = EthType.EtherType.ARP.ethType().toShort();
    public static final short TYPE_RARP = EthType.EtherType.RARP.ethType().toShort();
    public static final short TYPE_IPV4 = EthType.EtherType.IPV4.ethType().toShort();
    public static final short TYPE_IPV6 = EthType.EtherType.IPV6.ethType().toShort();
    public static final short TYPE_LLDP = EthType.EtherType.LLDP.ethType().toShort();
    public static final short TYPE_VLAN = EthType.EtherType.VLAN.ethType().toShort();
    public static final short TYPE_QINQ = EthType.EtherType.QINQ.ethType().toShort();
    public static final short TYPE_BSN = EthType.EtherType.BDDP.ethType().toShort();

    public static final short MPLS_UNICAST = EthType.EtherType.MPLS_UNICAST.ethType().toShort();
    public static final short MPLS_MULTICAST = EthType.EtherType.MPLS_MULTICAST.ethType().toShort();


    public static final short VLAN_UNTAGGED = (short) 0xffff;

    public static final short ETHERNET_HEADER_LENGTH = 14; // bytes
    public static final short VLAN_HEADER_LENGTH = 4; // bytes

    public static final short DATALAYER_ADDRESS_LENGTH = 6; // bytes

    private static final Map<Short, Deserializer<? extends IPacket>> ETHERTYPE_DESERIALIZER_MAP;

    static {
        ImmutableMap.Builder<Short, Deserializer<? extends IPacket>> builder =
                ImmutableMap.builder();

       for (EthType.EtherType ethType : EthType.EtherType.values()) {
           if (ethType.deserializer() != null) {
               builder.put(ethType.ethType().toShort(), ethType.deserializer());
           }
       }
       ETHERTYPE_DESERIALIZER_MAP = builder.build();
    }

    protected MacAddress destinationMACAddress;
    protected MacAddress sourceMACAddress;
    protected byte priorityCode;
    protected byte qInQPriorityCode;
    protected short vlanID;
    protected short qinqVID;
    protected short qinqTPID;
    protected short etherType;
    protected boolean pad = false;

    /**
     * By default, set Ethernet to untagged.
     */
    public Ethernet() {
        super();
        this.vlanID = Ethernet.VLAN_UNTAGGED;
        this.qinqVID = Ethernet.VLAN_UNTAGGED;
        this.qinqTPID = TYPE_QINQ;
    }

    /**
     * Gets the destination MAC address.
     *
     * @return the destination MAC as a byte array
     */
    public byte[] getDestinationMACAddress() {
        return this.destinationMACAddress.toBytes();
    }

    /**
     * Gets the destination MAC address.
     *
     * @return the destination MAC
     */
    public MacAddress getDestinationMAC() {
        return this.destinationMACAddress;
    }

    /**
     * Sets the destination MAC address.
     *
     * @param destMac the destination MAC to set
     * @return the Ethernet frame
     */
    public Ethernet setDestinationMACAddress(final MacAddress destMac) {
        this.destinationMACAddress = checkNotNull(destMac);
        return this;
    }

    /**
     * Sets the destination MAC address.
     *
     * @param destMac the destination MAC to set
     * @return the Ethernet frame
     */
    public Ethernet setDestinationMACAddress(final byte[] destMac) {
        this.destinationMACAddress = MacAddress.valueOf(destMac);
        return this;
    }

    /**
     * Sets the destination MAC address.
     *
     * @param destMac the destination MAC to set
     * @return the Ethernet frame
     */
    public Ethernet setDestinationMACAddress(final String destMac) {
        this.destinationMACAddress = MacAddress.valueOf(destMac);
        return this;
    }

    /**
     * Gets the source MAC address.
     *
     * @return the source MACAddress as a byte array
     */
    public byte[] getSourceMACAddress() {
        return this.sourceMACAddress.toBytes();
    }

    /**
     * Gets the source MAC address.
     *
     * @return the source MACAddress
     */
    public MacAddress getSourceMAC() {
        return this.sourceMACAddress;
    }

    /**
     * Sets the source MAC address.
     *
     * @param sourceMac the source MAC to set
     * @return the Ethernet frame
     */
    public Ethernet setSourceMACAddress(final MacAddress sourceMac) {
        this.sourceMACAddress = checkNotNull(sourceMac);
        return this;
    }

    /**
     * Sets the source MAC address.
     *
     * @param sourceMac the source MAC to set
     * @return the Ethernet frame
     */
    public Ethernet setSourceMACAddress(final byte[] sourceMac) {
        this.sourceMACAddress = MacAddress.valueOf(sourceMac);
        return this;
    }

    /**
     * Sets the source MAC address.
     *
     * @param sourceMac the source MAC to set
     * @return the Ethernet frame
     */
    public Ethernet setSourceMACAddress(final String sourceMac) {
        this.sourceMACAddress = MacAddress.valueOf(sourceMac);
        return this;
    }

    /**
     * Gets the priority code.
     *
     * @return the priorityCode
     */
    public byte getPriorityCode() {
        return this.priorityCode;
    }

    /**
     * Sets the priority code.
     *
     * @param priority the priorityCode to set
     * @return the Ethernet frame
     */
    public Ethernet setPriorityCode(final byte priority) {
        this.priorityCode = priority;
        return this;
    }

    /**
     * Gets the QinQ priority code.
     *
     * @return the qInQPriorityCode
     */
    public byte getQinQPriorityCode() {
        return this.qInQPriorityCode;
    }

    /**
     * Sets the QinQ priority code.
     *
     * @param priority the priorityCode to set
     * @return the Ethernet frame
     */
    public Ethernet setQinQPriorityCode(final byte priority) {
        this.qInQPriorityCode = priority;
        return this;
    }

    /**
     * Gets the VLAN ID.
     *
     * @return the vlanID
     */
    public short getVlanID() {
        return this.vlanID;
    }

    /**
     * Sets the VLAN ID.
     *
     * @param vlan the vlanID to set
     * @return the Ethernet frame
     */
    public Ethernet setVlanID(final short vlan) {
        this.vlanID = vlan;
        return this;
    }

    /**
     * Gets the QinQ VLAN ID.
     *
     * @return the QinQ vlanID
     */
    public short getQinQVID() {
        return this.qinqVID;
    }

    /**
     * Sets the QinQ VLAN ID.
     *
     * @param vlan the vlanID to set
     * @return the Ethernet frame
     */
    public Ethernet setQinQVID(final short vlan) {
        this.qinqVID = vlan;
        return this;
    }
    /**
     * Gets the QinQ TPID.
     *
     * @return the QinQ TPID
     */
    public short getQinQTPID() {
        return this.qinqTPID;
    }

    /**
     * Sets the QinQ TPID.
     *
     * @param tpId the TPID to set
     * @return the Ethernet frame
     */
    public Ethernet setQinQTPID(final short tpId) {
        if (tpId != TYPE_VLAN && tpId != TYPE_QINQ) {
           return null;
        }
        this.qinqTPID = tpId;
        return this;
    }
    /**
     * Gets the Ethernet type.
     *
     * @return the etherType
     */
    public short getEtherType() {
        return this.etherType;
    }

    /**
     * Sets the Ethernet type.
     *
     * @param ethType the etherType to set
     * @return the Ethernet frame
     */
    public Ethernet setEtherType(final short ethType) {
        this.etherType = ethType;
        return this;
    }

    /**
     * @return True if the Ethernet frame is broadcast, false otherwise
     */
    public boolean isBroadcast() {
        assert this.destinationMACAddress.length() == 6;
        return this.destinationMACAddress.isBroadcast();
    }

    /**
     * @return True is the Ethernet frame is multicast, False otherwise
     */
    public boolean isMulticast() {
        return this.destinationMACAddress.isMulticast();
    }

    /**
     * Pad this packet to 60 bytes minimum, filling with zeros?
     *
     * @return the pad
     */
    public boolean isPad() {
        return this.pad;
    }

    /**
     * Pad this packet to 60 bytes minimum, filling with zeros?
     *
     * @param pd
     *            the pad to set
     * @return this
     */
    public Ethernet setPad(final boolean pd) {
        this.pad = pd;
        return this;
    }

    @Override
    public byte[] serialize() {
        byte[] payloadData = null;
        if (this.payload != null) {
            this.payload.setParent(this);
            payloadData = this.payload.serialize();
        }
        int length = 14 + (this.vlanID == Ethernet.VLAN_UNTAGGED ? 0 : 4)
                + (this.qinqVID == Ethernet.VLAN_UNTAGGED ? 0 : 4)
                + (payloadData == null ? 0 : payloadData.length);
        if (this.pad && length < 60) {
            length = 60;
        }
        final byte[] data = new byte[length];
        final ByteBuffer bb = ByteBuffer.wrap(data);
        bb.put(this.destinationMACAddress.toBytes());
        bb.put(this.sourceMACAddress.toBytes());
        if (this.qinqVID != Ethernet.VLAN_UNTAGGED) {
            bb.putShort(this.qinqTPID);
            bb.putShort((short) (this.qInQPriorityCode << 13 | this.qinqVID & 0x0fff));
        }
        if (this.vlanID != Ethernet.VLAN_UNTAGGED) {
            bb.putShort(TYPE_VLAN);
            bb.putShort((short) (this.priorityCode << 13 | this.vlanID & 0x0fff));
        }
        bb.putShort(this.etherType);
        if (payloadData != null) {
            bb.put(payloadData);
        }
        if (this.pad) {
            Arrays.fill(data, bb.position(), data.length, (byte) 0x0);
        }
        return data;
    }

    @Override
    public IPacket deserialize(final byte[] data, final int offset,
                               final int length) {
        if (length <= 0) {
            return null;
        }
        final ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
        if (this.destinationMACAddress == null) {
            this.destinationMACAddress = MacAddress.valueOf(new byte[6]);
        }
        final byte[] dstAddr = new byte[MacAddress.MAC_ADDRESS_LENGTH];
        bb.get(dstAddr);
        this.destinationMACAddress = MacAddress.valueOf(dstAddr);

        if (this.sourceMACAddress == null) {
            this.sourceMACAddress = MacAddress.valueOf(new byte[6]);
        }
        final byte[] srcAddr = new byte[MacAddress.MAC_ADDRESS_LENGTH];
        bb.get(srcAddr);
        this.sourceMACAddress = MacAddress.valueOf(srcAddr);

        short ethType = bb.getShort();
        if (ethType == TYPE_QINQ) {
            final short tci = bb.getShort();
            this.qInQPriorityCode = (byte) (tci >> 13 & 0x07);
            this.qinqVID = (short) (tci & 0x0fff);
            this.qinqTPID = TYPE_QINQ;
            ethType = bb.getShort();
        }

        if (ethType == TYPE_VLAN) {
            final short tci = bb.getShort();
            this.priorityCode = (byte) (tci >> 13 & 0x07);
            this.vlanID = (short) (tci & 0x0fff);
            ethType = bb.getShort();

            // there might be one more tag with 1q TPID
            if (ethType == TYPE_VLAN) {
                // packet is double tagged with 1q TPIDs
                // We handle only double tagged packets here and assume that in this case
                // TYPE_QINQ above was not hit
                // We put the values retrieved above with TYPE_VLAN in
                // qInQ fields
                this.qInQPriorityCode = this.priorityCode;
                this.qinqVID = this.vlanID;
                this.qinqTPID = TYPE_VLAN;

                final short innerTci = bb.getShort();
                this.priorityCode = (byte) (innerTci >> 13 & 0x07);
                this.vlanID = (short) (innerTci & 0x0fff);
                ethType = bb.getShort();
            }
        } else {
            this.vlanID = Ethernet.VLAN_UNTAGGED;
        }
        this.etherType = ethType;

        IPacket payload;
        Deserializer<? extends IPacket> deserializer;
        if (Ethernet.ETHERTYPE_DESERIALIZER_MAP.containsKey(ethType)) {
            deserializer = Ethernet.ETHERTYPE_DESERIALIZER_MAP.get(ethType);
        } else {
            deserializer = Data.deserializer();
        }
        try {
            this.payload = deserializer.deserialize(data, bb.position(),
                                                    bb.limit() - bb.position());
            this.payload.setParent(this);
        } catch (DeserializationException e) {
            return this;
        }
        return this;
    }

    /**
     * Checks to see if a string is a valid MAC address.
     *
     * @param macAddress string to test if it is a valid MAC
     * @return True if macAddress is a valid MAC, False otherwise
     */
    public static boolean isMACAddress(final String macAddress) {
        final String[] macBytes = macAddress.split(":");
        if (macBytes.length != 6) {
            return false;
        }
        for (int i = 0; i < 6; ++i) {
            if (Ethernet.HEXES.indexOf(macBytes[i].toUpperCase().charAt(0)) == -1
                    || Ethernet.HEXES.indexOf(macBytes[i].toUpperCase().charAt(
                            1)) == -1) {
                return false;
            }
        }
        return true;
    }

    /**
     * Accepts a MAC address of the form 00:aa:11:bb:22:cc, case does not
     * matter, and returns a corresponding byte[].
     *
     * @param macAddress
     *            The MAC address to convert into a byte array
     * @return The macAddress as a byte array
     */
    public static byte[] toMACAddress(final String macAddress) {
        return MacAddress.valueOf(macAddress).toBytes();
    }

    /**
     * Accepts a MAC address and returns the corresponding long, where the MAC
     * bytes are set on the lower order bytes of the long.
     *
     * @param macAddress MAC address as a byte array
     * @return a long containing the mac address bytes
     */
    public static long toLong(final byte[] macAddress) {
        return MacAddress.valueOf(macAddress).toLong();
    }

    /**
     * Converts a long MAC address to a byte array.
     *
     * @param macAddress MAC address set on the lower order bytes of the long
     * @return the bytes of the mac address
     */
    public static byte[] toByteArray(final long macAddress) {
        return MacAddress.valueOf(macAddress).toBytes();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 7867;
        int result = super.hashCode();
        result = prime * result + this.destinationMACAddress.hashCode();
        result = prime * result + this.etherType;
        result = prime * result + this.qinqVID;
        result = prime * result + this.qInQPriorityCode;
        result = prime * result + this.vlanID;
        result = prime * result + this.priorityCode;
        result = prime * result + (this.pad ? 1231 : 1237);
        result = prime * result + this.sourceMACAddress.hashCode();
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
        if (!(obj instanceof Ethernet)) {
            return false;
        }
        final Ethernet other = (Ethernet) obj;
        if (!this.destinationMACAddress.equals(other.destinationMACAddress)) {
            return false;
        }
        if (this.qInQPriorityCode != other.qInQPriorityCode) {
            return false;
        }
        if (this.qinqVID != other.qinqVID) {
            return false;
        }
        if (this.priorityCode != other.priorityCode) {
            return false;
        }
        if (this.vlanID != other.vlanID) {
            return false;
        }
        if (this.etherType != other.etherType) {
            return false;
        }
        if (this.pad != other.pad) {
            return false;
        }
        if (!this.sourceMACAddress.equals(other.sourceMACAddress)) {
            return false;
        }
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString(java.lang.Object)
     */
    @Override
    public String toString() {

        final StringBuilder sb = new StringBuilder("\n");

        final IPacket pkt = this.getPayload();

        if (pkt instanceof ARP) {
            sb.append("arp");
        } else if (pkt instanceof LLDP) {
            sb.append("lldp");
        } else if (pkt instanceof ICMP) {
            sb.append("icmp");
        } else if (pkt instanceof IPv4) {
            sb.append("ip");
        } else if (pkt instanceof DHCP) {
            sb.append("dhcp");
        } else {
            /*
             * When we don't know the protocol, we print using
             * the well known hex format instead of a decimal
             * value.
             */
            sb.append(String.format(HEX_PROTO,
                                    Integer.toHexString(this.getEtherType() & 0xffff)));
        }

        if (this.getQinQVID() != Ethernet.VLAN_UNTAGGED) {
            sb.append("\ndl_qinqVlan: ");
            sb.append(this.getQinQVID());
            sb.append("\ndl_qinqVlan_pcp: ");
            sb.append(this.getQinQPriorityCode());
        }

        sb.append("\ndl_vlan: ");
        if (this.getVlanID() == Ethernet.VLAN_UNTAGGED) {
            sb.append("untagged");
        } else {
            sb.append(this.getVlanID());
        }
        sb.append("\ndl_vlan_pcp: ");
        sb.append(this.getPriorityCode());
        sb.append("\ndl_src: ");
        sb.append(bytesToHex(this.getSourceMACAddress()));
        sb.append("\ndl_dst: ");
        sb.append(bytesToHex(this.getDestinationMACAddress()));

        if (pkt instanceof ARP) {
            final ARP p = (ARP) pkt;
            sb.append("\nnw_src: ");
            sb.append(IPv4.fromIPv4Address(IPv4.toIPv4Address(p
                    .getSenderProtocolAddress())));
            sb.append("\nnw_dst: ");
            sb.append(IPv4.fromIPv4Address(IPv4.toIPv4Address(p
                    .getTargetProtocolAddress())));
        } else if (pkt instanceof LLDP) {
            sb.append("lldp packet");
        } else if (pkt instanceof ICMP) {
            final ICMP icmp = (ICMP) pkt;
            sb.append("\nicmp_type: ");
            sb.append(icmp.getIcmpType());
            sb.append("\nicmp_code: ");
            sb.append(icmp.getIcmpCode());
        } else if (pkt instanceof IPv4) {
            final IPv4 p = (IPv4) pkt;
            sb.append("\nnw_src: ");
            sb.append(IPv4.fromIPv4Address(p.getSourceAddress()));
            sb.append("\nnw_dst: ");
            sb.append(IPv4.fromIPv4Address(p.getDestinationAddress()));
            sb.append("\nnw_tos: ");
            sb.append(p.getDiffServ());
            sb.append("\nnw_proto: ");
            sb.append(p.getProtocol());

            IPacket payload = pkt.getPayload();
            if (payload != null) {
                if (payload instanceof TCP) {
                    sb.append("\ntp_src: ");
                    sb.append(((TCP) payload).getSourcePort());
                    sb.append("\ntp_dst: ");
                    sb.append(((TCP) payload).getDestinationPort());

                } else if (payload instanceof UDP) {
                    sb.append("\ntp_src: ");
                    sb.append(((UDP) payload).getSourcePort());
                    sb.append("\ntp_dst: ");
                    sb.append(((UDP) payload).getDestinationPort());
                } else if (payload instanceof ICMP) {
                    final ICMP icmp = (ICMP) payload;
                    sb.append("\nicmp_type: ");
                    sb.append(icmp.getIcmpType());
                    sb.append("\nicmp_code: ");
                    sb.append(icmp.getIcmpCode());
                }
            }
        } else if (pkt instanceof IPv6) {
            final IPv6 ipv6 = (IPv6) pkt;
            sb.append("\nipv6_src: ");
            sb.append(Ip6Address.valueOf(ipv6.getSourceAddress()).toString());
            sb.append("\nipv6_dst: ");
            sb.append(Ip6Address.valueOf(ipv6.getDestinationAddress()).toString());
            sb.append("\nipv6_proto: ");
            sb.append(ipv6.getNextHeader());

            IPacket payload = pkt.getPayload();
            if (payload != null && payload instanceof ICMP6) {
                final ICMP6 icmp6 = (ICMP6) payload;
                sb.append("\nicmp6_type: ");
                sb.append(icmp6.getIcmpType());
                sb.append("\nicmp6_code: ");
                sb.append(icmp6.getIcmpCode());

                payload = payload.getPayload();
                if (payload != null) {
                    if (payload instanceof NeighborSolicitation) {
                        final NeighborSolicitation ns = (NeighborSolicitation) payload;
                        sb.append("\nns_target_addr: ");
                        sb.append(Ip6Address.valueOf(ns.getTargetAddress()).toString());
                        ns.getOptions().forEach(option -> {
                            sb.append("\noption_type: ");
                            sb.append(option.type());
                            sb.append("\noption_data: ");
                            sb.append(bytesToHex(option.data()));
                        });
                    } else if (payload instanceof NeighborAdvertisement) {
                        final NeighborAdvertisement na = (NeighborAdvertisement) payload;
                        sb.append("\nna_target_addr: ");
                        sb.append(Ip6Address.valueOf(na.getTargetAddress()).toString());
                        sb.append("\nna_solicited_flag: ");
                        sb.append(na.getSolicitedFlag());
                        sb.append("\nna_router_flag: ");
                        sb.append(na.getRouterFlag());
                        sb.append("\nna_override_flag: ");
                        sb.append(na.getOverrideFlag());
                        na.getOptions().forEach(option -> {
                            sb.append("\noption_type: ");
                            sb.append(option.type());
                            sb.append("\noption_data: ");
                            sb.append(bytesToHex(option.data()));
                        });
                    } else if (payload instanceof RouterSolicitation) {
                        final RouterSolicitation rs = (RouterSolicitation) payload;
                        sb.append("\nrs");
                        rs.getOptions().forEach(option -> {
                            sb.append("\noption_type: ");
                            sb.append(option.type());
                            sb.append("\noption_data: ");
                            sb.append(bytesToHex(option.data()));
                        });
                    } else if (payload instanceof RouterAdvertisement) {
                        final RouterAdvertisement ra = (RouterAdvertisement) payload;
                        sb.append("\nra_hop_limit: ");
                        sb.append(ra.getCurrentHopLimit());
                        sb.append("\nra_mflag: ");
                        sb.append(ra.getMFlag());
                        sb.append("\nra_oflag: ");
                        sb.append(ra.getOFlag());
                        sb.append("\nra_reachable_time: ");
                        sb.append(ra.getReachableTime());
                        sb.append("\nra_retransmit_time: ");
                        sb.append(ra.getRetransmitTimer());
                        sb.append("\nra_router_lifetime: ");
                        sb.append(ra.getRouterLifetime());
                        ra.getOptions().forEach(option -> {
                            sb.append("\noption_type: ");
                            sb.append(option.type());
                            sb.append("\noption_data: ");
                            sb.append(bytesToHex(option.data()));
                        });
                    } else if (payload instanceof Redirect) {
                        final Redirect rd = (Redirect) payload;
                        sb.append("\nrd_target_addr: ");
                        sb.append(Ip6Address.valueOf(rd.getTargetAddress()).toString());
                        rd.getOptions().forEach(option -> {
                            sb.append("\noption_type: ");
                            sb.append(option.type());
                            sb.append("\noption_data: ");
                            sb.append(bytesToHex(option.data()));
                        });
                    }
                }
            }
        } else if (pkt instanceof DHCP) {
            sb.append("\ndhcp packet");
        } else if (pkt instanceof Data) {
            sb.append("\ndata packet");
        } else if (pkt instanceof LLC) {
            sb.append("\nllc packet");
        } else {
            sb.append("\nunknown packet");
        }

        return sb.toString();
    }

    public static String bytesToHex(byte[] in) {
        final StringBuilder builder = new StringBuilder();
        for (byte b : in) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    /**
     * Deserializer function for Ethernet packets.
     *
     * @return deserializer function
     */
    public static Deserializer<Ethernet> deserializer() {
        return (data, offset, length) -> {
            checkInput(data, offset, length, ETHERNET_HEADER_LENGTH);

            byte[] addressBuffer = new byte[DATALAYER_ADDRESS_LENGTH];

            ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
            Ethernet eth = new Ethernet();
            // Read destination MAC address into buffer
            bb.get(addressBuffer);
            eth.setDestinationMACAddress(addressBuffer);

            // Read source MAC address into buffer
            bb.get(addressBuffer);
            eth.setSourceMACAddress(addressBuffer);

            short ethType = bb.getShort();
            if (ethType == TYPE_QINQ) {
                // in this case we excpect 2 VLAN headers
                checkHeaderLength(length, ETHERNET_HEADER_LENGTH + VLAN_HEADER_LENGTH + VLAN_HEADER_LENGTH);
                final short tci = bb.getShort();
                eth.setQinQPriorityCode((byte) (tci >> 13 & 0x07));
                eth.setQinQVID((short) (tci & 0x0fff));
                eth.setQinQTPID(TYPE_QINQ);
                ethType = bb.getShort();
            }
            if (ethType == TYPE_VLAN) {
                checkHeaderLength(length, ETHERNET_HEADER_LENGTH + VLAN_HEADER_LENGTH);
                final short tci = bb.getShort();
                eth.setPriorityCode((byte) (tci >> 13 & 0x07));
                eth.setVlanID((short) (tci & 0x0fff));
                ethType = bb.getShort();

                if (ethType == TYPE_VLAN) {
                    // We handle only double tagged packets here and assume that in this case
                    // TYPE_QINQ above was not hit
                    // We put the values retrieved above with TYPE_VLAN in
                    // qInQ fields
                    checkHeaderLength(length, ETHERNET_HEADER_LENGTH + VLAN_HEADER_LENGTH);
                    eth.setQinQPriorityCode(eth.getPriorityCode());
                    eth.setQinQVID(eth.getVlanID());
                    eth.setQinQTPID(TYPE_VLAN);

                    final short innerTci = bb.getShort();
                    eth.setPriorityCode((byte) (innerTci >> 13 & 0x07));
                    eth.setVlanID((short) (innerTci & 0x0fff));
                    ethType = bb.getShort();
                }
            } else {
                eth.setVlanID(Ethernet.VLAN_UNTAGGED);
            }
            eth.setEtherType(ethType);

            IPacket payload;
            Deserializer<? extends IPacket> deserializer;
            if (Ethernet.ETHERTYPE_DESERIALIZER_MAP.containsKey(ethType)) {
                deserializer = Ethernet.ETHERTYPE_DESERIALIZER_MAP.get(ethType);
            } else {
                deserializer = Data.deserializer();
            }
            payload = deserializer.deserialize(data, bb.position(),
                                               bb.limit() - bb.position());
            payload.setParent(eth);
            eth.setPayload(payload);

            return eth;
        };
    }
}
