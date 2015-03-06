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
package org.onosproject.net.flow.instructions;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.flow.instructions.L2ModificationInstruction.*;

import java.util.Objects;

import org.onosproject.core.GroupId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.instructions.L0ModificationInstruction.L0SubType;
import org.onosproject.net.flow.instructions.L0ModificationInstruction.ModLambdaInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.L2SubType;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModEtherInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction.L3SubType;
import org.onosproject.net.flow.instructions.L3ModificationInstruction.ModIPInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction.ModIPv6FlowLabelInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction.ModTtlInstruction;

import org.onlab.packet.Ethernet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;

/**
 * Factory class for creating various traffic treatment instructions.
 */
public final class Instructions {


    // Ban construction
    private Instructions() {}

    /**
     * Creates an output instruction using the specified port number. This can
     * include logical ports such as CONTROLLER, FLOOD, etc.
     *
     * @param number port number
     * @return output instruction
     */
    public static OutputInstruction createOutput(final PortNumber number) {
        checkNotNull(number, "PortNumber cannot be null");
        return new OutputInstruction(number);
    }

    /**
     * Creates a drop instruction.
     * @return drop instruction
     */
    public static DropInstruction createDrop() {
        return new DropInstruction();
    }

    /**
     * Creates a group instruction.
     *
     * @param groupId Group Id
     * @return group instruction
     */
    public static GroupInstruction createGroup(final GroupId groupId) {
        checkNotNull(groupId, "GroupId cannot be null");
        return new GroupInstruction(groupId);
    }

    /**
     * Creates a l0 modification.
     * @param lambda the lambda to modify to.
     * @return a l0 modification
     */
    public static L0ModificationInstruction modL0Lambda(short lambda) {
        checkNotNull(lambda, "L0 lambda cannot be null");
        return new ModLambdaInstruction(L0SubType.LAMBDA, lambda);
    }

    /**
     * Creates a l2 src modification.
     * @param addr the mac address to modify to.
     * @return a l2 modification
     */
    public static L2ModificationInstruction modL2Src(MacAddress addr) {
        checkNotNull(addr, "Src l2 address cannot be null");
        return new ModEtherInstruction(L2SubType.ETH_SRC, addr);
    }

    /**
     * Creates a L2 dst modification.
     * @param addr the mac address to modify to.
     * @return a L2 modification
     */
    public static L2ModificationInstruction modL2Dst(MacAddress addr) {
        checkNotNull(addr, "Dst l2 address cannot be null");
        return new ModEtherInstruction(L2SubType.ETH_DST, addr);
    }

    /**
     * Creates a Vlan id modification.
     * @param vlanId the vlan id to modify to.
     * @return a L2 modification
     */
    public static L2ModificationInstruction modVlanId(VlanId vlanId) {
        checkNotNull(vlanId, "VLAN id cannot be null");
        return new ModVlanIdInstruction(vlanId);
    }

    /**
     * Creates a Vlan pcp modification.
     * @param vlanPcp the pcp to modify to.
     * @return a L2 modification
     */
    public static L2ModificationInstruction modVlanPcp(Byte vlanPcp) {
        checkNotNull(vlanPcp, "VLAN Pcp cannot be null");
        return new ModVlanPcpInstruction(vlanPcp);
    }

    /**
     * Strips the VLAN tag if one is present.
     * @return a L2 modification
     */
    public static L2ModificationInstruction stripVlanId() {
        return new StripVlanInstruction();
    }

    /**
     * Creates a MPLS label modification.
     * @param mplsLabel to set.
     * @return a L2 Modification
     */
    public static L2ModificationInstruction modMplsLabel(MplsLabel mplsLabel) {
        checkNotNull(mplsLabel, "MPLS label cannot be null");
        return new ModMplsLabelInstruction(mplsLabel);
    }

    /**
     * Creates a MPLS TTL modification.
     *
     * @return a L2 Modification
     */
    public static L2ModificationInstruction decMplsTtl() {
        return new ModMplsTtlInstruction();
    }

    /**
     * Creates a L3 IPv4 src modification.
     *
     * @param addr the IPv4 address to modify to
     * @return a L3 modification
     */
    public static L3ModificationInstruction modL3Src(IpAddress addr) {
        checkNotNull(addr, "Src l3 IPv4 address cannot be null");
        return new ModIPInstruction(L3SubType.IPV4_SRC, addr);
    }

    /**
     * Creates a L3 IPv4 dst modification.
     *
     * @param addr the IPv4 address to modify to
     * @return a L3 modification
     */
    public static L3ModificationInstruction modL3Dst(IpAddress addr) {
        checkNotNull(addr, "Dst l3 IPv4 address cannot be null");
        return new ModIPInstruction(L3SubType.IPV4_DST, addr);
    }

    /**
     * Creates a L3 IPv6 src modification.
     *
     * @param addr the IPv6 address to modify to
     * @return a L3 modification
     */
    public static L3ModificationInstruction modL3IPv6Src(IpAddress addr) {
        checkNotNull(addr, "Src l3 IPv6 address cannot be null");
        return new ModIPInstruction(L3SubType.IPV6_SRC, addr);
    }

