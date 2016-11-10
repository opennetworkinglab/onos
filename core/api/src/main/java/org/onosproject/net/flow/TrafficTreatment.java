/*
 * Copyright 2014-present Open Networking Laboratory
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

import org.onlab.packet.EthType;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.TpPort;
import org.onlab.packet.VlanId;
import org.onosproject.core.GroupId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.meter.MeterId;

/**
 * Abstraction of network traffic treatment.
 */
public interface TrafficTreatment {

    /**
     * Returns the list of treatment instructions that will be applied
     * further down the pipeline.
     * @return list of treatment instructions
     */
    List<Instruction> deferred();

    /**
     * Returns the list of treatment instructions that will be applied
     * immediately.
     * @return list of treatment instructions
     */
    List<Instruction> immediate();

    /**
     * Returns the list of all instructions in the treatment, both immediate and
     * deferred.
     *
     * @return list of treatment instructions
     */
    List<Instruction> allInstructions();

    /**
     * Returns the next table in the pipeline.
     * @return a table transition; may be null.
     */
    Instructions.TableTypeTransition tableTransition();

    /**
     * Whether the deferred treatment instructions will be cleared
     * by the device.
     * @return a boolean
     */
    boolean clearedDeferred();

    /**
     * Returns the metadata instruction if there is one.
     *
     * @return a metadata instruction that may be null
     */
    Instructions.MetadataInstruction writeMetadata();

    /**
     * Returns the meter instruction if there is one.
     *
     * @return a meter instruction that may be null
     */
    Instructions.MeterInstruction metered();

    /**
     * Builder of traffic treatment entities.
     */
    interface Builder {

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
        Builder drop();

        /**
         * Adds a punt-to-controller instruction.
         *
         * @return a treatment builder
         */
        Builder punt();

        /**
         * Set the output port.
         *
         * @param number the out port
         * @return a treatment builder
         */
        Builder setOutput(PortNumber number);

        /**
         * Sets the src l2 address.
         *
         * @param addr a macaddress
         * @return a treatment builder
         */
        Builder setEthSrc(MacAddress addr);

        /**
         * Sets the dst l2 address.
         *
         * @param addr a macaddress
         * @return a treatment builder
         */
        Builder setEthDst(MacAddress addr);

        /**
         * Sets the vlan id.
         *
         * @param id a vlanid
         * @return a treatment builder
         */
        Builder setVlanId(VlanId id);

        /**
         * Sets the vlan priority.
         *
         * @param pcp a vlan priority
         * @return a treatment builder
         */
        Builder setVlanPcp(Byte pcp);

        /**
         * Sets the src l3 address.
         *
         * @param addr an ip
         * @return a treatment builder
         */
        Builder setIpSrc(IpAddress addr);

        /**
         * Sets the dst l3 address.
         *
         * @param addr an ip
         * @return a treatment builder
         */
        Builder setIpDst(IpAddress addr);

        /**
         * Decrement the TTL in IP header by one.
         *
         * @return a treatment builder
         */
        Builder decNwTtl();

        /**
         * Copy the TTL to outer protocol layer.
         *
         * @return a treatment builder
         */
        Builder copyTtlOut();

        /**
         * Copy the TTL to inner protocol layer.
         *
         * @return a treatment builder
         */
        Builder copyTtlIn();

        /**
         * Push MPLS ether type.
         *
         * @return a treatment builder
         */
        Builder pushMpls();

        /**
         * Pops MPLS ether type.
         *
         * @return a treatment builder
         */
        Builder popMpls();

        /**
         * Pops MPLS ether type and set the new ethertype.
         *
         * @param etherType an ether type
         * @return a treatment builder
         */
        Builder popMpls(EthType etherType);

        /**
         * Sets the mpls label.
         *
         * @param mplsLabel MPLS label
         * @return a treatment builder
         */
        Builder setMpls(MplsLabel mplsLabel);

        /**
         * Sets the mpls bottom-of-stack indicator bit.
         *
         * @param mplsBos boolean to set BOS=1 (true) or BOS=0 (false)
         * @return a treatment builder.
         */
        Builder setMplsBos(boolean mplsBos);

