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
import org.onlab.packet.MacAddress;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Abstraction of a single traffic treatment step.
 */
public abstract class L2ModificationInstruction implements Instruction {

    /**
     * Represents the type of traffic treatment.
     */
    public enum L2SubType {
        /**
         * Ether src modification.
         */
        ETH_SRC,

        /**
         * Ether dst modification.
         */
        ETH_DST,

        /**
         * VLAN id modification.
         */
        VLAN_ID,

        /**
         * VLAN priority modification.
         */
        VLAN_PCP,

        /**
         * MPLS Label modification.
         */
        MPLS_LABEL,

        /**
         * MPLS Push modification.
         */
        MPLS_PUSH,

        /**
         * MPLS Pop modification.
         */
        MPLS_POP,

        /**
         * MPLS TTL modification.
         */
        DEC_MPLS_TTL,

        /**
         * VLAN Pop modification.
         */
        VLAN_POP,

        /**
         * VLAN Push modification.
         */
        VLAN_PUSH,

        /**
         * Tunnel id modification.
         */
        TUNNEL_ID,

        /**
         * MPLS BOS instruction.
         */
        MPLS_BOS
    }

    // TODO: Create factory class 'Instructions' that will have various factory
    // to create specific instructions.

    public abstract L2SubType subtype();

    @Override
    public final Type type() {
        return Type.L2MODIFICATION;
    }

    /**
     * Represents a L2 src/dst modification instruction.
     */
    public static final class ModEtherInstruction extends L2ModificationInstruction {

        private final L2SubType subtype;
        private final MacAddress mac;

        ModEtherInstruction(L2SubType subType, MacAddress addr) {

            this.subtype = subType;
            this.mac = addr;
        }

        @Override
        public L2SubType subtype() {
            return this.subtype;
        }

        public MacAddress mac() {
            return this.mac;
        }

