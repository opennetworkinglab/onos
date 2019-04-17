/*
 * Copyright 2016-present Open Networking Foundation
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

import com.google.common.collect.Lists;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.GroupId;
import org.onosproject.driver.extensions.Ofdpa3AllowVlanTranslationType;
import org.onosproject.driver.extensions.Ofdpa3PushCw;
import org.onosproject.driver.extensions.Ofdpa3PushL2Header;
import org.onosproject.driver.extensions.OfdpaSetAllowVlanTranslation;
import org.onosproject.driver.extensions.OfdpaSetVlanVid;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.group.DefaultGroupBucket;
import org.onosproject.net.group.DefaultGroupDescription;
import org.onosproject.net.group.DefaultGroupKey;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupKey;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import static org.onosproject.driver.pipeline.ofdpa.OfdpaGroupHandlerUtility.*;
import static org.onosproject.net.flow.instructions.L3ModificationInstruction.L3SubType.TTL_OUT;
import static org.onosproject.net.group.GroupDescription.Type.INDIRECT;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Group handler for OFDPA2 pipeline.
 */
public class Ofdpa3GroupHandler extends Ofdpa2GroupHandler {

    private static final int PW_INTERNAL_VLAN = 4094;
    private static final int MAX_DEPTH_UNPROTECTED_PW = 3;

    private final Logger log = getLogger(getClass());

    @Override
    protected GroupInfo createL2L3Chain(TrafficTreatment treatment, int nextId,
                                        ApplicationId appId, boolean mpls,
                                        TrafficSelector meta) {
        return isUnfiltered(treatment, meta) ?
                createUnfilteredL2L3Chain(treatment, nextId, appId, false) :
                createL2L3ChainInternal(treatment, nextId, appId, mpls, meta, false);
    }

