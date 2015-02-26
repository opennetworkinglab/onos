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
package org.onosproject.net.flow;

import java.util.Set;

import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.criteria.Criterion;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.MacAddress;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;

/**
 * Abstraction of a slice of network traffic.
 */
public interface TrafficSelector {

    /**
     * Returns selection criteria as an ordered list.
     *
     * @return list of criteria
     */
    Set<Criterion> criteria();

    /**
     * Returns the selection criterion for a particular type, if it exists in
     * this traffic selector.
     *
     * @param type criterion type to look up
     * @return the criterion of the specified type if one exists, otherwise null
     */
    Criterion getCriterion(Criterion.Type type);

    /**
     * Builder of traffic selector entities.
     */
    public interface Builder {

        /**
         * Adds a traffic selection criterion. If a same type criterion has
         * already been added, it will be replaced by this one.
         *
         * @param criterion new criterion
         * @return self
         */
        Builder add(Criterion criterion);

        /**
         * Matches an inport.
         *
         * @param port the inport
         * @return a selection builder
         */
        public Builder matchInPort(PortNumber port);

        /**
         * Matches a physical inport.
         *
         * @param port the physical inport
         * @return a selection builder
         */
        public Builder matchInPhyPort(PortNumber port);

        /**
         * Matches a metadata.
         *
         * @param metadata the metadata
         * @return a selection builder
         */
        public Builder matchMetadata(long metadata);

        /**
         * Matches a l2 dst address.
         *
         * @param addr a l2 address
         * @return a selection builder
         */
        public Builder matchEthDst(MacAddress addr);

        /**
         * Matches a l2 src address.
         *
         * @param addr a l2 address
         * @return a selection builder
         */
        public Builder matchEthSrc(MacAddress addr);

        /**
         * Matches the ethernet type.
         *
         * @param ethType an ethernet type
         * @return a selection builder
         */
        public Builder matchEthType(short ethType);

        /**
         * Matches the vlan id.
         *
         * @param vlanId a vlan id
         * @return a selection builder
         */
        public Builder matchVlanId(VlanId vlanId);

        /**
         * Matches a vlan priority.
         *
         * @param vlanPcp a vlan priority
         * @return a selection builder
         */
        public Builder matchVlanPcp(byte vlanPcp);

        /**
         * Matches an IP DSCP (6 bits in ToS field).
         *
         * @param ipDscp an IP DSCP value
         * @return a selection builder
         */
        public Builder matchIPDscp(byte ipDscp);

        /**
         * Matches an IP ECN (2 bits in ToS field).
         *
         * @param ipEcn an IP ECN value
         * @return a selection builder
         */
        public Builder matchIPEcn(byte ipEcn);

        /**
         * Matches the l3 protocol.
         *
         * @param proto a l3 protocol
         * @return a selection builder
         */
        public Builder matchIPProtocol(byte proto);

        /**
         * Matches a l3 IPv4 address.
         *
         * @param ip a l3 address
         * @return a selection builder
         */
        public Builder matchIPSrc(IpPrefix ip);

        /**
         * Matches a l3 IPv4 address.
         *
         * @param ip a l3 address
         * @return a selection builder
         */
        public Builder matchIPDst(IpPrefix ip);

        /**
         * Matches a TCP source port number.
         *
         * @param tcpPort a TCP source port number
         * @return a selection builder
         */
        public Builder matchTcpSrc(short tcpPort);

        /**
         * Matches a TCP destination port number.
         *
         * @param tcpPort a TCP destination port number
         * @return a selection builder
         */
        public Builder matchTcpDst(short tcpPort);

        /**
         * Matches an UDP source port number.
         *
         * @param udpPort an UDP source port number
         * @return a selection builder
         */
        public Builder matchUdpSrc(short udpPort);

        /**
         * Matches an UDP destination port number.
         *
         * @param udpPort an UDP destination port number
         * @return a selection builder
         */
        public Builder matchUdpDst(short udpPort);

        /**
         * Matches a SCTP source port number.
         *
         * @param sctpPort a SCTP source port number
         * @return a selection builder
         */
        public Builder matchSctpSrc(short sctpPort);

        /**
         * Matches a SCTP destination port number.
         *
         * @param sctpPort a SCTP destination port number
         * @return a selection builder
         */
        public Builder matchSctpDst(short sctpPort);

        /**
         * Matches an ICMP type.
         *
         * @param icmpType an ICMP type
         * @return a selection builder
         */
        public Builder matchIcmpType(byte icmpType);

        /**
         * Matches an ICMP code.
         *
         * @param icmpCode an ICMP code
         * @return a selection builder
         */
        public Builder matchIcmpCode(byte icmpCode);

        /**
         * Matches a l3 IPv6 address.
         *
         * @param ip a l3 IPv6 address
         * @return a selection builder
         */
        public Builder matchIPv6Src(IpPrefix ip);

        /**
         * Matches a l3 IPv6 address.
         *
         * @param ip a l3 IPv6 address
         * @return a selection builder
         */
        public Builder matchIPv6Dst(IpPrefix ip);

        /**
         * Matches an IPv6 flow label.
         *
         * @param flowLabel an IPv6 flow label
         * @return a selection builder
         */
        public Builder matchIPv6FlowLabel(int flowLabel);

        /**
         * Matches an ICMPv6 type.
         *
         * @param icmpv6Type an ICMPv6 type
         * @return a selection builder
         */
        public Builder matchIcmpv6Type(byte icmpv6Type);

        /**
         * Matches an ICMPv6 code.
         *
         * @param icmpv6Code an ICMPv6 code
         * @return a selection builder
         */
        public Builder matchIcmpv6Code(byte icmpv6Code);

        /**
         * Matches an IPv6 Neighbor Discovery target address.
         *
         * @param targetAddress an IPv6 Neighbor Discovery target address
         * @return a selection builder
         */
        public Builder matchIPv6NDTargetAddress(Ip6Address targetAddress);

        /**
         * Matches an IPv6 Neighbor Discovery source link-layer address.
         *
         * @param mac an IPv6 Neighbor Discovery source link-layer address
         * @return a selection builder
         */
        public Builder matchIPv6NDSourceLinkLayerAddress(MacAddress mac);

        /**
         * Matches an IPv6 Neighbor Discovery target link-layer address.
         *
         * @param mac an IPv6 Neighbor Discovery target link-layer address
         * @return a selection builder
         */
        public Builder matchIPv6NDTargetLinkLayerAddress(MacAddress mac);

        /**
         * Matches on a MPLS label.
         *
         * @param mplsLabel a MPLS label.
         * @return a selection builder
         */
        public Builder matchMplsLabel(MplsLabel mplsLabel);

        /**
         * Matches on IPv6 Extension Header pseudo-field fiags.
         *
         * @param exthdrFlags the IPv6 Extension Header pseudo-field fiags
         * @return a selection builder
         */
        public Builder matchIPv6ExthdrFlags(int exthdrFlags);

        /**
         * Matches an optical signal ID or lambda.
         *
         * @param lambda lamda
         * @return a selection builder
         */
        public Builder matchLambda(short lambda);

        /**
         * Matches an optical Signal Type.
         *
         * @param signalType signalType
         * @return a selection builder
         */
        public Builder matchOpticalSignalType(short signalType);

        /**
         * Builds an immutable traffic selector.
         *
         * @return traffic selector
         */
        TrafficSelector build();
    }
}
