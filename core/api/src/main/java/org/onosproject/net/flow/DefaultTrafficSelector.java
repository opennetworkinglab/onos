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
package org.onosproject.net.flow;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.TpPort;
import org.onlab.packet.VlanId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.ExtensionSelector;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Default traffic selector implementation.
 */
public final class DefaultTrafficSelector implements TrafficSelector {

    private static final Comparator<? super Criterion> TYPE_COMPARATOR =
            (c1, c2) -> c1.type().compareTo(c2.type());

    private final Set<Criterion> criteria;

    private static final TrafficSelector EMPTY
            = new DefaultTrafficSelector(Collections.emptySet());

    /**
     * Creates a new traffic selector with the specified criteria.
     *
     * @param criteria criteria
     */
    private DefaultTrafficSelector(Set<Criterion> criteria) {
        TreeSet<Criterion> elements = new TreeSet<>(TYPE_COMPARATOR);
        elements.addAll(criteria);
        this.criteria = ImmutableSet.copyOf(elements);
    }

    @Override
    public Set<Criterion> criteria() {
        return criteria;
    }

    @Override
    public Criterion getCriterion(Criterion.Type type) {
        for (Criterion c : criteria) {
            if (c.type() == type) {
                return c;
            }
        }
        return null;
    }

    @Override
    public int hashCode() {
        return criteria.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultTrafficSelector) {
            DefaultTrafficSelector that = (DefaultTrafficSelector) obj;
            return Objects.equals(criteria, that.criteria);

        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("criteria", criteria)
                .toString();
    }

    /**
     * Returns a new traffic selector builder.
     *
     * @return traffic selector builder
     */
    public static TrafficSelector.Builder builder() {
        return new Builder();
    }

    /**
     * Returns an empty traffic selector.
     *
     * @return empty traffic selector
     */
    public static TrafficSelector emptySelector() {
        return EMPTY;
    }

    /**
     * Returns a new traffic selector builder primed to produce entities
     * patterned after the supplied selector.
     *
     * @param selector base selector
     * @return traffic selector builder
     */
    public static TrafficSelector.Builder builder(TrafficSelector selector) {
        return new Builder(selector);
    }

    /**
     * Builder of traffic selector entities.
     */
    public static final class Builder implements TrafficSelector.Builder {

        private final Map<Criterion.Type, Criterion> selector = new HashMap<>();

        private Builder() {
        }

        private Builder(TrafficSelector selector) {
            for (Criterion c : selector.criteria()) {
                add(c);
            }
        }

        @Override
        public Builder add(Criterion criterion) {
            selector.put(criterion.type(), criterion);
            return this;
        }

        @Override
        public Builder matchInPort(PortNumber port) {
            return add(Criteria.matchInPort(port));
        }

        @Override
        public Builder matchInPhyPort(PortNumber port) {
            return add(Criteria.matchInPhyPort(port));
        }

        @Override
        public Builder matchMetadata(long metadata) {
            return add(Criteria.matchMetadata(metadata));
        }

        @Override
        public Builder matchEthDst(MacAddress addr) {
            return add(Criteria.matchEthDst(addr));
        }

        @Override
        public Builder matchEthSrc(MacAddress addr) {
            return add(Criteria.matchEthSrc(addr));
        }

        @Override
        public Builder matchEthType(short ethType) {
            return add(Criteria.matchEthType(ethType));
        }

        @Override
        public Builder matchVlanId(VlanId vlanId) {
            return add(Criteria.matchVlanId(vlanId));
        }

        @Override
        public Builder matchVlanPcp(byte vlanPcp) {
            return add(Criteria.matchVlanPcp(vlanPcp));
        }

        @Override
        public Builder matchIPDscp(byte ipDscp) {
            return add(Criteria.matchIPDscp(ipDscp));
        }

        @Override
        public Builder matchIPEcn(byte ipEcn) {
            return add(Criteria.matchIPEcn(ipEcn));
        }

        @Override
        public Builder matchIPProtocol(byte proto) {
            return add(Criteria.matchIPProtocol(proto));
        }

        @Override
        public Builder matchIPSrc(IpPrefix ip) {
            return add(Criteria.matchIPSrc(ip));
        }

        @Override
        public Builder matchIPDst(IpPrefix ip) {
            return add(Criteria.matchIPDst(ip));
        }

        @Deprecated
        @Override
        public Builder matchTcpSrc(short tcpPort) {
            return matchTcpSrc(TpPort.tpPort(tcpPort));
        }

        @Override
        public Builder matchTcpSrc(TpPort tcpPort) {
            return add(Criteria.matchTcpSrc(tcpPort));
        }