    @Override
    protected void processPwNextObjective(NextObjective nextObjective) {

        log.info("Started deploying nextObjective id={} for pseudowire", nextObjective.id());

        TrafficTreatment treatment = nextObjective.next().iterator().next();
        Deque<GroupKey> gkeyChain = new ArrayDeque<>();
        GroupChainElem groupChainElem;
        GroupKey groupKey;
        GroupDescription groupDescription;
        // Now we separate the mpls actions from the l2/l3 actions
        TrafficTreatment.Builder l2L3Treatment = DefaultTrafficTreatment.builder();
        TrafficTreatment.Builder mplsTreatment = DefaultTrafficTreatment.builder();
        createL2L3AndMplsTreatments(treatment, l2L3Treatment, mplsTreatment);
        // We create the chain from mpls intf group to
        // l2 intf group.
        GroupInfo groupInfo = createL2L3ChainInternal(
                l2L3Treatment.build(),
                nextObjective.id(),
                nextObjective.appId(),
                true,
                nextObjective.meta(),
                false
        );
        if (groupInfo == null) {
            log.error("Could not process nextObj={} in dev:{}", nextObjective.id(), deviceId);
            OfdpaPipelineUtility.fail(nextObjective, ObjectiveError.GROUPINSTALLATIONFAILED);
            return;
        }
        // We update the chain with the last two groups;
        gkeyChain.addFirst(groupInfo.innerMostGroupDesc().appCookie());
        gkeyChain.addFirst(groupInfo.nextGroupDesc().appCookie());
        // We retrieve also all mpls instructions.
        List<List<Instruction>> mplsInstructionSets = Lists.newArrayList();
        List<Instruction> mplsInstructionSet = Lists.newArrayList();
        L3ModificationInstruction l3Ins;
        for (Instruction ins : treatment.allInstructions()) {
            // Each mpls instruction set is delimited by a
            // copy ttl outward action.
            mplsInstructionSet.add(ins);
            if (ins.type() == Instruction.Type.L3MODIFICATION) {
                l3Ins = (L3ModificationInstruction) ins;
                if (l3Ins.subtype() == TTL_OUT) {
                    mplsInstructionSets.add(mplsInstructionSet);
                    mplsInstructionSet = Lists.newArrayList();
                }
            }
        }

        if (mplsInstructionSets.size() > MAX_DEPTH_UNPROTECTED_PW) {
            log.error("Next Objective for pseudo wire should have at "
                              + "most {} mpls instruction sets. Next Objective Id:{}",
                      MAX_DEPTH_UNPROTECTED_PW, nextObjective.id());
            OfdpaPipelineUtility.fail(nextObjective, ObjectiveError.BADPARAMS);
            return;
        }

        log.debug("Size of mpls instructions is {}.", mplsInstructionSets.size());
        log.debug("mpls instructions sets are {}.", mplsInstructionSets);

        int nextGid = groupInfo.nextGroupDesc().givenGroupId();
        int index;

        // We create the mpls tunnel label groups.
        // In this case we need to use also the
        // tunnel label group 2;
        // this is for inter-co pws
        if (mplsInstructionSets.size() == MAX_DEPTH_UNPROTECTED_PW) {

            log.debug("Creating inter-co pw mpls chains with nextid {}", nextObjective.id());

            // We deal with the label 2 group.
            index = getNextAvailableIndex();
            groupDescription = createMplsTunnelLabelGroup(
                    nextGid,
                    OfdpaMplsGroupSubType.MPLS_TUNNEL_LABEL_2,
                    index,
                    mplsInstructionSets.get(2),
                    nextObjective.appId()
            );
            groupKey = new DefaultGroupKey(
                    Ofdpa2Pipeline.appKryo.serialize(index)
            );
            // We update the chain.
            groupChainElem = new GroupChainElem(groupDescription, 1, false, deviceId);
            updatePendingGroups(
                    groupInfo.nextGroupDesc().appCookie(),
                    groupChainElem
            );
            gkeyChain.addFirst(groupKey);
            // We have to create tunnel label group and
            // l2 vpn group before to send the inner most
            // group. We update the nextGid.
            nextGid = groupDescription.givenGroupId();
            groupInfo = new GroupInfo(groupInfo.innerMostGroupDesc(), groupDescription);

            log.debug("Trying Label 2 Group: device:{} gid:{} gkey:{} nextId:{}",
                      deviceId, Integer.toHexString(nextGid),
                      groupKey, nextObjective.id());
        }

        // if treatment has 2 mpls labels, then this is a pseudowire from leaf to another leaf
        // inside a single co
        if (mplsInstructionSets.size() == 2) {

            log.debug("Creating leaf-leaf pw mpls chains with nextid {}", nextObjective.id());
            // We deal with the label 1 group.
            index = getNextAvailableIndex();
            groupDescription = createMplsTunnelLabelGroup(nextGid,
                                                           OfdpaMplsGroupSubType.MPLS_TUNNEL_LABEL_1,
                                                           index,
                                                           mplsInstructionSets.get(1),
                                                           nextObjective.appId());
            groupKey = new DefaultGroupKey(Ofdpa2Pipeline.appKryo.serialize(index));
            groupChainElem = new GroupChainElem(groupDescription, 1, false, deviceId);
            updatePendingGroups(groupInfo.nextGroupDesc().appCookie(), groupChainElem);
            gkeyChain.addFirst(groupKey);
            // We have to create the l2 vpn group before
            // to send the inner most group.
            nextGid = groupDescription.givenGroupId();
            groupInfo = new GroupInfo(groupInfo.innerMostGroupDesc(), groupDescription);

            log.debug("Trying Label 1 Group: device:{} gid:{} gkey:{} nextId:{}",
                      deviceId, Integer.toHexString(nextGid),
                      groupKey, nextObjective.id());
            // Finally we create the l2 vpn group.
            index = getNextAvailableIndex();
            groupDescription = createMplsL2VpnGroup(nextGid, index,
                                                    mplsInstructionSets.get(0), nextObjective.appId());
            groupKey = new DefaultGroupKey(Ofdpa2Pipeline.appKryo.serialize(index));
            groupChainElem = new GroupChainElem(groupDescription, 1, false, deviceId);
            updatePendingGroups(groupInfo.nextGroupDesc().appCookie(), groupChainElem);
            gkeyChain.addFirst(groupKey);
            OfdpaNextGroup ofdpaGrp = new OfdpaNextGroup(Collections.singletonList(gkeyChain), nextObjective);
            updatePendingNextObjective(groupKey, ofdpaGrp);

            log.debug("Trying L2 Vpn Group: device:{} gid:{} gkey:{} nextId:{}", deviceId,
                      Integer.toHexString(nextGid), groupKey, nextObjective.id());
            // Finally we send the innermost group.
            log.debug("Sending innermost group {} in group chain on device {} ",
                      Integer.toHexString(groupInfo.innerMostGroupDesc().givenGroupId()), deviceId);
            groupService.addGroup(groupInfo.innerMostGroupDesc());
        }

        // this is a pseudowire from leaf to spine,
        // only one label is used
        if (mplsInstructionSets.size() == 1) {

            log.debug("Creating leaf-spine pw mpls chains with nextid {}", nextObjective.id());

            // Finally we create the l2 vpn group.
            index = getNextAvailableIndex();
            groupDescription = createMplsL2VpnGroup(nextGid, index, mplsInstructionSets.get(0),
                                                    nextObjective.appId());
            groupKey = new DefaultGroupKey(Ofdpa2Pipeline.appKryo.serialize(index));
            groupChainElem = new GroupChainElem(groupDescription, 1, false, deviceId);
            updatePendingGroups(groupInfo.nextGroupDesc().appCookie(), groupChainElem);
            gkeyChain.addFirst(groupKey);
            OfdpaNextGroup ofdpaGrp = new OfdpaNextGroup(Collections.singletonList(gkeyChain), nextObjective);
            updatePendingNextObjective(groupKey, ofdpaGrp);

            log.debug("Trying L2 Vpn Group: device:{} gid:{} gkey:{} nextId:{}",
                      deviceId, Integer.toHexString(nextGid), groupKey, nextObjective.id());
            // Finally we send the innermost group.
            log.debug("Sending innermost group {} in group chain on device {} ",
                      Integer.toHexString(groupInfo.innerMostGroupDesc().givenGroupId()), deviceId);
            groupService.addGroup(groupInfo.innerMostGroupDesc());
        }
    }

