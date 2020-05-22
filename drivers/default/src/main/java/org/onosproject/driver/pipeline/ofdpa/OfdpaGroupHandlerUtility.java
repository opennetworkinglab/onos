/*
 * Copyright 2017-present Open Networking Foundation
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.onlab.packet.VlanId;
import org.onosproject.core.GroupId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.NextGroup;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.L2SubType;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.group.DefaultGroupBucket;
import org.onosproject.net.group.DefaultGroupKey;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupKey;
import org.onosproject.net.group.GroupService;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.onosproject.driver.pipeline.ofdpa.OfdpaPipelineUtility.isNotMplsBos;
import static org.onosproject.driver.pipeline.ofdpa.OfdpaGroupHandlerUtility.OfdpaMplsGroupSubType.OFDPA_GROUP_TYPE_SHIFT;
import static org.onosproject.driver.pipeline.ofdpa.OfdpaGroupHandlerUtility.OfdpaMplsGroupSubType.OFDPA_MPLS_SUBTYPE_SHIFT;
import static org.onosproject.net.flowobjective.NextObjective.Type.HASHED;
import static org.slf4j.LoggerFactory.getLogger;

public final class OfdpaGroupHandlerUtility {
    /*
     * OFDPA requires group-id's to have a certain form.
     * L2 Interface Groups have <4bits-0><12bits-vlanId><16bits-portId>
     * L3 Unicast Groups have <4bits-2><28bits-index>
     * L3 Unicast Groups for double-vlan have <4bits-2><1bit-1><12bits-vlanId><15bits-portId>
     * MPLS Interface Groups have <4bits-9><4bits:0><24bits-index>
     * L3 ECMP Groups have <4bits-7><28bits-index>
     * L2 Flood Groups have <4bits-4><12bits-vlanId><16bits-index>
     * L3 VPN Groups have <4bits-9><4bits-2><24bits-index>
     */
    public static final int L2_INTERFACE_TYPE = 0x00000000;
    static final int L2_UNFILTERED_TYPE = 0xb0000000;
    static final int L3_INTERFACE_TYPE = 0x50000000;
    static final int L3_UNICAST_TYPE = 0x20000000;
    public static final int L3_MULTICAST_TYPE = 0x60000000;
    static final int MPLS_INTERFACE_TYPE = 0x90000000;
    static final int MPLS_L3VPN_SUBTYPE = 0x92000000;
    static final int L3_ECMP_TYPE = 0x70000000;
    public static final int L2_FLOOD_TYPE = 0x40000000;
    public static final int L2_MULTICAST_TYPE = 0x30000000;
    static final int L2_LB_TYPE = 0xc0000000;

    static final int TYPE_MASK = 0x0fffffff;
    static final int SUBTYPE_MASK = 0x00ffffff;
    static final int TYPE_VLAN_MASK = 0x0000ffff;
    static final int TYPE_L3UG_DOUBLE_VLAN_MASK = 0x08000000;

    static final int THREE_NIBBLE_MASK = 0x0fff;
    static final int FOUR_NIBBLE_MASK = 0xffff;
    static final int PORT_LEN = 16;

    static final int PORT_LOWER_BITS_MASK = 0x3f;
    static final long PORT_HIGHER_BITS_MASK = ~PORT_LOWER_BITS_MASK;

    static final String HEX_PREFIX = "0x";
    private static final Logger log = getLogger(OfdpaGroupHandlerUtility.class);

    private OfdpaGroupHandlerUtility() {
        // Utility classes should not have a public or default constructor.
    }

    /**
     * Returns the outport in a traffic treatment.
     *
     * @param tt the treatment
     * @return the PortNumber for the outport or null
     */
    static PortNumber readOutPortFromTreatment(TrafficTreatment tt) {
        for (Instruction ins : tt.allInstructions()) {
            if (ins.type() == Instruction.Type.OUTPUT) {
                return ((Instructions.OutputInstruction) ins).port();
            }
        }
        return null;
    }

    /**
     * Returns the MPLS label-id in a traffic treatment.
     *
     * @param tt the traffic treatment
     * @return an integer representing the MPLS label-id, or -1 if not found
     */
    static int readLabelFromTreatment(TrafficTreatment tt) {
        for (Instruction ins : tt.allInstructions()) {
            if (ins.type() == Instruction.Type.L2MODIFICATION) {
                L2ModificationInstruction insl2 = (L2ModificationInstruction) ins;
                if (insl2.subtype() == L2SubType.MPLS_LABEL) {
                    return ((L2ModificationInstruction.ModMplsLabelInstruction) insl2)
                                .label().id();
                }
            }
        }
        return -1;
    }

    /**
     * Helper enum to handle the different MPLS group
     * types.
     */
    public enum OfdpaMplsGroupSubType {
        MPLS_INTF((short) 0),
        L2_VPN((short) 1),
        L3_VPN((short) 2),
        MPLS_TUNNEL_LABEL_1((short) 3),
        MPLS_TUNNEL_LABEL_2((short) 4),
        MPLS_SWAP_LABEL((short) 5),
        MPLS_ECMP((short) 8);

        private short value;
        public static final int OFDPA_GROUP_TYPE_SHIFT = 28;
        public static final int OFDPA_MPLS_SUBTYPE_SHIFT = 24;

        OfdpaMplsGroupSubType(short value) {
            this.value = value;
        }

        /**
         * Gets the value as an short.
         *
         * @return the value as an short
         */
        public short getValue() {
            return this.value;
        }

    }

    /**
     * Creates MPLS Label group id given a sub type and
     * the index.
     *
     * @param subType the MPLS Label group sub type
     * @param index the index of the group
     * @return the OFDPA group id
     */
    public static Integer makeMplsLabelGroupId(OfdpaMplsGroupSubType subType, int index) {
        index = index & 0x00FFFFFF;
        return index | (9 << OFDPA_GROUP_TYPE_SHIFT) | (subType.value << OFDPA_MPLS_SUBTYPE_SHIFT);
    }

    /**
     * Creates MPLS Forwarding group id given a sub type and
     * the index.
     *
     * @param subType the MPLS forwarding group sub type
     * @param index the index of the group
     * @return the OFDPA group id
     */
    public static Integer makeMplsForwardingGroupId(OfdpaMplsGroupSubType subType, int index) {
        index = index & 0x00FFFFFF;
        return index | (10 << OFDPA_GROUP_TYPE_SHIFT) | (subType.value << OFDPA_MPLS_SUBTYPE_SHIFT);
    }

    /**
     * Returns the set of existing output ports in the group represented by
     * allActiveKeys.
     *
     * @param allActiveKeys list of group key chain
     * @param groupService the group service to get group information
     * @param deviceId the device id to get group
     * @return a set of output port from the list of group key chain
     */
    public static Set<PortNumber> getExistingOutputPorts(List<Deque<GroupKey>> allActiveKeys,
                                                     GroupService groupService,
                                                     DeviceId deviceId) {
        Set<PortNumber> existingPorts = Sets.newHashSet();

        allActiveKeys.forEach(keyChain -> {
            GroupKey ifaceGroupKey = keyChain.peekLast();
            Group ifaceGroup = groupService.getGroup(deviceId, ifaceGroupKey);
            if (ifaceGroup != null && !ifaceGroup.buckets().buckets().isEmpty()) {
                ifaceGroup.buckets().buckets().forEach(bucket -> {
                    PortNumber portNumber = readOutPortFromTreatment(bucket.treatment());
                    if (portNumber != null) {
                        existingPorts.add(portNumber);
                    }
                });
            }
        });
        return existingPorts;
    }

    /**
     * Returns a list of all indices in the allActiveKeys list (that represents
     * a group) if the list element (a bucket or group-chain) has treatments
     * that match the given outport and label.
     *
     * @param allActiveKeys the representation of the group
     * @param groupService groups service for querying group information
     * @param deviceId the device id for the device that contains the group
     * @param portToMatch the port to match in the group buckets
     * @param labelToMatch the MPLS label-id to match in the group buckets
     * @return a list of indexes in the allActiveKeys list where the list element
     *         has treatments that match the given portToMatch and labelToMatch.
     *         Could be empty if no list elements were found to match the given
     *         port and label.
     */
    public static List<Integer> existingPortAndLabel(
                                               List<Deque<GroupKey>> allActiveKeys,
                                               GroupService groupService,
                                               DeviceId deviceId,
                                               PortNumber portToMatch,
                                               int labelToMatch) {
        List<Integer> indices = new ArrayList<>();
        int index = 0;
        for (Deque<GroupKey> keyChain : allActiveKeys) {
            GroupKey ifaceGroupKey = keyChain.peekLast();
            Group ifaceGroup = groupService.getGroup(deviceId, ifaceGroupKey);
            if (ifaceGroup != null && !ifaceGroup.buckets().buckets().isEmpty()) {
                PortNumber portNumber = readOutPortFromTreatment(
                   ifaceGroup.buckets().buckets().iterator().next().treatment());
                if (portNumber != null && portNumber.equals(portToMatch)) {
                    // check for label in the 2nd group of this chain
                    GroupKey secondKey = (GroupKey) keyChain.toArray()[1];
                    Group secondGroup = groupService.getGroup(deviceId, secondKey);
                    if (secondGroup != null &&
                            !secondGroup.buckets().buckets().isEmpty()) {
                        int label = readLabelFromTreatment(
                                        secondGroup.buckets().buckets()
                                        .iterator().next().treatment());
                        if (label == labelToMatch) {
                            indices.add(index);
                        }
                    }
                }
            }
            index++;
        }

        return indices;
    }

    /**
     * Get indices to remove comparing next group with next objective.
     *
     * @param allActiveKeys the representation of the group
     * @param nextObjective the next objective to verify
     * @param groupService groups service for querying group information
     * @param deviceId the device id for the device that contains the group
     * @return a list of indexes in the allActiveKeys to remove.
     */
    public static List<Integer> indicesToRemoveFromNextGroup(List<Deque<GroupKey>> allActiveKeys,
                                                       NextObjective nextObjective,
                                                       GroupService groupService,
                                                       DeviceId deviceId) {
        List<Integer> indicesToRemove = Lists.newArrayList();
        int index = 0;
        // Iterate over the chain in the next data
        for (Deque<GroupKey> keyChain : allActiveKeys) {
            // Valid chain should have at least two elements
            if (keyChain.size() >= 2) {
                // Get last group (l2if) and retrieve port number
                GroupKey ifaceGroupKey = keyChain.peekLast();
                Group ifaceGroup = groupService.getGroup(deviceId, ifaceGroupKey);
                if (ifaceGroup != null && !ifaceGroup.buckets().buckets().isEmpty()) {
                    PortNumber portNumber = readOutPortFromTreatment(
                            ifaceGroup.buckets().buckets().iterator().next().treatment());
                    // If there is not a port number continue
                    if (portNumber != null) {
                        // check for label in the 2nd group of this chain
                        GroupKey secondKey = (GroupKey) keyChain.toArray()[1];
                        Group secondGroup = groupService.getGroup(deviceId, secondKey);
                        // If there is not a second group or there are no buckets continue
                        if (secondGroup != null && !secondGroup.buckets().buckets().isEmpty()) {
                            // Get label or -1
                            int label = readLabelFromTreatment(
                                    secondGroup.buckets().buckets()
                                            .iterator().next().treatment());
                            // Iterate over the next treatments looking for the port and the label
                            boolean matches = false;
                            for (TrafficTreatment t : nextObjective.next()) {
                                PortNumber tPort = readOutPortFromTreatment(t);
                                int tLabel = readLabelFromTreatment(t);
                                if (tPort != null && tPort.equals(portNumber) && tLabel == label) {
                                    // We found it, exit
                                    matches = true;
                                    break;
                                }
                            }
                            // Not found, we have to remove it
                            if (!matches) {
                                indicesToRemove.add(index);
                            }
                        }
                    }
                }
            }
            index++;
        }
        return indicesToRemove;
    }

    /**
     * The purpose of this function is to verify if the hashed next
     * objective is supported by the current pipeline.
     *
     * @param nextObjective the hashed objective to verify
     * @return true if the hashed objective is supported. Otherwise false.
     */
    public static boolean verifyHashedNextObjective(NextObjective nextObjective) {
        // if it is not hashed, there is something wrong;
        if (nextObjective.type() != HASHED) {
            return false;
        }
        // The case non supported is the MPLS-ECMP. For now, we try
        // to create a MPLS-ECMP for the transport of a VPWS. The
        // necessary info are contained in the meta selector. In particular
        // we are looking for the case of BoS==False;
        TrafficSelector metaSelector = nextObjective.meta();
        if (metaSelector != null && isNotMplsBos(metaSelector)) {
            return false;
        }

        return true;
    }

    /**
     * Checks if given next objective is L2 hash next objective.
     *
     * @param nextObj next objective
     * @return true if the next objective is L2 hash next objective
     */
    static boolean isL2Hash(NextObjective nextObj) {
        return nextObj.next().stream()
                .flatMap(t -> t.allInstructions().stream()).allMatch(i -> i.type() == Instruction.Type.OUTPUT);
    }

    /**
     * Generates a list of group buckets from given list of group information
     * and group bucket type.
     *
     * @param groupInfos a list of group information
     * @param bucketType group bucket type
     * @return list of group bucket generate from group information
     */
    static List<GroupBucket> generateNextGroupBuckets(List<GroupInfo> groupInfos,
                                                       GroupDescription.Type bucketType) {
        List<GroupBucket> newBuckets = Lists.newArrayList();

        groupInfos.forEach(groupInfo -> {
            GroupDescription groupDesc = groupInfo.nextGroupDesc();
            TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();
            treatmentBuilder.group(new GroupId(groupDesc.givenGroupId()));
            GroupBucket newBucket = null;
            switch (bucketType) {
                case ALL:
                    newBucket =
                            DefaultGroupBucket.createAllGroupBucket(treatmentBuilder.build());
                    break;
                case INDIRECT:
                    newBucket =
                            DefaultGroupBucket.createIndirectGroupBucket(treatmentBuilder.build());
                    break;
                case SELECT:
                    newBucket =
                            DefaultGroupBucket.createSelectGroupBucket(treatmentBuilder.build());
                    break;
                case FAILOVER:
                    // TODO: support failover bucket type
                default:
                    log.warn("Unknown bucket type: {}", bucketType);
                    break;
            }

            if (newBucket != null) {
                newBuckets.add(newBucket);
            }

        });

        return ImmutableList.copyOf(newBuckets);
    }

    static List<GroupBucket> createL3MulticastBucket(List<GroupInfo> groupInfos) {
        List<GroupBucket> l3McastBuckets = new ArrayList<>();
        // For each inner group
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
        // Done return the new list of buckets
        return l3McastBuckets;
    }

    static Group retrieveTopLevelGroup(List<Deque<GroupKey>> allActiveKeys,
                                       DeviceId deviceId,
                                       GroupService groupService,
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

    /**
     * Extracts VlanId from given group ID.
     *
     * @param groupId the group ID
     * @return vlan id of the group
     */
    public static VlanId extractVlanIdFromGroupId(int groupId) {
        // Extract the 9th to 20th bit from group id as vlan id.
        short vlanId = (short) ((groupId & 0x0fff0000) >> 16);
        return VlanId.vlanId(vlanId);
    }

    public static GroupKey l2FloodGroupKey(VlanId vlanId, DeviceId deviceId) {
        int hash = Objects.hash(deviceId, vlanId);
        hash = L2_FLOOD_TYPE | TYPE_MASK & hash;
        return new DefaultGroupKey(Ofdpa2Pipeline.appKryo.serialize(hash));
    }

    public static GroupKey l2MulticastGroupKey(VlanId vlanId, DeviceId deviceId) {
        int hash = Objects.hash(deviceId, vlanId);
        hash = L2_MULTICAST_TYPE | TYPE_MASK & hash;
        return new DefaultGroupKey(Ofdpa2Pipeline.appKryo.serialize(hash));
    }

    public static int l2GroupId(VlanId vlanId, long portNum) {
        return L2_INTERFACE_TYPE | (vlanId.toShort() << 16) | (int) portNum;
    }

    public static int l2UnfilteredGroupId(long portNum) {
        return L2_UNFILTERED_TYPE | (int) portNum;
    }

    /**
     * Returns a hash as the L2 Interface Group Key.
     *
     * Keep the lower 6-bit for port since port number usually smaller than 64.
     * Hash other information into remaining 22 bits.
     *
     * @param deviceId Device ID
     * @param vlanId VLAN ID
     * @param portNumber Port number
     * @return L2 interface group key
     */
    public static int l2InterfaceGroupKey(DeviceId deviceId, VlanId vlanId, long portNumber) {
        int portLowerBits = (int) portNumber & PORT_LOWER_BITS_MASK;
        long portHigherBits = portNumber & PORT_HIGHER_BITS_MASK;
        int hash = Objects.hash(deviceId, vlanId, portHigherBits);
        return L2_INTERFACE_TYPE | (TYPE_MASK & hash << 6) | portLowerBits;
    }

    /**
     * Returns a hash as the L2 Unfiltered Interface Group Key.
     *
     * Keep the lower 6-bit for port since port number usually smaller than 64.
     * Hash other information into remaining 22 bits.
     *
     * @param deviceId Device ID
     * @param portNumber Port number
     * @return L2 unfiltered interface group key
     */
    public static int l2UnfilteredGroupKey(DeviceId deviceId, long portNumber) {
        int portLowerBits = (int) portNumber & PORT_LOWER_BITS_MASK;
        long portHigherBits = portNumber & PORT_HIGHER_BITS_MASK;
        int hash = Objects.hash(deviceId, portHigherBits);
        return L2_UNFILTERED_TYPE | (TYPE_MASK & hash << 6) | portLowerBits;
    }

    /**
     * Returns a hash as the L2 Hash Group Key.
     *
     * Keep the lower 6-bit for port since port number usually smaller than 64.
     * Hash other information into remaining 28 bits.
     *
     * @param deviceId Device ID
     * @param portNumber Port number
     * @return L2 hash group key
     */
    public static int l2HashGroupKey(DeviceId deviceId, long portNumber) {
        int portLowerBits = (int) portNumber & PORT_LOWER_BITS_MASK;
        long portHigherBits = portNumber & PORT_HIGHER_BITS_MASK;
        int hash = Objects.hash(deviceId, portHigherBits);
        return L2_LB_TYPE | (TYPE_MASK & hash << 6) | portLowerBits;
    }

    /**
     * Utility class for moving group information around.
     *
     * Example: Suppose we are trying to create a group-chain A-B-C-D, where
     * A is the top level group, and D is the inner-most group, typically L2 Interface.
     * The innerMostGroupDesc is always D. At various stages of the creation
     * process the nextGroupDesc may be C or B. The nextGroupDesc exists to
     * inform the referencing group about which group it needs to point to,
     * and wait for. In some cases the group chain may simply be A-B. In this case,
     * both innerMostGroupDesc and nextGroupDesc will be B.
     */
    public static class GroupInfo {
        /**
         * Description of the inner-most group of the group chain.
         * It is always an L2 interface group.
         */
        private GroupDescription innerMostGroupDesc;

        /**
         * Description of the next group in the group chain.
         * It can be L2 interface, L3 interface, L3 unicast, L3 VPN group.
         * It is possible that nextGroupDesc is the same as the innerMostGroup.
         */
        private GroupDescription nextGroupDesc;

        GroupInfo(GroupDescription innerMostGroupDesc, GroupDescription nextGroupDesc) {
            this.innerMostGroupDesc = innerMostGroupDesc;
            this.nextGroupDesc = nextGroupDesc;
        }

        /**
         * Getter for innerMostGroupDesc.
         *
         * @return the inner most group description
         */
        public GroupDescription innerMostGroupDesc() {
            return innerMostGroupDesc;
        }

        /**
         * Getter for the next group description.
         *
         * @return the next group description
         */
        public GroupDescription nextGroupDesc() {
            return nextGroupDesc;
        }

        /**
         * Setter of nextGroupDesc.
         *
         * @param nextGroupDesc the given value to set
         */
        public void nextGroupDesc(GroupDescription nextGroupDesc) {
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
     * resulted in these group-chains.
     *
     */
    public static class OfdpaNextGroup implements NextGroup {
        private final NextObjective nextObj;
        private final List<Deque<GroupKey>> gkeys;

        public OfdpaNextGroup(List<Deque<GroupKey>> gkeys, NextObjective nextObj) {
            this.nextObj = nextObj;
            this.gkeys = gkeys;
        }

        public NextObjective nextObjective() {
            return nextObj;
        }

        public List<Deque<GroupKey>> allKeys() {
            return gkeys;
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
     * referenced groups in the group chain to be created.
     */
    public static class GroupChainElem {
        private GroupDescription groupDescription;
        private AtomicInteger waitOnGroups;
        private boolean addBucketToGroup;
        private DeviceId deviceId;

        public GroupChainElem(GroupDescription groupDescription, int waitOnGroups,
                       boolean addBucketToGroup, DeviceId deviceId) {
            this.groupDescription = groupDescription;
            this.waitOnGroups = new AtomicInteger(waitOnGroups);
            this.addBucketToGroup = addBucketToGroup;
            this.deviceId = deviceId;
        }

        /**
         * This method atomically decrements the counter for the number of
         * groups this GroupChainElement is waiting on, for notifications from
         * the Group Service. When this method returns a value of 0, this
         * GroupChainElement is ready to be processed.
         *
         * @return integer indication of the number of notifications being waited on
         */
        int decrementAndGetGroupsWaitedOn() {
            return waitOnGroups.decrementAndGet();
        }

        public GroupDescription groupDescription() {
            return groupDescription;
        }

        public boolean addBucketToGroup() {
            return addBucketToGroup;
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

    public static class GroupChecker implements Runnable {
        final Logger log = getLogger(getClass());
        private Ofdpa2GroupHandler groupHandler;

        public GroupChecker(Ofdpa2GroupHandler groupHandler) {
            this.groupHandler = groupHandler;
        }

        @Override
        public void run() {
            // GroupChecker execution needs to be protected
            // from unhandled exceptions
            try {
                if (groupHandler.pendingGroups.size() != 0) {
                    log.debug("pending groups being checked: {}",
                              groupHandler.pendingGroups.asMap().keySet());
                }
                if (groupHandler.pendingAddNextObjectives.size() != 0) {
                    log.debug("pending add-next-obj being checked: {}",
                              groupHandler.pendingAddNextObjectives.asMap().keySet());
                }
                if (groupHandler.pendingRemoveNextObjectives.size() != 0) {
                    log.debug("pending remove-next-obj being checked: {}",
                              groupHandler.pendingRemoveNextObjectives.asMap().values());
                }
                if (groupHandler.pendingUpdateNextObjectives.size() != 0) {
                    log.debug("pending update-next-obj being checked: {}",
                              groupHandler.pendingUpdateNextObjectives.keySet());
                }

                Set<GroupKey> keys = groupHandler.pendingGroups.asMap().keySet()
                        .stream()
                        .filter(key -> groupHandler.groupService
                                .getGroup(groupHandler.deviceId, key) != null)
                        .collect(Collectors.toSet());
                Set<GroupKey> otherkeys = groupHandler.pendingAddNextObjectives
                        .asMap().keySet().stream()
                        .filter(otherkey -> groupHandler.groupService
                                .getGroup(groupHandler.deviceId, otherkey) != null)
                        .collect(Collectors.toSet());
                keys.addAll(otherkeys);
                keys.forEach(key -> groupHandler.processPendingAddGroupsOrNextObjs(key, false));

                keys = groupHandler.pendingUpdateNextObjectives.keySet()
                        .stream()
                        .filter(key -> groupHandler.groupService
                                .getGroup(groupHandler.deviceId, key) != null)
                        .collect(Collectors.toSet());
                keys.forEach(key -> groupHandler.processPendingUpdateNextObjs(key));

                Set<GroupKey> k = Sets.newHashSet();
                groupHandler.pendingRemoveNextObjectives.asMap().values().forEach(keylist -> keylist.stream()
                        .filter(key -> groupHandler.groupService.getGroup(groupHandler.deviceId, key) == null)
                        .forEach(k::add));
                k.forEach(key -> groupHandler.processPendingRemoveNextObjs(key));

            } catch (Exception exception) {
                // Just log. It is safe for now.
                log.warn("Uncaught exception is detected: {}", exception.getMessage());
                log.debug("Uncaught exception is detected (full stack trace): ", exception);
            }
        }
    }

    /**
     * Helper method to decide whether L2 Interface group or L2 Unfiltered group needs to be created.
     * L2 Unfiltered group will be created if meta has VlanIdCriterion with VlanId.ANY, and
     * treatment has set Vlan ID action.
     *
     * @param treatment  treatment passed in by the application as part of the nextObjective
     * @param meta       metadata passed in by the application as part of the nextObjective
     * @return true if L2 Unfiltered group needs to be created, false otherwise.
     */
    public static boolean createUnfiltered(TrafficTreatment treatment, TrafficSelector meta) {
        if (meta == null || treatment == null) {
            return false;
        }
        VlanIdCriterion vlanIdCriterion = (VlanIdCriterion) meta.getCriterion(Criterion.Type.VLAN_VID);
        if (vlanIdCriterion == null || !vlanIdCriterion.vlanId().equals(VlanId.ANY)) {
            return false;
        }

        return treatment.allInstructions().stream()
                .filter(i -> (i.type() == Instruction.Type.L2MODIFICATION
                        && ((L2ModificationInstruction) i).subtype() == L2ModificationInstruction.L2SubType.VLAN_ID))
                .count() == 1;
    }

    /**
     * Returns a hash as the L3 Unicast Group Key.
     *
     * Keep the lower 6-bit for port since port number usually smaller than 64.
     * Hash other information into remaining 28 bits.
     *
     * @param deviceId Device ID
     * @param vlanId vlan ID
     * @param portNumber Port number
     * @return L3 unicast group key
     */
    public static int doubleVlanL3UnicastGroupKey(DeviceId deviceId, VlanId vlanId, long portNumber) {
        int portLowerBits = (int) portNumber & PORT_LOWER_BITS_MASK;
        long portHigherBits = portNumber & PORT_HIGHER_BITS_MASK;
        int hash = Objects.hash(deviceId, portHigherBits, vlanId);
        return  L3_UNICAST_TYPE | (TYPE_MASK & hash << 6) | portLowerBits;
    }

    public static int doubleVlanL3UnicastGroupId(VlanId vlanId, long portNum) {
        // <4bits-2><1bit-1><12bits-vlanId><15bits-portId>
        return L3_UNICAST_TYPE | 1 << 27 | (vlanId.toShort() << 15) | (int) (portNum & 0x7FFF);
    }

    /**
     * Helper method to decide whether L2 Interface group or L2 Unfiltered group needs to be created.
     * L2 Unfiltered group will be created if meta has VlanIdCriterion with VlanId.ANY, and
     * treatment has set Vlan ID action.
     *
     * @param treatment  treatment passed in by the application as part of the nextObjective
     * @param meta       metadata passed in by the application as part of the nextObjective
     * @return true if L2 Unfiltered group needs to be created, false otherwise.
     */
     static boolean isUnfiltered(TrafficTreatment treatment, TrafficSelector meta) {
        if (meta == null || treatment == null) {
            return false;
        }
        VlanIdCriterion vlanIdCriterion = (VlanIdCriterion) meta.getCriterion(Criterion.Type.VLAN_VID);
        if (vlanIdCriterion == null || !vlanIdCriterion.vlanId().equals(VlanId.ANY)) {
            return false;
        }

        return treatment.allInstructions().stream()
                .filter(i -> (i.type() == Instruction.Type.L2MODIFICATION
                        && ((L2ModificationInstruction) i).subtype() == L2ModificationInstruction.L2SubType.VLAN_ID))
                .count() == 1;
    }

}
