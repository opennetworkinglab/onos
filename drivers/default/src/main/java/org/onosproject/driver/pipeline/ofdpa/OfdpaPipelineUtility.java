/*
 * Copyright 2019-present Open Networking Foundation
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

package org.onosproject.driver.pipeline.ofdpa;

import org.onlab.packet.Ethernet;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.EthTypeCriterion;
import org.onosproject.net.flow.criteria.ExtensionCriterion;
import org.onosproject.net.flow.criteria.ExtensionSelector;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.MplsBosCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.L2SubType;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModTunnelIdInstruction;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveError;

import static org.onosproject.net.behaviour.Pipeliner.ACCUMULATOR_ENABLED;
import static org.onosproject.net.flow.criteria.Criterion.Type.*;
import static org.onosproject.net.flow.instructions.Instruction.Type.L2MODIFICATION;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModVlanIdInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModEtherInstruction;

public final class OfdpaPipelineUtility {

    private OfdpaPipelineUtility() {
        // Utility classes should not have a public or default constructor.
    }

    // Ofdpa specific tables number
    public static final int PORT_TABLE = 0;
    public static final int VLAN_TABLE = 10;
    static final int VLAN_1_TABLE = 11;
    static final int MPLS_L2_PORT_FLOW_TABLE = 13;
    static final int MPLS_L2_PORT_PCP_TRUST_FLOW_TABLE = 16;
    public static final int TMAC_TABLE = 20;
    public static final int UNICAST_ROUTING_TABLE = 30;
    public static final int MULTICAST_ROUTING_TABLE = 40;
    public static final int MPLS_TABLE_0 = 23;
    public static final int MPLS_TABLE_1 = 24;
    public static final int MPLS_L3_TYPE_TABLE = 27;
    public static final int MPLS_TYPE_TABLE = 29;
    public static final int BRIDGING_TABLE = 50;
    public static final int ACL_TABLE = 60;
    static final int EGRESS_VLAN_FLOW_TABLE = 210;
    static final int EGRESS_DSCP_PCP_REMARK_FLOW_TABLE = 230;
    static final int EGRESS_TPID_FLOW_TABLE = 235;
    static final int MAC_LEARNING_TABLE = 254;

    // OF max port number
    static final long OFPP_MAX = 0xffffff00L;

    // Priority values
    static final int HIGHEST_PRIORITY = 0xffff;
    static final int DEFAULT_PRIORITY = 0x8000;
    static final int LOWEST_PRIORITY = 0x0;

    // MPLS L2 table values
    static final int MPLS_L2_PORT_PRIORITY = 2;
    static final int MPLS_TUNNEL_ID_BASE = 0x10000;
    static final int MPLS_TUNNEL_ID_MAX = 0x1FFFF;
    static final int MPLS_UNI_PORT_MAX = 0x0000FFFF;
    static final int MPLS_NNI_PORT_BASE = 0x00020000;
    static final int MPLS_NNI_PORT_MAX = 0x0002FFFF;

    // Egress table values
    static final short ALLOW_VLAN_TRANSLATION = 1;
    static final int COPY_FIELD_NBITS = 12;
    static final int COPY_FIELD_OFFSET = 0;

    // Flow retry values
    static final int MAX_RETRY_ATTEMPTS = 10;
    static final int RETRY_MS = 1000;

    //////////////////////////////
    // Helper and utility methods
    //////////////////////////////

    /**
     * Check whether the accumulator is enabled or not.
     * @param pipeline the pipeline
     * @return true if the accumulator is enabled. Otherwise not
     */
    static boolean isAccumulatorEnabled(Ofdpa2Pipeline pipeline) {
        Driver driver = pipeline.data().driver();
        // we cannot determine the property
        if (driver == null) {
            return false;
        }
        return Boolean.parseBoolean(driver.getProperty(ACCUMULATOR_ENABLED));
    }

    static void pass(Objective obj) {
        obj.context().ifPresent(context -> context.onSuccess(obj));
    }

    static void fail(Objective obj, ObjectiveError error) {
        obj.context().ifPresent(context -> context.onError(obj, error));
    }

    /**
     * Returns true iff the given selector matches on BOS==true, indicating that
     * the selector is trying to match on a label that is bottom-of-stack.
     *
     * @param selector the given match
     * @return true iff BoS==true; false if BOS==false, or BOS matching is not
     *         expressed in the given selector
     */
    static boolean isMplsBos(TrafficSelector selector) {
        MplsBosCriterion bosCriterion = (MplsBosCriterion) selector.getCriterion(MPLS_BOS);
        return bosCriterion != null && bosCriterion.mplsBos();
    }

    /**
     * Returns true iff the given selector matches on BOS==false, indicating
     * that the selector is trying to match on a label that is not the
     * bottom-of-stack label.
     *
     * @param selector the given match
     * @return true iff BoS==false;
     *         false if BOS==true, or BOS matching is not expressed in the given selector
     */
    static boolean isNotMplsBos(TrafficSelector selector) {
        MplsBosCriterion bosCriterion = (MplsBosCriterion) selector.getCriterion(MPLS_BOS);
        return bosCriterion != null && !bosCriterion.mplsBos();
    }

    /**
     * Returns true iff the forwarding objective includes a treatment to pop the
     * MPLS label.
     *
     * @param fwd the given forwarding objective
     * @return true iff mpls pop treatment exists
     */
    static boolean isMplsPop(ForwardingObjective fwd) {
        if (fwd.treatment() != null) {
            for (Instruction instr : fwd.treatment().allInstructions()) {
                if (instr instanceof L2ModificationInstruction
                        && ((L2ModificationInstruction) instr)
                        .subtype() == L2SubType.MPLS_POP) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns true iff the given selector matches on ethtype==ipv6, indicating
     * that the selector is trying to match on ipv6 traffic.
     *
     * @param selector the given match
     * @return true iff ethtype==ipv6; false otherwise
     */
    static boolean isIpv6(TrafficSelector selector) {
        EthTypeCriterion ethTypeCriterion = (EthTypeCriterion) selector.getCriterion(ETH_TYPE);
        return ethTypeCriterion != null && ethTypeCriterion.ethType().toShort() == Ethernet.TYPE_IPV6;
    }

    /**
     * Reads vlan id from selector.
     *
     * @param selector the given match
     * @return the vlan id if found. null otherwise
     */
    static VlanId readVlanFromSelector(TrafficSelector selector) {
        if (selector == null) {
            return null;
        }
        Criterion criterion = selector.getCriterion(Criterion.Type.VLAN_VID);
        return (criterion == null)
                ? null : ((VlanIdCriterion) criterion).vlanId();
    }

    /**
     * Reads eth dst from selector.
     *
     * @param selector the given match
     * @return the eth dst if found. null otherwise
     */
    static MacAddress readEthDstFromSelector(TrafficSelector selector) {
        if (selector == null) {
            return null;
        }
        Criterion criterion = selector.getCriterion(Criterion.Type.ETH_DST);
        return (criterion == null)
                ? null : ((EthCriterion) criterion).mac();
    }

    /**
     * Reads ipv4 dst from selector.
     *
     * @param selector the given match
     * @return the ipv4 dst if found. null otherwise
     */
    static IpPrefix readIpDstFromSelector(TrafficSelector selector) {
        if (selector == null) {
            return null;
        }
        Criterion criterion = selector.getCriterion(Criterion.Type.IPV4_DST);
        return (criterion == null) ? null : ((IPCriterion) criterion).ip();
    }

    /**
     * Reads vlan id from treatment.
     *
     * @param treatment the given actions
     * @return the vlan id if found. null otherwise
     */
    static VlanId readVlanFromTreatment(TrafficTreatment treatment) {
        if (treatment == null) {
            return null;
        }
        for (Instruction i : treatment.allInstructions()) {
            if (i instanceof ModVlanIdInstruction) {
                return ((ModVlanIdInstruction) i).vlanId();
            }
        }
        return null;
    }

    /**
     * Reads eth dst from treatment.
     *
     * @param treatment the given actions
     * @return the eth dst if found. null otherwise
     */
    static MacAddress readEthDstFromTreatment(TrafficTreatment treatment) {
        if (treatment == null) {
            return null;
        }
        for (Instruction i : treatment.allInstructions()) {
            if (i instanceof ModEtherInstruction) {
                ModEtherInstruction modEtherInstruction = (ModEtherInstruction) i;
                if (modEtherInstruction.subtype() == L2SubType.ETH_DST) {
                    return modEtherInstruction.mac();
                }
            }
        }
        return null;
    }

    /**
     * Reads extensions from selector.
     * @param selector the given match
     * @return the extensions if found. null otherwise
     */
    static ExtensionSelector readExtensionFromSelector(TrafficSelector selector) {
        if (selector == null) {
            return null;
        }
        ExtensionCriterion criterion = (ExtensionCriterion) selector.getCriterion(Criterion.Type.EXTENSION);
        return (criterion == null) ? null : criterion.extensionSelector();
    }

    /**
     * Determines if the filtering objective will be used for a pseudowire.
     *
     * @param filteringObjective the filtering objective
     * @return True if objective was created for a pseudowire, false otherwise.
     */
    static boolean isPseudowire(FilteringObjective filteringObjective) {
        if (filteringObjective.meta() != null) {
            TrafficTreatment treatment = filteringObjective.meta();
            for (Instruction instr : treatment.immediate()) {
                if (instr.type().equals(Instruction.Type.L2MODIFICATION)) {

                    L2ModificationInstruction l2Instr = (L2ModificationInstruction) instr;
                    if (l2Instr.subtype().equals(L2SubType.TUNNEL_ID)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Utility function to get the mod tunnel id instruction
     * if present.
     *
     * @param treatment the treatment to analyze
     * @return the mod tunnel id instruction if present,
     * otherwise null
     */
    static ModTunnelIdInstruction getModTunnelIdInstruction(TrafficTreatment treatment) {
        if (treatment == null) {
            return null;
        }
        L2ModificationInstruction l2ModificationInstruction;
        for (Instruction instruction : treatment.allInstructions()) {
            if (instruction.type() == L2MODIFICATION) {
                l2ModificationInstruction = (L2ModificationInstruction) instruction;
                if (l2ModificationInstruction.subtype() == L2SubType.TUNNEL_ID) {
                    return (ModTunnelIdInstruction) l2ModificationInstruction;
                }
            }
        }
        return null;
    }

    /**
     * Utility function to get the output instruction
     * if present.
     *
     * @param treatment the treatment to analyze
     * @return the output instruction if present,
     * otherwise null
     */
    static Instructions.OutputInstruction getOutputInstruction(TrafficTreatment treatment) {
        if (treatment == null) {
            return null;
        }
        for (Instruction instruction : treatment.allInstructions()) {
            if (instruction.type() == Instruction.Type.OUTPUT) {
                return (Instructions.OutputInstruction) instruction;
            }
        }
        return null;
    }

    /**
     * Determines if the filtering objective will be used for double-tagged packets.
     *
     * @param fob Filtering objective
     * @return True if the objective was created for double-tagged packets, false otherwise.
     */
    static boolean isDoubleTagged(FilteringObjective fob) {
        return fob.meta() != null &&
                fob.meta().allInstructions().stream().anyMatch(inst -> inst.type() == L2MODIFICATION
                        && ((L2ModificationInstruction) inst).subtype() == L2SubType.VLAN_POP) &&
                fob.conditions().stream().anyMatch(criterion -> criterion.type() == VLAN_VID) &&
                fob.conditions().stream().anyMatch(criterion -> criterion.type() == INNER_VLAN_VID);
    }

}