    /**
     * Helper method to create a mpls tunnel label group.
     *
     * @param nextGroupId the next group in the chain
     * @param subtype the mpls tunnel label group subtype
     * @param index the index of the group
     * @param instructions the instructions to push
     * @param applicationId the application id
     * @return the group description
     */
    private GroupDescription createMplsTunnelLabelGroup(int nextGroupId,
                                                        OfdpaMplsGroupSubType subtype,
                                                        int index,
                                                        List<Instruction> instructions,
                                                        ApplicationId applicationId) {
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        // We add all the instructions.
        instructions.forEach(treatment::add);
        // We point the group to the next group.
        treatment.group(new GroupId(nextGroupId));
        GroupBucket groupBucket = DefaultGroupBucket
                .createIndirectGroupBucket(treatment.build());
        // Finally we build the group description.
        int groupId = makeMplsLabelGroupId(subtype, index);
        GroupKey groupKey = new DefaultGroupKey(
                Ofdpa2Pipeline.appKryo.serialize(index)
        );
        return new DefaultGroupDescription(
                deviceId,
                INDIRECT,
                new GroupBuckets(Collections.singletonList(groupBucket)),
                groupKey,
                groupId,
                applicationId
        );
    }

    /**
     * Helper method to create a mpls l2 vpn group.
     *
     * @param nextGroupId the next group in the chain
     * @param index the index of the group
     * @param instructions the instructions to push
     * @param applicationId the application id
     * @return the group description
     */
    private GroupDescription createMplsL2VpnGroup(int nextGroupId,
                                                  int index,
                                                  List<Instruction> instructions,
                                                  ApplicationId applicationId) {
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        // We add the extensions and the instructions.
        treatment.extension(new Ofdpa3PushL2Header(), deviceId);
        treatment.pushVlan();
        instructions.forEach(treatment::add);
        treatment.extension(new Ofdpa3PushCw(), deviceId);
        // We point the group to the next group.
        treatment.group(new GroupId(nextGroupId));
        GroupBucket groupBucket = DefaultGroupBucket
                .createIndirectGroupBucket(treatment.build());
        // Finally we build the group description.
        int groupId = makeMplsLabelGroupId(OfdpaMplsGroupSubType.L2_VPN, index);
        GroupKey groupKey = new DefaultGroupKey(
                Ofdpa2Pipeline.appKryo.serialize(index)
        );
        return new DefaultGroupDescription(
                deviceId,
                INDIRECT,
                new GroupBuckets(Collections.singletonList(groupBucket)),
                groupKey,
                groupId,
                applicationId
        );
    }

