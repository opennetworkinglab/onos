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

import java.util.List;

import org.onosproject.core.GroupId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficTreatment.Builder;
import org.onosproject.net.flow.instructions.Instruction;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.MplsLabel;
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
         *
         * @param instruction an instruction
         * @return a treatment builder
         */
        Builder add(Instruction instruction);

        /**
         * Adds a drop instruction.
         *
         * @return a treatment builder
         */
        public Builder drop();

        /**
         * Adds a punt-to-controller instruction.
         *
         * @return a treatment builder
         */
        public Builder punt();

        /**
         * Set the output port.
         *
         * @param number the out port
         * @return a treatment builder
         */
        public Builder setOutput(PortNumber number);

        /**
         * Sets the src l2 address.
         *
         * @param addr a macaddress
         * @return a treatment builder
         */
        public Builder setEthSrc(MacAddress addr);

        /**
         * Sets the dst l2 address.
         *
         * @param addr a macaddress
         * @return a treatment builder
         */
        public Builder setEthDst(MacAddress addr);

        /**
         * Sets the vlan id.
         *
         * @param id a vlanid
         * @return a treatment builder
         */
        public Builder setVlanId(VlanId id);

        /**
         * Sets the vlan priority.
         *
         * @param pcp a vlan priority
         * @return a treatment builder
         */
        public Builder setVlanPcp(Byte pcp);

        /**
         * Strips the vlan tag if there is one.
         * @return a treatment builder
         */
        public Builder stripVlan();

        /**
         * Sets the src l3 address.
         *
         * @param addr an ip
         * @return a treatment builder
         */
        public Builder setIpSrc(IpAddress addr);

        /**
         * Sets the dst l3 address.
         *
         * @param addr an ip
         * @return a treatment builder
         */
        public Builder setIpDst(IpAddress addr);

        /**
         * Decrement the TTL in IP header by one.
         *
         * @return a treatment builder
         */
        public Builder decNwTtl();

        /**
         * Copy the TTL to outer protocol layer.
         *
         * @return a treatment builder
         */
        public Builder copyTtlOut();

        /**
         * Copy the TTL to inner protocol layer.
         *
         * @return a treatment builder
         */
        public Builder copyTtlIn();

        /**
         * Push MPLS ether type.
         *
         * @return a treatment builder.
         */
        public Builder pushMpls();

        /**
         * Pops MPLS ether type.
         *
         * @return a treatment builder.
         */
        public Builder popMpls();

        /**
         * Pops MPLS ether type and set the new ethertype.
         *
         * @param etherType an ether type
         * @return a treatment builder.
         */
        public Builder popMpls(Short etherType);

        /**
         * Sets the mpls label.
         *
         * @param mplsLabel MPLS label.
         * @return a treatment builder.
         */
        public Builder setMpls(MplsLabel mplsLabel);

        /**
         * Decrement MPLS TTL.
         *
         * @return a treatment builder
         */
        public Builder decMplsTtl();

        /**
         * Sets the optical channel ID or lambda.
         *
         * @param lambda optical channel ID
         * @return a treatment builder
         */
        public Builder setLambda(short lambda);

        /**
         * Sets the group ID.
         *
         * @param groupId group ID
         * @return a treatment builder
         */
        public Builder group(GroupId groupId);


        /**
         * Sets the next table type to transition to.
         *
         * @param type the table type
         * @return a treatement builder
         */
        public Builder transition(FlowRule.Type type);

        /**
         * Pops outermost VLAN tag.
         *
         * @return a treatment builder.
         */
        public Builder popVlan();

        /**
         * Builds an immutable traffic treatment descriptor.
         *
         * @return traffic treatment
         */
        TrafficTreatment build();


    }

}
