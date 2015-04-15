/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.net.flow.criteria;


/**
 * Representation of a single header field selection.
 */
public interface Criterion {

    /**
     * Types of fields to which the selection criterion may apply.
     */
    // From page 75 of OpenFlow 1.5.0 spec
    public enum Type {
        /** Switch input port. */
        IN_PORT,
        /** Switch physical input port. */
        IN_PHY_PORT,
        /** Metadata passed between tables. */
        METADATA,
        /** Ethernet destination address. */
        ETH_DST,
        /** Ethernet source address. */
        ETH_SRC,
        /** Ethernet frame type. */
        ETH_TYPE,
        /** VLAN id. */
        VLAN_VID,
        /** VLAN priority. */
        VLAN_PCP,
        /** IP DSCP (6 bits in ToS field). */
        IP_DSCP,
        /** IP ECN (2 bits in ToS field). */
        IP_ECN,
        /** IP protocol. */
        IP_PROTO,
        /** IPv4 source address. */
        IPV4_SRC,
        /** IPv4 destination address. */
        IPV4_DST,
        /** TCP source port. */
        TCP_SRC,
        /** TCP destination port. */
        TCP_DST,
        /** UDP source port. */
        UDP_SRC,
        /** UDP destination port. */
        UDP_DST,
        /** SCTP source port. */
        SCTP_SRC,
        /** SCTP destination port. */
        SCTP_DST,
        /** ICMP type. */
        ICMPV4_TYPE,
        /** ICMP code. */
        ICMPV4_CODE,
        /** ARP opcode. */
        ARP_OP,
        /** ARP source IPv4 address. */
        ARP_SPA,
        /** ARP target IPv4 address. */
        ARP_TPA,
        /** ARP source hardware address. */
        ARP_SHA,
        /** ARP target hardware address. */
        ARP_THA,
        /** IPv6 source address. */
        IPV6_SRC,
        /** IPv6 destination address. */
        IPV6_DST,
        /** IPv6 Flow Label. */
        IPV6_FLABEL,
        /** ICMPv6 type. */
        ICMPV6_TYPE,
        /** ICMPv6 code. */
        ICMPV6_CODE,
        /** Target address for ND. */
        IPV6_ND_TARGET,
        /** Source link-layer for ND. */
        IPV6_ND_SLL,
        /** Target link-layer for ND. */
        IPV6_ND_TLL,
        /** MPLS label. */
        MPLS_LABEL,
        /** MPLS TC. */
        MPLS_TC,
        /** MPLS BoS bit. */
        MPLS_BOS,
        /** PBB I-SID. */
        PBB_ISID,
        /** Logical Port Metadata. */
        TUNNEL_ID,
        /** IPv6 Extension Header pseudo-field. */
        IPV6_EXTHDR,
        /** Unassigned value: 40. */
        UNASSIGNED_40,
        /** PBB UCA header field. */
        PBB_UCA,
        /** TCP flags. */
        TCP_FLAGS,
        /** Output port from action set metadata. */
        ACTSET_OUTPUT,
        /** Packet type value. */
        PACKET_TYPE,

        //
        // NOTE: Everything below is defined elsewhere: ONOS-specific,
        // extensions, etc.
        //
        /** Optical channel signal ID (lambda). */
        OCH_SIGID,
        /** Optical channel signal type (fixed or flexible). */
        OCH_SIGTYPE,

        /**
         * An empty criterion.
         */
        DUMMY
    }

    /**
     * Returns the type of criterion.
     *
     * @return type of criterion
     */
    public Type type();

    /**
     * Bit definitions for IPv6 Extension Header pseudo-field.
     * From page 79 of OpenFlow 1.5.0 spec.
     */
    public enum IPv6ExthdrFlags {
        /** "No next header" encountered. */
        NONEXT(1 << 0),
        /** Encrypted Sec Payload header present. */
        ESP(1 << 1),
        /** Authentication header present. */
        AUTH(1 << 2),
        /** 1 or 2 dest headers present. */
        DEST(1 << 3),
        /** Fragment header present. */
        FRAG(1 << 4),
        /** Router header present. */
        ROUTER(1 << 5),
        /** Hop-by-hop header present. */
        HOP(1 << 6),
        /** Unexpected repeats encountered. */
        UNREP(1 << 7),
        /** Unexpected sequencing encountered. */
        UNSEQ(1 << 8);

        private int value;

        IPv6ExthdrFlags(int value) {
            this.value = value;
        }

        /**
         * Gets the value as an integer.
         *
         * @return the value as an integer
         */
        public int getValue() {
            return this.value;
        }
    }
}
