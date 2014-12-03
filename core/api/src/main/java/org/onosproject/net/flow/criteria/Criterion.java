/*
 * Copyright 2014 Open Networking Laboratory
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
    // From page 42 of OpenFlow 1.3.x spec
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
        /** Optical channel signal ID (lambda). */
        OCH_SIGID,
        /** Optical channel signal type (fixed or flexible). */
        OCH_SIGTYPE
    }

    /**
     * Returns the type of criterion.
     * @return type of criterion
     */
    public Type type();

    // TODO: Create factory class 'Criteria' that will have various factory
    // to create specific criterions.

}
