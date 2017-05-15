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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.GroupId;
import org.onosproject.driver.extensions.OfdpaSetVlanVid;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.NextGroup;
import org.onosproject.net.behaviour.PipelinerContext;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.TunnelIdCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flowobjective.DefaultNextObjective;
import org.onosproject.net.flowobjective.FlowObjectiveStore;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.Objective.Operation;
import org.onosproject.net.flowobjective.ObjectiveContext;
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
import java.util.stream.Collectors;

import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.driver.pipeline.ofdpa.Ofdpa2Pipeline.*;
import static org.onosproject.driver.pipeline.ofdpa.OfdpaGroupHandlerUtility.*;
import static org.onosproject.net.flow.criteria.Criterion.Type.TUNNEL_ID;
import static org.onosproject.net.flow.criteria.Criterion.Type.VLAN_VID;
import static org.onosproject.net.group.GroupDescription.Type.ALL;
import static org.onosproject.net.group.GroupDescription.Type.SELECT;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Group handler that emulates Broadcom OF-DPA TTP.
 */
public class Ofdpa2GroupHandler {
    protected final Logger log = getLogger(getClass());

    // Services, Stores
    protected GroupService groupService;
    protected StorageService storageService;
    protected FlowObjectiveStore flowObjectiveStore;

    // index number for group creation
    private AtomicCounter nextIndex;

    protected DeviceId deviceId;
    private Cache<GroupKey, List<OfdpaGroupHandlerUtility.OfdpaNextGroup>> pendingAddNextObjectives;
    private Cache<NextObjective, List<GroupKey>> pendingRemoveNextObjectives;
    private Cache<GroupKey, Set<OfdpaGroupHandlerUtility.GroupChainElem>> pendingGroups;
    private ConcurrentHashMap<GroupKey, Set<NextObjective>> pendingUpdateNextObjectives;

    // local store for pending bucketAdds - by design there can be multiple
    // pending bucket for a group
    protected ConcurrentHashMap<Integer, Set<NextObjective>> pendingBuckets =
            new ConcurrentHashMap<>();

    private ScheduledExecutorService groupCheckerExecutor =
            Executors.newScheduledThreadPool(2, groupedThreads("onos/pipeliner", "ofdpa-%d", log));

    public Cache<GroupKey, List<OfdpaNextGroup>> pendingAddNextObjectives() {
        return pendingAddNextObjectives;
    }

    public Cache<GroupKey, Set<GroupChainElem>> pendingGroups() {
        return pendingGroups;
    }

    /**
     * Determines whether this pipeline support copy ttl instructions or not.
     *
     * @return true if copy ttl instructions are supported
     */
    protected boolean supportCopyTtl() {
        return true;
    }

    /**
     * Determines whether this pipeline support set mpls bos instruction or not.
     *
     * @return true if set mpls bos instruction is supported
     */
    protected boolean supportSetMplsBos() {
        return true;
    }

    /**
     * Determines whether this pipeline requires popping VLAN before pushing MPLS.
     * <p>
     * If required, pop vlan before push mpls and add an arbitrary vlan back afterward.
     * MPLS interface group will substitute the arbitrary VLAN with expected VLAN later on.
     *
     * @return true if this pipeline requires popping VLAN before pushing MPLS
     */
    protected boolean requireVlanPopBeforeMplsPush() {
        return false;
    }

    protected void init(DeviceId deviceId, PipelinerContext context) {
        ServiceDirectory serviceDirectory = context.directory();
        this.deviceId = deviceId;
        this.flowObjectiveStore = context.store();
        this.groupService = serviceDirectory.get(GroupService.class);
        this.storageService = serviceDirectory.get(StorageService.class);
        this.nextIndex = storageService.getAtomicCounter("group-id-index-counter");

        pendingAddNextObjectives = CacheBuilder.newBuilder()
                .expireAfterWrite(20, TimeUnit.SECONDS)
                .removalListener((RemovalNotification<GroupKey, List<OfdpaNextGroup>> notification) -> {
                    if (notification.getCause() == RemovalCause.EXPIRED &&
                            Objects.nonNull(notification.getValue())) {
                        notification.getValue()
                                .forEach(ofdpaNextGrp ->
                                                 fail(ofdpaNextGrp.nextObjective(),
                                                      ObjectiveError.GROUPINSTALLATIONFAILED));
                    }
                }).build();

        pendingRemoveNextObjectives = CacheBuilder.newBuilder()
                .expireAfterWrite(20, TimeUnit.SECONDS)
                .removalListener((RemovalNotification<NextObjective, List<GroupKey>> notification) -> {
                    if (notification.getCause() == RemovalCause.EXPIRED) {
                        fail(notification.getKey(),
                                            ObjectiveError.GROUPREMOVALFAILED);
                    }
                }).build();
        pendingGroups = CacheBuilder.newBuilder()
                .expireAfterWrite(20, TimeUnit.SECONDS)
                .removalListener((RemovalNotification<GroupKey, Set<GroupChainElem>> notification) -> {
                    if (notification.getCause() == RemovalCause.EXPIRED) {
                        log.error("Unable to install group with key {} and pending GCEs: {}",
                                  notification.getKey(), notification.getValue());
                    }
                }).build();
        pendingUpdateNextObjectives = new ConcurrentHashMap<>();
        GroupChecker groupChecker = new GroupChecker(this);
        groupCheckerExecutor.scheduleAtFixedRate(groupChecker, 0, 500, TimeUnit.MILLISECONDS);
        groupService.addListener(new InnerGroupListener());
    }

    //////////////////////////////////////
    //  Group Creation
    //////////////////////////////////////