    /**
     * Helper method for dividing the l2/l3 instructions from the mpls
     * instructions.
     *
     * @param treatment the treatment to analyze
     * @param l2L3Treatment the l2/l3 treatment builder
     * @param mplsTreatment the mpls treatment builder
     */
    private void createL2L3AndMplsTreatments(TrafficTreatment treatment,
                                             TrafficTreatment.Builder l2L3Treatment,
                                             TrafficTreatment.Builder mplsTreatment) {

        for (Instruction ins : treatment.allInstructions()) {

            if (ins.type() == Instruction.Type.L2MODIFICATION) {
                L2ModificationInstruction l2ins = (L2ModificationInstruction) ins;
                switch (l2ins.subtype()) {
                    // These instructions have to go in the l2/l3 treatment.
                    case ETH_DST:
                    case ETH_SRC:
                    case VLAN_ID:
                    case VLAN_POP:
                        l2L3Treatment.add(ins);
                        break;
                    // These instructions have to go in the mpls treatment.
                    case MPLS_BOS:
                    case DEC_MPLS_TTL:
                    case MPLS_LABEL:
                    case MPLS_PUSH:
                        mplsTreatment.add(ins);
                        break;
                    default:
                    log.warn("Driver does not handle TrafficTreatment"
                            + " L2Mod {} for pw next-obj", l2ins.subtype());
                        break;
                }
            } else if (ins.type() == Instruction.Type.OUTPUT) {
                // The output goes in the l2/l3 treatment.
                l2L3Treatment.add(ins);
            } else if (ins.type() == Instruction.Type.L3MODIFICATION) {
                 // We support partially the l3 instructions.
                L3ModificationInstruction l3ins = (L3ModificationInstruction) ins;
                switch (l3ins.subtype()) {
                    case TTL_OUT:
                        mplsTreatment.add(ins);
                        break;
                    default:
                    log.warn("Driver does not handle TrafficTreatment"
                            + " L3Mod for pw next-obj", l3ins.subtype());
                }

            } else {
                log.warn("Driver does not handle this type of TrafficTreatment"
                        + " instruction for pw next-obj: {} - {}",
                         ins.type(), ins);
            }
        }
    }
    // TODO Introduce in the future an inner class to return two treatments

