package org.onlab.onos.net.flow;

import java.util.List;

import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.flow.instructions.Instruction;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;

/**
 * Abstraction of network traffic treatment.
 */
public interface TrafficTreatment {

    /**
     * Returns list of instructions on how to treat traffic.
     *
     * @return list of treatment instructions
     */
    List<Instruction> instructions();

    /**
     * Builder of traffic treatment entities.
     */
    public interface Builder {

        /**
         * Adds an instruction to the builder.
         * @param instruction an instruction
         * @return a treatment builder
         */
        Builder add(Instruction instruction);

        /**
         * Adds a drop instruction and does not return a builder.
         */
        public void drop();

        /**
         * Set the output port.
         * @param number the out port
         * @return a treatment builder
         */
        public Builder setOutput(PortNumber number);

        /**
         * Sets the src l2 address.
         * @param addr a macaddress
         * @return a treatment builder
         */
        public Builder setEthSrc(MacAddress addr);

        /**
         * Sets the dst l2 address.
         * @param addr a macaddress
         * @return a treatment builder
         */
        public Builder setEthDst(MacAddress addr);

        /**
         * Sets the vlan id.
         * @param id a vlanid
         * @return a treatment builder
         */
        public Builder setVlanId(VlanId id);

        /**
         * Sets the vlan priority.
         * @param pcp a vlan priority
         * @return a treatment builder
         */
        public Builder setVlanPcp(Byte pcp);

        /**
         * Sets the src l3 address.
         * @param addr an ip
         * @return a treatment builder
         */
        public Builder setIpSrc(IpPrefix addr);

        /**
         * Sets the dst l3 address.
         * @param addr an ip
         * @return a treatment builder
         */
        public Builder setIpDst(IpPrefix addr);

        /**
         * Builds an immutable traffic treatment descriptor.
         *
         * @return traffic treatment
         */
        TrafficTreatment build();
    }

}
