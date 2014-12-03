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

import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.instructions.L0ModificationInstruction.L0SubType;
import org.onosproject.net.flow.instructions.L0ModificationInstruction.ModLambdaInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.L2SubType;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModEtherInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction.L3SubType;
import org.onosproject.net.flow.instructions.L3ModificationInstruction.ModIPInstruction;

import org.onlab.packet.Ethernet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
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
     * Creates a MPLS label modification.
     * @param mplsLabel to set.
     * @return a L2 Modification
     */
    public static L2ModificationInstruction modMplsLabel(Integer mplsLabel) {
        checkNotNull(mplsLabel, "MPLS label cannot be null");
        return new ModMplsLabelInstruction(mplsLabel);
    }
    /**
     * Creates a L3 src modification.
     * @param addr the ip address to modify to.
     * @return a L3 modification
     */
    public static L3ModificationInstruction modL3Src(IpAddress addr) {
        checkNotNull(addr, "Src l3 address cannot be null");
        return new ModIPInstruction(L3SubType.IP_SRC, addr);
    }

    /**
     * Creates a L3 dst modification.
     * @param addr the ip address to modify to.
     * @return a L3 modification
     */
    public static L3ModificationInstruction modL3Dst(IpAddress addr) {
        checkNotNull(addr, "Dst l3 address cannot be null");
        return new ModIPInstruction(L3SubType.IP_DST, addr);
    }

    /**
     * Creates a mpls header instruction.
     * @return a L2 modification.
     */
    public static Instruction pushMpls() {
        return new PushHeaderInstructions(L2SubType.MPLS_PUSH,
                                          new Ethernet().setEtherType(Ethernet.MPLS_UNICAST));
    }

    /**
     * Creates a mpls header instruction.
     * @return a L2 modification.
     */
    public static Instruction popMpls() {
        return new PushHeaderInstructions(L2SubType.MPLS_POP,
                                          new Ethernet().setEtherType(Ethernet.MPLS_UNICAST));
    }

    /*
     *  Output instructions
     */

    public static final class DropInstruction implements Instruction {
        @Override
        public Type type() {
            return Type.DROP;
        }

        @Override
        public String toString() {
            return toStringHelper(type()).toString();

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
                DropInstruction that = (DropInstruction) obj;
                return Objects.equals(type(), that.type());

            }
            return false;
        }
    }


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

}


