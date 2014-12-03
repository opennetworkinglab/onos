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

import java.util.Objects;

import org.onlab.packet.Ethernet;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;

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
        MPLS_POP
    }

    // TODO: Create factory class 'Instructions' that will have various factory
    // to create specific instructions.

    public abstract L2SubType subtype();

    @Override
    public Type type() {
        return Type.L2MODIFICATION;
    }

    /**
     * Represents a L2 src/dst modification instruction.
     */
    public static final class ModEtherInstruction extends L2ModificationInstruction {

        private final L2SubType subtype;
        private final MacAddress mac;

        public ModEtherInstruction(L2SubType subType, MacAddress addr) {

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
                        Objects.equals(this.type(), that.type()) &&
                        Objects.equals(subtype, that.subtype);

            }
            return false;
        }


    }

    public static final class PushHeaderInstructions extends
            L2ModificationInstruction {

        private final L2SubType subtype;
        private final Ethernet ethernetType;

        public PushHeaderInstructions(L2SubType subType, Ethernet ethernetType) {
            this.subtype = subType;
            this.ethernetType = ethernetType;
        }

        public Ethernet ethernetType() {
            return ethernetType;
        }

        @Override
        public L2SubType subtype() {
            return this.subtype;
        }

        @Override
        public String toString() {
            return toStringHelper(subtype().toString()).toString();
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
            if (obj instanceof PushHeaderInstructions) {
                PushHeaderInstructions that = (PushHeaderInstructions) obj;
                return  Objects.equals(this.type(), that.type()) &&
                        Objects.equals(subtype, that.subtype);

            }
            return false;
        }
    }



    /**
     * Represents a VLAN id modification instruction.
     */
    public static final class ModVlanIdInstruction extends L2ModificationInstruction {

        private final VlanId vlanId;

        public ModVlanIdInstruction(VlanId vlanId) {
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
                return  Objects.equals(vlanId, that.vlanId) &&
                        Objects.equals(this.type(), that.type()) &&
                        Objects.equals(this.subtype(), that.subtype());

            }
            return false;
        }


    }

    /**
     * Represents a VLAN PCP modification instruction.
     */
    public static final class ModVlanPcpInstruction extends L2ModificationInstruction {

        private final Byte vlanPcp;

        public ModVlanPcpInstruction(Byte vlanPcp) {
            this.vlanPcp = vlanPcp;
        }

        @Override
        public L2SubType subtype() {
            return L2SubType.VLAN_PCP;
        }

        public Byte vlanPcp() {
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
                return  Objects.equals(vlanPcp, that.vlanPcp) &&
                        Objects.equals(this.type(), that.type()) &&
                        Objects.equals(this.subtype(), that.subtype());

            }
            return false;
        }

    }


    /**
     * Represents a MPLS label modification.
     */
    public static final class ModMplsLabelInstruction extends
    L2ModificationInstruction {

        private final Integer mplsLabel;

        public ModMplsLabelInstruction(Integer mplsLabel) {
            this.mplsLabel = mplsLabel;
        }

        public Integer label() {
            return mplsLabel;
        }

        @Override
        public L2SubType subtype() {
            return L2SubType.MPLS_LABEL;
        }

        @Override
        public String toString() {
            return toStringHelper(type().toString())
                    .add("mpls", mplsLabel.intValue()).toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(mplsLabel);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof ModMplsLabelInstruction) {
                ModMplsLabelInstruction that = (ModMplsLabelInstruction) obj;
                return Objects.equals(mplsLabel, that.mplsLabel) &&
                        Objects.equals(this.type(), that.type());


            }
            return false;
        }
    }
}