    /**
     * Internal implementation of createL2L3Chain for L2 unfiltered interface group.
     *
     * @param treatment that needs to be broken up to create the group chain
     * @param nextId of the next objective that needs this group chain
     * @param appId of the application that sent this next objective
     * @param useSetVlanExtension use the setVlanVid extension that has is_present bit set to 0.
     * @return GroupInfo containing the GroupDescription of the
     *         L2 Unfiltered Interface group(inner) and the GroupDescription of the (outer)
     *         L3Unicast group. May return null if there is an error in processing the chain.
     */
    private GroupInfo createUnfilteredL2L3Chain(TrafficTreatment treatment, int nextId,
                                                  ApplicationId appId, boolean useSetVlanExtension) {
        // for the l2 unfiltered interface group, get port info
        // for the l3 unicast group, get the src/dst mac, and vlan info
        TrafficTreatment.Builder outerTtb = DefaultTrafficTreatment.builder();
        TrafficTreatment.Builder innerTtb = DefaultTrafficTreatment.builder();
        VlanId vlanId;
        long portNum = 0;
        MacAddress srcMac;
        MacAddress dstMac;
        for (Instruction ins : treatment.allInstructions()) {
            if (ins.type() == Instruction.Type.L2MODIFICATION) {
                L2ModificationInstruction l2ins = (L2ModificationInstruction) ins;
                switch (l2ins.subtype()) {
                    case ETH_DST:
                        dstMac = ((L2ModificationInstruction.ModEtherInstruction) l2ins).mac();
                        outerTtb.setEthDst(dstMac);
                        break;
                    case ETH_SRC:
                        srcMac = ((L2ModificationInstruction.ModEtherInstruction) l2ins).mac();
                        outerTtb.setEthSrc(srcMac);
                        break;
                    case VLAN_ID:
                        vlanId = ((L2ModificationInstruction.ModVlanIdInstruction) l2ins).vlanId();
                        if (useSetVlanExtension) {
                            OfdpaSetVlanVid ofdpaSetVlanVid = new OfdpaSetVlanVid(vlanId);
                            outerTtb.extension(ofdpaSetVlanVid, deviceId);
                        } else {
                            outerTtb.setVlanId(vlanId);
                        }
                        break;
                    default:
                        break;
                }
            } else if (ins.type() == Instruction.Type.OUTPUT) {
                portNum = ((Instructions.OutputInstruction) ins).port().toLong();
                innerTtb.add(ins);
            } else {
                log.debug("Driver does not handle this type of TrafficTreatment"
                                  + " instruction in l2l3chain:  {} - {}", ins.type(),
                          ins);
            }
        }

        innerTtb.extension(new OfdpaSetAllowVlanTranslation(
                Ofdpa3AllowVlanTranslationType.ALLOW), deviceId);

        // assemble information for ofdpa l2 unfiltered interface group
        int l2groupId = l2UnfilteredGroupId(portNum);
        // a globally unique groupkey that is different for ports in the same device,
        // but different for the same portnumber on different devices. Also different
        // for the various group-types created out of the same next objective.
        int l2gk = l2UnfilteredGroupKey(deviceId, portNum);
        final GroupKey l2groupkey = new DefaultGroupKey(Ofdpa3Pipeline.appKryo.serialize(l2gk));

        // assemble information for outer group (L3Unicast)
        GroupDescription outerGrpDesc;
        int l3unicastIndex = getNextAvailableIndex();
        int l3groupId = L3_UNICAST_TYPE | (TYPE_MASK & l3unicastIndex);
        final GroupKey l3groupkey = new DefaultGroupKey(
                Ofdpa3Pipeline.appKryo.serialize(l3unicastIndex));
        outerTtb.group(new GroupId(l2groupId));
        // create the l3unicast group description to wait for the
        // l2 unfiltered interface group to be processed
        GroupBucket l3unicastGroupBucket =
                DefaultGroupBucket.createIndirectGroupBucket(outerTtb.build());
        outerGrpDesc = new DefaultGroupDescription(
                deviceId,
                GroupDescription.Type.INDIRECT,
                new GroupBuckets(Collections.singletonList(l3unicastGroupBucket)),
                l3groupkey,
                l3groupId,
                appId);
        log.debug("Trying L3Unicast: device:{} gid:{} gkey:{} nextid:{}",
                  deviceId, Integer.toHexString(l3groupId),
                  l3groupkey, nextId);

        // store l2groupkey with the groupChainElem for the outer-group that depends on it
        GroupChainElem gce = new GroupChainElem(outerGrpDesc, 1, false, deviceId);
        updatePendingGroups(l2groupkey, gce);

        // create group description for the inner l2 unfiltered interface group
        GroupBucket l2InterfaceGroupBucket =
                DefaultGroupBucket.createIndirectGroupBucket(innerTtb.build());
        GroupDescription l2groupDescription =
                new DefaultGroupDescription(deviceId,
                                            GroupDescription.Type.INDIRECT,
                                            new GroupBuckets(Collections.singletonList(l2InterfaceGroupBucket)),
                                            l2groupkey,
                                            l2groupId,
                                            appId);
        log.debug("Trying L2Unfiltered: device:{} gid:{} gkey:{} nextId:{}",
                  deviceId, Integer.toHexString(l2groupId), l2groupkey, nextId);
        return new GroupInfo(l2groupDescription, outerGrpDesc);
    }

}