    /**
     * Creates a L3 IPv6 dst modification.
     *
     * @param addr the IPv6 address to modify to
     * @return a L3 modification
     */
    public static L3ModificationInstruction modL3IPv6Dst(IpAddress addr) {
        checkNotNull(addr, "Dst l3 IPv6 address cannot be null");
        return new ModIPInstruction(L3SubType.IPV6_DST, addr);
    }

    /**
     * Creates a L3 IPv6 Flow Label modification.
     *
     * @param flowLabel the IPv6 flow label to modify to (20 bits)
     * @return a L3 modification
     */
    public static L3ModificationInstruction modL3IPv6FlowLabel(int flowLabel) {
        return new ModIPv6FlowLabelInstruction(flowLabel);
    }

    /**
     * Creates a L3 TTL modification.
     * @return a L3 modification
     */
    public static L3ModificationInstruction decNwTtl() {
        return new ModTtlInstruction(L3SubType.DEC_TTL);
    }

    /**
     * Creates a L3 TTL modification.
     * @return a L3 modification
     */
    public static L3ModificationInstruction copyTtlOut() {
        return new ModTtlInstruction(L3SubType.TTL_OUT);
    }

    /**
     * Creates a L3 TTL modification.
     * @return a L3 modification
     */
    public static L3ModificationInstruction copyTtlIn() {
        return new ModTtlInstruction(L3SubType.TTL_IN);
    }

    /**
     * Creates a mpls header instruction.
     * @return a L2 modification.
     */
    public static Instruction pushMpls() {
        return new PushHeaderInstructions(L2SubType.MPLS_PUSH,
                                          Ethernet.MPLS_UNICAST);
    }

    /**
     * Creates a mpls header instruction.
     * @return a L2 modification.
     */
    public static Instruction popMpls() {
        return new PushHeaderInstructions(L2SubType.MPLS_POP,
                                          Ethernet.MPLS_UNICAST);
    }

    /**
     * Creates a mpls header instruction.
     *
     * @param etherType Ethernet type to set
     * @return a L2 modification.
     */
    public static Instruction popMpls(Short etherType) {
        checkNotNull(etherType, "Ethernet type cannot be null");
        return new PushHeaderInstructions(L2SubType.MPLS_POP, etherType);
    }

    /**
     * Creates a vlan header instruction.
     * @return a L2 modification.
     */
    public static Instruction popVlan() {
        return new PopVlanInstruction(L2SubType.VLAN_POP);
    }

    /**
     * Sends the packet to the table described in 'type'.
     * @param type flow rule table type
     * @return table type transition instruction
     */
    public static Instruction transition(FlowRule.Type type) {
        checkNotNull(type, "Table type cannot be null");
        return new TableTypeTransition(type);
    }

    /**
     *  Drop instruction.
     */
    public static final class DropInstruction implements Instruction {

        private DropInstruction() {}

        @Override
        public Type type() {
            return Type.DROP;
        }

        @Override
        public String toString() {
            return toStringHelper(type().toString()).toString();

        }

        @Override
        public int hashCode() {
            return Objects.hash(type());
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof DropInstruction) {
                return true;
            }
            return false;
        }
    }

    /**
     *  Output Instruction.
     */
    public static final class OutputInstruction implements Instruction {
        private final PortNumber port;

        private OutputInstruction(PortNumber port) {
            this.port = port;
        }

        public PortNumber port() {
            return port;
        }

        @Override
        public Type type() {
            return Type.OUTPUT;
        }
        @Override
        public String toString() {
            return toStringHelper(type().toString())
                    .add("port", port).toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type(), port);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof OutputInstruction) {
                OutputInstruction that = (OutputInstruction) obj;
                return Objects.equals(port, that.port);

            }
            return false;
        }
    }

    /**
     *  Group Instruction.
     */
    public static final class GroupInstruction implements Instruction {
        private final GroupId groupId;

        private GroupInstruction(GroupId groupId) {
            this.groupId = groupId;
        }

        public GroupId groupId() {
            return groupId;
        }

        @Override
        public Type type() {
            return Type.GROUP;
        }

        @Override
        public String toString() {
            return toStringHelper(type().toString())
                    .add("group ID", groupId.id()).toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type(), groupId);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof GroupInstruction) {
                GroupInstruction that = (GroupInstruction) obj;
                return Objects.equals(groupId, that.groupId);

            }
            return false;
        }
    }

    // FIXME: Temporary support for this. This should probably become it's own
    // type like other instructions.
    public static class TableTypeTransition implements Instruction {
        private final FlowRule.Type tableType;

        TableTypeTransition(FlowRule.Type type) {
            this.tableType = type;
        }

        @Override
        public Type type() {
            return Type.TABLE;
        }

        public FlowRule.Type tableType() {
            return this.tableType;
        }

        @Override
        public String toString() {
            return toStringHelper(type().toString())
                    .add("tableType", this.tableType).toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type(), tableType);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof TableTypeTransition) {
                TableTypeTransition that = (TableTypeTransition) obj;
                return Objects.equals(tableType, that.tableType);

            }
            return false;
        }

    }
}


