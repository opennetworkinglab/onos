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
import org.onosproject.driver.extensions.Ofdpa3AllowVlanTranslationType;
import org.onosproject.driver.extensions.OfdpaSetAllowVlanTranslation;
import org.onosproject.driver.extensions.OfdpaSetVlanVid;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.NextGroup;
import org.onosproject.net.behaviour.PipelinerContext;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.criteria.TunnelIdCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flow.instructions.Instructions.GroupInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flowobjective.DefaultNextObjective;
import org.onosproject.net.flowobjective.DefaultNextTreatment;
import org.onosproject.net.flowobjective.FlowObjectiveStore;
import org.onosproject.net.flowobjective.IdNextTreatment;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.NextTreatment;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.driver.pipeline.ofdpa.Ofdpa2Pipeline.*;
import static org.onosproject.driver.pipeline.ofdpa.OfdpaPipelineUtility.*;
import static org.onosproject.driver.pipeline.ofdpa.OfdpaGroupHandlerUtility.*;
import static org.onosproject.driver.pipeline.ofdpa.OfdpaGroupHandlerUtility.L2_MULTICAST_TYPE;
import static org.onosproject.driver.pipeline.ofdpa.OfdpaGroupHandlerUtility.l2MulticastGroupKey;
import static org.onosproject.net.flow.criteria.Criterion.Type.IN_PORT;
import static org.onosproject.net.flow.criteria.Criterion.Type.TUNNEL_ID;
import static org.onosproject.net.flow.criteria.Criterion.Type.VLAN_VID;
import static org.onosproject.net.group.GroupDescription.Type.ALL;
import static org.onosproject.net.group.GroupDescription.Type.INDIRECT;
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
    Cache<GroupKey, List<OfdpaGroupHandlerUtility.OfdpaNextGroup>> pendingAddNextObjectives;
    Cache<NextObjective, List<GroupKey>> pendingRemoveNextObjectives;
    Cache<GroupKey, Set<OfdpaGroupHandlerUtility.GroupChainElem>> pendingGroups;
    ConcurrentHashMap<GroupKey, Set<NextObjective>> pendingUpdateNextObjectives;
    // local store for pending bucketAdds - by design there can be multiple
    // pending bucket for a group
    protected ConcurrentHashMap<Integer, Set<NextObjective>> pendingBuckets =
            new ConcurrentHashMap<>();
    private ScheduledExecutorService groupCheckerExecutor =
            Executors.newScheduledThreadPool(2, groupedThreads("onos/pipeliner", "ofdpa-%d", log));
    private InnerGroupListener innerGroupListener = new InnerGroupListener();
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

    /**
     * Determines whether this pipeline requires AllowVlanTransition in L2 unfiltered group.
     *
     * @return true if the AllowVlanTransition action is required
     */
    protected boolean requireAllowVlanTransition() {
        return true;
    }

    public void init(DeviceId deviceId, PipelinerContext context) {
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
        groupService.addListener(innerGroupListener);
    }

    // Terminate internal references
    public void terminate() {
        if (nextIndex != null) {
            nextIndex.destroy();
        }
        nextIndex = null;
        if (pendingAddNextObjectives != null) {
            pendingAddNextObjectives.cleanUp();
        }
        pendingAddNextObjectives = null;
        if (pendingRemoveNextObjectives != null) {
            pendingRemoveNextObjectives.cleanUp();
        }
        pendingRemoveNextObjectives = null;
        if (pendingGroups != null) {
            pendingGroups.cleanUp();
        }
        pendingGroups = null;
        if (groupCheckerExecutor != null) {
            groupCheckerExecutor.shutdown();
        }
        groupCheckerExecutor = null;
        if (groupService != null) {
            groupService.removeListener(innerGroupListener);
        }
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
                if (isL2Hash(nextObjective)) {
                    processL2HashedNextObjective(nextObjective);
                    return;
                }
                processEcmpHashedNextObjective(nextObjective);
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
     * Similar to processBroadcastNextObjective but handles L2 Multicast Next Objectives.
     *
     * @param nextObj  NextObjective of L2_MULTICAST with chained NextObjectives for single homed access ports
     */
    private void processL2MulticastNextObjective(NextObjective nextObj) {

        VlanId assignedVlan = readVlanFromSelector(nextObj.meta());
        if (assignedVlan == null) {
            log.warn("VLAN ID required by L2 multicast next objective is missing. Aborting group creation.");
            fail(nextObj, ObjectiveError.BADPARAMS);
            return;
        }

        // Group info should contain only single homed hosts for a given vlanId
        List<GroupInfo> groupInfos = prepareL2InterfaceGroup(nextObj, assignedVlan);
        createL2MulticastGroup(nextObj, assignedVlan, groupInfos);
    }

    private void createL2MulticastGroup(NextObjective nextObj, VlanId vlanId,  List<GroupInfo> groupInfos) {
        // Realize & represent L2 multicast group in OFDPA driver layer
        // TODO : Need to identify significance of OfdpaNextGroup.
        Integer l2MulticastGroupId = L2_MULTICAST_TYPE | (vlanId.toShort() << 16);
        final GroupKey l2MulticastGroupKey = l2MulticastGroupKey(vlanId, deviceId);
        List<Deque<GroupKey>> l2MulticastAllGroup = Lists.newArrayList();
        groupInfos.forEach(groupInfo -> {
            Deque<GroupKey> groupKeyChain = new ArrayDeque<>();
            groupKeyChain.addFirst(groupInfo.innerMostGroupDesc().appCookie());
            groupKeyChain.addFirst(l2MulticastGroupKey);
            l2MulticastAllGroup.add(groupKeyChain);
        });
        OfdpaNextGroup ofdpaL2MulticastGroup = new OfdpaNextGroup(l2MulticastAllGroup, nextObj);
        updatePendingNextObjective(l2MulticastGroupKey, ofdpaL2MulticastGroup);
        // Group Chain Hierarchy creation using group service and thus in device level
        List<GroupBucket> l2McastBuckets = new ArrayList<>();
        groupInfos.forEach(groupInfo -> {
            // Points to L2 interface group directly.
            TrafficTreatment.Builder trafficTreatment = DefaultTrafficTreatment.builder();
            trafficTreatment.group(new GroupId(groupInfo.innerMostGroupDesc().givenGroupId()));
            GroupBucket bucket = DefaultGroupBucket.createAllGroupBucket(trafficTreatment.build());
            l2McastBuckets.add(bucket);
        });
        GroupDescription l2MulticastGroupDescription =
                new DefaultGroupDescription(
                        deviceId,
                        ALL,
                        new GroupBuckets(l2McastBuckets),
                        l2MulticastGroupKey,
                        l2MulticastGroupId,
                        nextObj.appId());
        GroupChainElem l2MulticastGce = new GroupChainElem(l2MulticastGroupDescription,
                                                           groupInfos.size(), false, deviceId);
        groupInfos.forEach(groupInfo -> {
            updatePendingGroups(groupInfo.innerMostGroupDesc().appCookie(), l2MulticastGce);
            groupService.addGroup(groupInfo.innerMostGroupDesc());
        });
    }

    /**
     * As per the OFDPA 2.0 TTP, packets are sent out of ports by using
     * a chain of groups. The simple Next Objective passed in by the application
     * is broken up into a group chain. The following chains can be created
     * depending on the parameters in the Next Objective.
     * 1. L2 Interface group (no chaining)
     * 2. L3 Unicast group -> L2 Interface group
     * 3. MPLS Interface group -> L2 Interface group
     * 4. MPLS Swap group -> MPLS Interface group -> L2 Interface group
     * 5. PW initiation group chain
     *
     * @param nextObj  the nextObjective of type SIMPLE
     */
    private void processSimpleNextObjective(NextObjective nextObj) {
        TrafficTreatment treatment = nextObj.next().iterator().next();
        // determine if plain L2 or L3->L2 or MPLS Swap -> MPLS Interface -> L2
        boolean plainL2 = true;
        boolean mplsSwap = false;
        MplsLabel mplsLabel = null;
        for (Instruction ins : treatment.allInstructions()) {
            if (ins.type() == Instruction.Type.L2MODIFICATION) {
                L2ModificationInstruction l2ins = (L2ModificationInstruction) ins;
                if (l2ins.subtype() == L2ModificationInstruction.L2SubType.ETH_DST ||
                        l2ins.subtype() == L2ModificationInstruction.L2SubType.ETH_SRC) {
                    plainL2 = false;
                }
                // mpls label in simple next objectives is used only to indicate
                // a MPLS Swap group before the MPLS Interface Group
                if (l2ins.subtype() == L2ModificationInstruction.L2SubType.MPLS_LABEL) {
                    mplsSwap = true;
                    mplsLabel = ((L2ModificationInstruction.ModMplsLabelInstruction) l2ins).label();
                }
            }
        }
        if (plainL2) {
            createL2InterfaceGroup(nextObj);
            return;
        }
        // In order to understand if it is a pseudowire related
        // next objective we look for the tunnel id in the meta.
        boolean isPw = false;
        if (nextObj.meta() != null) {
            TunnelIdCriterion tunnelIdCriterion = (TunnelIdCriterion) nextObj
                    .meta()
                    .getCriterion(TUNNEL_ID);
            if (tunnelIdCriterion != null) {
                isPw = true;
            }
        }
        if (mplsSwap && !isPw) {
            log.debug("Creating a MPLS Swap -> MPLS Interface -> L2 Interface group chain.");
            // break up simple next objective to GroupChain objects
            GroupInfo groupInfo = createL2L3Chain(treatment, nextObj.id(),
                                                  nextObj.appId(), true,
                                                  nextObj.meta());
            if (groupInfo == null) {
                log.error("Could not process nextObj={} in dev:{}", nextObj.id(), deviceId);
                fail(nextObj, ObjectiveError.BADPARAMS);
                return;
            }
            Deque<GroupKey> gkeyChain = new ArrayDeque<>();
            gkeyChain.addFirst(groupInfo.innerMostGroupDesc().appCookie()); // l2 interface
            gkeyChain.addFirst(groupInfo.nextGroupDesc().appCookie()); // mpls interface
            // creating the mpls swap group and adding it to the chain
            int nextGid = groupInfo.nextGroupDesc().givenGroupId();
            int index = getNextAvailableIndex();
            GroupDescription swapGroupDescription = createMplsSwap(
                    nextGid,
                    OfdpaMplsGroupSubType.MPLS_SWAP_LABEL,
                    index,
                    mplsLabel,
                    nextObj.appId()
            );
            // ensure swap group is added after L2L3 chain
            GroupKey swapGroupKey = swapGroupDescription.appCookie();
            GroupChainElem swapChainElem = new GroupChainElem(swapGroupDescription,
                                                               1, false, deviceId);
            updatePendingGroups(groupInfo.nextGroupDesc().appCookie(), swapChainElem);
            gkeyChain.addFirst(swapGroupKey);
            // ensure nextObjective waits on the outermost groupKey
            List<Deque<GroupKey>> allGroupKeys = Lists.newArrayList();
            allGroupKeys.add(gkeyChain);
            OfdpaNextGroup ofdpaGrp = new OfdpaNextGroup(allGroupKeys, nextObj);
            updatePendingNextObjective(swapGroupKey, ofdpaGrp);
            // now we are ready to send the l2 groupDescription (inner), as all the stores
            // that will get async replies have been updated. By waiting to update
            // the stores, we prevent nasty race conditions.
            groupService.addGroup(groupInfo.innerMostGroupDesc());
        } else if (!isPw) {
            boolean isMpls = false;
            if (nextObj.meta() != null) {
                isMpls = isNotMplsBos(nextObj.meta());
            }
            log.debug("Creating a {} -> L2 Interface group chain.",
                      (isMpls) ? "MPLS Interface" : "L3 Unicast");
            // break up simple next objective to GroupChain objects
            GroupInfo groupInfo = createL2L3Chain(treatment, nextObj.id(),
                                                  nextObj.appId(), isMpls,
                                                  nextObj.meta());
            if (groupInfo == null) {
                log.error("Could not process nextObj={} in dev:{}", nextObj.id(), deviceId);
                fail(nextObj, ObjectiveError.BADPARAMS);
                return;
            }
            // create object for local and distributed storage
            Deque<GroupKey> gkeyChain = new ArrayDeque<>();
            gkeyChain.addFirst(groupInfo.innerMostGroupDesc().appCookie());
            gkeyChain.addFirst(groupInfo.nextGroupDesc().appCookie());
            List<Deque<GroupKey>> allGroupKeys = Lists.newArrayList();
            allGroupKeys.add(gkeyChain);
            OfdpaNextGroup ofdpaGrp = new OfdpaNextGroup(allGroupKeys, nextObj);
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
        groupService.addGroup(l2InterfaceGroupDesc);    }

    /**
     * Creates an Mpls group of type swap.
     *
     * @param nextGroupId the next group in the chain
     * @param subtype the mpls swap label group subtype
     * @param index the index of the group
     * @param mplsLabel the mpls label to swap
     * @param applicationId the application id
     * @return the group description
     */
    protected GroupDescription createMplsSwap(int nextGroupId,
                                              OfdpaMplsGroupSubType subtype,
                                              int index,
                                              MplsLabel mplsLabel,
                                              ApplicationId applicationId) {
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.setMpls(mplsLabel);
        // We point the group to the next group.
        treatment.group(new GroupId(nextGroupId));
        GroupBucket groupBucket = DefaultGroupBucket
                .createIndirectGroupBucket(treatment.build());
        // Finally we build the group description.
        int groupId = makeMplsLabelGroupId(subtype, index);
        GroupKey groupKey = new DefaultGroupKey(
                Ofdpa2Pipeline.appKryo.serialize(index));
        return new DefaultGroupDescription(
                deviceId,
                INDIRECT,
                new GroupBuckets(Collections.singletonList(groupBucket)),
                groupKey,
                groupId,
                applicationId);
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
                log.debug("Driver does not handle this type of TrafficTreatment"
                        + " instruction in l2l3chain:  {} - {}", ins.type(),
                          ins);
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

        // Handling L2 multicast cases.
        MacAddress dstMac = readEthDstFromSelector(nextObj.meta());
        if (dstMac != null && dstMac.isMulticast()) {
            processL2MulticastNextObjective(nextObj);
            return;
        }

        // FIXME Improve the logic
        //       If L2 load balancer is not involved, use L2IG. Otherwise, use L2UG.
        //       The purpose is to make sure existing XConnect logic can still work on a configured port.
        List<GroupInfo> groupInfos;
        if (nextObj.nextTreatments().stream().allMatch(n -> n.type() == NextTreatment.Type.TREATMENT)) {
            groupInfos = prepareL2InterfaceGroup(nextObj, assignedVlan);
            log.debug("prepareL2InterfaceGroup");
        } else {
            groupInfos = prepareL2UnfilteredGroup(nextObj);
            log.debug("prepareL2UnfilteredGroup");
        }

        if (groupInfos == null || groupInfos.isEmpty()) {
            log.warn("No buckets for Broadcast NextObj {}", nextObj);
            fail(nextObj, ObjectiveError.GROUPMISSING);
            return;
        }

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

    private List<GroupInfo> prepareL2UnfilteredGroup(NextObjective nextObj) {
        ImmutableList.Builder<GroupInfo> groupInfoBuilder = ImmutableList.builder();
        // break up broadcast next objective to multiple groups
        Collection<TrafficTreatment> treatments = nextObj.nextTreatments().stream()
                .filter(nt -> nt.type() == NextTreatment.Type.TREATMENT)
                .map(nt -> ((DefaultNextTreatment) nt).treatment())
                .collect(Collectors.toSet());
        Collection<Integer> nextIds = nextObj.nextTreatments().stream()
                .filter(nt -> nt.type() == NextTreatment.Type.ID)
                .map(nt -> ((IdNextTreatment) nt).nextId())
                .collect(Collectors.toSet());

        // Each treatment is converted to an L2 unfiltered group
        treatments.forEach(treatment -> {
            TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
            // Extract port information
            PortNumber port = treatment.allInstructions().stream()
                    .map(instr -> (Instructions.OutputInstruction) instr)
                    .map(instr -> instr.port())
                    .findFirst().orElse(null);
            if (port == null) {
                log.debug("Skip bucket without output instruction");
                return;
            }
            tBuilder.setOutput(port);
            if (requireAllowVlanTransition()) {
                tBuilder.extension(new OfdpaSetAllowVlanTranslation(Ofdpa3AllowVlanTranslationType.ALLOW), deviceId);
            }

            // Build L2UG
            int l2ugk = l2UnfilteredGroupKey(deviceId, port.toLong());
            final GroupKey l2UnfilterGroupKey = new DefaultGroupKey(appKryo.serialize(l2ugk));
            int l2UnfilteredGroupId = L2_UNFILTERED_TYPE | ((int) port.toLong() & FOUR_NIBBLE_MASK);
            GroupBucket l2UnfilteredGroupBucket = DefaultGroupBucket.createIndirectGroupBucket(tBuilder.build());
            GroupDescription l2UnfilteredGroupDesc = new DefaultGroupDescription(deviceId,
                    GroupDescription.Type.INDIRECT,
                    new GroupBuckets(Collections.singletonList(l2UnfilteredGroupBucket)),
                    l2UnfilterGroupKey,
                    l2UnfilteredGroupId,
                    nextObj.appId());
            log.debug("Trying L2-Unfiltered: device:{} gid:{} gkey:{} nextid:{}",
                    deviceId, Integer.toHexString(l2UnfilteredGroupId), l2UnfilterGroupKey, nextObj.id());
            groupInfoBuilder.add(new GroupInfo(l2UnfilteredGroupDesc, l2UnfilteredGroupDesc));
        });
        // Save the current count
        int counts = groupInfoBuilder.build().size();
        // Lookup each nextId in the store and obtain the group information
        nextIds.forEach(nextId -> {
            NextGroup nextGroup = flowObjectiveStore.getNextGroup(nextId);
            if (nextGroup != null) {
                List<Deque<GroupKey>> allActiveKeys = appKryo.deserialize(nextGroup.data());
                GroupKey topGroupKey = allActiveKeys.get(0).getFirst();
                GroupDescription groupDesc = groupService.getGroup(deviceId, topGroupKey);
                if (groupDesc != null) {
                    log.debug("Trying L2-Hash device:{} gid:{}, gkey:{}, nextid:{}",
                              deviceId, Integer.toHexString(((Group) groupDesc).id().id()), topGroupKey, nextId);
                    groupInfoBuilder.add(new GroupInfo(groupDesc, groupDesc));
                } else {
                    log.error("Not found L2-Hash device:{}, gkey:{}, nextid:{}", deviceId, topGroupKey, nextId);
                }
            } else {
                log.error("Not found NextGroup device:{}, nextid:{}", deviceId, nextId);
            }
        });
        // Compare the size before and after to detect problems during the creation
        ImmutableList<GroupInfo> groupInfos = groupInfoBuilder.build();
        return (counts + nextIds.size()) == groupInfos.size() ? groupInfos : ImmutableList.of();
    }

    private List<GroupInfo> prepareL2InterfaceGroup(NextObjective nextObj, VlanId assignedVlan) {
        ImmutableList.Builder<GroupInfo> groupInfoBuilder = ImmutableList.builder();
        // break up broadcast next objective to multiple groups
        Collection<TrafficTreatment> buckets = nextObj.nextTreatments().stream()
                .filter(nt -> nt.type() == NextTreatment.Type.TREATMENT)
                .map(nt -> ((DefaultNextTreatment) nt).treatment())
                .collect(Collectors.toSet());

        // Each treatment is converted to an L2 interface group
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
                log.debug("Can't find output port for the bucket {}.", treatment);
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
                    ((l2InterfaceGroupVlan.toShort() & THREE_NIBBLE_MASK) << PORT_LEN) |
                    ((int) portNum.toLong() & FOUR_NIBBLE_MASK);
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

    private GroupInfo prepareL3UnicastGroup(NextObjective nextObj, NextGroup next) {

       ImmutableList.Builder<GroupInfo> groupInfoBuilder = ImmutableList.builder();
       TrafficTreatment treatment = nextObj.next().iterator().next();

       VlanId assignedVlan = readVlanFromSelector(nextObj.meta());
       if (assignedVlan == null) {
            log.warn("VLAN ID required by next obj is missing. Abort.");
            return null;
       }

       List<GroupInfo> l2GroupInfos = prepareL2InterfaceGroup(nextObj, assignedVlan);
       GroupDescription l2InterfaceGroupDesc = l2GroupInfos.get(0).innerMostGroupDesc();
       GroupKey l2groupkey = l2InterfaceGroupDesc.appCookie();

       TrafficTreatment.Builder outerTtb = DefaultTrafficTreatment.builder();
       VlanId vlanid = null;
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
                            outerTtb.setVlanId(vlanid);
                        break;
                    default:
                        break;
                }
            } else {
                log.debug("Driver does not handle this type of TrafficTreatment"
                        + " instruction in l2l3chain:  {} - {}", ins.type(), ins);
            }
       }

       GroupId l2groupId = new GroupId(l2InterfaceGroupDesc.givenGroupId());
       outerTtb.group(l2groupId);

       // we need the top level group's key to point the flow to it
       List<Deque<GroupKey>> gkeys = appKryo.deserialize(next.data());
       GroupKey l3groupkey = gkeys.get(0).peekFirst();
       GroupId grpId = groupService.getGroup(deviceId, l3groupkey).id();
       int l3groupId = grpId.id();

       // create the l3unicast group description to wait for the
       // l2 interface group to be processed
       GroupBucket l3UnicastGroupBucket =  DefaultGroupBucket.createIndirectGroupBucket(outerTtb.build());

       GroupDescription l3UnicastGroupDescription = new DefaultGroupDescription(deviceId,
                                                        GroupDescription.Type.INDIRECT,
                                                        new GroupBuckets(Collections.singletonList(
                                                        l3UnicastGroupBucket)), l3groupkey,
                                                        l3groupId, nextObj.appId());

      // store l2groupkey with the groupChainElem for the outer-group that depends on it
      GroupChainElem gce = new GroupChainElem(l3UnicastGroupDescription, 1, false, deviceId);
      updatePendingGroups(l2groupkey, gce);

      log.debug("Trying L3-Interface: device:{} gid:{} gkey:{} nextid:{}",
                        deviceId, Integer.toHexString(l3groupId), l3groupkey, nextObj.id());

      groupInfoBuilder.add(new GroupInfo(l2InterfaceGroupDesc,
                    l3UnicastGroupDescription));

      return groupInfoBuilder.build().iterator().next();
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
        // Let's create a new list mcast buckets
        List<GroupBucket> l3McastBuckets = createL3MulticastBucket(groupInfos);

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
    protected void processEcmpHashedNextObjective(NextObjective nextObj) {
        // storage for all group keys in the chain of groups created
        List<Deque<GroupKey>> allGroupKeys = new ArrayList<>();
        List<GroupInfo> unsentGroups = new ArrayList<>();
        createEcmpHashBucketChains(nextObj, allGroupKeys, unsentGroups);
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
    protected void createEcmpHashBucketChains(NextObjective nextObj,
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
            // here we only deal with 0 and 1 label push
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
     * Create L2 hash group.
     *
     * @param nextObj next objective
     */
    private void processL2HashedNextObjective(NextObjective nextObj) {
        int l2LbIndex = Optional.ofNullable(nextObj.meta().getCriterion(IN_PORT))
                .map(c -> (PortCriterion) c).map(PortCriterion::port).map(PortNumber::toLong).map(Long::intValue)
                .orElse(-1);
        if (l2LbIndex == -1) {
            log.warn("l2LbIndex is not found in the meta of L2 hash objective. Abort");
            return;
        }

        // Storage for all group keys in the chain of groups created
        List<Deque<GroupKey>> allGroupKeys = new ArrayList<>();
        List<GroupInfo> unsentGroups = new ArrayList<>();
        createL2HashBuckets(nextObj, allGroupKeys, unsentGroups);

        // Create L2 load balancing group
        List<GroupBucket> l2LbGroupBuckets = new ArrayList<>();
        for (GroupInfo gi : unsentGroups) {
            // create load balancing bucket to point to the outer group
            TrafficTreatment.Builder ttb = DefaultTrafficTreatment.builder();
            ttb.group(new GroupId(gi.nextGroupDesc().givenGroupId()));
            GroupBucket sbucket = DefaultGroupBucket.createSelectGroupBucket(ttb.build());
            l2LbGroupBuckets.add(sbucket);
        }

        int l2LbGroupId = L2_LB_TYPE | (TYPE_MASK & l2LbIndex);
        int l2lbgk = l2HashGroupKey(deviceId, l2LbIndex);
        GroupKey l2LbGroupKey = new DefaultGroupKey(appKryo.serialize(l2lbgk));
        GroupDescription l2LbGroupDesc =
                new DefaultGroupDescription(
                        deviceId,
                        SELECT,
                        new GroupBuckets(l2LbGroupBuckets),
                        l2LbGroupKey,
                        l2LbGroupId,
                        nextObj.appId());
        GroupChainElem l2LbGce = new GroupChainElem(l2LbGroupDesc, l2LbGroupBuckets.size(), false, deviceId);

        // Create objects for local and distributed storage
        allGroupKeys.forEach(gKeyChain -> gKeyChain.addFirst(l2LbGroupKey));
        OfdpaNextGroup ofdpaGrp = new OfdpaNextGroup(allGroupKeys, nextObj);
        // Store l2LbGroupKey with the ofdpaGroupChain for the nextObjective that depends on it
        updatePendingNextObjective(l2LbGroupKey, ofdpaGrp);
        log.debug("Trying L2-LB: device:{} gid:{} gkey:{} nextId:{}",
                deviceId, Integer.toHexString(l2LbGroupId), l2LbGroupKey, nextObj.id());
        // finally we are ready to send the innermost groups
        for (GroupInfo gi : unsentGroups) {
            log.debug("Sending innermost group {} in group chain on device {} ",
                    Integer.toHexString(gi.innerMostGroupDesc().givenGroupId()), deviceId);
            updatePendingGroups(gi.nextGroupDesc().appCookie(), l2LbGce);
            groupService.addGroup(gi.innerMostGroupDesc());
        }
    }

    /**
     * Create L2 hash group buckets.
     *
     * @param nextObj next objective
     */
    private void createL2HashBuckets(NextObjective nextObj,
                                  List<Deque<GroupKey>> allGroupKeys, List<GroupInfo> unsentGroups) {
        List<GroupInfo> groupInfos = prepareL2UnfilteredGroup(nextObj);
        groupInfos.forEach(groupInfo -> {
            // Update allGroupKeys
            Deque<GroupKey> gKeyChain = new ArrayDeque<>();
            gKeyChain.addFirst(groupInfo.innerMostGroupDesc().appCookie());
            allGroupKeys.add(gKeyChain);

            // Update unsent group list
            unsentGroups.add(groupInfo);
        });
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
                // its possible that portnumbers are same but labels are different
                int label = readLabelFromTreatment(trafficTreatment);
                if (label == -1) {
                    duplicateBuckets.add(trafficTreatment);
                } else {
                    List<Integer> existing = existingPortAndLabel(allActiveKeys,
                                                 groupService, deviceId,
                                                 portNumber, label);
                    if (!existing.isEmpty()) {
                        duplicateBuckets.add(trafficTreatment);
                    } else {
                        nonDuplicateBuckets.add(trafficTreatment);
                    }
                }
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
            if (isL2Hash(nextObjective)) {
                addBucketToL2HashGroup(objectiveToAdd, allActiveKeys);
                return;
            }
            addBucketToEcmpHashGroup(objectiveToAdd, allActiveKeys);
        } else if (nextObjective.type() == NextObjective.Type.BROADCAST) {
            addBucketToBroadcastGroup(objectiveToAdd, allActiveKeys);
        }
    }

    private void addBucketToL2HashGroup(NextObjective nextObjective,
                                          List<Deque<GroupKey>> allActiveKeys) {
        // storage for all group keys in the chain of groups created
        List<Deque<GroupKey>> allGroupKeys = new ArrayList<>();
        List<GroupInfo> unsentGroups = new ArrayList<>();
        List<GroupBucket> newBuckets;
        // Prepare the l2 unfiltered groups
        createL2HashBuckets(nextObjective, allGroupKeys, unsentGroups);
        // now we can create the buckets to add to the outermost L2 hash group
        newBuckets = generateNextGroupBuckets(unsentGroups, SELECT);
        // retrieve the original l2 load balance group
        Group l2hashGroup = retrieveTopLevelGroup(allActiveKeys, deviceId,
                                                  groupService, nextObjective.id());
        if (l2hashGroup == null) {
            fail(nextObjective, ObjectiveError.GROUPMISSING);
            return;
        }
        GroupKey l2hashGroupKey = l2hashGroup.appCookie();
        int l2hashGroupId = l2hashGroup.id().id();
        GroupDescription l2hashGroupDesc = new DefaultGroupDescription(deviceId,
                                                                       SELECT,
                                                                       new GroupBuckets(newBuckets),
                                                                       l2hashGroupKey,
                                                                       l2hashGroupId,
                                                                       nextObjective.appId());
        GroupChainElem l2hashGce = new GroupChainElem(l2hashGroupDesc,
                                                      unsentGroups.size(),
                                                      true,
                                                      deviceId);
        // update new bucket-chains
        List<Deque<GroupKey>> addedKeys = new ArrayList<>();
        for (Deque<GroupKey> newBucketChain : allGroupKeys) {
            newBucketChain.addFirst(l2hashGroupKey);
            addedKeys.add(newBucketChain);
        }
        updatePendingNextObjective(l2hashGroupKey,
                                   new OfdpaNextGroup(addedKeys, nextObjective));
        log.debug("Adding to L2HASH: device:{} gid:{} group key:{} nextId:{}",
                  deviceId, Integer.toHexString(l2hashGroupId),
                  l2hashGroupKey, nextObjective.id());
        unsentGroups.forEach(groupInfo -> {
            // send the innermost group
            log.debug("Sending innermost group {} in group chain on device {} ",
                      Integer.toHexString(groupInfo.innerMostGroupDesc().givenGroupId()),
                      deviceId);
            updatePendingGroups(groupInfo.nextGroupDesc().appCookie(), l2hashGce);
            groupService.addGroup(groupInfo.innerMostGroupDesc());
        });
    }

    private void addBucketToEcmpHashGroup(NextObjective nextObjective,
                                      List<Deque<GroupKey>> allActiveKeys) {
        // storage for all group keys in the chain of groups created
        List<Deque<GroupKey>> allGroupKeys = new ArrayList<>();
        List<GroupInfo> unsentGroups = new ArrayList<>();
        List<GroupBucket> newBuckets;
        createEcmpHashBucketChains(nextObjective, allGroupKeys, unsentGroups);
        // now we can create the buckets to add to the outermost L3 ECMP group
        newBuckets = generateNextGroupBuckets(unsentGroups, SELECT);
        // retrieve the original L3 ECMP group
        Group l3ecmpGroup = retrieveTopLevelGroup(allActiveKeys, deviceId,
                                                  groupService, nextObjective.id());
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
        Group l2FloodGroup = retrieveTopLevelGroup(allActiveKeys, deviceId,
                                                   groupService, nextObj.id());

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
        // Create the buckets to add to the outermost L3 Multicast group
        List<GroupBucket> newBuckets = createL3MulticastBucket(groupInfos);

        // get the group being edited
        Group l3mcastGroup = retrieveTopLevelGroup(allActiveKeys, deviceId,
                                                   groupService, nextObj.id());
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
     * Removes buckets in the top level group of a possible group-chain. Does
     * not remove the groups in the group-chain pointed to by this bucket, as they
     * may be in use (referenced by other groups) elsewhere.
     *
     * @param nextObjective a next objective that contains information for the
     *                          buckets to be removed from the group
     * @param next the representation of the existing group-chains for this next
     *          objective, from which the top-level buckets to remove are determined
     */
    protected void removeBucketFromGroup(NextObjective nextObjective, NextGroup next) {
        if (nextObjective.type() != NextObjective.Type.HASHED &&
                nextObjective.type() != NextObjective.Type.BROADCAST) {
            log.warn("RemoveBuckets not applied to nextType:{} in dev:{} for next:{}",
                    nextObjective.type(), deviceId, nextObjective.id());
            fail(nextObjective, ObjectiveError.UNSUPPORTED);
            return;
        }
        List<Deque<GroupKey>> allActiveKeys = appKryo.deserialize(next.data());
        List<Integer> indicesToRemove = Lists.newArrayList();
        for (TrafficTreatment treatment : nextObjective.next()) {
            // find the top-level bucket in the group-chain by matching the
            // outport and label from different groups in the chain
            PortNumber portToRemove = readOutPortFromTreatment(treatment);
            int labelToRemove = readLabelFromTreatment(treatment);
            if (portToRemove == null) {
                log.warn("treatment {} of next objective {} has no outport.. "
                        + "cannot remove bucket from group in dev: {}", treatment,
                        nextObjective.id(), deviceId);
                continue;
            }
            List<Integer> existing = existingPortAndLabel(allActiveKeys,
                                                          groupService, deviceId,
                                                          portToRemove, labelToRemove);
            indicesToRemove.addAll(existing);

        }

        List<Deque<GroupKey>> chainsToRemove = Lists.newArrayList();
        indicesToRemove.forEach(index -> chainsToRemove
                                .add(allActiveKeys.get(index)));
        if (chainsToRemove.isEmpty()) {
            log.warn("Could not find appropriate group-chain for removing bucket"
                    + " for next id {} in dev:{}", nextObjective.id(), deviceId);
            fail(nextObjective, ObjectiveError.BADPARAMS);
            return;
        }
        removeBucket(chainsToRemove, nextObjective);
    }

    /**
     * Removes top-level buckets from a group that represents the given next objective.
     *
     * @param chainsToRemove a list of group bucket chains to remove
     * @param nextObjective the next objective that contains information for the
     *                  buckets to be removed from the group
     */
    protected void removeBucket(List<Deque<GroupKey>> chainsToRemove,
                                NextObjective nextObjective) {
        List<GroupBucket> bucketsToRemove = Lists.newArrayList();
        //first group key is the one we want to modify
        GroupKey modGroupKey = chainsToRemove.get(0).peekFirst();
        Group modGroup = groupService.getGroup(deviceId, modGroupKey);
        if (modGroup == null) {
            log.warn("removeBucket(): Attempt to modify non-existent group {} for device {}", modGroupKey, deviceId);
            return;
        }
        for (Deque<GroupKey> foundChain : chainsToRemove) {
            //second group key is the one we wish to remove the reference to
            if (foundChain.size() < 2) {
                // additional check to make sure second group key exists in
                // the chain.
                log.warn("Can't find second group key from chain {}",
                         foundChain);
                continue;
            }
            GroupKey pointedGroupKey = foundChain.stream()
                                           .collect(Collectors.toList()).get(1);
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
                + "for next id {} in device {}",
                Integer.toHexString(modGroup.id().id()),
                pointedGroupIds, nextObjective.id(), deviceId);
        addPendingUpdateNextObjective(modGroupKey, nextObjective);
        groupService.removeBucketsFromGroup(deviceId, modGroupKey,
                                            removeBuckets, modGroupKey,
                                            nextObjective.appId());
        // update store - synchronize access as there may be multiple threads
        // trying to remove buckets from the same group, each with its own
        // potentially stale copy of allActiveKeys
        synchronized (flowObjectiveStore) {
            // get a fresh copy of what the store holds
            NextGroup next = flowObjectiveStore.getNextGroup(nextObjective.id());
            List<Deque<GroupKey>> allActiveKeys = appKryo.deserialize(next.data());
            allActiveKeys = Lists.newArrayList(allActiveKeys);
            // Note that since we got a new object, and ArrayDeque does not implement
            // Object.equals(), we have to check the deque elems one by one
            allActiveKeys
                .removeIf(active ->
                    chainsToRemove.stream().anyMatch(remove ->
                        Arrays.equals(remove.toArray(new GroupKey[0]),
                                      active.toArray(new GroupKey[0]))));
            // If no buckets in the group, then retain an entry for the
            // top level group which still exists.
            if (allActiveKeys.isEmpty()) {
                ArrayDeque<GroupKey> top = new ArrayDeque<>();
                top.add(modGroupKey);
                allActiveKeys.add(top);
            }
            flowObjectiveStore.putNextGroup(nextObjective.id(),
                                            new OfdpaNextGroup(allActiveKeys,
                                                               nextObjective));
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

        allActiveKeys
                .forEach(groupChain -> groupChain.forEach(groupKey -> groupService
                        .removeGroup(deviceId, groupKey, nextObjective.appId())));
        flowObjectiveStore.removeNextGroup(nextObjective.id());
    }

    /**
     * modifies group with next objective.
     *
     * @param nextObjective the NextObjective
     * @param nextGroup the NextGroup
    */
    protected void modifyBucketFromGroup(NextObjective nextObjective, NextGroup nextGroup) {
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
                modifySimpleNextObjective(nextObjective, nextGroup);
                break;
            default:
                fail(nextObjective, ObjectiveError.UNKNOWN);
                log.warn("Unknown next objective type {}", nextObjective.type());
        }
    }

     /**
     * As per the OFDPA 2.0 TTP, packets are sent out of ports by using
     * a chain of groups. The simple Next Objective passed in by the application
     * is broken up into a group chain. The following chains can be modified
     * depending on the parameters in the Next Objective.
     * 1. L2 Interface group (no chaining)
     * 2. L3 Unicast group -> L2 Interface group
     *
     * @param nextObj  the nextObjective of type SIMPLE
     */
    private void modifySimpleNextObjective(NextObjective nextObj, NextGroup nextGroup) {
        TrafficTreatment treatment = nextObj.next().iterator().next();
        // determine if plain L2 or L3->L2 chain
        boolean plainL2 = true;
        for (Instruction ins : treatment.allInstructions()) {
            if (ins.type() == Instruction.Type.L2MODIFICATION) {
                L2ModificationInstruction l2ins = (L2ModificationInstruction) ins;
                if (l2ins.subtype() == L2ModificationInstruction.L2SubType.ETH_DST ||
                        l2ins.subtype() == L2ModificationInstruction.L2SubType.ETH_SRC ||
                        l2ins.subtype() == L2ModificationInstruction.L2SubType.VLAN_ID) {
                    plainL2 = false;
                }
            }
        }
        if (plainL2) {
            modifyBucketInL2Group(nextObj, nextGroup);
        } else {
            modifyBucketInL3Group(nextObj, nextGroup);
        }
        return;
    }


    /**
     * Modify buckets in the L2 interface group.
     *
     * @param nextObjective a next objective that contains information for the
     *                      buckets to be modified in the group
     * @param next the representation of the existing group-chains for this next
     *             objective, from which the innermost group buckets to remove are determined
     */

    protected void modifyBucketInL2Group(NextObjective nextObjective, NextGroup next) {

        VlanId assignedVlan = readVlanFromSelector(nextObjective.meta());
        if (assignedVlan == null) {
            log.warn("VLAN ID required by simple next obj is missing. Abort.");
            fail(nextObjective, ObjectiveError.BADPARAMS);
            return;
        }

        List<GroupInfo> groupInfos = prepareL2InterfaceGroup(nextObjective, assignedVlan);

        // There is only one L2 interface group in this case
        GroupDescription l2InterfaceGroupDesc = groupInfos.get(0).innerMostGroupDesc();

        // Replace group bucket for L2 interface group
        groupService.setBucketsForGroup(deviceId,
                                        l2InterfaceGroupDesc.appCookie(),
                                        l2InterfaceGroupDesc.buckets(),
                                        l2InterfaceGroupDesc.appCookie(),
                                        l2InterfaceGroupDesc.appId());

        // update store - synchronize access as there may be multiple threads
        // trying to remove buckets from the same group, each with its own
        // potentially stale copy of allActiveKeys
        synchronized (flowObjectiveStore) {
            // NOTE: The groupKey is computed by deviceId, VLAN and portNum. It remains the same when we modify L2IG.
            //       Therefore we use the same groupKey of the existing group.
            List<Deque<GroupKey>> allActiveKeys = appKryo.deserialize(next.data());
            flowObjectiveStore.putNextGroup(nextObjective.id(),
                                            new OfdpaNextGroup(allActiveKeys,
                                                               nextObjective));
        }

    }

    protected void modifyBucketInL3Group(NextObjective nextObjective, NextGroup next) {

        //get l3 group
        GroupInfo groupInfo = prepareL3UnicastGroup(nextObjective, next);
        if (groupInfo == null) {
            log.warn("Null groupInfo retrieved for next obj. Abort.");
            fail(nextObjective, ObjectiveError.BADPARAMS);
            return;
        }

        GroupDescription l3UnicastGroupDesc = groupInfo.nextGroupDesc();

        // Replace group bucket for L3 UC interface group
        groupService.setBucketsForGroup(deviceId, l3UnicastGroupDesc.appCookie(),
                        l3UnicastGroupDesc.buckets(), l3UnicastGroupDesc.appCookie(),
                        l3UnicastGroupDesc.appId());

        // create object for local and distributed storage
        Deque<GroupKey> gkeyChain = new ArrayDeque<>();
        gkeyChain.addFirst(groupInfo.innerMostGroupDesc().appCookie());
        gkeyChain.addFirst(groupInfo.nextGroupDesc().appCookie());
        List<Deque<GroupKey>> allGroupKeys = Lists.newArrayList();
        allGroupKeys.add(gkeyChain);
        OfdpaNextGroup ofdpaGrp = new OfdpaNextGroup(allGroupKeys, nextObjective);
        // store l3groupkey with the ofdpaNextGroup for the nextObjective that depends on it
        updatePendingNextObjective(groupInfo.nextGroupDesc().appCookie(), ofdpaGrp);

        // update store - synchronize access as there may be multiple threads
        // trying to update bucket from the same group, each with its own
        // potentially stale copy of allActiveKeys
        synchronized (flowObjectiveStore) {

            List<Deque<GroupKey>> modifiedGroupKeys = Lists.newArrayList();
            ArrayDeque<GroupKey> top = new ArrayDeque<>();
            top.add(l3UnicastGroupDesc.appCookie());
            top.add(groupInfo.innerMostGroupDesc().appCookie()); //l2 group key
            modifiedGroupKeys.add(top);

            flowObjectiveStore.putNextGroup(nextObjective.id(),
                                            new OfdpaNextGroup(modifiedGroupKeys,
                                                               nextObjective));
        }
    }


    /**
     *  Checks existing buckets in {@link NextGroup}  to verify if they match
     *  the buckets in the given {@link NextObjective}. Adds or removes buckets
     *  to ensure that the buckets match up.
     *
     * @param nextObjective the next objective to verify
     * @param next the representation of the existing group which has to be
     *             modified to match the given next objective
     */
    protected void verifyGroup(NextObjective nextObjective, NextGroup next) {
        if (nextObjective.type() == NextObjective.Type.SIMPLE) {
            log.warn("verification not supported for indirect group");
            fail(nextObjective, ObjectiveError.UNSUPPORTED);
            return;
        }
        log.trace("Call to verify device:{} nextId:{}", deviceId, nextObjective.id());
        List<Deque<GroupKey>> allActiveKeys = appKryo.deserialize(next.data());
        List<TrafficTreatment> bucketsToCreate = Lists.newArrayList();
        List<Integer> indicesToRemove = Lists.newArrayList();

        // Iterating over the treatments of the next objective allows
        // to detect missing buckets and/or duplicate buckets (to be removed)
        for (TrafficTreatment bkt : nextObjective.next()) {
            PortNumber portNumber = readOutPortFromTreatment(bkt);
            int label = readLabelFromTreatment(bkt);
            if (portNumber == null) {
                log.warn("treatment {} of next objective {} has no outport.. "
                        + "cannot remove bucket from group in dev: {}", bkt, nextObjective.id(), deviceId);
                fail(nextObjective, ObjectiveError.BADPARAMS);
                return;
            }
            List<Integer> existing = existingPortAndLabel(allActiveKeys, groupService, deviceId, portNumber, label);
            if (existing.isEmpty()) {
                // if it doesn't exist, mark this bucket for creation
                bucketsToCreate.add(bkt);
            }
            if (existing.size() > 1) {
                // if it exists but there are duplicates, mark the others for removal
                existing.remove(0);
                indicesToRemove.addAll(existing);
            }
        }

        // Detect situation where the next data has more buckets
        // (not duplicates) respect to the next objective
        if (allActiveKeys.size() > nextObjective.next().size() &&
                // ignore specific case of empty group
                !(nextObjective.next().size() == 0 && allActiveKeys.size() == 1
                && allActiveKeys.get(0).size() == 1)) {
            log.warn("Mismatch detected between next and flowobjstore for device {}: " +
                    "nextId:{}, nextObjective-size:{} next-size:{} .. correcting",
                    deviceId, nextObjective.id(), nextObjective.next().size(),
                    allActiveKeys.size());
            List<Integer> otherIndices = indicesToRemoveFromNextGroup(allActiveKeys, nextObjective,
                    groupService, deviceId);
            // Filter out the indices not present
            otherIndices = otherIndices.stream()
                    .filter(index -> !indicesToRemove.contains(index))
                    .collect(Collectors.toList());
            // Add all to the final list
            indicesToRemove.addAll(otherIndices);
        }

        log.trace("Buckets to create {}", bucketsToCreate);
        log.trace("Indices to remove {}", indicesToRemove);

        if (!bucketsToCreate.isEmpty()) {
            log.info("creating {} buckets as part of nextId: {} verification",
                     bucketsToCreate.size(), nextObjective.id());
            //create a nextObjective only with these buckets
            NextObjective.Builder nextObjBuilder = DefaultNextObjective.builder()
                    .withId(nextObjective.id())
                    .withType(nextObjective.type())
                    .withMeta(nextObjective.meta())
                    .fromApp(nextObjective.appId());
            bucketsToCreate.forEach(nextObjBuilder::addTreatment);
            // According to the next type we call the proper add function
            if (nextObjective.type() == NextObjective.Type.HASHED) {
                if (isL2Hash(nextObjective)) {
                    addBucketToL2HashGroup(nextObjBuilder.addToExisting(), allActiveKeys);
                } else {
                    addBucketToEcmpHashGroup(nextObjBuilder.addToExisting(), allActiveKeys);
                }
            } else {
                addBucketToBroadcastGroup(nextObjBuilder.addToExisting(), allActiveKeys);
            }
        }

        if (!indicesToRemove.isEmpty()) {
            log.info("removing {} buckets as part of nextId: {} verification",
                     indicesToRemove.size(), nextObjective.id());
            List<Deque<GroupKey>> chainsToRemove = Lists.newArrayList();
            indicesToRemove.forEach(index -> chainsToRemove.add(allActiveKeys.get(index)));
            removeBucket(chainsToRemove, nextObjective);
        }

        log.trace("Checking mismatch with GroupStore device:{} nextId:{}",
                  deviceId, nextObjective.id());
        if (bucketsToCreate.isEmpty() && indicesToRemove.isEmpty()) {
            // flowObjective store record is in-sync with nextObjective passed-in
            // Nevertheless groupStore may not be in sync due to bug in the store
            // - see CORD-1844. XXX When this bug is fixed, the rest of this verify
            // method will not be required.
            GroupKey topGroupKey = allActiveKeys.get(0).peekFirst();
            Group topGroup = groupService.getGroup(deviceId, topGroupKey);
            // topGroup should not be null - adding guard
            if (topGroup == null) {
                log.warn("topGroup {} not found in GroupStore device:{}, nextId:{}",
                         topGroupKey, deviceId, nextObjective.id());
                fail(nextObjective, ObjectiveError.GROUPMISSING);
                return;
            }
            int actualGroupSize = topGroup.buckets().buckets().size();
            int objGroupSize = nextObjective.next().size();
            if (actualGroupSize != objGroupSize) {
                log.warn("Mismatch detected in device:{}, nextId:{}, nextObjective-size"
                        + ":{} group-size:{} .. correcting", deviceId, nextObjective.id(),
                        objGroupSize, actualGroupSize);
            }
            if (actualGroupSize > objGroupSize) {
                // Group in the device has more chains
                List<GroupBucket> bucketsToRemove = Lists.newArrayList();
                //check every bucket in the actual group
                for (GroupBucket bucket : topGroup.buckets().buckets()) {
                    GroupInstruction g = (GroupInstruction) bucket.treatment()
                                            .allInstructions().iterator().next();
                    GroupId gidToCheck = g.groupId(); // the group pointed to
                    boolean matches = false;
                    for (Deque<GroupKey> validChain : allActiveKeys) {
                        if (validChain.size() < 2) {
                            continue;
                        }
                        GroupKey pointedGroupKey = validChain.stream().collect(Collectors.toList()).get(1);
                        Group pointedGroup = groupService.getGroup(deviceId, pointedGroupKey);
                        if (pointedGroup != null && gidToCheck.equals(pointedGroup.id())) {
                            matches = true;
                            break;
                        }
                    }
                    if (!matches) {
                        log.warn("Removing bucket pointing to groupId:{}", gidToCheck);
                        bucketsToRemove.add(bucket);
                    }
                }
                // remove buckets for which there was no record in the obj store
                if (bucketsToRemove.isEmpty()) {
                    log.warn("Mismatch detected but could not determine which"
                            + "buckets to remove");
                } else {
                    GroupBuckets removeBuckets = new GroupBuckets(bucketsToRemove);
                    groupService.removeBucketsFromGroup(deviceId, topGroupKey, removeBuckets, topGroupKey,
                            nextObjective.appId());
                }
            } else if (actualGroupSize < objGroupSize) {
                // Group in the device has less chains
                // should also add buckets not in group-store but in obj-store
                List<GroupBucket> bucketsToAdd = Lists.newArrayList();
                //check every bucket in the obj
                for (Deque<GroupKey> validChain : allActiveKeys) {
                    if (validChain.size() < 2) {
                        continue;
                    }
                    GroupKey pointedGroupKey = validChain.stream().collect(Collectors.toList()).get(1);
                    Group pointedGroup = groupService.getGroup(deviceId, pointedGroupKey);
                    if (pointedGroup == null) {
                        // group should exist, otherwise cannot be added as bucket
                        continue;
                    }
                    boolean matches = false;
                    for (GroupBucket bucket : topGroup.buckets().buckets()) {
                        GroupInstruction g = (GroupInstruction) bucket.treatment()
                                                .allInstructions().iterator().next();
                        GroupId gidToCheck = g.groupId(); // the group pointed to
                        if (pointedGroup.id().equals(gidToCheck)) {
                            matches = true;
                            break;
                        }
                    }
                    if (!matches) {
                        log.warn("Adding bucket pointing to groupId:{}", pointedGroup);
                        TrafficTreatment t = DefaultTrafficTreatment.builder()
                                                .group(pointedGroup.id())
                                                .build();
                        // Create the proper bucket according to the next type
                        if (nextObjective.type() == NextObjective.Type.HASHED) {
                            bucketsToAdd.add(DefaultGroupBucket.createSelectGroupBucket(t));
                        } else {
                            bucketsToAdd.add(DefaultGroupBucket.createAllGroupBucket(t));
                        }
                    }
                }
                if (bucketsToAdd.isEmpty()) {
                    log.warn("Mismatch detected but could not determine which "
                            + "buckets to add");
                } else {
                    GroupBuckets addBuckets = new GroupBuckets(bucketsToAdd);
                    groupService.addBucketsToGroup(deviceId, topGroupKey,
                                                   addBuckets, topGroupKey,
                                                   nextObjective.appId());
                }
            }
        }
        log.trace("Verify done for device:{} nextId:{}", deviceId, nextObjective.id());
        pass(nextObjective);
    }

    //////////////////////////////////////
    //  Helper methods and classes
    //////////////////////////////////////

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

    protected void addPendingRemoveNextObjective(NextObjective nextObjective,
                                                 List<GroupKey> groupKeys) {
        pendingRemoveNextObjectives.put(nextObjective, groupKeys);
    }

    protected int getNextAvailableIndex() {
        return (int) nextIndex.incrementAndGet();
    }

    protected void processPendingUpdateNextObjs(GroupKey groupKey) {
        Set<NextObjective> nextObjs = pendingUpdateNextObjectives.remove(groupKey);
        if (nextObjs != null) {
            nextObjs.forEach(nextObj -> {
                log.debug("Group {} updated, update pending next objective {}.",
                          groupKey, nextObj);
                pass(nextObj);
            });
        }
    }

    protected void processPendingRemoveNextObjs(GroupKey key) {
        pendingRemoveNextObjectives.asMap().forEach((nextObjective, groupKeys) -> {
            groupKeys.remove(key);
            if (groupKeys.isEmpty()) {
                pendingRemoveNextObjectives.invalidate(nextObjective);
                pass(nextObjective);
            }
        });
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
                allActiveKeys = Lists.newArrayList(allActiveKeys);
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
