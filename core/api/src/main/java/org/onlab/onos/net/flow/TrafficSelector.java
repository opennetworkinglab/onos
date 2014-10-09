package org.onlab.onos.net.flow;

import java.util.Set;

import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.flow.criteria.Criterion;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
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
         * @param port the inport
         * @return a selection builder
         */
        public Builder matchInport(PortNumber port);

        /**
         * Matches a l2 src address.
         * @param addr a l2 address
         * @return a selection builder
         */
        public Builder matchEthSrc(MacAddress addr);

        /**
         * Matches a l2 dst address.
         * @param addr a l2 address
         * @return a selection builder
         */
        public Builder matchEthDst(MacAddress addr);

        /**
         * Matches the ethernet type.
         * @param ethType an ethernet type
         * @return a selection builder
         */
        public Builder matchEthType(short ethType);

        /**
         * Matches the vlan id.
         * @param vlanId a vlan id
         * @return a selection builder
         */
        public Builder matchVlanId(VlanId vlanId);

        /**
         * Matches a vlan priority.
         * @param vlanPcp a vlan priority
         * @return a selection builder
         */
        public Builder matchVlanPcp(Byte vlanPcp);

        /**
         * Matches the l3 protocol.
         * @param proto a l3 protocol
         * @return a selection builder
         */
        public Builder matchIPProtocol(Byte proto);

        /**
         * Matches a l3 address.
         * @param ip a l3 address
         * @return a selection builder
         */
        public Builder matchIPSrc(IpPrefix ip);

        /**
         * Matches a l3 address.
         * @param ip a l3 address
         * @return a selection builder
         */
        public Builder matchIPDst(IpPrefix ip);

        /**
         * Matches a TCP source port number.
         * @param tcpPort a TCP source port number
         * @return a selection builder
         */
        public Builder matchTcpSrc(Short tcpPort);

        /**
         * Matches a TCP destination port number.
         * @param tcpPort a TCP destination port number
         * @return a selection builder
         */
        public Builder matchTcpDst(Short tcpPort);

        /**
         * Builds an immutable traffic selector.
         *
         * @return traffic selector
         */
        TrafficSelector build();
    }

}
