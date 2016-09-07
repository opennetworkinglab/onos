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
package org.onosproject.driver.pipeline;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.DefaultGroupId;
import org.onosproject.driver.extensions.OfdpaSetVlanVid;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.NextGroup;
import org.onosproject.net.behaviour.PipelinerContext;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flowobjective.FlowObjectiveStore;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.group.DefaultGroupBucket;
import org.onosproject.net.group.DefaultGroupDescription;
import org.onosproject.net.group.DefaultGroupKey;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupEvent;
import org.onosproject.net.group.GroupKey;
import org.onosproject.net.group.GroupListener;
import org.onosproject.net.group.GroupService;
import org.onosproject.store.service.AtomicCounter;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Group handler for OFDPA2 pipeline.
 */
public class Ofdpa2GroupHandler {
    /*
     * OFDPA requires group-id's to have a certain form.
     * L2 Interface Groups have <4bits-0><12bits-vlanid><16bits-portid>
     * L3 Unicast Groups have <4bits-2><28bits-index>
     * MPLS Interface Groups have <4bits-9><4bits:0><24bits-index>
     * L3 ECMP Groups have <4bits-7><28bits-index>
     * L2 Flood Groups have <4bits-4><12bits-vlanid><16bits-index>
     * L3 VPN Groups have <4bits-9><4bits-2><24bits-index>
     */
    protected static final int L2_INTERFACE_TYPE = 0x00000000;
    protected static final int L3_INTERFACE_TYPE = 0x50000000;
    protected static final int L3_UNICAST_TYPE = 0x20000000;
    protected static final int L3_MULTICAST_TYPE = 0x60000000;
    protected static final int MPLS_INTERFACE_TYPE = 0x90000000;
    protected static final int MPLS_L3VPN_SUBTYPE = 0x92000000;
    protected static final int L3_ECMP_TYPE = 0x70000000;
    protected static final int L2_FLOOD_TYPE = 0x40000000;

    protected static final int TYPE_MASK = 0x0fffffff;
    protected static final int SUBTYPE_MASK = 0x00ffffff;
    protected static final int TYPE_VLAN_MASK = 0x0000ffff;

    protected static final int PORT_LOWER_BITS_MASK = 0x3f;
    protected static final long PORT_HIGHER_BITS_MASK = ~PORT_LOWER_BITS_MASK;

    private final Logger log = getLogger(getClass());
    private ServiceDirectory serviceDirectory;
    protected GroupService groupService;
    protected StorageService storageService;

    protected DeviceId deviceId;
    private FlowObjectiveStore flowObjectiveStore;
    private Cache<GroupKey, List<OfdpaNextGroup>> pendingAddNextObjectives;
    private Cache<NextObjective, List<GroupKey>> pendingRemoveNextObjectives;
    private ConcurrentHashMap<GroupKey, Set<GroupChainElem>> pendingGroups;
    private ScheduledExecutorService groupChecker =
            Executors.newScheduledThreadPool(2, groupedThreads("onos/pipeliner", "ofdpa2-%d", log));

    // index number for group creation
    private AtomicCounter nextIndex;

    // local store for pending bucketAdds - by design there can only be one
    // pending bucket for a group
    protected ConcurrentHashMap<Integer, NextObjective> pendingBuckets = new ConcurrentHashMap<>();

    protected void init(DeviceId deviceId, PipelinerContext context) {
        this.deviceId = deviceId;
        this.flowObjectiveStore = context.store();
        this.serviceDirectory = context.directory();
        this.groupService = serviceDirectory.get(GroupService.class);
        this.storageService = serviceDirectory.get(StorageService.class);
        this.nextIndex = storageService.getAtomicCounter("group-id-index-counter");

        pendingAddNextObjectives = CacheBuilder.newBuilder()
                .expireAfterWrite(20, TimeUnit.SECONDS)
                .removalListener((
                        RemovalNotification<GroupKey, List<OfdpaNextGroup>> notification) -> {
                    if (notification.getCause() == RemovalCause.EXPIRED) {
                        notification.getValue().forEach(ofdpaNextGrp ->
                                Ofdpa2Pipeline.fail(ofdpaNextGrp.nextObj,
                                        ObjectiveError.GROUPINSTALLATIONFAILED));
                    }
                }).build();

        pendingRemoveNextObjectives = CacheBuilder.newBuilder()
                .expireAfterWrite(20, TimeUnit.SECONDS)
                .removalListener((
                        RemovalNotification<NextObjective, List<GroupKey>> notification) -> {
                    if (notification.getCause() == RemovalCause.EXPIRED) {
                            Ofdpa2Pipeline.fail(notification.getKey(),
                                    ObjectiveError.GROUPREMOVALFAILED);
                    }
                }).build();
        pendingGroups = new ConcurrentHashMap<>();
        groupChecker.scheduleAtFixedRate(new GroupChecker(), 0, 500, TimeUnit.MILLISECONDS);

        groupService.addListener(new InnerGroupListener());
    }

    //////////////////////////////////////
    //  Group Creation
    //////////////////////////////////////

    protected void addGroup(NextObjective nextObjective) {
        switch (nextObjective.type()) {
            case SIMPLE:
                Collection<TrafficTreatment> treatments = nextObjective.next();
                if (treatments.size() != 1) {
                    log.error("Next Objectives of type Simple should only have a "
                                    + "single Traffic Treatment. Next Objective Id:{}",
                            nextObjective.id());
                    Ofdpa2Pipeline.fail(nextObjective, ObjectiveError.BADPARAMS);
                    return;
                }
                processSimpleNextObjective(nextObjective);
                break;
            case BROADCAST:
                processBroadcastNextObjective(nextObjective);
                break;
            case HASHED:
                processHashedNextObjective(nextObjective);
                break;
            case FAILOVER:
                Ofdpa2Pipeline.fail(nextObjective, ObjectiveError.UNSUPPORTED);
                log.warn("Unsupported next objective type {}", nextObjective.type());
                break;
            default:
                Ofdpa2Pipeline.fail(nextObjective, ObjectiveError.UNKNOWN);
                log.warn("Unknown next objective type {}", nextObjective.type());
        }
    }

