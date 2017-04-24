/*
 * Copyright 2016-present Open Networking Laboratory
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

import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.GroupId;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.group.DefaultGroupBucket;
import org.onosproject.net.group.DefaultGroupDescription;
import org.onosproject.net.group.DefaultGroupKey;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupKey;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import static org.onosproject.driver.pipeline.ofdpa.OfdpaGroupHandlerUtility.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Group handler for CpqD OFDPA pipeline.
 */
public class CpqdOfdpa2GroupHandler extends Ofdpa2GroupHandler {
    private final Logger log = getLogger(getClass());

    @Override
    protected GroupInfo createL2L3Chain(TrafficTreatment treatment, int nextId,
                                        ApplicationId appId, boolean mpls,
                                        TrafficSelector meta) {
        // for the l2interface group, get vlan and port info
        // for the outer group, get the src/dst mac, and vlan info
        TrafficTreatment.Builder outerTtb = DefaultTrafficTreatment.builder();
        TrafficTreatment.Builder innerTtb = DefaultTrafficTreatment.builder();
        VlanId vlanid = null;
        long portNum = 0;
        boolean setVlan = false, popVlan = false;
        MacAddress srcMac = MacAddress.ZERO;
        MacAddress dstMac = MacAddress.ZERO;
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
                        vlanid = ((L2ModificationInstruction.ModVlanIdInstruction) l2ins).vlanId();
                        outerTtb.setVlanId(vlanid);
                        setVlan = true;
                        break;
                    case VLAN_POP:
                        innerTtb.popVlan();
                        popVlan = true;
                        break;
                    case DEC_MPLS_TTL:
                    case MPLS_LABEL:
                    case MPLS_POP:
                    case MPLS_PUSH:
                    case VLAN_PCP:
                    case VLAN_PUSH:
                    default:
                        break;
                }
            } else if (ins.type() == Instruction.Type.OUTPUT) {
                portNum = ((Instructions.OutputInstruction) ins).port().toLong();
                innerTtb.add(ins);
            } else {
                log.warn("Driver does not handle this type of TrafficTreatment"
                                 + " instruction in nextObjectives:  {}", ins.type());
            }
        }

        if (vlanid == null && meta != null) {
            // use metadata if available
            Criterion vidCriterion = meta.getCriterion(Criterion.Type.VLAN_VID);
            if (vidCriterion != null) {
                vlanid = ((VlanIdCriterion) vidCriterion).vlanId();
            }
            // if vlan is not set, use the vlan in metadata for outerTtb
            if (vlanid != null && !setVlan) {
                outerTtb.setVlanId(vlanid);
            }
        }

        if (vlanid == null) {
            log.error("Driver cannot process an L2/L3 group chain without "
                              + "egress vlan information for dev: {} port:{}",
                      deviceId, portNum);
            return null;
        }

        if (!setVlan && !popVlan) {
            // untagged outgoing port
            TrafficTreatment.Builder temp = DefaultTrafficTreatment.builder();
            temp.popVlan();
            innerTtb.build().allInstructions().forEach(i -> temp.add(i));
            innerTtb = temp;
        }

        // assemble information for ofdpa l2interface group
        int l2groupId = L2_INTERFACE_TYPE | (vlanid.toShort() << 16) | (int) portNum;
        // a globally unique groupkey that is different for ports in the same device,
        // but different for the same portnumber on different devices. Also different
        // for the various group-types created out of the same next objective.
        int l2gk = l2InterfaceGroupKey(deviceId, vlanid, portNum);
        final GroupKey l2groupkey = new DefaultGroupKey(Ofdpa2Pipeline.appKryo.serialize(l2gk));

        // assemble information for outer group
        GroupDescription outerGrpDesc = null;
        if (mpls) {
            // outer group is MPLSInteface
            int mplsInterfaceIndex = getNextAvailableIndex();
            int mplsgroupId = MPLS_INTERFACE_TYPE | (SUBTYPE_MASK & mplsInterfaceIndex);
            final GroupKey mplsgroupkey = new DefaultGroupKey(
                    Ofdpa2Pipeline.appKryo.serialize(mplsInterfaceIndex));
            outerTtb.group(new GroupId(l2groupId));
            // create the mpls-interface group description to wait for the
            // l2 interface group to be processed
            GroupBucket mplsinterfaceGroupBucket =
                    DefaultGroupBucket.createIndirectGroupBucket(outerTtb.build());
            outerGrpDesc = new DefaultGroupDescription(
                    deviceId,
                    GroupDescription.Type.INDIRECT,
                    new GroupBuckets(Collections.singletonList(
                            mplsinterfaceGroupBucket)),
                    mplsgroupkey,
                    mplsgroupId,
                    appId);
            log.debug("Trying MPLS-Interface: device:{} gid:{} gkey:{} nextid:{}",
                      deviceId, Integer.toHexString(mplsgroupId),
                      mplsgroupkey, nextId);
        } else {
            // outer group is L3Unicast
            int l3unicastIndex = getNextAvailableIndex();
            int l3groupId = L3_UNICAST_TYPE | (TYPE_MASK & l3unicastIndex);
            final GroupKey l3groupkey = new DefaultGroupKey(
                    Ofdpa2Pipeline.appKryo.serialize(l3unicastIndex));
            outerTtb.group(new GroupId(l2groupId));
            // create the l3unicast group description to wait for the
            // l2 interface group to be processed
            GroupBucket l3unicastGroupBucket =
                    DefaultGroupBucket.createIndirectGroupBucket(outerTtb.build());
            outerGrpDesc = new DefaultGroupDescription(
                    deviceId,
                    GroupDescription.Type.INDIRECT,
                    new GroupBuckets(Collections.singletonList(
                            l3unicastGroupBucket)),
                    l3groupkey,
                    l3groupId,
                    appId);
            log.debug("Trying L3Unicast: device:{} gid:{} gkey:{} nextid:{}",
                      deviceId, Integer.toHexString(l3groupId),
                      l3groupkey, nextId);
        }

        // store l2groupkey with the groupChainElem for the outer-group that depends on it
        GroupChainElem gce = new GroupChainElem(outerGrpDesc,
                                                1,
                                                false,
                                                deviceId);
        updatePendingGroups(l2groupkey, gce);

        // create group description for the inner l2interfacegroup
        GroupBucket l2InterfaceGroupBucket =
                DefaultGroupBucket.createIndirectGroupBucket(innerTtb.build());
        GroupDescription l2groupDescription =
                new DefaultGroupDescription(
                        deviceId,
                        GroupDescription.Type.INDIRECT,
                        new GroupBuckets(Collections.singletonList(
                                l2InterfaceGroupBucket)),
                        l2groupkey,
                        l2groupId,
                        appId);
        log.debug("Trying L2Interface: device:{} gid:{} gkey:{} nextId:{}",
                  deviceId, Integer.toHexString(l2groupId),
                  l2groupkey, nextId);
        return new GroupInfo(l2groupDescription, outerGrpDesc);
    }

    /**
     * In OFDPA2 we do not support the MPLS-ECMP, while we do in
     * CPQD implementation.
     *
     * @param nextObjective the hashed next objective to support.
     */
    @Override
    protected void processHashedNextObjective(NextObjective nextObjective) {
        // The case for MPLS-ECMP. For now, we try to create a MPLS-ECMP for
        // the transport of a VPWS. The necessary info are contained in the
        // meta selector. In particular we are looking for the case of BoS==False;
        TrafficSelector metaSelector = nextObjective.meta();
        if (metaSelector != null && Ofdpa2Pipeline.isNotMplsBos(metaSelector)) {
            // storage for all group keys in the chain of groups created
            List<Deque<GroupKey>> allGroupKeys = new ArrayList<>();
            List<GroupInfo> unsentGroups = new ArrayList<>();
            createHashBucketChains(nextObjective, allGroupKeys, unsentGroups);
            // now we can create the outermost MPLS ECMP group
            List<GroupBucket> mplsEcmpGroupBuckets = new ArrayList<>();
            for (GroupInfo gi : unsentGroups) {
                // create ECMP bucket to point to the outer group
                TrafficTreatment.Builder ttb = DefaultTrafficTreatment.builder();
                ttb.group(new GroupId(gi.nextGroupDesc().givenGroupId()));
                GroupBucket sbucket = DefaultGroupBucket
                        .createSelectGroupBucket(ttb.build());
                mplsEcmpGroupBuckets.add(sbucket);
            }
            int mplsEcmpIndex = getNextAvailableIndex();
            int mplsEcmpGroupId = makeMplsForwardingGroupId(OfdpaMplsGroupSubType.MPLS_ECMP, mplsEcmpIndex);
            GroupKey mplsEmpGroupKey = new DefaultGroupKey(
                    Ofdpa2Pipeline.appKryo.serialize(mplsEcmpIndex)
            );
            GroupDescription mplsEcmpGroupDesc = new DefaultGroupDescription(
                    deviceId,
                    GroupDescription.Type.SELECT,
                    new GroupBuckets(mplsEcmpGroupBuckets),
                    mplsEmpGroupKey,
                    mplsEcmpGroupId,
                    nextObjective.appId()
            );
            GroupChainElem mplsEcmpGce = new GroupChainElem(mplsEcmpGroupDesc,
                                                            mplsEcmpGroupBuckets.size(),
                                                            false,
                                                            deviceId);

            // create objects for local and distributed storage
            allGroupKeys.forEach(gkeyChain -> gkeyChain.addFirst(mplsEmpGroupKey));
            OfdpaNextGroup ofdpaGrp = new OfdpaNextGroup(allGroupKeys, nextObjective);

            // store mplsEcmpGroupKey with the ofdpaGroupChain for the nextObjective
            // that depends on it
            updatePendingNextObjective(mplsEmpGroupKey, ofdpaGrp);

            log.debug("Trying MPLS-ECMP: device:{} gid:{} gkey:{} nextId:{}",
                      deviceId, Integer.toHexString(mplsEcmpGroupId),
                      mplsEmpGroupKey, nextObjective.id());

            // finally we are ready to send the innermost groups
            for (GroupInfo gi : unsentGroups) {
                log.debug("Sending innermost group {} in group chain on device {} ",
                          Integer.toHexString(gi.innerMostGroupDesc().givenGroupId()), deviceId);
                updatePendingGroups(gi.nextGroupDesc().appCookie(), mplsEcmpGce);
                groupService.addGroup(gi.innerMostGroupDesc());
            }
            return;
        }
        super.processHashedNextObjective(nextObjective);
    }
}