        @Deprecated
        @Override
        public Builder matchTcpDst(short tcpPort) {
            return matchTcpDst(TpPort.tpPort(tcpPort));
        }

        @Override
        public Builder matchTcpDst(TpPort tcpPort) {
            return add(Criteria.matchTcpDst(tcpPort));
        }

        @Deprecated
        @Override
        public Builder matchUdpSrc(short udpPort) {
            return matchUdpSrc(TpPort.tpPort(udpPort));
        }

        @Override
        public Builder matchUdpSrc(TpPort udpPort) {
            return add(Criteria.matchUdpSrc(udpPort));
        }

        @Deprecated
        @Override
        public Builder matchUdpDst(short udpPort) {
            return matchUdpDst(TpPort.tpPort(udpPort));
        }

        @Override
        public Builder matchUdpDst(TpPort udpPort) {
            return add(Criteria.matchUdpDst(udpPort));
        }

        @Deprecated
        @Override
        public Builder matchSctpSrc(short sctpPort) {
            return matchSctpSrc(TpPort.tpPort(sctpPort));
        }

        @Override
        public Builder matchSctpSrc(TpPort sctpPort) {
            return add(Criteria.matchSctpSrc(sctpPort));
        }

        @Deprecated
        @Override
        public Builder matchSctpDst(short sctpPort) {
            return matchSctpDst(TpPort.tpPort(sctpPort));
        }

        @Override
        public Builder matchSctpDst(TpPort sctpPort) {
            return add(Criteria.matchSctpDst(sctpPort));
        }

        @Override
        public Builder matchIcmpType(byte icmpType) {
            return add(Criteria.matchIcmpType(icmpType));
        }

        @Override
        public Builder matchIcmpCode(byte icmpCode) {
            return add(Criteria.matchIcmpCode(icmpCode));
        }

        @Override
        public Builder matchIPv6Src(IpPrefix ip) {
            return add(Criteria.matchIPv6Src(ip));
        }

        @Override
        public Builder matchIPv6Dst(IpPrefix ip) {
            return add(Criteria.matchIPv6Dst(ip));
        }

        @Override
        public Builder matchIPv6FlowLabel(int flowLabel) {
            return add(Criteria.matchIPv6FlowLabel(flowLabel));
        }

        @Override
        public Builder matchIcmpv6Type(byte icmpv6Type) {
            return add(Criteria.matchIcmpv6Type(icmpv6Type));
        }

        @Override
        public Builder matchIcmpv6Code(byte icmpv6Code) {
            return add(Criteria.matchIcmpv6Code(icmpv6Code));
        }

        @Override
        public Builder matchIPv6NDTargetAddress(Ip6Address targetAddress) {
            return add(Criteria.matchIPv6NDTargetAddress(targetAddress));
        }

        @Override
        public Builder matchIPv6NDSourceLinkLayerAddress(MacAddress mac) {
            return add(Criteria.matchIPv6NDSourceLinkLayerAddress(mac));
        }

        @Override
        public Builder matchIPv6NDTargetLinkLayerAddress(MacAddress mac) {
            return add(Criteria.matchIPv6NDTargetLinkLayerAddress(mac));
        }

        @Override
        public Builder matchMplsLabel(MplsLabel mplsLabel) {
            return add(Criteria.matchMplsLabel(mplsLabel));
        }

        @Override
        public Builder matchMplsBos(boolean mplsBos) {
            return add(Criteria.matchMplsBos(mplsBos));
        }

        @Override
        public TrafficSelector.Builder matchTunnelId(long tunnelId) {
            return add(Criteria.matchTunnelId(tunnelId));
        }

        @Override
        public Builder matchIPv6ExthdrFlags(short exthdrFlags) {
            return add(Criteria.matchIPv6ExthdrFlags(exthdrFlags));
        }

        @Override
        public Builder matchArpTpa(Ip4Address addr) {
            return add(Criteria.matchArpTpa(addr));
        }

        @Override
        public Builder matchArpSpa(Ip4Address addr) {
            return add(Criteria.matchArpSpa(addr));
        }

        @Override
        public Builder matchArpTha(MacAddress addr) {
            return add(Criteria.matchArpTha(addr));
        }

        @Override
        public Builder matchArpSha(MacAddress addr) {
            return add(Criteria.matchArpSha(addr));
        }

        @Override
        public Builder matchArpOp(int arpOp) {
            return add(Criteria.matchArpOp(arpOp));
        }

        @Override
        public TrafficSelector.Builder extension(ExtensionSelector extensionSelector,
                                                 DeviceId deviceId) {
            return add(Criteria.extension(extensionSelector, deviceId));
        }

        @Override
        public TrafficSelector build() {
            return new DefaultTrafficSelector(ImmutableSet.copyOf(selector.values()));
        }
    }
}
