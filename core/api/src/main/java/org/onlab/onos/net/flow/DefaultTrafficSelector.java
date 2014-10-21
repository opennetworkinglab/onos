package org.onlab.onos.net.flow;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.flow.criteria.Criteria;
import org.onlab.onos.net.flow.criteria.Criterion;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;

/**
 * Default traffic selector implementation.
 */
public final class DefaultTrafficSelector implements TrafficSelector {

    private final Set<Criterion> criteria;

    /**
     * Creates a new traffic selector with the specified criteria.
     *
     * @param criteria criteria
     */
    private DefaultTrafficSelector(Set<Criterion> criteria) {
        this.criteria = ImmutableSet.copyOf(criteria);
    }

    @Override
    public Set<Criterion> criteria() {
        return criteria;
    }

    @Override
    public int hashCode() {
        return Objects.hash(criteria);
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
     * Returns a new traffic selector builder primed to produce entities
     * patterned after the supplied selector.
     *
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
        public Builder matchInport(PortNumber port) {
            return add(Criteria.matchInPort(port));
        }

        @Override
        public Builder matchEthSrc(MacAddress addr) {
            return add(Criteria.matchEthSrc(addr));
        }

        @Override
        public Builder matchEthDst(MacAddress addr) {
            return add(Criteria.matchEthDst(addr));
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
        public Builder matchVlanPcp(Byte vlanPcp) {
            return add(Criteria.matchVlanPcp(vlanPcp));
        }

        @Override
        public Builder matchIPProtocol(Byte proto) {
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

        @Override
        public Builder matchTcpSrc(Short tcpPort) {
            return add(Criteria.matchTcpSrc(tcpPort));
        }

        @Override
        public Builder matchTcpDst(Short tcpPort) {
            return add(Criteria.matchTcpDst(tcpPort));
        }

        @Override
        public TrafficSelector build() {
            return new DefaultTrafficSelector(ImmutableSet.copyOf(selector.values()));
        }

    }

}