    /**
     * As per the OFDPA 2.0 TTP, packets are sent out of ports by using
     * a chain of groups. The simple Next Objective passed
     * in by the application has to be broken up into a group chain
     * comprising of an L3 Unicast Group that points to an L2 Interface
     * Group which in-turn points to an output port. In some cases, the simple
     * next Objective can just be an L2 interface without the need for chaining.
     *
     * @param nextObj  the nextObjective of type SIMPLE
     */
    private void processSimpleNextObjective(NextObjective nextObj) {
        TrafficTreatment treatment = nextObj.next().iterator().next();
        // determine if plain L2 or L3->L2
        boolean plainL2 = true;
        for (Instruction ins : treatment.allInstructions()) {
            if (ins.type() == Instruction.Type.L2MODIFICATION) {
                L2ModificationInstruction l2ins = (L2ModificationInstruction) ins;
                if (l2ins.subtype() == L2ModificationInstruction.L2SubType.ETH_DST ||
                        l2ins.subtype() == L2ModificationInstruction.L2SubType.ETH_SRC) {
                    plainL2 = false;
                    break;
                }
            }
        }

        if (plainL2) {
            createL2InterfaceGroup(nextObj);
            return;
        }

        // break up simple next objective to GroupChain objects
        GroupInfo groupInfo = createL2L3Chain(treatment, nextObj.id(),
                                              nextObj.appId(), false,
                                              nextObj.meta());
        if (groupInfo == null) {
            log.error("Could not process nextObj={} in dev:{}", nextObj.id(), deviceId);
            return;
        }
        // create object for local and distributed storage
        Deque<GroupKey> gkeyChain = new ArrayDeque<>();
        gkeyChain.addFirst(groupInfo.innerMostGroupDesc.appCookie());
        gkeyChain.addFirst(groupInfo.nextGroupDesc.appCookie());
        OfdpaNextGroup ofdpaGrp = new OfdpaNextGroup(
                Collections.singletonList(gkeyChain),
                nextObj);

        // store l3groupkey with the ofdpaNextGroup for the nextObjective that depends on it
        updatePendingNextObjective(groupInfo.nextGroupDesc.appCookie(), ofdpaGrp);

        // now we are ready to send the l2 groupDescription (inner), as all the stores
        // that will get async replies have been updated. By waiting to update
        // the stores, we prevent nasty race conditions.
        groupService.addGroup(groupInfo.innerMostGroupDesc);
    }

    /**
     * Creates a simple L2 Interface Group.
     *
     * @param nextObj the next Objective
     */
    private void createL2InterfaceGroup(NextObjective nextObj) {
        VlanId assignedVlan = Ofdpa2Pipeline.readVlanFromSelector(nextObj.meta());
        if (assignedVlan == null) {
            log.warn("VLAN ID required by simple next obj is missing. Abort.");
            Ofdpa2Pipeline.fail(nextObj, ObjectiveError.BADPARAMS);
            return;
        }

        List<GroupInfo> groupInfos = prepareL2InterfaceGroup(nextObj, assignedVlan);

        // There is only one L2 interface group in this case
        GroupDescription l2InterfaceGroupDesc = groupInfos.get(0).innerMostGroupDesc;

        // Put all dependency information into allGroupKeys
        List<Deque<GroupKey>> allGroupKeys = Lists.newArrayList();
        Deque<GroupKey> gkeyChain = new ArrayDeque<>();
        gkeyChain.addFirst(l2InterfaceGroupDesc.appCookie());
        allGroupKeys.add(gkeyChain);

        // Point the next objective to this group
        OfdpaNextGroup ofdpaGrp = new OfdpaNextGroup(allGroupKeys, nextObj);
        updatePendingNextObjective(l2InterfaceGroupDesc.appCookie(), ofdpaGrp);

        // Start installing the inner-most group
        groupService.addGroup(l2InterfaceGroupDesc);
    }

    /**
     * Creates one of two possible group-chains from the treatment
     * passed in. Depending on the MPLS boolean, this method either creates
     * an L3Unicast Group --&gt; L2Interface Group, if mpls is false;
     * or MPLSInterface Group --&gt; L2Interface Group, if mpls is true;
     * The returned 'inner' group description is always the L2 Interface group.
     *
     * @param treatment that needs to be broken up to create the group chain
     * @param nextId of the next objective that needs this group chain
     * @param appId of the application that sent this next objective
     * @param mpls determines if L3Unicast or MPLSInterface group is created
     * @param meta metadata passed in by the application as part of the nextObjective
     * @return GroupInfo containing the GroupDescription of the
     *         L2Interface group(inner) and the GroupDescription of the (outer)
     *         L3Unicast/MPLSInterface group. May return null if there is an
     *         error in processing the chain
     */
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
                        OfdpaSetVlanVid ofdpaSetVlanVid = new OfdpaSetVlanVid(vlanid);
                        outerTtb.extension(ofdpaSetVlanVid, deviceId);
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
                OfdpaSetVlanVid ofdpaSetVlanVid = new OfdpaSetVlanVid(vlanid);
                outerTtb.extension(ofdpaSetVlanVid, deviceId);
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
            outerTtb.group(new DefaultGroupId(l2groupId));
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
            outerTtb.group(new DefaultGroupId(l2groupId));
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
        GroupChainElem gce = new GroupChainElem(outerGrpDesc, 1, false);
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
     * As per the OFDPA 2.0 TTP, packets are sent out of ports by using
     * a chain of groups. The broadcast Next Objective passed in by the application
     * has to be broken up into a group chain comprising of an
     * L2 Flood group whose buckets point to L2 Interface groups.
     *
     * @param nextObj  the nextObjective of type BROADCAST
     */
    private void processBroadcastNextObjective(NextObjective nextObj) {
        VlanId assignedVlan = Ofdpa2Pipeline.readVlanFromSelector(nextObj.meta());
        if (assignedVlan == null) {
            log.warn("VLAN ID required by broadcast next obj is missing. Abort.");
            Ofdpa2Pipeline.fail(nextObj, ObjectiveError.BADPARAMS);
            return;
        }

        List<GroupInfo> groupInfos = prepareL2InterfaceGroup(nextObj, assignedVlan);

        IpPrefix ipDst = Ofdpa2Pipeline.readIpDstFromSelector(nextObj.meta());
        if (ipDst != null) {
            if (ipDst.isMulticast()) {
                createL3MulticastGroup(nextObj, assignedVlan, groupInfos);
            } else {
                log.warn("Broadcast NextObj with non-multicast IP address {}", nextObj);
                Ofdpa2Pipeline.fail(nextObj, ObjectiveError.BADPARAMS);
                return;
            }
        } else {
            createL2FloodGroup(nextObj, assignedVlan, groupInfos);
        }
    }