        /**
         * Decrement MPLS TTL.
         *
         * @return a treatment builder
         */
        Builder decMplsTtl();

        /**
         * Sets the group ID.
         *
         * @param groupId group ID
         * @return a treatment builder
         */
        Builder group(GroupId groupId);

        /**
         * Sets the Queue ID.
         *
         * @param queueId a queue ID
         * @return a treatment builder
         */
        Builder setQueue(long queueId);

        /**
         * Sets the Queue ID for a specific port.
         *
         * @param queueId a queue ID
         * @param port a port number
         * @return a treatment builder
         */
        Builder setQueue(long queueId, PortNumber port);

        /**
         * Sets a meter to be used by this flow.
         *
         * @param meterId a meter id
         * @return a treatment builder
         */
        Builder meter(MeterId meterId);

        /**
         * Sets the next table id to transition to.
         *
         * @param tableId the table table
         * @return a treatement builder
         */
        Builder transition(Integer tableId);


        /**
         * Pops outermost VLAN tag.
         *
         * @return a treatment builder
         */
        Builder popVlan();

        /**
         * Pushes a new VLAN tag.
         *
         * @return a treatment builder
         */
        Builder pushVlan();

        /**
         * Pushes a new VLAN tag using the supplied Ethernet type.
         *
         * @param ethType ethernet type
         * @return a treatment builder
         */
        Builder pushVlan(EthType ethType);

        /**
         * Any instructions preceded by this method call will be deferred.
         * @return a treatment builder
         */
        Builder deferred();

        /**
         * Any instructions preceded by this method call will be immediate.
         * @return a treatment builder
         */
        Builder immediate();


        /**
         * Instructs the device to clear the deferred instructions set.
         * @return a treatment builder
         */
        Builder wipeDeferred();

        /**
         * Writes metadata to associate with a packet.
         * <pre>
         * {@code
         * new_metadata = (old_metadata &  ̃mask) | (value & mask)
         * }
         * </pre>
         *
         * @param value the metadata to write
         * @param mask  the masked bits for the value
         * @return a treatment builder
         */
        Builder writeMetadata(long value, long mask);

        /**
         * Sets the tunnel id.
         *
         * @param tunnelId a tunnel id
         * @return a treatment builder
         */
        Builder setTunnelId(long tunnelId);

        /**
         * Sets the src TCP port.
         *
         * @param port a port number
         * @return a treatment builder
         */
        Builder setTcpSrc(TpPort port);

        /**
         * Sets the dst TCP port.
         *
         * @param port a port number
         * @return a treatment builder
         */
        Builder setTcpDst(TpPort port);

        /**
         * Sets the src UDP port.
         *
         * @param port a port number
         * @return a treatment builder
         */
        Builder setUdpSrc(TpPort port);

        /**
         * Sets the dst UDP port.
         *
         * @param port a port number
         * @return a treatment builder
         */
        Builder setUdpDst(TpPort port);

        /**
         * Sets the arp src ip address.
         *
         * @param addr an ip
         * @return a treatment builder
         */
        Builder setArpSpa(IpAddress addr);

        /**
         * Sets the arp src mac address.
         *
         * @param addr a macaddress
         * @return a treatment builder
         */
        Builder setArpSha(MacAddress addr);

        /**
         * Sets the arp operation.
         *
         * @param op the value of arp operation.
         * @return a treatment builder.
         */
        Builder setArpOp(short op);

        /**
         * Uses an extension treatment.
         *
         * @param extension extension treatment
         * @param deviceId device ID
         * @return a treatment builder
         */
        Builder extension(ExtensionTreatment extension, DeviceId deviceId);

        /**
         * Add all instructions from another treatment.
         *
         * @param treatment another treatment
         * @return a treatment builder
         */
        Builder addTreatment(TrafficTreatment treatment);

        /**
         * Builds an immutable traffic treatment descriptor.
         * <p>
         * If the treatment is empty when build() is called, it will add a default
         * drop rule automatically. For a treatment that is actually empty, use
         * {@link org.onosproject.net.flow.DefaultTrafficTreatment#emptyTreatment}.
         * </p>
         *
         * @return traffic treatment
         */
        TrafficTreatment build();

    }

}
