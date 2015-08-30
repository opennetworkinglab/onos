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
package org.onosproject.net.flow.instructions;

import org.onlab.packet.EthType;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.TpPort;
import org.onlab.packet.VlanId;
import org.onosproject.core.GroupId;
import org.onosproject.net.IndexedLambda;
import org.onosproject.net.Lambda;
import org.onosproject.net.OchSignal;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.instructions.L0ModificationInstruction.L0SubType;
import org.onosproject.net.flow.instructions.L0ModificationInstruction.ModLambdaInstruction;
import org.onosproject.net.flow.instructions.L0ModificationInstruction.ModOchSignalInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction.L3SubType;
import org.onosproject.net.flow.instructions.L3ModificationInstruction.ModIPInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction.ModIPv6FlowLabelInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction.ModTtlInstruction;
import org.onosproject.net.flow.instructions.L4ModificationInstruction.L4SubType;
import org.onosproject.net.flow.instructions.L4ModificationInstruction.ModTransportPortInstruction;
import org.onosproject.net.meter.MeterId;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

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
     *
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

    public static MeterInstruction meterTraffic(final MeterId meterId) {
        checkNotNull(meterId, "meter id cannot be null");
        return new MeterInstruction(meterId);
    }

    /**
     * Creates a l0 modification.
     *
     * @param lambda the lambda to modify to
     * @return a l0 modification
     * @deprecated in Cardinal Release. Use {@link #modL0Lambda(Lambda)} instead.
     */
    @Deprecated
    public static L0ModificationInstruction modL0Lambda(short lambda) {
        checkNotNull(lambda, "L0 lambda cannot be null");
        return new ModLambdaInstruction(L0SubType.LAMBDA, lambda);
    }

    /**
     * Creates an L0 modification with the specified OCh signal.
     *
     * @param lambda OCh signal
     * @return an L0 modification
     */
    public static L0ModificationInstruction modL0Lambda(Lambda lambda) {
        checkNotNull(lambda, "L0 OCh signal cannot be null");

        if (lambda instanceof IndexedLambda) {
            return new ModLambdaInstruction(L0SubType.LAMBDA, (short) ((IndexedLambda) lambda).index());
        } else if (lambda instanceof OchSignal) {
            return new ModOchSignalInstruction((OchSignal) lambda);
        } else {
            throw new UnsupportedOperationException(String.format("Unsupported type: %s", lambda));
        }
    }

    /**
     * Creates a l2 src modification.
     *
     * @param addr the mac address to modify to
     * @return a l2 modification
     */
    public static L2ModificationInstruction modL2Src(MacAddress addr) {
        checkNotNull(addr, "Src l2 address cannot be null");
        return new L2ModificationInstruction.ModEtherInstruction(
                L2ModificationInstruction.L2SubType.ETH_SRC, addr);
    }

    /**
     * Creates a L2 dst modification.
     *
     * @param addr the mac address to modify to
     * @return a L2 modification
     */
    public static L2ModificationInstruction modL2Dst(MacAddress addr) {
        checkNotNull(addr, "Dst l2 address cannot be null");
        return new L2ModificationInstruction.ModEtherInstruction(
                L2ModificationInstruction.L2SubType.ETH_DST, addr);
    }

    /**
     * Creates a VLAN ID modification.
     *
     * @param vlanId the VLAN ID to modify to
     * @return a L2 modification
     */
    public static L2ModificationInstruction modVlanId(VlanId vlanId) {
        checkNotNull(vlanId, "VLAN id cannot be null");
        return new L2ModificationInstruction.ModVlanIdInstruction(vlanId);
    }

    /**
     * Creates a VLAN PCP modification.
     *
     * @param vlanPcp the PCP to modify to
     * @return a L2 modification
     */
    public static L2ModificationInstruction modVlanPcp(Byte vlanPcp) {
        checkNotNull(vlanPcp, "VLAN Pcp cannot be null");
        return new L2ModificationInstruction.ModVlanPcpInstruction(vlanPcp);
    }

    /**
     * Creates a MPLS label modification.
     *
     * @param mplsLabel MPLS label to set
     * @return a L2 Modification
     */
    public static L2ModificationInstruction modMplsLabel(MplsLabel mplsLabel) {
        checkNotNull(mplsLabel, "MPLS label cannot be null");
        return new L2ModificationInstruction.ModMplsLabelInstruction(mplsLabel);
    }

    /**
     * Creates a MPLS BOS bit modification.
     *
     * @param mplsBos MPLS BOS bit to set (true) or unset (false)
     * @return a L2 Modification
     */
    public static L2ModificationInstruction modMplsBos(boolean mplsBos) {
        return new L2ModificationInstruction.ModMplsBosInstruction(mplsBos);
    }

    /**
     * Creates a MPLS decrement TTL modification.
     *
     * @return a L2 Modification
     */
    public static L2ModificationInstruction decMplsTtl() {
        return new L2ModificationInstruction.ModMplsTtlInstruction();
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
     * Creates a L3 decrement TTL modification.
     *
     * @return a L3 modification
     */
    public static L3ModificationInstruction decNwTtl() {
        return new ModTtlInstruction(L3SubType.DEC_TTL);
    }

    /**
     * Creates a L3 copy TTL to outer header modification.
     *
     * @return a L3 modification
     */
    public static L3ModificationInstruction copyTtlOut() {
        return new ModTtlInstruction(L3SubType.TTL_OUT);
    }

    /**
     * Creates a L3 copy TTL to inner header modification.
     *
     * @return a L3 modification
     */
    public static L3ModificationInstruction copyTtlIn() {
        return new ModTtlInstruction(L3SubType.TTL_IN);
    }

    /**
     * Creates a push MPLS header instruction.
     *
     * @return a L2 modification.
     */
    public static Instruction pushMpls() {
        return new L2ModificationInstruction.PushHeaderInstructions(
                L2ModificationInstruction.L2SubType.MPLS_PUSH,
                                          EthType.EtherType.MPLS_UNICAST.ethType());
    }

    /**
     * Creates a pop MPLS header instruction.
     *
     * @return a L2 modification.
     */
    public static Instruction popMpls() {
        return new L2ModificationInstruction.PushHeaderInstructions(
                L2ModificationInstruction.L2SubType.MPLS_POP,
                EthType.EtherType.MPLS_UNICAST.ethType());
    }

    /**
     * Creates a pop MPLS header instruction with a particular ethertype.
     *
     * @param etherType Ethernet type to set
     * @return a L2 modification.
     * @deprecated in Cardinal Release
     */
    @Deprecated
    public static Instruction popMpls(int etherType) {
        checkNotNull(etherType, "Ethernet type cannot be null");
        return new L2ModificationInstruction.PushHeaderInstructions(
                L2ModificationInstruction.L2SubType.MPLS_POP, new EthType(etherType));
    }


    /**
     * Creates a pop MPLS header instruction with a particular ethertype.
     *
     * @param etherType Ethernet type to set
     * @return a L2 modification.
     */
    public static Instruction popMpls(EthType etherType) {
        checkNotNull(etherType, "Ethernet type cannot be null");
        return new L2ModificationInstruction.PushHeaderInstructions(
                L2ModificationInstruction.L2SubType.MPLS_POP, etherType);
    }

    /**
     * Creates a pop VLAN header instruction.
     *
     * @return a L2 modification
     */
    public static Instruction popVlan() {
        return new L2ModificationInstruction.PopVlanInstruction(
                L2ModificationInstruction.L2SubType.VLAN_POP);
    }

    /**
     * Creates a push VLAN header instruction.
     *
     * @return a L2 modification
     */
    public static Instruction pushVlan() {
        return new L2ModificationInstruction.PushHeaderInstructions(
                L2ModificationInstruction.L2SubType.VLAN_PUSH,
                EthType.EtherType.VLAN.ethType());
    }

    /**
     * Sends the packet to the table id.
     *
     * @param tableId flow rule table id
     * @return table type transition instruction
     */
    public static Instruction transition(Integer tableId) {
        checkNotNull(tableId, "Table id cannot be null");
        return new TableTypeTransition(tableId);
    }

    /**
     * Writes metadata to associate with a packet.
     *
     * @param metadata the metadata value to write
     * @param metadataMask the bits to mask for the metadata value
     * @return metadata instruction
     */
    public static Instruction writeMetadata(long metadata, long metadataMask) {
        return new MetadataInstruction(metadata, metadataMask);
    }

    /**
     * Creates a Tunnel ID modification.
     *
     * @param tunnelId the Tunnel ID to modify to
     * @return a L2 modification
     */
    public static L2ModificationInstruction modTunnelId(long tunnelId) {
        checkNotNull(tunnelId, "Tunnel id cannot be null");
        return new L2ModificationInstruction.ModTunnelIdInstruction(tunnelId);
    }

    /**
     * Creates a TCP src modification.
     *
     * @param port the TCP port number to modify to
     * @return a L4 modification
     * @deprecated in Drake release
     */
    @Deprecated
    public static L4ModificationInstruction modTcpSrc(short port) {
       checkNotNull(port, "Src TCP port cannot be null");
       return new ModTransportPortInstruction(L4SubType.TCP_SRC, TpPort.tpPort(port));
    }

    /**
     * Creates a TCP src modification.
     *
     * @param port the TCP port number to modify to
     * @return a L4 modification
     */
    public static L4ModificationInstruction modTcpSrc(TpPort port) {
       checkNotNull(port, "Src TCP port cannot be null");
       return new ModTransportPortInstruction(L4SubType.TCP_SRC, port);
    }

    /**
     * Creates a TCP dst modification.
     *
     * @param port the TCP port number to modify to
     * @return a L4 modification
     * @deprecated in Drake release
     */
    @Deprecated
    public static L4ModificationInstruction modTcpDst(short port) {
        checkNotNull(port, "Dst TCP port cannot be null");
        return new ModTransportPortInstruction(L4SubType.TCP_DST, TpPort.tpPort(port));
    }

    /**
     * Creates a TCP dst modification.
     *
     * @param port the TCP port number to modify to
     * @return a L4 modification
     */
    public static L4ModificationInstruction modTcpDst(TpPort port) {
        checkNotNull(port, "Dst TCP port cannot be null");
        return new ModTransportPortInstruction(L4SubType.TCP_DST, port);
    }

    /**
     * Creates a UDP src modification.
     *
     * @param port the UDP port number to modify to
     * @return a L4 modification
     * @deprecated in Drake release
     */
    @Deprecated
    public static L4ModificationInstruction modUdpSrc(short port) {
        checkNotNull(port, "Src UDP port cannot be null");
        return new ModTransportPortInstruction(L4SubType.UDP_SRC, TpPort.tpPort(port));
    }

    /**
     * Creates a UDP src modification.
     *
     * @param port the UDP port number to modify to
     * @return a L4 modification
     */
    public static L4ModificationInstruction modUdpSrc(TpPort port) {
        checkNotNull(port, "Src UDP port cannot be null");
        return new ModTransportPortInstruction(L4SubType.UDP_SRC, port);
    }

    /**
     * Creates a UDP dst modification.
     *
     * @param port the UDP port number to modify to
     * @return a L4 modification
     * @deprecated in Drake release
     */
    @Deprecated
    public static L4ModificationInstruction modUdpDst(short port) {
        checkNotNull(port, "Dst UDP port cannot be null");
        return new ModTransportPortInstruction(L4SubType.UDP_DST, TpPort.tpPort(port));
    }

    /**
     * Creates a UDP dst modification.
     *
     * @param port the UDP port number to modify to
     * @return a L4 modification
     */
    public static L4ModificationInstruction modUdpDst(TpPort port) {
        checkNotNull(port, "Dst UDP port cannot be null");
        return new ModTransportPortInstruction(L4SubType.UDP_DST, port);
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
            return Objects.hash(type().ordinal());
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
            return Objects.hash(type().ordinal(), port);
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
            return Objects.hash(type().ordinal(), groupId);
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

    /**
     * A meter instruction.
     */
    public static final class MeterInstruction implements Instruction {
        private final MeterId meterId;

        private MeterInstruction(MeterId meterId) {
            this.meterId = meterId;
        }

        public MeterId meterId() {
            return meterId;
        }

        @Override
        public Type type() {
            return Type.METER;
        }

        @Override
        public String toString() {
            return toStringHelper(type().toString())
                    .add("meter ID", meterId.id()).toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type().ordinal(), meterId);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof MeterInstruction) {
                MeterInstruction that = (MeterInstruction) obj;
                return Objects.equals(meterId, that.meterId);

            }
            return false;
        }
    }

    /**
     *  Transition instruction.
     */
    public static class TableTypeTransition implements Instruction {
        private final Integer tableId;

        TableTypeTransition(Integer tableId) {
            this.tableId = tableId;
        }

        @Override
        public Type type() {
            return Type.TABLE;
        }

        public Integer tableId() {
            return this.tableId;
        }

        @Override
        public String toString() {
            return toStringHelper(type().toString())
                    .add("tableId", this.tableId).toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type().ordinal(), tableId);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof TableTypeTransition) {
                TableTypeTransition that = (TableTypeTransition) obj;
                return Objects.equals(tableId, that.tableId);

            }
            return false;
        }
    }

    /**
     *  Metadata instruction.
     */
    public static class MetadataInstruction implements Instruction {
        private final long metadata;
        private final long metadataMask;

        MetadataInstruction(long metadata, long metadataMask) {
            this.metadata = metadata;
            this.metadataMask = metadataMask;
        }

        @Override
        public Type type() {
            return Type.METADATA;
        }

        public long metadata() {
            return this.metadata;
        }

        public long metadataMask() {
            return this.metadataMask;
        }

        @Override
        public String toString() {
            return toStringHelper(type().toString())
                    .add("metadata", Long.toHexString(this.metadata))
                    .add("metadata mask", Long.toHexString(this.metadataMask))
                    .toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type().ordinal(), metadata, metadataMask);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof MetadataInstruction) {
                MetadataInstruction that = (MetadataInstruction) obj;
                return Objects.equals(metadata, that.metadata) &&
                        Objects.equals(metadataMask, that.metadataMask);

            }
            return false;
        }
    }

}


