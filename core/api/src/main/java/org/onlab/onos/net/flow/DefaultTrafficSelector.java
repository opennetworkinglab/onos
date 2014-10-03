package org.onlab.onos.net.flow;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.flow.criteria.Criteria;
import org.onlab.onos.net.flow.criteria.Criterion;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.slf4j.Logger;

public final class DefaultTrafficSelector implements TrafficSelector {

    private final Set<Criterion> selector;

    private DefaultTrafficSelector(Set<Criterion> selector) {
        this.selector = Collections.unmodifiableSet(selector);
    }

    @Override
    public Set<Criterion> criteria() {
        return selector;
    }

    @Override
    public int hashCode() {
        return Objects.hash(selector);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultTrafficSelector) {
            DefaultTrafficSelector that = (DefaultTrafficSelector) obj;
            return Objects.equals(selector, that.selector);

        }
        return false;
    }



    public static class Builder implements TrafficSelector.Builder {

        private final Logger log = getLogger(getClass());

        private final Set<Criterion> selector = new HashSet<>();

        @Override
        public Builder add(Criterion criterion) {
            selector.add(criterion);
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
        public TrafficSelector build() {
            return new DefaultTrafficSelector(selector);
        }

    }

}