    private List<GroupInfo> prepareL2InterfaceGroup(NextObjective nextObj, VlanId assignedVlan) {
        ImmutableList.Builder<GroupInfo> groupInfoBuilder = ImmutableList.builder();

        // break up broadcast next objective to multiple groups
        Collection<TrafficTreatment> buckets = nextObj.next();

        // each treatment is converted to an L2 interface group
        for (TrafficTreatment treatment : buckets) {
            TrafficTreatment.Builder newTreatment = DefaultTrafficTreatment.builder();
            PortNumber portNum = null;
            VlanId egressVlan = null;
            // ensure that the only allowed treatments are pop-vlan and output
            for (Instruction ins : treatment.allInstructions()) {
                if (ins.type() == Instruction.Type.L2MODIFICATION) {
                    L2ModificationInstruction l2ins = (L2ModificationInstruction) ins;
                    switch (l2ins.subtype()) {
                        case VLAN_POP:
                            newTreatment.add(l2ins);
                            break;
                        case VLAN_ID:
                            egressVlan = ((L2ModificationInstruction.ModVlanIdInstruction) l2ins).vlanId();
                            break;
                        default:
                            log.debug("action {} not permitted for broadcast nextObj",
                                    l2ins.subtype());
                            break;
                    }
                } else if (ins.type() == Instruction.Type.OUTPUT) {
                    portNum = ((Instructions.OutputInstruction) ins).port();
                    newTreatment.add(ins);
                } else {
                    log.debug("TrafficTreatment of type {} not permitted in " +
                            " broadcast nextObjective", ins.type());
                }
            }

            // assemble info for l2 interface group
            VlanId l2InterfaceGroupVlan =
                    (egressVlan != null && !assignedVlan.equals(egressVlan)) ?
                            egressVlan : assignedVlan;
            int l2gk = l2InterfaceGroupKey(deviceId, l2InterfaceGroupVlan, portNum.toLong());
            final GroupKey l2InterfaceGroupKey =
                    new DefaultGroupKey(Ofdpa2Pipeline.appKryo.serialize(l2gk));
            int l2InterfaceGroupId = L2_INTERFACE_TYPE | (l2InterfaceGroupVlan.toShort() << 16) |
                    (int) portNum.toLong();
            GroupBucket l2InterfaceGroupBucket =
                    DefaultGroupBucket.createIndirectGroupBucket(newTreatment.build());
            GroupDescription l2InterfaceGroupDescription =
                    new DefaultGroupDescription(
                            deviceId,
                            GroupDescription.Type.INDIRECT,
                            new GroupBuckets(Collections.singletonList(
                                    l2InterfaceGroupBucket)),
                            l2InterfaceGroupKey,
                            l2InterfaceGroupId,
                            nextObj.appId());
            log.debug("Trying L2-Interface: device:{} gid:{} gkey:{} nextid:{}",
                    deviceId, Integer.toHexString(l2InterfaceGroupId),
                    l2InterfaceGroupKey, nextObj.id());

            groupInfoBuilder.add(new GroupInfo(l2InterfaceGroupDescription,
                    l2InterfaceGroupDescription));
        }
        return groupInfoBuilder.build();
    }

    private void createL2FloodGroup(NextObjective nextObj, VlanId vlanId, List<GroupInfo> groupInfos) {
        // assemble info for l2 flood group
        // since there can be only one flood group for a vlan, its index is always the same - 0
        Integer l2floodgroupId = L2_FLOOD_TYPE | (vlanId.toShort() << 16);
        int l2floodgk = getNextAvailableIndex();
        final GroupKey l2floodgroupkey = new DefaultGroupKey(Ofdpa2Pipeline.appKryo.serialize(l2floodgk));

        // collection of group buckets pointing to all the l2 interface groups
        List<GroupBucket> l2floodBuckets = Lists.newArrayList();
        groupInfos.forEach(groupInfo -> {
            GroupDescription l2intGrpDesc = groupInfo.nextGroupDesc;
            TrafficTreatment.Builder ttb = DefaultTrafficTreatment.builder();
            ttb.group(new DefaultGroupId(l2intGrpDesc.givenGroupId()));
            GroupBucket abucket = DefaultGroupBucket.createAllGroupBucket(ttb.build());
            l2floodBuckets.add(abucket);
        });
        // create the l2flood group-description to wait for all the
        // l2interface groups to be processed
        GroupDescription l2floodGroupDescription =
                new DefaultGroupDescription(
                        deviceId,
                        GroupDescription.Type.ALL,
                        new GroupBuckets(l2floodBuckets),
                        l2floodgroupkey,
                        l2floodgroupId,
                        nextObj.appId());
        log.debug("Trying L2-Flood: device:{} gid:{} gkey:{} nextid:{}",
                deviceId, Integer.toHexString(l2floodgroupId),
                l2floodgroupkey, nextObj.id());

        // Put all dependency information into allGroupKeys
        List<Deque<GroupKey>> allGroupKeys = Lists.newArrayList();
        groupInfos.forEach(groupInfo -> {
            Deque<GroupKey> gkeyChain = new ArrayDeque<>();
            // In this case we should have L2 interface group only
            gkeyChain.addFirst(groupInfo.nextGroupDesc.appCookie());
            gkeyChain.addFirst(l2floodgroupkey);
            allGroupKeys.add(gkeyChain);
        });

        // Point the next objective to this group
        OfdpaNextGroup ofdpaGrp = new OfdpaNextGroup(allGroupKeys, nextObj);
        updatePendingNextObjective(l2floodgroupkey, ofdpaGrp);

        GroupChainElem gce = new GroupChainElem(l2floodGroupDescription,
                groupInfos.size(), false);
        groupInfos.forEach(groupInfo -> {
            // Point this group to the next group
            updatePendingGroups(groupInfo.nextGroupDesc.appCookie(), gce);
            // Start installing the inner-most group
            groupService.addGroup(groupInfo.innerMostGroupDesc);
        });
    }