    /**
     * Adds a list of group chain by given NextObjective.
     *
     * @param nextObjective the NextObjective
     */
    protected void addGroup(NextObjective nextObjective) {
        switch (nextObjective.type()) {
            case SIMPLE:
                Collection<TrafficTreatment> treatments = nextObjective.next();
                if (treatments.size() != 1) {
                    log.error("Next Objectives of type Simple should only have a "
                                    + "single Traffic Treatment. Next Objective Id:{}",
                            nextObjective.id());
                    fail(nextObjective, ObjectiveError.BADPARAMS);
                    return;
                }
                processSimpleNextObjective(nextObjective);
                break;
            case BROADCAST:
                processBroadcastNextObjective(nextObjective);
                break;
            case HASHED:
                if (!verifyHashedNextObjective(nextObjective)) {
                    log.error("Next Objectives of type hashed not supported. Next Objective Id:{}",
                              nextObjective.id());
                    fail(nextObjective, ObjectiveError.BADPARAMS);
                    return;
                }
                processHashedNextObjective(nextObjective);
                break;
            case FAILOVER:
                fail(nextObjective, ObjectiveError.UNSUPPORTED);
                log.warn("Unsupported next objective type {}", nextObjective.type());
                break;
            default:
                fail(nextObjective, ObjectiveError.UNKNOWN);
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

        boolean isMpls = false;
        // In order to understand if it is a pseudo wire related
        // next objective we look for the tunnel id in the meta.
        boolean isPw = false;
        if (nextObj.meta() != null) {
            isMpls = isNotMplsBos(nextObj.meta());

            TunnelIdCriterion tunnelIdCriterion = (TunnelIdCriterion) nextObj
                    .meta()
                    .getCriterion(TUNNEL_ID);
            if (tunnelIdCriterion != null) {
                isPw = true;
            }

        }

        if (!isPw) {
            // break up simple next objective to GroupChain objects
            GroupInfo groupInfo = createL2L3Chain(treatment, nextObj.id(),
                                                  nextObj.appId(), isMpls,
                                                  nextObj.meta());
            if (groupInfo == null) {
                log.error("Could not process nextObj={} in dev:{}", nextObj.id(), deviceId);
                return;
            }
            // create object for local and distributed storage
            Deque<GroupKey> gkeyChain = new ArrayDeque<>();
            gkeyChain.addFirst(groupInfo.innerMostGroupDesc().appCookie());
            gkeyChain.addFirst(groupInfo.nextGroupDesc().appCookie());
            OfdpaNextGroup ofdpaGrp =
                    new OfdpaNextGroup(Collections.singletonList(gkeyChain), nextObj);

            // store l3groupkey with the ofdpaNextGroup for the nextObjective that depends on it
            updatePendingNextObjective(groupInfo.nextGroupDesc().appCookie(), ofdpaGrp);

            // now we are ready to send the l2 groupDescription (inner), as all the stores
            // that will get async replies have been updated. By waiting to update
            // the stores, we prevent nasty race conditions.
            groupService.addGroup(groupInfo.innerMostGroupDesc());
        } else {
            // We handle the pseudo wire with a different a procedure.
            // This procedure is meant to handle both initiation and
            // termination of the pseudo wire.
            processPwNextObjective(nextObj);
        }
    }

    /**
     * Creates a simple L2 Interface Group.
     *
     * @param nextObj the next Objective
     */
    private void createL2InterfaceGroup(NextObjective nextObj) {
        VlanId assignedVlan = readVlanFromSelector(nextObj.meta());
        if (assignedVlan == null) {
            log.warn("VLAN ID required by simple next obj is missing. Abort.");
            fail(nextObj, ObjectiveError.BADPARAMS);
            return;
        }

        List<GroupInfo> groupInfos = prepareL2InterfaceGroup(nextObj, assignedVlan);

        // There is only one L2 interface group in this case
        GroupDescription l2InterfaceGroupDesc = groupInfos.get(0).innerMostGroupDesc();

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
        return createL2L3ChainInternal(treatment, nextId, appId, mpls, meta, true);
    }

    /**
     * Internal implementation of createL2L3Chain.
     * <p>
     * The is_present bit in set_vlan_vid action is required to be 0 in OFDPA i12.
     * Since it is non-OF spec, we need an extension treatment for that.
     * The useSetVlanExtension must be set to false for OFDPA i12.
     * </p>
     *
     * @param treatment that needs to be broken up to create the group chain
     * @param nextId of the next objective that needs this group chain
     * @param appId of the application that sent this next objective
     * @param mpls determines if L3Unicast or MPLSInterface group is created
     * @param meta metadata passed in by the application as part of the nextObjective
     * @param useSetVlanExtension use the setVlanVid extension that has is_present bit set to 0.
     * @return GroupInfo containing the GroupDescription of the
     *         L2Interface group(inner) and the GroupDescription of the (outer)
     *         L3Unicast/MPLSInterface group. May return null if there is an
     *         error in processing the chain
     */
    protected GroupInfo createL2L3ChainInternal(TrafficTreatment treatment, int nextId,
                                                ApplicationId appId, boolean mpls,
                                                TrafficSelector meta, boolean useSetVlanExtension) {
        // for the l2interface group, get vlan and port info
        // for the outer group, get the src/dst mac, and vlan info
        TrafficTreatment.Builder outerTtb = DefaultTrafficTreatment.builder();
        TrafficTreatment.Builder innerTtb = DefaultTrafficTreatment.builder();
        VlanId vlanid = null;
        long portNum = 0;
        boolean setVlan = false, popVlan = false;
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
                        vlanid = ((L2ModificationInstruction.ModVlanIdInstruction) l2ins).vlanId();
                        if (useSetVlanExtension) {
                            OfdpaSetVlanVid ofdpaSetVlanVid = new OfdpaSetVlanVid(vlanid);
                            outerTtb.extension(ofdpaSetVlanVid, deviceId);
                        } else {
                            outerTtb.setVlanId(vlanid);
                        }
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
            Criterion vidCriterion = meta.getCriterion(VLAN_VID);
            if (vidCriterion != null) {
                vlanid = ((VlanIdCriterion) vidCriterion).vlanId();
            }
            // if vlan is not set, use the vlan in metadata for outerTtb
            if (vlanid != null && !setVlan) {
                if (useSetVlanExtension) {
                    OfdpaSetVlanVid ofdpaSetVlanVid = new OfdpaSetVlanVid(vlanid);
                    outerTtb.extension(ofdpaSetVlanVid, deviceId);
                } else {
                    outerTtb.setVlanId(vlanid);
                }
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
            innerTtb.build().allInstructions().forEach(temp::add);
            innerTtb = temp;
        }

        // assemble information for ofdpa l2interface group
        int l2groupId = l2GroupId(vlanid, portNum);
        // a globally unique groupkey that is different for ports in the same device,
        // but different for the same portnumber on different devices. Also different
        // for the various group-types created out of the same next objective.
        int l2gk = l2InterfaceGroupKey(deviceId, vlanid, portNum);
        final GroupKey l2groupkey = new DefaultGroupKey(appKryo.serialize(l2gk));

        // assemble information for outer group
        GroupDescription outerGrpDesc;
        if (mpls) {
            // outer group is MPLS Interface
            int mplsInterfaceIndex = getNextAvailableIndex();
            int mplsGroupId = MPLS_INTERFACE_TYPE | (SUBTYPE_MASK & mplsInterfaceIndex);
            final GroupKey mplsGroupKey = new DefaultGroupKey(
                               appKryo.serialize(mplsInterfaceIndex));
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
                    mplsGroupKey,
                    mplsGroupId,
                    appId);
            log.debug("Trying MPLS-Interface: device:{} gid:{} gkey:{} nextid:{}",
                    deviceId, Integer.toHexString(mplsGroupId),
                    mplsGroupKey, nextId);
        } else {
            // outer group is L3Unicast
            int l3unicastIndex = getNextAvailableIndex();
            int l3groupId = L3_UNICAST_TYPE | (TYPE_MASK & l3unicastIndex);
            final GroupKey l3groupkey = new DefaultGroupKey(
                               appKryo.serialize(l3unicastIndex));
            outerTtb.group(new GroupId(l2groupId));
            // create the l3unicast group description to wait for the
            // l2 interface group to be processed
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
        }

        // store l2groupkey with the groupChainElem for the outer-group that depends on it
        GroupChainElem gce = new GroupChainElem(outerGrpDesc, 1, false, deviceId);
        updatePendingGroups(l2groupkey, gce);

        // create group description for the inner l2 interface group
        GroupBucket l2InterfaceGroupBucket =
                DefaultGroupBucket.createIndirectGroupBucket(innerTtb.build());
        GroupDescription l2groupDescription =
                new DefaultGroupDescription(deviceId,
                                            GroupDescription.Type.INDIRECT,
                                            new GroupBuckets(Collections.singletonList(l2InterfaceGroupBucket)),
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
     * L2 Flood group or L3 Multicast group, whose buckets point to L2 Interface groups.
     *
     * @param nextObj  the nextObjective of type BROADCAST
     */
    private void processBroadcastNextObjective(NextObjective nextObj) {
        VlanId assignedVlan = readVlanFromSelector(nextObj.meta());
        if (assignedVlan == null) {
            log.warn("VLAN ID required by broadcast next obj is missing. Abort.");
            fail(nextObj, ObjectiveError.BADPARAMS);
            return;
        }

        List<GroupInfo> groupInfos = prepareL2InterfaceGroup(nextObj, assignedVlan);

        IpPrefix ipDst = readIpDstFromSelector(nextObj.meta());
        if (ipDst != null) {
            if (ipDst.isMulticast()) {
                createL3MulticastGroup(nextObj, assignedVlan, groupInfos);
            } else {
                log.warn("Broadcast NextObj with non-multicast IP address {}", nextObj);
                fail(nextObj, ObjectiveError.BADPARAMS);
            }
        } else {
            createL2FloodGroup(nextObj, assignedVlan, groupInfos);
        }
    }

    private List<GroupInfo> prepareL2InterfaceGroup(NextObjective nextObj,
                                                    VlanId assignedVlan) {
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

            if (portNum == null) {
                log.warn("Can't find output port for the bucket {}.", treatment);
                continue;
            }

            // assemble info for l2 interface group
            VlanId l2InterfaceGroupVlan =
                    (egressVlan != null && !assignedVlan.equals(egressVlan)) ?
                            egressVlan : assignedVlan;
            int l2gk = l2InterfaceGroupKey(deviceId, l2InterfaceGroupVlan, portNum.toLong());
            final GroupKey l2InterfaceGroupKey =
                    new DefaultGroupKey(appKryo.serialize(l2gk));
            int l2InterfaceGroupId = L2_INTERFACE_TYPE |
                    ((l2InterfaceGroupVlan.toShort() & THREE_BIT_MASK) << PORT_LEN) |
                    ((int) portNum.toLong() & FOUR_BIT_MASK);
            GroupBucket l2InterfaceGroupBucket =
                    DefaultGroupBucket.createIndirectGroupBucket(newTreatment.build());
            GroupDescription l2InterfaceGroupDescription =
                    new DefaultGroupDescription(deviceId,
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

    private void createL2FloodGroup(NextObjective nextObj, VlanId vlanId,
                                    List<GroupInfo> groupInfos) {
        // assemble info for l2 flood group. Since there can be only one flood
        // group for a vlan, its index is always the same - 0
        Integer l2FloodGroupId = L2_FLOOD_TYPE | (vlanId.toShort() << 16);
        final GroupKey l2FloodGroupKey = l2FloodGroupKey(vlanId, deviceId);

        // collection of group buckets pointing to all the l2 interface groups
        List<GroupBucket> l2floodBuckets = generateNextGroupBuckets(groupInfos, ALL);
        // create the l2flood group-description to wait for all the
        // l2interface groups to be processed
        GroupDescription l2floodGroupDescription =
                new DefaultGroupDescription(
                        deviceId,
                        ALL,
                        new GroupBuckets(l2floodBuckets),
                        l2FloodGroupKey,
                        l2FloodGroupId,
                        nextObj.appId());
        log.debug("Trying L2-Flood: device:{} gid:{} gkey:{} nextid:{}",
                deviceId, Integer.toHexString(l2FloodGroupId),
                l2FloodGroupKey, nextObj.id());

        // Put all dependency information into allGroupKeys
        List<Deque<GroupKey>> allGroupKeys = Lists.newArrayList();
        groupInfos.forEach(groupInfo -> {
            Deque<GroupKey> groupKeyChain = new ArrayDeque<>();
            // In this case we should have L2 interface group only
            groupKeyChain.addFirst(groupInfo.nextGroupDesc().appCookie());
            groupKeyChain.addFirst(l2FloodGroupKey);
            allGroupKeys.add(groupKeyChain);
        });

        // Point the next objective to this group
        OfdpaNextGroup ofdpaGrp = new OfdpaNextGroup(allGroupKeys, nextObj);
        updatePendingNextObjective(l2FloodGroupKey, ofdpaGrp);

        GroupChainElem gce = new GroupChainElem(l2floodGroupDescription,
                groupInfos.size(), false, deviceId);
        groupInfos.forEach(groupInfo -> {
            // Point this group to the next group
            updatePendingGroups(groupInfo.nextGroupDesc().appCookie(), gce);
            // Start installing the inner-most group
            groupService.addGroup(groupInfo.innerMostGroupDesc());
        });
    }

    private void createL3MulticastGroup(NextObjective nextObj, VlanId vlanId,
                                        List<GroupInfo> groupInfos) {
        List<GroupBucket> l3McastBuckets = new ArrayList<>();
        groupInfos.forEach(groupInfo -> {
            // Points to L3 interface group if there is one.
            // Otherwise points to L2 interface group directly.
            GroupDescription nextGroupDesc = (groupInfo.nextGroupDesc() != null) ?
                    groupInfo.nextGroupDesc() : groupInfo.innerMostGroupDesc();
            TrafficTreatment.Builder ttb = DefaultTrafficTreatment.builder();
            ttb.group(new GroupId(nextGroupDesc.givenGroupId()));
            GroupBucket abucket = DefaultGroupBucket.createAllGroupBucket(ttb.build());
            l3McastBuckets.add(abucket);
        });

        int l3MulticastIndex = getNextAvailableIndex();
        int l3MulticastGroupId = L3_MULTICAST_TYPE |
                vlanId.toShort() << 16 | (TYPE_VLAN_MASK & l3MulticastIndex);
        final GroupKey l3MulticastGroupKey =
                new DefaultGroupKey(appKryo.serialize(l3MulticastIndex));

        GroupDescription l3MulticastGroupDesc = new DefaultGroupDescription(deviceId,
                ALL,
                new GroupBuckets(l3McastBuckets),
                l3MulticastGroupKey,
                l3MulticastGroupId,
                nextObj.appId());

        // Put all dependency information into allGroupKeys
        List<Deque<GroupKey>> allGroupKeys = Lists.newArrayList();
        groupInfos.forEach(groupInfo -> {
            Deque<GroupKey> gkeyChain = new ArrayDeque<>();
            gkeyChain.addFirst(groupInfo.innerMostGroupDesc().appCookie());
            // Add L3 interface group to the chain if there is one.
            if (!groupInfo.nextGroupDesc().equals(groupInfo.innerMostGroupDesc())) {
                gkeyChain.addFirst(groupInfo.nextGroupDesc().appCookie());
            }
            gkeyChain.addFirst(l3MulticastGroupKey);
            allGroupKeys.add(gkeyChain);
        });

        // Point the next objective to this group
        OfdpaNextGroup ofdpaGrp = new OfdpaNextGroup(allGroupKeys, nextObj);
        updatePendingNextObjective(l3MulticastGroupKey, ofdpaGrp);

        GroupChainElem outerGce = new GroupChainElem(l3MulticastGroupDesc,
                groupInfos.size(), false, deviceId);
        groupInfos.forEach(groupInfo -> {
            // Point this group (L3 multicast) to the next group
            updatePendingGroups(groupInfo.nextGroupDesc().appCookie(), outerGce);

            // Point next group to inner-most group, if any
            if (!groupInfo.nextGroupDesc().equals(groupInfo.innerMostGroupDesc())) {
                GroupChainElem innerGce = new GroupChainElem(groupInfo.nextGroupDesc(),
                        1, false, deviceId);
                updatePendingGroups(groupInfo.innerMostGroupDesc().appCookie(), innerGce);
            }

            // Start installing the inner-most group
            groupService.addGroup(groupInfo.innerMostGroupDesc());
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
    protected void processHashedNextObjective(NextObjective nextObj) {
        // storage for all group keys in the chain of groups created
        List<Deque<GroupKey>> allGroupKeys = new ArrayList<>();
        List<GroupInfo> unsentGroups = new ArrayList<>();
        createHashBucketChains(nextObj, allGroupKeys, unsentGroups);

        // now we can create the outermost L3 ECMP group
        List<GroupBucket> l3ecmpGroupBuckets = new ArrayList<>();
        for (GroupInfo gi : unsentGroups) {
            // create ECMP bucket to point to the outer group
            TrafficTreatment.Builder ttb = DefaultTrafficTreatment.builder();
            ttb.group(new GroupId(gi.nextGroupDesc().givenGroupId()));
            GroupBucket sbucket = DefaultGroupBucket
                    .createSelectGroupBucket(ttb.build());
            l3ecmpGroupBuckets.add(sbucket);
        }
        int l3ecmpIndex = getNextAvailableIndex();
        int l3ecmpGroupId = L3_ECMP_TYPE | (TYPE_MASK & l3ecmpIndex);
        GroupKey l3ecmpGroupKey = new DefaultGroupKey(
                                          appKryo.serialize(l3ecmpIndex));
        GroupDescription l3ecmpGroupDesc =
                new DefaultGroupDescription(
                        deviceId,
                        SELECT,
                        new GroupBuckets(l3ecmpGroupBuckets),
                        l3ecmpGroupKey,
                        l3ecmpGroupId,
                        nextObj.appId());
        GroupChainElem l3ecmpGce = new GroupChainElem(l3ecmpGroupDesc,
                l3ecmpGroupBuckets.size(),
                false, deviceId);

        // create objects for local and distributed storage
        allGroupKeys.forEach(gKeyChain -> gKeyChain.addFirst(l3ecmpGroupKey));
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
                    Integer.toHexString(gi.innerMostGroupDesc().givenGroupId()), deviceId);
            updatePendingGroups(gi.nextGroupDesc().appCookie(), l3ecmpGce);
            groupService.addGroup(gi.innerMostGroupDesc());
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
    protected void createHashBucketChains(NextObjective nextObj,
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
                            innermostLabel =
                                    ((L2ModificationInstruction.ModMplsLabelInstruction) l2ins).label();
                        }
                    }
                }
            }

            Deque<GroupKey> gKeyChain = new ArrayDeque<>();
            // XXX we only deal with 0 and 1 label push right now
            if (labelsPushed == 0) {
                GroupInfo noLabelGroupInfo;
                TrafficSelector metaSelector = nextObj.meta();
                if (metaSelector != null) {
                    if (isNotMplsBos(metaSelector)) {
                        noLabelGroupInfo = createL2L3Chain(bucket, nextObj.id(),
                                                           nextObj.appId(), true,
                                                           nextObj.meta());
                    } else {
                        noLabelGroupInfo = createL2L3Chain(bucket, nextObj.id(),
                                                           nextObj.appId(), false,
                                                           nextObj.meta());
                    }
                } else {
                    noLabelGroupInfo = createL2L3Chain(bucket, nextObj.id(),
                                                       nextObj.appId(), false,
                                                       nextObj.meta());
                }
                if (noLabelGroupInfo == null) {
                    log.error("Could not process nextObj={} in dev:{}",
                            nextObj.id(), deviceId);
                    return;
                }
                gKeyChain.addFirst(noLabelGroupInfo.innerMostGroupDesc().appCookie());
                gKeyChain.addFirst(noLabelGroupInfo.nextGroupDesc().appCookie());

                // we can't send the inner group description yet, as we have to
                // create the dependent ECMP group first. So we store..
                unsentGroups.add(noLabelGroupInfo);

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
                if (requireVlanPopBeforeMplsPush()) {
                    l3vpnTtb.popVlan();
                }
                l3vpnTtb.pushMpls()
                        .setMpls(innermostLabel)
                        .group(new GroupId(onelabelGroupInfo.nextGroupDesc().givenGroupId()));
                if (supportCopyTtl()) {
                    l3vpnTtb.copyTtlOut();
                }
                if (supportSetMplsBos()) {
                    l3vpnTtb.setMplsBos(true);
                }
                if (requireVlanPopBeforeMplsPush()) {
                    l3vpnTtb.pushVlan().setVlanId(VlanId.vlanId(VlanId.RESERVED));
                }

                GroupBucket l3vpnGrpBkt  =
                        DefaultGroupBucket.createIndirectGroupBucket(l3vpnTtb.build());
                int l3vpnIndex = getNextAvailableIndex();
                int l3vpnGroupId = MPLS_L3VPN_SUBTYPE | (SUBTYPE_MASK & l3vpnIndex);
                GroupKey l3vpnGroupKey = new DefaultGroupKey(
                             appKryo.serialize(l3vpnIndex));
                GroupDescription l3vpnGroupDesc =
                        new DefaultGroupDescription(
                                deviceId,
                                GroupDescription.Type.INDIRECT,
                                new GroupBuckets(Collections.singletonList(l3vpnGrpBkt)),
                                l3vpnGroupKey,
                                l3vpnGroupId,
                                nextObj.appId());
                GroupChainElem l3vpnGce = new GroupChainElem(l3vpnGroupDesc,
                                                             1,
                                                             false,
                                                             deviceId);
                updatePendingGroups(onelabelGroupInfo.nextGroupDesc().appCookie(), l3vpnGce);

                gKeyChain.addFirst(onelabelGroupInfo.innerMostGroupDesc().appCookie());
                gKeyChain.addFirst(onelabelGroupInfo.nextGroupDesc().appCookie());
                gKeyChain.addFirst(l3vpnGroupKey);

                //now we can replace the outerGrpDesc with the one we just created
                onelabelGroupInfo.nextGroupDesc(l3vpnGroupDesc);

                // we can't send the innermost group yet, as we have to create
                // the dependent ECMP group first. So we store ...
                unsentGroups.add(onelabelGroupInfo);

                log.debug("Trying L3VPN: device:{} gid:{} group key:{} nextId:{}",
                        deviceId, Integer.toHexString(l3vpnGroupId),
                        l3vpnGroupKey, nextObj.id());

            } else {
                log.warn("Driver currently does not handle more than 1 MPLS "
                        + "labels. Not processing nextObjective {}", nextObj.id());
                return;
            }

            // all groups in this chain
            allGroupKeys.add(gKeyChain);
        }
    }

    /**
     * Processes the pseudo wire related next objective.
     * This procedure try to reuse the mpls label groups,
     * the mpls interface group and the l2 interface group.
     *
     * @param nextObjective the objective to process.
     */
    protected void processPwNextObjective(NextObjective nextObjective) {
        log.warn("Pseudo wire extensions are not supported in OFDPA 2.0 {}",
                 nextObjective.id());
    }

    //////////////////////////////////////
    //  Group Editing
    //////////////////////////////////////
    /**
     *  Adds a bucket to the top level group of a group-chain, and creates the chain.
     *  Ensures that bucket being added is not a duplicate, by checking existing
     *  buckets for the same output port.
     *
     * @param nextObjective the bucket information for a next group
     * @param next the representation of the existing group-chain for this next objective
     */
    protected void addBucketToGroup(NextObjective nextObjective, NextGroup next) {
        if (nextObjective.type() != NextObjective.Type.HASHED &&
                nextObjective.type() != NextObjective.Type.BROADCAST) {
            log.warn("AddBuckets not applied to nextType:{} in dev:{} for next:{}",
                     nextObjective.type(), deviceId, nextObjective.id());
            fail(nextObjective, ObjectiveError.UNSUPPORTED);
            return;
        }

        // first check to see if bucket being added is not a duplicate of an
        // existing bucket. If it is for an existing output port, then its a
        // duplicate.
        Set<TrafficTreatment> duplicateBuckets = Sets.newHashSet();
        List<Deque<GroupKey>> allActiveKeys = appKryo.deserialize(next.data());
        Set<PortNumber> existingPorts = getExistingOutputPorts(allActiveKeys,
                                                               groupService,
                                                               deviceId);
        Set<TrafficTreatment> nonDuplicateBuckets = Sets.newHashSet();
        NextObjective objectiveToAdd;

        nextObjective.next().forEach(trafficTreatment -> {
            PortNumber portNumber = readOutPortFromTreatment(trafficTreatment);
            if (portNumber == null) {
                return;
            }
            if (existingPorts.contains(portNumber)) {
                duplicateBuckets.add(trafficTreatment);
            } else {
                nonDuplicateBuckets.add(trafficTreatment);
            }
        });

        if (duplicateBuckets.isEmpty()) {
            // use the original objective
            objectiveToAdd = nextObjective;
        } else if (!nonDuplicateBuckets.isEmpty()) {
            // only use the non-duplicate buckets if there are any
            log.debug("Some buckets {} already exist in next id {}, duplicate "
                    + "buckets will be ignored.", duplicateBuckets, nextObjective.id());
            // new next objective with non duplicate treatments
            NextObjective.Builder builder = DefaultNextObjective.builder()
                    .withType(nextObjective.type())
                    .withId(nextObjective.id())
                    .withMeta(nextObjective.meta())
                    .fromApp(nextObjective.appId());
            nonDuplicateBuckets.forEach(builder::addTreatment);

            ObjectiveContext context = nextObjective.context().orElse(null);
            objectiveToAdd = builder.addToExisting(context);
        } else {
            // buckets to add are already there - nothing to do
            log.debug("buckets already exist {} in next: {} ..ignoring bucket add",
                      duplicateBuckets, nextObjective.id());
            pass(nextObjective);
            return;
        }

        if (nextObjective.type() == NextObjective.Type.HASHED) {
            addBucketToHashGroup(objectiveToAdd, allActiveKeys);
        } else if (nextObjective.type() == NextObjective.Type.BROADCAST) {
            addBucketToBroadcastGroup(objectiveToAdd, allActiveKeys);
        }
    }

    private void addBucketToHashGroup(NextObjective nextObjective,
                                      List<Deque<GroupKey>> allActiveKeys) {
        // storage for all group keys in the chain of groups created
        List<Deque<GroupKey>> allGroupKeys = new ArrayList<>();
        List<GroupInfo> unsentGroups = new ArrayList<>();
        List<GroupBucket> newBuckets;
        createHashBucketChains(nextObjective, allGroupKeys, unsentGroups);

        // now we can create the buckets to add to the outermost L3 ECMP group
        newBuckets = generateNextGroupBuckets(unsentGroups, SELECT);

        // retrieve the original L3 ECMP group
        Group l3ecmpGroup = retrieveTopLevelGroup(allActiveKeys, nextObjective.id());
        if (l3ecmpGroup == null) {
            fail(nextObjective, ObjectiveError.GROUPMISSING);
            return;
        }
        GroupKey l3ecmpGroupKey = l3ecmpGroup.appCookie();
        int l3ecmpGroupId = l3ecmpGroup.id().id();

        // Although GroupDescriptions are not necessary for adding buckets to
        // existing groups, we still use one in the GroupChainElem. When the latter is
        // processed, the info will be extracted for the bucketAdd call to groupService
        GroupDescription l3ecmpGroupDesc =
                new DefaultGroupDescription(deviceId,
                                            SELECT,
                                            new GroupBuckets(newBuckets),
                                            l3ecmpGroupKey,
                                            l3ecmpGroupId,
                                            nextObjective.appId());
        GroupChainElem l3ecmpGce = new GroupChainElem(l3ecmpGroupDesc,
                                                      unsentGroups.size(),
                                                      true,
                                                      deviceId);

        // update new bucket-chains
        List<Deque<GroupKey>> addedKeys = new ArrayList<>();
        for (Deque<GroupKey> newBucketChain : allGroupKeys) {
            newBucketChain.addFirst(l3ecmpGroupKey);
            addedKeys.add(newBucketChain);
        }
        updatePendingNextObjective(l3ecmpGroupKey,
                                   new OfdpaNextGroup(addedKeys, nextObjective));
        log.debug("Adding to L3ECMP: device:{} gid:{} group key:{} nextId:{}",
                deviceId, Integer.toHexString(l3ecmpGroupId),
                l3ecmpGroupKey, nextObjective.id());

        unsentGroups.forEach(groupInfo -> {
            // send the innermost group
            log.debug("Sending innermost group {} in group chain on device {} ",
                      Integer.toHexString(groupInfo.innerMostGroupDesc().givenGroupId()),
                      deviceId);
            updatePendingGroups(groupInfo.nextGroupDesc().appCookie(), l3ecmpGce);
            groupService.addGroup(groupInfo.innerMostGroupDesc());
        });
    }

    private void addBucketToBroadcastGroup(NextObjective nextObj,
                                           List<Deque<GroupKey>> allActiveKeys) {
        VlanId assignedVlan = readVlanFromSelector(nextObj.meta());
        if (assignedVlan == null) {
            log.warn("VLAN ID required by broadcast next obj is missing. "
                    + "Aborting add bucket to broadcast group for next:{} in dev:{}",
                    nextObj.id(), deviceId);
            fail(nextObj, ObjectiveError.BADPARAMS);
            return;
        }
        List<GroupInfo> groupInfos = prepareL2InterfaceGroup(nextObj, assignedVlan);
        IpPrefix ipDst = readIpDstFromSelector(nextObj.meta());
        if (ipDst != null) {
            if (ipDst.isMulticast()) {
                addBucketToL3MulticastGroup(nextObj, allActiveKeys, groupInfos, assignedVlan);
            } else {
                log.warn("Broadcast NextObj with non-multicast IP address {}", nextObj);
                fail(nextObj, ObjectiveError.BADPARAMS);
            }
        } else {
            addBucketToL2FloodGroup(nextObj, allActiveKeys, groupInfos, assignedVlan);
        }
    }

    private void addBucketToL2FloodGroup(NextObjective nextObj,
                                         List<Deque<GroupKey>> allActiveKeys,
                                         List<GroupInfo> groupInfos,
                                         VlanId assignedVlan) {
        Group l2FloodGroup = retrieveTopLevelGroup(allActiveKeys, nextObj.id());

        if (l2FloodGroup == null) {
            log.warn("Can't find L2 flood group while adding bucket to it. NextObj = {}",
                     nextObj);
            fail(nextObj, ObjectiveError.GROUPMISSING);
            return;
        }

        GroupKey l2floodGroupKey = l2FloodGroup.appCookie();
        int l2floodGroupId = l2FloodGroup.id().id();
        List<GroupBucket> newBuckets = generateNextGroupBuckets(groupInfos, ALL);

        GroupDescription l2FloodGroupDescription =
                new DefaultGroupDescription(deviceId,
                                            ALL,
                                            new GroupBuckets(newBuckets),
                                            l2floodGroupKey,
                                            l2floodGroupId,
                                            nextObj.appId());

        GroupChainElem l2FloodGroupChainElement =
                new GroupChainElem(l2FloodGroupDescription,
                                   groupInfos.size(),
                                   true,
                                   deviceId);


        //ensure assignedVlan applies to the chosen group
        VlanId floodGroupVlan = extractVlanIdFromGroupId(l2floodGroupId);

        if (!floodGroupVlan.equals(assignedVlan)) {
            log.warn("VLAN ID {} does not match Flood group {} to which bucket is "
                             + "being added, for next:{} in dev:{}. Abort.", assignedVlan,
                     Integer.toHexString(l2floodGroupId), nextObj.id(), deviceId);
            fail(nextObj, ObjectiveError.BADPARAMS);
            return;
        }
        List<Deque<GroupKey>> addedKeys = new ArrayList<>();
        groupInfos.forEach(groupInfo -> {
            // update original NextGroup with new bucket-chain
            Deque<GroupKey> newBucketChain = new ArrayDeque<>();
            newBucketChain.addFirst(groupInfo.nextGroupDesc().appCookie());
            newBucketChain.addFirst(l2floodGroupKey);
            addedKeys.add(newBucketChain);

            log.debug("Adding to L2FLOOD: device:{} gid:{} group key:{} nextId:{}",
                      deviceId, Integer.toHexString(l2floodGroupId),
                      l2floodGroupKey, nextObj.id());
            // send the innermost group
            log.debug("Sending innermost group {} in group chain on device {} ",
                      Integer.toHexString(groupInfo.innerMostGroupDesc().givenGroupId()),
                      deviceId);

            updatePendingGroups(groupInfo.nextGroupDesc().appCookie(), l2FloodGroupChainElement);

            DeviceId innerMostGroupDevice = groupInfo.innerMostGroupDesc().deviceId();
            GroupKey innerMostGroupKey = groupInfo.innerMostGroupDesc().appCookie();
            Group existsL2IGroup = groupService.getGroup(innerMostGroupDevice, innerMostGroupKey);

            if (existsL2IGroup != null) {
                // group already exist
                processPendingAddGroupsOrNextObjs(innerMostGroupKey, true);
            } else {
                groupService.addGroup(groupInfo.innerMostGroupDesc());
            }
        });

        updatePendingNextObjective(l2floodGroupKey,
                                   new OfdpaNextGroup(addedKeys, nextObj));
    }

    private void addBucketToL3MulticastGroup(NextObjective nextObj,
                                             List<Deque<GroupKey>> allActiveKeys,
                                             List<GroupInfo> groupInfos,
                                             VlanId assignedVlan) {
        // create the buckets to add to the outermost L3 Multicast group
        List<GroupBucket> newBuckets = Lists.newArrayList();
        groupInfos.forEach(groupInfo -> {
            // Points to L3 interface group if there is one.
            // Otherwise points to L2 interface group directly.
            GroupDescription nextGroupDesc = (groupInfo.nextGroupDesc() != null) ?
                    groupInfo.nextGroupDesc() : groupInfo.innerMostGroupDesc();
            TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();
            treatmentBuilder.group(new GroupId(nextGroupDesc.givenGroupId()));
            GroupBucket newBucket = DefaultGroupBucket.createAllGroupBucket(treatmentBuilder.build());
            newBuckets.add(newBucket);
        });

        // get the group being edited
        Group l3mcastGroup = retrieveTopLevelGroup(allActiveKeys, nextObj.id());
        if (l3mcastGroup == null) {
            fail(nextObj, ObjectiveError.GROUPMISSING);
            return;
        }
        GroupKey l3mcastGroupKey = l3mcastGroup.appCookie();
        int l3mcastGroupId = l3mcastGroup.id().id();

        //ensure assignedVlan applies to the chosen group
        VlanId expectedVlan = extractVlanIdFromGroupId(l3mcastGroupId);
        if (!expectedVlan.equals(assignedVlan)) {
            log.warn("VLAN ID {} does not match L3 Mcast group {} to which bucket is "
                    + "being added, for next:{} in dev:{}. Abort.", assignedVlan,
                    Integer.toHexString(l3mcastGroupId), nextObj.id(), deviceId);
            fail(nextObj, ObjectiveError.BADPARAMS);
        }
        GroupDescription l3mcastGroupDescription =
                new DefaultGroupDescription(deviceId,
                                            ALL,
                                            new GroupBuckets(newBuckets),
                                            l3mcastGroupKey,
                                            l3mcastGroupId,
                                            nextObj.appId());
        GroupChainElem l3mcastGce = new GroupChainElem(l3mcastGroupDescription,
                                                       groupInfos.size(),
                                                       true,
                                                       deviceId);

        List<Deque<GroupKey>> addedKeys = new ArrayList<>();
        groupInfos.forEach(groupInfo -> {
            // update original NextGroup with new bucket-chain
            Deque<GroupKey> newBucketChain = new ArrayDeque<>();
            newBucketChain.addFirst(groupInfo.innerMostGroupDesc().appCookie());
            // Add L3 interface group to the chain if there is one.
            if (!groupInfo.nextGroupDesc().equals(groupInfo.innerMostGroupDesc())) {
                newBucketChain.addFirst(groupInfo.nextGroupDesc().appCookie());
            }
            newBucketChain.addFirst(l3mcastGroupKey);
            addedKeys.add(newBucketChain);

            updatePendingGroups(groupInfo.nextGroupDesc().appCookie(), l3mcastGce);
            // Point next group to inner-most group, if any
            if (!groupInfo.nextGroupDesc().equals(groupInfo.innerMostGroupDesc())) {
                GroupChainElem innerGce = new GroupChainElem(groupInfo.nextGroupDesc(),
                                                             1,
                                                             false,
                                                             deviceId);
                updatePendingGroups(groupInfo.innerMostGroupDesc().appCookie(), innerGce);
            }
            log.debug("Adding to L3MCAST: device:{} gid:{} group key:{} nextId:{}",
                      deviceId, Integer.toHexString(l3mcastGroupId),
                      l3mcastGroupKey, nextObj.id());
            // send the innermost group
            log.debug("Sending innermost group {} in group chain on device {} ",
                      Integer.toHexString(groupInfo.innerMostGroupDesc().givenGroupId()),
                      deviceId);
            groupService.addGroup(groupInfo.innerMostGroupDesc());

        });

        updatePendingNextObjective(l3mcastGroupKey,
                                   new OfdpaNextGroup(addedKeys, nextObj));
    }

    /**
     * Removes the bucket in the top level group of a possible group-chain. Does
     * not remove the groups in the group-chain pointed to by this bucket, as they
     * may be in use (referenced by other groups) elsewhere.
     *
     * @param nextObjective the bucket information for a next group
     * @param next the representation of the existing group-chain for this next objective
     */
    protected void removeBucketFromGroup(NextObjective nextObjective, NextGroup next) {
        if (nextObjective.type() != NextObjective.Type.HASHED &&
                nextObjective.type() != NextObjective.Type.BROADCAST) {
            log.warn("RemoveBuckets not applied to nextType:{} in dev:{} for next:{}",
                    nextObjective.type(), deviceId, nextObjective.id());
            fail(nextObjective, ObjectiveError.UNSUPPORTED);
            return;
        }
        Set<PortNumber> portsToRemove = Sets.newHashSet();
        Collection<TrafficTreatment> treatments = nextObjective.next();
        for (TrafficTreatment treatment : treatments) {
            // find the bucket to remove by noting the outport, and figuring out the
            // top-level group in the group-chain that indirectly references the port
            PortNumber portToRemove = readOutPortFromTreatment(treatment);
            if (portToRemove == null) {
                log.warn("treatment {} of next objective {} has no outport.. cannot remove bucket"
                       + "from group in dev: {}", treatment, nextObjective.id(), deviceId);
            } else {
                portsToRemove.add(portToRemove);
            }
        }

        if (portsToRemove.isEmpty()) {
            log.warn("next objective {} has no outport.. cannot remove bucket"
                             + "from group in dev: {}", nextObjective.id(), deviceId);
            fail(nextObjective, ObjectiveError.BADPARAMS);
        }

        List<Deque<GroupKey>> allActiveKeys = appKryo.deserialize(next.data());
        List<Deque<GroupKey>> chainsToRemove = Lists.newArrayList();
        for (Deque<GroupKey> gkeys : allActiveKeys) {
            // last group in group chain should have a single bucket pointing to port
            GroupKey groupWithPort = gkeys.peekLast();
            Group group = groupService.getGroup(deviceId, groupWithPort);
            if (group == null) {
                log.warn("Inconsistent group chain found when removing bucket"
                        + "for next:{} in dev:{}", nextObjective.id(), deviceId);
                continue;
            }
            if (group.buckets().buckets().isEmpty()) {
                log.warn("Can't get output port information from group {} " +
                                 "because there is no bucket in the group.",
                         group.id().toString());
                continue;
            }
            PortNumber pout = readOutPortFromTreatment(
                                  group.buckets().buckets().get(0).treatment());
            if (portsToRemove.contains(pout)) {
                chainsToRemove.add(gkeys);
            }
        }
        if (chainsToRemove.isEmpty()) {
            log.warn("Could not find appropriate group-chain for removing bucket"
                    + " for next id {} in dev:{}", nextObjective.id(), deviceId);
            fail(nextObjective, ObjectiveError.BADPARAMS);
            return;
        }
        List<GroupBucket> bucketsToRemove = Lists.newArrayList();
        //first group key is the one we want to modify
        GroupKey modGroupKey = chainsToRemove.get(0).peekFirst();
        Group modGroup = groupService.getGroup(deviceId, modGroupKey);
        for (Deque<GroupKey> foundChain : chainsToRemove) {
            //second group key is the one we wish to remove the reference to
            if (foundChain.size() < 2) {
                // additional check to make sure second group key exist in
                // the chain.
                log.warn("Can't find second group key from chain {}",
                         foundChain);
                continue;
            }
            GroupKey pointedGroupKey = foundChain.stream().collect(Collectors.toList()).get(1);
            Group pointedGroup = groupService.getGroup(deviceId, pointedGroupKey);

            if (pointedGroup == null) {
                continue;
            }

            GroupBucket bucket;
            if (nextObjective.type() == NextObjective.Type.HASHED) {
                bucket = DefaultGroupBucket.createSelectGroupBucket(
                        DefaultTrafficTreatment.builder()
                                .group(pointedGroup.id())
                                .build());
            } else {
                bucket = DefaultGroupBucket.createAllGroupBucket(
                        DefaultTrafficTreatment.builder()
                                .group(pointedGroup.id())
                                .build());
            }

            bucketsToRemove.add(bucket);
        }

        GroupBuckets removeBuckets = new GroupBuckets(bucketsToRemove);
        List<String> pointedGroupIds; // for debug log
        pointedGroupIds = bucketsToRemove.stream()
                .map(GroupBucket::treatment)
                .map(TrafficTreatment::allInstructions)
                .flatMap(List::stream)
                .filter(inst -> inst instanceof Instructions.GroupInstruction)
                .map(inst -> (Instructions.GroupInstruction) inst)
                .map(Instructions.GroupInstruction::groupId)
                .map(GroupId::id)
                .map(Integer::toHexString)
                .map(id -> HEX_PREFIX + id)
                .collect(Collectors.toList());

        log.debug("Removing buckets from group id 0x{} pointing to group id(s) {} "
                + "for next id {} in device {}", Integer.toHexString(modGroup.id().id()),
                pointedGroupIds, nextObjective.id(), deviceId);
        addPendingUpdateNextObjective(modGroupKey, nextObjective);
        groupService.removeBucketsFromGroup(deviceId, modGroupKey,
                                            removeBuckets, modGroupKey,
                                            nextObjective.appId());
        // update store - synchronize access
        synchronized (flowObjectiveStore) {
            // get fresh copy of what the store holds
            next = flowObjectiveStore.getNextGroup(nextObjective.id());
            allActiveKeys = appKryo.deserialize(next.data());
            // Note that since we got a new object, and ArrayDeque does not implement
            // Object.equals(), we have to check the deque last elems one by one
            allActiveKeys.removeIf(active -> chainsToRemove.stream().anyMatch(remove ->
                                       remove.peekLast().equals(active.peekLast())));
            // If no buckets in the group, then retain an entry for the
            // top level group which still exists.
            if (allActiveKeys.isEmpty()) {
                ArrayDeque<GroupKey> top = new ArrayDeque<>();
                top.add(modGroupKey);
                allActiveKeys.add(top);
            }
            flowObjectiveStore.putNextGroup(nextObjective.id(),
                                            new OfdpaNextGroup(allActiveKeys, nextObjective));
        }
    }

    /**
     * Removes all groups in multiple possible group-chains that represent the next-obj.
     *
     * @param nextObjective the next objective to remove
     * @param next the NextGroup that represents the existing group-chain for
     *             this next objective
     */
    protected void removeGroup(NextObjective nextObjective, NextGroup next) {
        List<Deque<GroupKey>> allActiveKeys = appKryo.deserialize(next.data());

        List<GroupKey> groupKeys = allActiveKeys.stream()
                .map(Deque::getFirst).collect(Collectors.toList());
        addPendingRemoveNextObjective(nextObjective, groupKeys);

        allActiveKeys.forEach(groupChain -> groupChain.forEach(groupKey ->
                groupService.removeGroup(deviceId, groupKey, nextObjective.appId())));
        flowObjectiveStore.removeNextGroup(nextObjective.id());
    }

    protected void updatePendingNextObjective(GroupKey groupKey, OfdpaNextGroup nextGrp) {
        pendingAddNextObjectives.asMap().compute(groupKey, (k, val) -> {
            if (val == null) {
                val = new CopyOnWriteArrayList<>();
            }
            val.add(nextGrp);
            return val;
        });
    }

    protected void updatePendingGroups(GroupKey groupKey, GroupChainElem gce) {
        pendingGroups.asMap().compute(groupKey, (k, val) -> {
            if (val == null) {
                val = Sets.newConcurrentHashSet();
            }
            val.add(gce);
            return val;
        });
    }

    protected void addPendingUpdateNextObjective(GroupKey groupKey,
                                                 NextObjective nextObjective) {
        pendingUpdateNextObjectives.compute(groupKey, (gKey, nextObjs) -> {
            if (nextObjs != null) {
                nextObjs.add(nextObjective);
            } else {
                nextObjs = Sets.newHashSet(nextObjective);
            }
            return nextObjs;
        });
    }

    private void processPendingUpdateNextObjs(GroupKey groupKey) {
        pendingUpdateNextObjectives.compute(groupKey, (gKey, nextObjs) -> {
            if (nextObjs != null) {

                nextObjs.forEach(nextObj -> {
                    log.debug("Group {} updated, update pending next objective {}.",
                              groupKey, nextObj);

                    pass(nextObj);
                });
            }
            return Sets.newHashSet();
        });
    }

    private void processPendingRemoveNextObjs(GroupKey key) {
        pendingRemoveNextObjectives.asMap().forEach((nextObjective, groupKeys) -> {
            if (groupKeys.isEmpty()) {
                pendingRemoveNextObjectives.invalidate(nextObjective);
                pass(nextObjective);
            } else {
                groupKeys.remove(key);
            }
        });
    }

    protected int getNextAvailableIndex() {
        return (int) nextIndex.incrementAndGet();
    }

    protected Group retrieveTopLevelGroup(List<Deque<GroupKey>> allActiveKeys,
                                          int nextid) {
        GroupKey topLevelGroupKey;
        if (!allActiveKeys.isEmpty()) {
            topLevelGroupKey = allActiveKeys.get(0).peekFirst();
        } else {
            log.warn("Could not determine top level group while processing"
                             + "next:{} in dev:{}", nextid, deviceId);
            return null;
        }
        Group topGroup = groupService.getGroup(deviceId, topLevelGroupKey);
        if (topGroup == null) {
            log.warn("Could not find top level group while processing "
                             + "next:{} in dev:{}", nextid, deviceId);
        }
        return topGroup;
    }

    protected void processPendingAddGroupsOrNextObjs(GroupKey key, boolean added) {
        //first check for group chain
        Set<OfdpaGroupHandlerUtility.GroupChainElem> gceSet = pendingGroups.asMap().remove(key);
        if (gceSet != null) {
            for (GroupChainElem gce : gceSet) {
                log.debug("Group service {} group key {} in device {}. "
                                  + "Processing next group in group chain with group id 0x{}",
                          (added) ? "ADDED" : "processed",
                          key, deviceId,
                          Integer.toHexString(gce.groupDescription().givenGroupId()));
                processGroupChain(gce);
            }
        } else {
            // otherwise chain complete - check for waiting nextObjectives
            List<OfdpaGroupHandlerUtility.OfdpaNextGroup> nextGrpList =
                    pendingAddNextObjectives.getIfPresent(key);
            if (nextGrpList != null) {
                pendingAddNextObjectives.invalidate(key);
                nextGrpList.forEach(nextGrp -> {
                    log.debug("Group service {} group key {} in device:{}. "
                                      + "Done implementing next objective: {} <<-->> gid:0x{}",
                              (added) ? "ADDED" : "processed",
                              key, deviceId, nextGrp.nextObjective().id(),
                              Integer.toHexString(groupService.getGroup(deviceId, key)
                                                          .givenGroupId()));
                    pass(nextGrp.nextObjective());
                    updateFlowObjectiveStore(nextGrp.nextObjective().id(), nextGrp);

                    // check if addBuckets waiting for this completion
                    pendingBuckets.compute(nextGrp.nextObjective().id(), (nextId, pendBkts) -> {
                        if (pendBkts != null) {
                            pendBkts.forEach(pendBkt -> addBucketToGroup(pendBkt, nextGrp));
                        }
                        return null;
                    });
                });
            }
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
        if (gce.addBucketToGroup()) {
            groupService.addBucketsToGroup(gce.groupDescription().deviceId(),
                                           gce.groupDescription().appCookie(),
                                           gce.groupDescription().buckets(),
                                           gce.groupDescription().appCookie(),
                                           gce.groupDescription().appId());
        } else {
            groupService.addGroup(gce.groupDescription());
        }
    }

    protected void addPendingRemoveNextObjective(NextObjective nextObjective,
                                                 List<GroupKey> groupKeys) {
        pendingRemoveNextObjectives.put(nextObjective, groupKeys);
    }

    private void updateFlowObjectiveStore(Integer nextId, OfdpaNextGroup nextGrp) {
        synchronized (flowObjectiveStore) {
            // get fresh copy of what the store holds
            NextGroup next = flowObjectiveStore.getNextGroup(nextId);
            if (next == null || nextGrp.nextObjective().op() == Operation.ADD) {
                flowObjectiveStore.putNextGroup(nextId, nextGrp);
                return;
            }
            if (nextGrp.nextObjective().op() == Operation.ADD_TO_EXISTING) {
                List<Deque<GroupKey>> allActiveKeys = appKryo.deserialize(next.data());
                // If active keys shows only the top-level group without a chain of groups,
                // then it represents an empty group. Update by replacing empty chain.
                if (allActiveKeys.size() == 1 && allActiveKeys.get(0).size() == 1) {
                    allActiveKeys.clear();
                }
                allActiveKeys.addAll(nextGrp.allKeys());
                flowObjectiveStore.putNextGroup(nextId,
                    new OfdpaNextGroup(allActiveKeys, nextGrp.nextObjective()));
            }
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
                case GROUP_UPDATED:
                    processPendingUpdateNextObjs(event.subject().appCookie());
                    break;
                default:
                    break;
            }
        }
    }
}