        @Override
        public String toString() {
            return toStringHelper(subtype().toString())
                    .add("mac", mac).toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type(), subtype, mac);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof ModEtherInstruction) {
                ModEtherInstruction that = (ModEtherInstruction) obj;
                return  Objects.equals(mac, that.mac) &&
                        Objects.equals(subtype, that.subtype);
            }
            return false;
        }
    }

    // TODO This instruction is reused for Pop-Mpls. Consider renaming.
    public static final class PushHeaderInstructions extends
            L2ModificationInstruction {


        private final L2SubType subtype;
        private final EthType ethernetType; // Ethernet type value: 16 bits

        PushHeaderInstructions(L2SubType subType, EthType ethernetType) {
            this.subtype = subType;
            this.ethernetType = ethernetType;
        }

        public EthType ethernetType() {
            return ethernetType;
        }

        @Override
        public L2SubType subtype() {
            return this.subtype;
        }

        @Override
        public String toString() {
            return toStringHelper(subtype().toString())
                    .add("ethernetType", ethernetType())
                    .toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type(), subtype, ethernetType);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof PushHeaderInstructions) {
                PushHeaderInstructions that = (PushHeaderInstructions) obj;
                return  Objects.equals(subtype, that.subtype) &&
                        Objects.equals(this.ethernetType, that.ethernetType);
            }
            return false;
        }
    }



    /**
     * Represents a VLAN id modification instruction.
     */
    public static final class ModVlanIdInstruction extends L2ModificationInstruction {

        private final VlanId vlanId;

        ModVlanIdInstruction(VlanId vlanId) {
            this.vlanId = vlanId;
        }

        @Override
        public L2SubType subtype() {
            return L2SubType.VLAN_ID;
        }

        public VlanId vlanId() {
            return this.vlanId;
        }

        @Override
        public String toString() {
            return toStringHelper(subtype().toString())
                    .add("id", vlanId).toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type(), subtype(), vlanId);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof ModVlanIdInstruction) {
                ModVlanIdInstruction that = (ModVlanIdInstruction) obj;
                return  Objects.equals(vlanId, that.vlanId);
            }
            return false;
        }
    }

    /**
     * Represents a VLAN PCP modification instruction.
     */
    public static final class ModVlanPcpInstruction extends L2ModificationInstruction {

        private static final byte MASK = 0x7;
        private final byte vlanPcp;

        ModVlanPcpInstruction(byte vlanPcp) {
            this.vlanPcp = (byte) (vlanPcp & MASK);
        }

        @Override
        public L2SubType subtype() {
            return L2SubType.VLAN_PCP;
        }

        public byte vlanPcp() {
            return this.vlanPcp;
        }

        @Override
        public String toString() {
            return toStringHelper(subtype().toString())
                    .add("pcp", Long.toHexString(vlanPcp)).toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type(), subtype(), vlanPcp);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof ModVlanPcpInstruction) {
                ModVlanPcpInstruction that = (ModVlanPcpInstruction) obj;
                return  Objects.equals(vlanPcp, that.vlanPcp);
            }
            return false;
        }
    }

    /**
     * Represents a VLAN POP modification instruction.
     */
    public static final class PopVlanInstruction extends L2ModificationInstruction {
        private final L2SubType subtype;

        PopVlanInstruction(L2SubType subType) {
            this.subtype = subType;
        }

        @Override
        public L2SubType subtype() {
            return subtype;
        }

        @Override
        public String toString() {
            return toStringHelper(subtype().toString())
                    .toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type(), subtype);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof PopVlanInstruction) {
                PopVlanInstruction that = (PopVlanInstruction) obj;
                return  Objects.equals(subtype, that.subtype);
            }
            return false;
        }
    }

    /**
     * Represents a MPLS label modification.
     */
    public static final class ModMplsLabelInstruction
            extends L2ModificationInstruction {

        private final MplsLabel mplsLabel;

        ModMplsLabelInstruction(MplsLabel mplsLabel) {
            this.mplsLabel = mplsLabel;
        }

        /**
         * @deprecated in Drake Release.
         * @return integer value of label
         */
        // Consider changing return value to MplsLabel
        // after deprecation process so that it'll be symmetric to
        // MplsCriterion#label()
        @Deprecated
        public Integer label() {
            return mplsLabel.toInt();
        }

        public MplsLabel mplsLabel() {
            return mplsLabel;
        }

        @Override
        public L2SubType subtype() {
            return L2SubType.MPLS_LABEL;
        }

        @Override
        public String toString() {
            return toStringHelper(subtype().toString())
                    .add("mpls", mplsLabel).toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type(), subtype(), mplsLabel);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof ModMplsLabelInstruction) {
                ModMplsLabelInstruction that = (ModMplsLabelInstruction) obj;
                return Objects.equals(mplsLabel, that.mplsLabel);
            }
            return false;
        }
    }

    /**
     * Represents a MPLS BOS modification.
     */
    public static final class ModMplsBosInstruction
            extends L2ModificationInstruction {

        private final boolean mplsBos;

        ModMplsBosInstruction(boolean mplsBos) {
            this.mplsBos = mplsBos;
        }

        public boolean mplsBos() {
            return mplsBos;
        }

        @Override
        public L2SubType subtype() {
            return L2SubType.MPLS_BOS;
        }

        @Override
        public String toString() {
            return toStringHelper(subtype().toString()).add("bos", mplsBos)
                    .toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type(), subtype(), mplsBos);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof ModMplsBosInstruction) {
                ModMplsBosInstruction that = (ModMplsBosInstruction) obj;
                return Objects.equals(mplsBos, that.mplsBos());
            }
            return false;
        }
    }

    /**
     * Represents a MPLS TTL modification.
     */
    public static final class ModMplsTtlInstruction
            extends L2ModificationInstruction {

        ModMplsTtlInstruction() {
        }

        @Override
        public L2SubType subtype() {
            return L2SubType.DEC_MPLS_TTL;
        }

        @Override
        public String toString() {
            return toStringHelper(subtype().toString())
                    .toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type(), subtype());
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof ModMplsTtlInstruction) {
                return true;
            }
            return false;
        }
    }

    /**
     * Represents a Tunnel id modification.
     */
    public static final class ModTunnelIdInstruction
            extends L2ModificationInstruction {

        private final long tunnelId;

        ModTunnelIdInstruction(long tunnelId) {
            this.tunnelId = tunnelId;
        }

        public long tunnelId() {
            return this.tunnelId;
        }

        @Override
        public L2SubType subtype() {
            return L2SubType.TUNNEL_ID;
        }

        @Override
        public String toString() {
            return toStringHelper(subtype().toString())
                    .add("id", Long.toHexString(tunnelId))
                    .toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type(), subtype(), tunnelId);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof ModTunnelIdInstruction) {
                ModTunnelIdInstruction that = (ModTunnelIdInstruction) obj;
                return  Objects.equals(tunnelId, that.tunnelId);
            }
            return false;
        }
    }
}