    private void createL3MulticastGroup(NextObjective nextObj, VlanId vlanId, List<GroupInfo> groupInfos) {
        List<GroupBucket> l3McastBuckets = new ArrayList<>();
        groupInfos.forEach(groupInfo -> {
            // Points to L3 interface group if there is one.
            // Otherwise points to L2 interface group directly.
            GroupDescription nextGroupDesc = (groupInfo.nextGroupDesc != null) ?
                    groupInfo.nextGroupDesc : groupInfo.innerMostGroupDesc;
            TrafficTreatment.Builder ttb = DefaultTrafficTreatment.builder();
            ttb.group(new DefaultGroupId(nextGroupDesc.givenGroupId()));
            GroupBucket abucket = DefaultGroupBucket.createAllGroupBucket(ttb.build());
            l3McastBuckets.add(abucket);
        });

        int l3MulticastIndex = getNextAvailableIndex();
        int l3MulticastGroupId = L3_MULTICAST_TYPE | vlanId.toShort() << 16 | (TYPE_VLAN_MASK & l3MulticastIndex);
        final GroupKey l3MulticastGroupKey = new DefaultGroupKey(Ofdpa2Pipeline.appKryo.serialize(l3MulticastIndex));

        GroupDescription l3MulticastGroupDesc = new DefaultGroupDescription(deviceId,
                GroupDescription.Type.ALL,
                new GroupBuckets(l3McastBuckets),
                l3MulticastGroupKey,
                l3MulticastGroupId,
                nextObj.appId());

        // Put all dependency information into allGroupKeys
        List<Deque<GroupKey>> allGroupKeys = Lists.newArrayList();
        groupInfos.forEach(groupInfo -> {
            Deque<GroupKey> gkeyChain = new ArrayDeque<>();
            gkeyChain.addFirst(groupInfo.innerMostGroupDesc.appCookie());
            // Add L3 interface group to the chain if there is one.
            if (!groupInfo.nextGroupDesc.equals(groupInfo.innerMostGroupDesc)) {
                gkeyChain.addFirst(groupInfo.nextGroupDesc.appCookie());
            }
            gkeyChain.addFirst(l3MulticastGroupKey);
            allGroupKeys.add(gkeyChain);
        });

        // Point the next objective to this group
        OfdpaNextGroup ofdpaGrp = new OfdpaNextGroup(allGroupKeys, nextObj);
        updatePendingNextObjective(l3MulticastGroupKey, ofdpaGrp);

        GroupChainElem outerGce = new GroupChainElem(l3MulticastGroupDesc,
                groupInfos.size(), false);
        groupInfos.forEach(groupInfo -> {
            // Point this group (L3 multicast) to the next group
            updatePendingGroups(groupInfo.nextGroupDesc.appCookie(), outerGce);

            // Point next group to inner-most group, if any
            if (!groupInfo.nextGroupDesc.equals(groupInfo.innerMostGroupDesc)) {
                GroupChainElem innerGce = new GroupChainElem(groupInfo.nextGroupDesc,
                        1, false);
                updatePendingGroups(groupInfo.innerMostGroupDesc.appCookie(), innerGce);
            }

            // Start installing the inner-most group
            groupService.addGroup(groupInfo.innerMostGroupDesc);
        });
    }

    /**
     * As per the OFDPA 2.0 TTP, packets are sent out of ports by using
     * a chain of groups. The hashed Next Objective passed in by the application
     * has to be broken up into a group chain comprising of an
     * L3 ECMP group as the top level group. Buckets of this group can point
     * to a variety of groups in a group chain, depending on the whether
     * MPLS labels are being pushed or not.
     * <p>
     * NOTE: We do not create MPLS ECMP groups as they are unimplemented in
     *       OF-DPA 2.0 (even though it is in the spec). Therefore we do not
     *       check the nextObjective meta to see what is matching before being
     *       sent to this nextObjective.
     *
     * @param nextObj  the nextObjective of type HASHED
     */
    private void processHashedNextObjective(NextObjective nextObj) {
        // storage for all group keys in the chain of groups created
        List<Deque<GroupKey>> allGroupKeys = new ArrayList<>();
        List<GroupInfo> unsentGroups = new ArrayList<>();
        createHashBucketChains(nextObj, allGroupKeys, unsentGroups);

        // now we can create the outermost L3 ECMP group
        List<GroupBucket> l3ecmpGroupBuckets = new ArrayList<>();
        for (GroupInfo gi : unsentGroups) {
            // create ECMP bucket to point to the outer group
            TrafficTreatment.Builder ttb = DefaultTrafficTreatment.builder();
            ttb.group(new DefaultGroupId(gi.nextGroupDesc.givenGroupId()));
            GroupBucket sbucket = DefaultGroupBucket
                    .createSelectGroupBucket(ttb.build());
            l3ecmpGroupBuckets.add(sbucket);
        }
        int l3ecmpIndex = getNextAvailableIndex();
        int l3ecmpGroupId = L3_ECMP_TYPE | (TYPE_MASK & l3ecmpIndex);
        GroupKey l3ecmpGroupKey = new DefaultGroupKey(
                                          Ofdpa2Pipeline.appKryo.serialize(l3ecmpIndex));
        GroupDescription l3ecmpGroupDesc =
                new DefaultGroupDescription(
                        deviceId,
                        GroupDescription.Type.SELECT,
                        new GroupBuckets(l3ecmpGroupBuckets),
                        l3ecmpGroupKey,
                        l3ecmpGroupId,
                        nextObj.appId());
        GroupChainElem l3ecmpGce = new GroupChainElem(l3ecmpGroupDesc,
                l3ecmpGroupBuckets.size(),
                false);

        // create objects for local and distributed storage
        allGroupKeys.forEach(gkeyChain -> gkeyChain.addFirst(l3ecmpGroupKey));
        OfdpaNextGroup ofdpaGrp = new OfdpaNextGroup(allGroupKeys, nextObj);

        // store l3ecmpGroupKey with the ofdpaGroupChain for the nextObjective
        // that depends on it
        updatePendingNextObjective(l3ecmpGroupKey, ofdpaGrp);

        log.debug("Trying L3ECMP: device:{} gid:{} gkey:{} nextId:{}",
                deviceId, Integer.toHexString(l3ecmpGroupId),
                l3ecmpGroupKey, nextObj.id());
        // finally we are ready to send the innermost groups
        for (GroupInfo gi : unsentGroups) {
            log.debug("Sending innermost group {} in group chain on device {} ",
                    Integer.toHexString(gi.innerMostGroupDesc.givenGroupId()), deviceId);
            updatePendingGroups(gi.nextGroupDesc.appCookie(), l3ecmpGce);
            groupService.addGroup(gi.innerMostGroupDesc);
        }

    }

