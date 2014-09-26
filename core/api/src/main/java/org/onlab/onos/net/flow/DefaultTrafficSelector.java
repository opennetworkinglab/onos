package org.onlab.onos.net.flow;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.flow.criteria.Criteria;
import org.onlab.onos.net.flow.criteria.Criterion;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.slf4j.Logger;

public final class DefaultTrafficSelector implements TrafficSelector {

    private final List<Criterion> selector;

    private DefaultTrafficSelector(List<Criterion> selector) {
        this.selector = Collections.unmodifiableList(selector);
    }

    @Override
    public List<Criterion> criteria() {
        return selector;
    }

    public static class Builder implements TrafficSelector.Builder {

        private final Logger log = getLogger(getClass());

        private final List<Criterion> selector = new LinkedList<>();

        @Override
        public Builder add(Criterion criterion) {
            selector.add(criterion);
            return this;
        }

        public Builder matchInport(PortNumber port) {
            return add(Criteria.matchInPort(port));
        }

        public Builder matchEthSrc(MacAddress addr) {
            return add(Criteria.matchEthSrc(addr));
        }

        public Builder matchEthDst(MacAddress addr) {
            return add(Criteria.matchEthDst(addr));
        }

        public Builder matchEthType(short ethType) {
            return add(Criteria.matchEthType(ethType));
        }

        public Builder matchVlanId(VlanId vlanId) {
            return add(Criteria.matchVlanId(vlanId));
        }

        public Builder matchVlanPcp(Byte vlanPcp) {
            return add(Criteria.matchVlanPcp(vlanPcp));
        }

        public Builder matchIPProtocol(Byte proto) {
            return add(Criteria.matchIPProtocol(proto));
        }

        public Builder matchIPSrc(IpPrefix ip) {
            return add(Criteria.matchIPSrc(ip));
        }

        public Builder matchIPDst(IpPrefix ip) {
            return add(Criteria.matchIPDst(ip));
        }

        @Override
        public TrafficSelector build() {
            return new DefaultTrafficSelector(selector);
        }

    }

}