    /**
     * Creates group chains for all buckets in a hashed group, and stores the
     * GroupInfos and GroupKeys for all the groups in the lists passed in, which
     * should be empty.
     * <p>
     * Does not create the top level ECMP group. Does not actually send the
     * groups to the groupService.
     *
     * @param nextObj  the Next Objective with buckets that need to be converted
     *                  to group chains
     * @param allGroupKeys  a list to store groupKey for each bucket-group-chain
     * @param unsentGroups  a list to store GroupInfo for each bucket-group-chain
     */
    private void createHashBucketChains(NextObjective nextObj,
                                        List<Deque<GroupKey>> allGroupKeys,
                                        List<GroupInfo> unsentGroups) {
        // break up hashed next objective to multiple groups
        Collection<TrafficTreatment> buckets = nextObj.next();

        for (TrafficTreatment bucket : buckets) {
            //figure out how many labels are pushed in each bucket
            int labelsPushed = 0;
            MplsLabel innermostLabel = null;
            for (Instruction ins : bucket.allInstructions()) {
                if (ins.type() == Instruction.Type.L2MODIFICATION) {
                    L2ModificationInstruction l2ins = (L2ModificationInstruction) ins;
                    if (l2ins.subtype() == L2ModificationInstruction.L2SubType.MPLS_PUSH) {
                        labelsPushed++;
                    }
                    if (l2ins.subtype() == L2ModificationInstruction.L2SubType.MPLS_LABEL) {
                        if (innermostLabel == null) {
                            innermostLabel = ((L2ModificationInstruction.ModMplsLabelInstruction) l2ins).label();
                        }
                    }
                }
            }

            Deque<GroupKey> gkeyChain = new ArrayDeque<>();
            // XXX we only deal with 0 and 1 label push right now
            if (labelsPushed == 0) {
                GroupInfo nolabelGroupInfo = createL2L3Chain(bucket, nextObj.id(),
                        nextObj.appId(), false,
                        nextObj.meta());
                if (nolabelGroupInfo == null) {
                    log.error("Could not process nextObj={} in dev:{}",
                            nextObj.id(), deviceId);
                    return;
                }
                gkeyChain.addFirst(nolabelGroupInfo.innerMostGroupDesc.appCookie());
                gkeyChain.addFirst(nolabelGroupInfo.nextGroupDesc.appCookie());

                // we can't send the inner group description yet, as we have to
                // create the dependent ECMP group first. So we store..
                unsentGroups.add(nolabelGroupInfo);

            } else if (labelsPushed == 1) {
                GroupInfo onelabelGroupInfo = createL2L3Chain(bucket, nextObj.id(),
                        nextObj.appId(), true,
                        nextObj.meta());
                if (onelabelGroupInfo == null) {
                    log.error("Could not process nextObj={} in dev:{}",
                            nextObj.id(), deviceId);
                    return;
                }
                // we need to add another group to this chain - the L3VPN group
                TrafficTreatment.Builder l3vpnTtb = DefaultTrafficTreatment.builder();
                l3vpnTtb.pushMpls()
                        .setMpls(innermostLabel)
                        .setMplsBos(true)
                        .copyTtlOut()
                        .group(new DefaultGroupId(
                                onelabelGroupInfo.nextGroupDesc.givenGroupId()));
                GroupBucket l3vpnGrpBkt  =
                        DefaultGroupBucket.createIndirectGroupBucket(l3vpnTtb.build());
                int l3vpnIndex = getNextAvailableIndex();
                int l3vpngroupId = MPLS_L3VPN_SUBTYPE | (SUBTYPE_MASK & l3vpnIndex);
                GroupKey l3vpngroupkey = new DefaultGroupKey(
                             Ofdpa2Pipeline.appKryo.serialize(l3vpnIndex));
                GroupDescription l3vpnGroupDesc =
                        new DefaultGroupDescription(
                                deviceId,
                                GroupDescription.Type.INDIRECT,
                                new GroupBuckets(Collections.singletonList(
                                        l3vpnGrpBkt)),
                                l3vpngroupkey,
                                l3vpngroupId,
                                nextObj.appId());
                GroupChainElem l3vpnGce = new GroupChainElem(l3vpnGroupDesc, 1, false);
                updatePendingGroups(onelabelGroupInfo.nextGroupDesc.appCookie(), l3vpnGce);

                gkeyChain.addFirst(onelabelGroupInfo.innerMostGroupDesc.appCookie());
                gkeyChain.addFirst(onelabelGroupInfo.nextGroupDesc.appCookie());
                gkeyChain.addFirst(l3vpngroupkey);

                //now we can replace the outerGrpDesc with the one we just created
                onelabelGroupInfo.nextGroupDesc = l3vpnGroupDesc;

                // we can't send the innermost group yet, as we have to create
                // the dependent ECMP group first. So we store ...
                unsentGroups.add(onelabelGroupInfo);

                log.debug("Trying L3VPN: device:{} gid:{} gkey:{} nextId:{}",
                        deviceId, Integer.toHexString(l3vpngroupId),
                        l3vpngroupkey, nextObj.id());

            } else {
                log.warn("Driver currently does not handle more than 1 MPLS "
                        + "labels. Not processing nextObjective {}", nextObj.id());
                return;
            }

            // all groups in this chain
            allGroupKeys.add(gkeyChain);
        }
    }

    //////////////////////////////////////
    //  Group Editing
    //////////////////////////////////////

    /**
     *  Adds a bucket to the top level group of a group-chain, and creates the chain.
     *
     * @param nextObjective the next group to add a bucket to
     * @param next the representation of the existing group-chain for this next objective
     */
    protected void addBucketToGroup(NextObjective nextObjective, NextGroup next) {
        if (nextObjective.type() != NextObjective.Type.HASHED) {
            log.warn("AddBuckets not applied to nextType:{} in dev:{} for next:{}",
                    nextObjective.type(), deviceId, nextObjective.id());
            return;
        }
        if (nextObjective.next().size() > 1) {
            log.warn("Only one bucket can be added at a time");
            return;
        }
        // storage for all group keys in the chain of groups created
        List<Deque<GroupKey>> allGroupKeys = new ArrayList<>();
        List<GroupInfo> unsentGroups = new ArrayList<>();
        createHashBucketChains(nextObjective, allGroupKeys, unsentGroups);

        // now we can create the outermost L3 ECMP group bucket to add
        GroupInfo gi = unsentGroups.get(0); // only one bucket, so only one group-chain
        TrafficTreatment.Builder ttb = DefaultTrafficTreatment.builder();
        ttb.group(new DefaultGroupId(gi.nextGroupDesc.givenGroupId()));
        GroupBucket sbucket = DefaultGroupBucket.createSelectGroupBucket(ttb.build());

        // recreate the original L3 ECMP group id and description
        int l3ecmpGroupId = L3_ECMP_TYPE | nextObjective.id() << 12;
        GroupKey l3ecmpGroupKey = new DefaultGroupKey(Ofdpa2Pipeline.appKryo.serialize(l3ecmpGroupId));

        // Although GroupDescriptions are not necessary for adding buckets to
        // existing groups, we use one in the GroupChainElem. When the latter is
        // processed, the info will be extracted for the bucketAdd call to groupService
        GroupDescription l3ecmpGroupDesc =
                new DefaultGroupDescription(
                        deviceId,
                        GroupDescription.Type.SELECT,
                        new GroupBuckets(Collections.singletonList(sbucket)),
                        l3ecmpGroupKey,
                        l3ecmpGroupId,
                        nextObjective.appId());
        GroupChainElem l3ecmpGce = new GroupChainElem(l3ecmpGroupDesc, 1, true);

        // update original NextGroup with new bucket-chain
        // don't need to update pendingNextObjectives -- group already exists
        Deque<GroupKey> newBucketChain = allGroupKeys.get(0);
        newBucketChain.addFirst(l3ecmpGroupKey);
        List<Deque<GroupKey>> allOriginalKeys = Ofdpa2Pipeline.appKryo.deserialize(next.data());
        allOriginalKeys.add(newBucketChain);
        flowObjectiveStore.putNextGroup(nextObjective.id(),
                new OfdpaNextGroup(allOriginalKeys, nextObjective));

        log.debug("Adding to L3ECMP: device:{} gid:{} gkey:{} nextId:{}",
                deviceId, Integer.toHexString(l3ecmpGroupId),
                l3ecmpGroupKey, nextObjective.id());
        // send the innermost group
        log.debug("Sending innermost group {} in group chain on device {} ",
                Integer.toHexString(gi.innerMostGroupDesc.givenGroupId()), deviceId);
        updatePendingGroups(gi.nextGroupDesc.appCookie(), l3ecmpGce);
        groupService.addGroup(gi.innerMostGroupDesc);

    }

    /**
     * Removes the bucket in the top level group of a possible group-chain. Does
     * not remove the groups in a group-chain pointed to by this bucket, as they
     * may be in use (referenced by other groups) elsewhere.
     *
     * @param nextObjective the next group to remove a bucket from
     * @param next the representation of the existing group-chain for this next objective
     */
    protected void removeBucketFromGroup(NextObjective nextObjective, NextGroup next) {
        if (nextObjective.type() != NextObjective.Type.HASHED) {
            log.warn("RemoveBuckets not applied to nextType:{} in dev:{} for next:{}",
                    nextObjective.type(), deviceId, nextObjective.id());
            return;
        }
        Collection<TrafficTreatment> treatments = nextObjective.next();
        TrafficTreatment treatment = treatments.iterator().next();
        // find the bucket to remove by noting the outport, and figuring out the
        // top-level group in the group-chain that indirectly references the port
        PortNumber outport = null;
        for (Instruction ins : treatment.allInstructions()) {
            if (ins instanceof Instructions.OutputInstruction) {
                outport = ((Instructions.OutputInstruction) ins).port();
                break;
            }
        }
        if (outport == null) {
            log.error("next objective {} has no outport", nextObjective.id());
            return;
        }

        List<Deque<GroupKey>> allgkeys = Ofdpa2Pipeline.appKryo.deserialize(next.data());
        Deque<GroupKey> foundChain = null;
        int index = 0;
        for (Deque<GroupKey> gkeys : allgkeys) {
            GroupKey groupWithPort = gkeys.peekLast();
            Group group = groupService.getGroup(deviceId, groupWithPort);
            if (group == null) {
                log.warn("Inconsistent group chain");
                continue;
            }
            // last group in group chain should have a single bucket pointing to port
            List<Instruction> lastIns = group.buckets().buckets().iterator()
                    .next().treatment().allInstructions();
            for (Instruction i : lastIns) {
                if (i instanceof Instructions.OutputInstruction) {
                    PortNumber lastport = ((Instructions.OutputInstruction) i).port();
                    if (lastport.equals(outport)) {
                        foundChain = gkeys;
                        break;
                    }
                }
            }
            if (foundChain != null) {
                break;
            }
            index++;
        }
        if (foundChain != null) {
            //first groupkey is the one we want to modify
            GroupKey modGroupKey = foundChain.peekFirst();
            Group modGroup = groupService.getGroup(deviceId, modGroupKey);
            //second groupkey is the one we wish to remove the reference to
            GroupKey pointedGroupKey = null;
            int i = 0;
            for (GroupKey gk : foundChain) {
                if (i++ == 1) {
                    pointedGroupKey = gk;
                    break;
                }
            }
            Group pointedGroup = groupService.getGroup(deviceId, pointedGroupKey);
            GroupBucket bucket = DefaultGroupBucket.createSelectGroupBucket(
                    DefaultTrafficTreatment.builder()
                            .group(pointedGroup.id())
                            .build());
            GroupBuckets removeBuckets = new GroupBuckets(Collections
                    .singletonList(bucket));
            log.debug("Removing buckets from group id {} for next id {} in device {}",
                    modGroup.id(), nextObjective.id(), deviceId);
            groupService.removeBucketsFromGroup(deviceId, modGroupKey,
                    removeBuckets, modGroupKey,
                    nextObjective.appId());
            //update store
            allgkeys.remove(index);
            flowObjectiveStore.putNextGroup(nextObjective.id(),
                    new OfdpaNextGroup(allgkeys, nextObjective));
        } else {
            log.warn("Could not find appropriate group-chain for removing bucket"
                    + " for next id {} in dev:{}", nextObjective.id(), deviceId);
        }
    }

    /**
     * Removes all groups in multiple possible group-chains that represent the next
     * objective.
     *
     * @param nextObjective the next objective to remove
     * @param next the NextGroup that represents the existing group-chain for
     *             this next objective
     */
    protected void removeGroup(NextObjective nextObjective, NextGroup next) {
        List<Deque<GroupKey>> allgkeys = Ofdpa2Pipeline.appKryo.deserialize(next.data());

        List<GroupKey> groupKeys = allgkeys.stream()
                .map(Deque::getFirst).collect(Collectors.toList());
        pendingRemoveNextObjectives.put(nextObjective, groupKeys);

        allgkeys.forEach(groupChain -> groupChain.forEach(groupKey ->
                groupService.removeGroup(deviceId, groupKey, nextObjective.appId())));
        flowObjectiveStore.removeNextGroup(nextObjective.id());
    }

    //////////////////////////////////////
    //  Helper Methods and Classes
    //////////////////////////////////////

    private void updatePendingNextObjective(GroupKey key, OfdpaNextGroup value) {
        List<OfdpaNextGroup> nextList = new CopyOnWriteArrayList<OfdpaNextGroup>();
        nextList.add(value);
        List<OfdpaNextGroup> ret = pendingAddNextObjectives.asMap()
                .putIfAbsent(key, nextList);
        if (ret != null) {
            ret.add(value);
        }
    }

    protected void updatePendingGroups(GroupKey gkey, GroupChainElem gce) {
        Set<GroupChainElem> gceSet = Collections.newSetFromMap(
                new ConcurrentHashMap<GroupChainElem, Boolean>());
        gceSet.add(gce);
        Set<GroupChainElem> retval = pendingGroups.putIfAbsent(gkey, gceSet);
        if (retval != null) {
            retval.add(gce);
        }
    }

    /**
     * Processes next element of a group chain. Assumption is that if this
     * group points to another group, the latter has already been created
     * and this driver has received notification for it. A second assumption is
     * that if there is another group waiting for this group then the appropriate
     * stores already have the information to act upon the notification for the
     * creation of this group.
     * <p>
     * The processing of the GroupChainElement depends on the number of groups
     * this element is waiting on. For all group types other than SIMPLE, a
     * GroupChainElement could be waiting on multiple groups.
     *
     * @param gce the group chain element to be processed next
     */
    private void processGroupChain(GroupChainElem gce) {
        int waitOnGroups = gce.decrementAndGetGroupsWaitedOn();
        if (waitOnGroups != 0) {
            log.debug("GCE: {} not ready to be processed", gce);
            return;
        }
        log.debug("GCE: {} ready to be processed", gce);
        if (gce.addBucketToGroup) {
            groupService.addBucketsToGroup(gce.groupDescription.deviceId(),
                    gce.groupDescription.appCookie(),
                    gce.groupDescription.buckets(),
                    gce.groupDescription.appCookie(),
                    gce.groupDescription.appId());
        } else {
            groupService.addGroup(gce.groupDescription);
        }
    }

    private class GroupChecker implements Runnable {
        @Override
        public void run() {
            Set<GroupKey> keys = pendingGroups.keySet().stream()
                    .filter(key -> groupService.getGroup(deviceId, key) != null)
                    .collect(Collectors.toSet());
            Set<GroupKey> otherkeys = pendingAddNextObjectives.asMap().keySet().stream()
                    .filter(otherkey -> groupService.getGroup(deviceId, otherkey) != null)
                    .collect(Collectors.toSet());
            keys.addAll(otherkeys);

            keys.forEach(key ->
                    processPendingAddGroupsOrNextObjs(key, false));
        }
    }

    private class InnerGroupListener implements GroupListener {
        @Override
        public void event(GroupEvent event) {
            log.trace("received group event of type {}", event.type());
            switch (event.type()) {
                case GROUP_ADDED:
                    processPendingAddGroupsOrNextObjs(event.subject().appCookie(), true);
                    break;
                case GROUP_REMOVED:
                    processPendingRemoveNextObjs(event.subject().appCookie());
                    break;
                default:
                    break;
            }
        }
    }

    private void processPendingAddGroupsOrNextObjs(GroupKey key, boolean added) {
        //first check for group chain
        Set<GroupChainElem> gceSet = pendingGroups.remove(key);
        if (gceSet != null) {
            for (GroupChainElem gce : gceSet) {
                log.debug("Group service {} group key {} in device {}. "
                                + "Processing next group in group chain with group id 0x{}",
                        (added) ? "ADDED" : "processed",
                        key, deviceId,
                        Integer.toHexString(gce.groupDescription.givenGroupId()));
                processGroupChain(gce);
            }
        } else {
            // otherwise chain complete - check for waiting nextObjectives
            List<OfdpaNextGroup> nextGrpList = pendingAddNextObjectives.getIfPresent(key);
            if (nextGrpList != null) {
                pendingAddNextObjectives.invalidate(key);
                nextGrpList.forEach(nextGrp -> {
                    log.debug("Group service {} group key {} in device:{}. "
                                    + "Done implementing next objective: {} <<-->> gid:0x{}",
                            (added) ? "ADDED" : "processed",
                            key, deviceId, nextGrp.nextObjective().id(),
                            Integer.toHexString(groupService.getGroup(deviceId, key)
                                    .givenGroupId()));
                    Ofdpa2Pipeline.pass(nextGrp.nextObjective());
                    flowObjectiveStore.putNextGroup(nextGrp.nextObjective().id(), nextGrp);
                    // check if addBuckets waiting for this completion
                    NextObjective pendBkt = pendingBuckets
                            .remove(nextGrp.nextObjective().id());
                    if (pendBkt != null) {
                        addBucketToGroup(pendBkt, nextGrp);
                    }
                });
            }
        }
    }

    private void processPendingRemoveNextObjs(GroupKey key) {
        pendingRemoveNextObjectives.asMap().forEach((nextObjective, groupKeys) -> {
            if (groupKeys.isEmpty()) {
                pendingRemoveNextObjectives.invalidate(nextObjective);
                Ofdpa2Pipeline.pass(nextObjective);
            } else {
                groupKeys.remove(key);
            }
        });
    }

    protected int getNextAvailableIndex() {
        return (int) nextIndex.incrementAndGet();
    }

    /**
     * Returns a hash as the L2 Interface Group Key.
     *
     * Keep the lower 6-bit for port since port number usually smaller than 64.
     * Hash other information into remaining 28 bits.
     *
     * @param deviceId Device ID
     * @param vlanId VLAN ID
     * @param portNumber Port number
     * @return L2 interface group key
     */
    protected int l2InterfaceGroupKey(
            DeviceId deviceId, VlanId vlanId, long portNumber) {
        int portLowerBits = (int) portNumber & PORT_LOWER_BITS_MASK;
        long portHigherBits = portNumber & PORT_HIGHER_BITS_MASK;
        int hash = Objects.hash(deviceId, vlanId, portHigherBits);
        return L2_INTERFACE_TYPE | (TYPE_MASK & hash << 6) | portLowerBits;
    }

    /**
     * Utility class for moving group information around.
     */
    protected class GroupInfo {
        /**
         * Description of the inner-most group of the group chain.
         * It is always an L2 interface group.
         */
        private GroupDescription innerMostGroupDesc;

        /**
         * Description of the next group in the group chain.
         * It can be L2 interface, L3 interface, L3 unicast, L3 VPN group.
         * It is possible that nextGroup is the same as the innerMostGroup.
         */
        private GroupDescription nextGroupDesc;

        GroupInfo(GroupDescription innerMostGroupDesc, GroupDescription nextGroupDesc) {
            this.innerMostGroupDesc = innerMostGroupDesc;
            this.nextGroupDesc = nextGroupDesc;
        }
    }

    /**
     * Represents an entire group-chain that implements a Next-Objective from
     * the application. The objective is represented as a list of deques, where
     * each deque is a separate chain of groups.
     * <p>
     * For example, an ECMP group with 3 buckets, where each bucket points to
     * a group chain of L3 Unicast and L2 interface groups will look like this:
     * <ul>
     * <li>List[0] is a Deque of GroupKeyECMP(first)-GroupKeyL3(middle)-GroupKeyL2(last)
     * <li>List[1] is a Deque of GroupKeyECMP(first)-GroupKeyL3(middle)-GroupKeyL2(last)
     * <li>List[2] is a Deque of GroupKeyECMP(first)-GroupKeyL3(middle)-GroupKeyL2(last)
     * </ul>
     * where the first element of each deque is the same, representing the
     * top level ECMP group, while every other element represents a unique groupKey.
     * <p>
     * Also includes information about the next objective that
     * resulted in this group-chain.
     *
     */
    protected class OfdpaNextGroup implements NextGroup {
        private final NextObjective nextObj;
        private final List<Deque<GroupKey>> gkeys;

        public OfdpaNextGroup(List<Deque<GroupKey>> gkeys, NextObjective nextObj) {
            this.gkeys = gkeys;
            this.nextObj = nextObj;
        }

        public List<Deque<GroupKey>> groupKey() {
            return gkeys;
        }

        public NextObjective nextObjective() {
            return nextObj;
        }

        @Override
        public byte[] data() {
            return Ofdpa2Pipeline.appKryo.serialize(gkeys);
        }
    }

    /**
     * Represents a group element that is part of a chain of groups.
     * Stores enough information to create a Group Description to add the group
     * to the switch by requesting the Group Service. Objects instantiating this
     * class are meant to be temporary and live as long as it is needed to wait for
     * preceding groups in the group chain to be created.
     */
    protected class GroupChainElem {
        private GroupDescription groupDescription;
        private AtomicInteger waitOnGroups;
        private boolean addBucketToGroup;

        GroupChainElem(GroupDescription groupDescription, int waitOnGroups,
                boolean addBucketToGroup) {
            this.groupDescription = groupDescription;
            this.waitOnGroups = new AtomicInteger(waitOnGroups);
            this.addBucketToGroup = addBucketToGroup;
        }

        /**
         * This methods atomically decrements the counter for the number of
         * groups this GroupChainElement is waiting on, for notifications from
         * the Group Service. When this method returns a value of 0, this
         * GroupChainElement is ready to be processed.
         *
         * @return integer indication of the number of notifications being waited on
         */
        int decrementAndGetGroupsWaitedOn() {
            return waitOnGroups.decrementAndGet();
        }

        @Override
        public String toString() {
            return (Integer.toHexString(groupDescription.givenGroupId()) +
                    " groupKey: " + groupDescription.appCookie() +
                    " waiting-on-groups: " + waitOnGroups.get() +
                    " addBucketToGroup: " + addBucketToGroup +
                    " device: " + deviceId);
        }
    }
}
