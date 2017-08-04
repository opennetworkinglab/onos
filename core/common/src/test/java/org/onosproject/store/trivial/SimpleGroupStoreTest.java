/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.store.trivial;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.onosproject.net.DeviceId.deviceId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.MacAddress;
import org.onlab.packet.MplsLabel;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.core.GroupId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.group.DefaultGroup;
import org.onosproject.net.group.DefaultGroupBucket;
import org.onosproject.net.group.DefaultGroupDescription;
import org.onosproject.net.group.DefaultGroupKey;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupEvent;
import org.onosproject.net.group.GroupKey;
import org.onosproject.net.group.GroupOperation;
import org.onosproject.net.group.GroupStore.UpdateType;
import org.onosproject.net.group.GroupStoreDelegate;
import org.onosproject.net.group.StoredGroupBucketEntry;
import org.onosproject.net.group.StoredGroupEntry;

import com.google.common.collect.Iterables;

/**
 * Test of the simple DeviceStore implementation.
 */
public class SimpleGroupStoreTest {

    private SimpleGroupStore simpleGroupStore;
    private final ApplicationId appId =
            new DefaultApplicationId(2, "org.groupstore.test");

    public static final DeviceId D1 = deviceId("of:1");

    @Before
    public void setUp() throws Exception {
        simpleGroupStore = new SimpleGroupStore();
        simpleGroupStore.activate();
    }

    @After
    public void tearDown() throws Exception {
        simpleGroupStore.deactivate();
    }

    private class InternalGroupStoreDelegate
                implements GroupStoreDelegate {
        private GroupId createdGroupId = null;
        private GroupKey createdGroupKey;
        private GroupBuckets createdBuckets;
        private GroupEvent.Type expectedEvent;

        public InternalGroupStoreDelegate(GroupKey key,
                                          GroupBuckets buckets,
                                          GroupEvent.Type expectedEvent) {
            this.createdBuckets = buckets;
            this.createdGroupKey = key;
            this.expectedEvent = expectedEvent;
        }
        @Override
        public void notify(GroupEvent event) {
            assertEquals(expectedEvent, event.type());
            assertEquals(Group.Type.SELECT, event.subject().type());
            assertEquals(D1, event.subject().deviceId());
            assertEquals(createdGroupKey, event.subject().appCookie());
            assertEquals(createdBuckets.buckets(), event.subject().buckets().buckets());
            if (expectedEvent == GroupEvent.Type.GROUP_ADD_REQUESTED) {
                createdGroupId = event.subject().id();
                assertEquals(Group.GroupState.PENDING_ADD,
                             event.subject().state());
            } else if (expectedEvent == GroupEvent.Type.GROUP_ADDED) {
                createdGroupId = event.subject().id();
                assertEquals(Group.GroupState.ADDED,
                             event.subject().state());
            } else if (expectedEvent == GroupEvent.Type.GROUP_UPDATED) {
                createdGroupId = event.subject().id();
                assertEquals(true,
                             event.subject().buckets().
                             buckets().containsAll(createdBuckets.buckets()));
                assertEquals(true,
                             createdBuckets.buckets().
                             containsAll(event.subject().buckets().buckets()));
                for (GroupBucket bucket:event.subject().buckets().buckets()) {
                    Optional<GroupBucket> matched = createdBuckets.buckets()
                            .stream()
                            .filter((expected) -> expected.equals(bucket))
                            .findFirst();
                    assertEquals(matched.get().packets(),
                                 bucket.packets());
                    assertEquals(matched.get().bytes(),
                                 bucket.bytes());
                }
                assertEquals(Group.GroupState.ADDED,
                             event.subject().state());
            } else if (expectedEvent == GroupEvent.Type.GROUP_UPDATE_REQUESTED) {
                assertEquals(Group.GroupState.PENDING_UPDATE,
                             event.subject().state());
                for (GroupBucket bucket:event.subject().buckets().buckets()) {
                    Optional<GroupBucket> matched = createdBuckets.buckets()
                            .stream()
                            .filter((expected) -> expected.equals(bucket))
                            .findFirst();
                    assertEquals(matched.get().weight(),
                            bucket.weight());
                    assertEquals(matched.get().watchGroup(),
                            bucket.watchGroup());
                    assertEquals(matched.get().watchPort(),
                            bucket.watchPort());
                }
            } else if (expectedEvent == GroupEvent.Type.GROUP_REMOVE_REQUESTED) {
                assertEquals(Group.GroupState.PENDING_DELETE,
                             event.subject().state());
            } else if (expectedEvent == GroupEvent.Type.GROUP_REMOVED) {
                createdGroupId = event.subject().id();
                assertEquals(Group.GroupState.PENDING_DELETE,
                             event.subject().state());
            } else if (expectedEvent == GroupEvent.Type.GROUP_ADD_FAILED) {
                createdGroupId = event.subject().id();
                assertEquals(Group.GroupState.PENDING_ADD,
                        event.subject().state());
            } else if (expectedEvent == GroupEvent.Type.GROUP_UPDATE_FAILED) {
                createdGroupId = event.subject().id();
                assertEquals(Group.GroupState.PENDING_UPDATE,
                        event.subject().state());
            } else if (expectedEvent == GroupEvent.Type.GROUP_REMOVE_FAILED) {
                createdGroupId = event.subject().id();
                assertEquals(Group.GroupState.PENDING_DELETE,
                        event.subject().state());
            }
        }

        public void verifyGroupId(GroupId id) {
            assertEquals(createdGroupId, id);
        }
    }

    /**
     * Tests group store operations. The following operations are tested:
     * a)Tests device group audit completion status change
     * b)Tests storeGroup operation
     * c)Tests getGroupCount operation
     * d)Tests getGroup operation
     * e)Tests getGroups operation
     * f)Tests addOrUpdateGroupEntry operation from southbound
     * g)Tests updateGroupDescription for ADD operation from northbound
     * h)Tests updateGroupDescription for REMOVE operation from northbound
     * i)Tests deleteGroupDescription operation from northbound
     * j)Tests removeGroupEntry operation from southbound
     */
    @Test
    public void testGroupStoreOperations() {
        // Set the Device AUDIT completed in the store
        simpleGroupStore.deviceInitialAuditCompleted(D1, true);

        // Testing storeGroup operation
        GroupKey newKey = new DefaultGroupKey("group1".getBytes());
        testStoreAndGetGroup(newKey);

        // Testing addOrUpdateGroupEntry operation from southbound
        GroupKey currKey = newKey;
        testAddGroupEntryFromSB(currKey);

        // Testing updateGroupDescription for ADD operation from northbound
        newKey = new DefaultGroupKey("group1AddBuckets".getBytes());
        testAddBuckets(currKey, newKey);

        // Testing updateGroupDescription for REMOVE operation from northbound
        currKey = newKey;
        newKey = new DefaultGroupKey("group1RemoveBuckets".getBytes());
        testRemoveBuckets(currKey, newKey);

        // Testing updateGroupDescription for SET operation from northbound
        currKey = newKey;
        newKey = new DefaultGroupKey("group1SetBuckets".getBytes());
        testSetBuckets(currKey, newKey);

        // Testing addOrUpdateGroupEntry operation from southbound
        currKey = newKey;
        testUpdateGroupEntryFromSB(currKey);

        // Testing deleteGroupDescription operation from northbound
        testDeleteGroup(currKey);

        // Testing removeGroupEntry operation from southbound
        testRemoveGroupFromSB(currKey);

        // Testing removing all groups on the given device by deviceid
        newKey = new DefaultGroupKey("group1".getBytes());
        testStoreAndGetGroup(newKey);
        testDeleteGroupOnDevice(newKey);

        // Testing removing all groups on the given device
        newKey = new DefaultGroupKey("group1".getBytes());
        testStoreAndGetGroup(newKey);
        testPurgeGroupEntries();
    }

    // Testing storeGroup operation
    private void testStoreAndGetGroup(GroupKey key) {
        PortNumber[] ports = {PortNumber.portNumber(31),
                              PortNumber.portNumber(32)};
        List<PortNumber> outPorts = new ArrayList<>();
        outPorts.addAll(Arrays.asList(ports));

        List<GroupBucket> buckets = new ArrayList<>();
        for (PortNumber portNumber: outPorts) {
            TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
            tBuilder.setOutput(portNumber)
                    .setEthDst(MacAddress.valueOf("00:00:00:00:00:02"))
                    .setEthSrc(MacAddress.valueOf("00:00:00:00:00:01"))
                    .pushMpls()
                    .setMpls(MplsLabel.mplsLabel(106));
            buckets.add(DefaultGroupBucket.createSelectGroupBucket(
                                                        tBuilder.build()));
        }
        GroupBuckets groupBuckets = new GroupBuckets(buckets);
        GroupDescription groupDesc = new DefaultGroupDescription(
                                                 D1,
                                                 Group.Type.SELECT,
                                                 groupBuckets,
                                                 key,
                                                 null,
                                                 appId);
        InternalGroupStoreDelegate checkStoreGroupDelegate =
                new InternalGroupStoreDelegate(key,
                                               groupBuckets,
                                               GroupEvent.Type.GROUP_ADD_REQUESTED);
        simpleGroupStore.setDelegate(checkStoreGroupDelegate);
        // Testing storeGroup operation
        simpleGroupStore.storeGroupDescription(groupDesc);

        // Testing getGroupCount operation
        assertEquals(1, simpleGroupStore.getGroupCount(D1));

        // Testing getGroup operation
        Group createdGroup = simpleGroupStore.getGroup(D1, key);
        checkStoreGroupDelegate.verifyGroupId(createdGroup.id());

        // Testing getGroups operation
        Iterable<Group> createdGroups = simpleGroupStore.getGroups(D1);
        int groupCount = 0;
        for (Group group:createdGroups) {
            checkStoreGroupDelegate.verifyGroupId(group.id());
            groupCount++;
        }
        assertEquals(1, groupCount);
        simpleGroupStore.unsetDelegate(checkStoreGroupDelegate);
    }

    // Testing addOrUpdateGroupEntry operation from southbound
    private void testAddGroupEntryFromSB(GroupKey currKey) {
        Group existingGroup = simpleGroupStore.getGroup(D1, currKey);

        InternalGroupStoreDelegate addGroupEntryDelegate =
                new InternalGroupStoreDelegate(currKey,
                                               existingGroup.buckets(),
                                               GroupEvent.Type.GROUP_ADDED);
        simpleGroupStore.setDelegate(addGroupEntryDelegate);
        simpleGroupStore.addOrUpdateGroupEntry(existingGroup);
        simpleGroupStore.unsetDelegate(addGroupEntryDelegate);
    }

    // Testing addOrUpdateGroupEntry operation from southbound
    private void testUpdateGroupEntryFromSB(GroupKey currKey) {
        Group existingGroup = simpleGroupStore.getGroup(D1, currKey);
        int totalPkts = 0;
        int totalBytes = 0;
        List<GroupBucket> newBucketList = new ArrayList<>();
        for (GroupBucket bucket:existingGroup.buckets().buckets()) {
            StoredGroupBucketEntry newBucket =
                    (StoredGroupBucketEntry)
                    DefaultGroupBucket.createSelectGroupBucket(bucket.treatment());
            newBucket.setPackets(10);
            newBucket.setBytes(10 * 256 * 8);
            totalPkts += 10;
            totalBytes += 10 * 256 * 8;
            newBucketList.add(newBucket);
        }
        GroupBuckets updatedBuckets = new GroupBuckets(newBucketList);
        Group updatedGroup = new DefaultGroup(existingGroup.id(),
                                              existingGroup.deviceId(),
                                              existingGroup.type(),
                                              updatedBuckets);
        ((StoredGroupEntry) updatedGroup).setPackets(totalPkts);
        ((StoredGroupEntry) updatedGroup).setBytes(totalBytes);

        InternalGroupStoreDelegate updateGroupEntryDelegate =
                new InternalGroupStoreDelegate(currKey,
                                               updatedBuckets,
                                               GroupEvent.Type.GROUP_UPDATED);
        simpleGroupStore.setDelegate(updateGroupEntryDelegate);
        simpleGroupStore.addOrUpdateGroupEntry(updatedGroup);
        simpleGroupStore.unsetDelegate(updateGroupEntryDelegate);
    }

    // Testing updateGroupDescription for ADD operation from northbound
    private void testAddBuckets(GroupKey currKey, GroupKey addKey) {
        Group existingGroup = simpleGroupStore.getGroup(D1, currKey);
        List<GroupBucket> buckets = new ArrayList<>();
        buckets.addAll(existingGroup.buckets().buckets());

        PortNumber[] newNeighborPorts = {PortNumber.portNumber(41),
                                         PortNumber.portNumber(42)};
        List<PortNumber> newOutPorts = new ArrayList<>();
        newOutPorts.addAll(Collections.singletonList(newNeighborPorts[0]));

        List<GroupBucket> toAddBuckets = new ArrayList<>();
        for (PortNumber portNumber: newOutPorts) {
            TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
            tBuilder.setOutput(portNumber)
                    .setEthDst(MacAddress.valueOf("00:00:00:00:00:03"))
                    .setEthSrc(MacAddress.valueOf("00:00:00:00:00:01"))
                    .pushMpls()
                    .setMpls(MplsLabel.mplsLabel(106));
            toAddBuckets.add(DefaultGroupBucket.createSelectGroupBucket(
                                                        tBuilder.build()));
        }
        GroupBuckets toAddGroupBuckets = new GroupBuckets(toAddBuckets);
        buckets.addAll(toAddBuckets);
        GroupBuckets updatedGroupBuckets = new GroupBuckets(buckets);
        InternalGroupStoreDelegate updateGroupDescDelegate =
                new InternalGroupStoreDelegate(addKey,
                                               updatedGroupBuckets,
                                               GroupEvent.Type.GROUP_UPDATE_REQUESTED);
        simpleGroupStore.setDelegate(updateGroupDescDelegate);
        simpleGroupStore.updateGroupDescription(D1,
                                                currKey,
                                                UpdateType.ADD,
                                                toAddGroupBuckets,
                                                addKey);
        simpleGroupStore.unsetDelegate(updateGroupDescDelegate);

        short weight = 5;
        toAddBuckets = new ArrayList<>();
        for (PortNumber portNumber: newOutPorts) {
            TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
            tBuilder.setOutput(portNumber)
                    .setEthDst(MacAddress.valueOf("00:00:00:00:00:03"))
                    .setEthSrc(MacAddress.valueOf("00:00:00:00:00:01"))
                    .pushMpls()
                    .setMpls(MplsLabel.mplsLabel(106));
            toAddBuckets.add(DefaultGroupBucket.createSelectGroupBucket(
                    tBuilder.build(), weight));
        }

        toAddGroupBuckets = new GroupBuckets(toAddBuckets);
        buckets = new ArrayList<>();
        buckets.addAll(existingGroup.buckets().buckets());
        buckets.addAll(toAddBuckets);
        updatedGroupBuckets = new GroupBuckets(buckets);
        updateGroupDescDelegate =
                new InternalGroupStoreDelegate(addKey,
                                               updatedGroupBuckets,
                                               GroupEvent.Type.GROUP_UPDATE_REQUESTED);
        simpleGroupStore.setDelegate(updateGroupDescDelegate);
        simpleGroupStore.updateGroupDescription(D1,
                                                addKey,
                                                UpdateType.ADD,
                                                toAddGroupBuckets,
                                                addKey);
        simpleGroupStore.unsetDelegate(updateGroupDescDelegate);
    }

    // Testing updateGroupDescription for REMOVE operation from northbound
    private void testRemoveBuckets(GroupKey currKey, GroupKey removeKey) {
        Group existingGroup = simpleGroupStore.getGroup(D1, currKey);
        List<GroupBucket> buckets = new ArrayList<>();
        buckets.addAll(existingGroup.buckets().buckets());

        List<GroupBucket> toRemoveBuckets = new ArrayList<>();

        // There should be 4 buckets in the current group
        toRemoveBuckets.add(buckets.remove(0));
        toRemoveBuckets.add(buckets.remove(1));
        GroupBuckets toRemoveGroupBuckets = new GroupBuckets(toRemoveBuckets);

        GroupBuckets remainingGroupBuckets = new GroupBuckets(buckets);
        InternalGroupStoreDelegate removeGroupDescDelegate =
                new InternalGroupStoreDelegate(removeKey,
                                               remainingGroupBuckets,
                                               GroupEvent.Type.GROUP_UPDATE_REQUESTED);
        simpleGroupStore.setDelegate(removeGroupDescDelegate);
        simpleGroupStore.updateGroupDescription(D1,
                                                currKey,
                                                UpdateType.REMOVE,
                                                toRemoveGroupBuckets,
                                                removeKey);
        simpleGroupStore.unsetDelegate(removeGroupDescDelegate);
    }

    // Testing updateGroupDescription for SET operation from northbound
    private void testSetBuckets(GroupKey currKey, GroupKey setKey) {
        List<GroupBucket> toSetBuckets = new ArrayList<>();

        short weight = 5;
        PortNumber portNumber = PortNumber.portNumber(42);
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
        tBuilder.setOutput(portNumber)
                .setEthDst(MacAddress.valueOf("00:00:00:00:00:03"))
                .setEthSrc(MacAddress.valueOf("00:00:00:00:00:01"))
                .pushMpls()
                .setMpls(MplsLabel.mplsLabel(106));
        toSetBuckets.add(DefaultGroupBucket.createSelectGroupBucket(
                tBuilder.build(), weight));

        GroupBuckets toSetGroupBuckets = new GroupBuckets(toSetBuckets);
        InternalGroupStoreDelegate updateGroupDescDelegate =
                new InternalGroupStoreDelegate(setKey,
                        toSetGroupBuckets,
                        GroupEvent.Type.GROUP_UPDATE_REQUESTED);
        simpleGroupStore.setDelegate(updateGroupDescDelegate);
        simpleGroupStore.updateGroupDescription(D1,
                currKey,
                UpdateType.SET,
                toSetGroupBuckets,
                setKey);
        simpleGroupStore.unsetDelegate(updateGroupDescDelegate);
    }

    // Testing deleteGroupDescription operation from northbound
    private void testDeleteGroup(GroupKey currKey) {
        Group existingGroup = simpleGroupStore.getGroup(D1, currKey);
        InternalGroupStoreDelegate deleteGroupDescDelegate =
                new InternalGroupStoreDelegate(currKey,
                                               existingGroup.buckets(),
                                               GroupEvent.Type.GROUP_REMOVE_REQUESTED);
        simpleGroupStore.setDelegate(deleteGroupDescDelegate);
        simpleGroupStore.deleteGroupDescription(D1, currKey);
        simpleGroupStore.unsetDelegate(deleteGroupDescDelegate);
    }

    // Testing deleteGroupDescription operation from northbound
    private void testDeleteGroupOnDevice(GroupKey currKey) {
        assertThat(simpleGroupStore.getGroupCount(D1), is(1));
        simpleGroupStore.purgeGroupEntry(D1);
        assertThat(simpleGroupStore.getGroupCount(D1), is(0));
    }

    // Testing purgeGroupEntries
    private void testPurgeGroupEntries() {
        assertThat(simpleGroupStore.getGroupCount(D1), is(1));
        simpleGroupStore.purgeGroupEntries();
        assertThat(simpleGroupStore.getGroupCount(D1), is(0));
    }

    // Testing removeGroupEntry operation from southbound
    private void testRemoveGroupFromSB(GroupKey currKey) {
        Group existingGroup = simpleGroupStore.getGroup(D1, currKey);
        InternalGroupStoreDelegate removeGroupEntryDelegate =
                new InternalGroupStoreDelegate(currKey,
                                               existingGroup.buckets(),
                                               GroupEvent.Type.GROUP_REMOVED);
        simpleGroupStore.setDelegate(removeGroupEntryDelegate);
        simpleGroupStore.removeGroupEntry(existingGroup);

        // Testing getGroup operation
        existingGroup = simpleGroupStore.getGroup(D1, currKey);
        assertEquals(null, existingGroup);
        assertEquals(0, Iterables.size(simpleGroupStore.getGroups(D1)));
        assertEquals(0, simpleGroupStore.getGroupCount(D1));

        simpleGroupStore.unsetDelegate(removeGroupEntryDelegate);
    }

    @Test
    public void testGroupOperationFailure() {

        simpleGroupStore.deviceInitialAuditCompleted(D1, true);

        ApplicationId appId =
                new DefaultApplicationId(2, "org.groupstore.test");
        GroupKey key = new DefaultGroupKey("group1".getBytes());
        PortNumber[] ports = {PortNumber.portNumber(31),
                PortNumber.portNumber(32)};
        List<PortNumber> outPorts = new ArrayList<>();
        outPorts.add(ports[0]);
        outPorts.add(ports[1]);

        List<GroupBucket> buckets = new ArrayList<>();
        for (PortNumber portNumber: outPorts) {
            TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
            tBuilder.setOutput(portNumber)
                    .setEthDst(MacAddress.valueOf("00:00:00:00:00:02"))
                    .setEthSrc(MacAddress.valueOf("00:00:00:00:00:01"))
                    .pushMpls()
                    .setMpls(MplsLabel.mplsLabel(106));
            buckets.add(DefaultGroupBucket.createSelectGroupBucket(
                    tBuilder.build()));
        }
        GroupBuckets groupBuckets = new GroupBuckets(buckets);
        GroupDescription groupDesc = new DefaultGroupDescription(
                D1,
                Group.Type.SELECT,
                groupBuckets,
                key,
                null,
                appId);
        InternalGroupStoreDelegate checkStoreGroupDelegate =
                new InternalGroupStoreDelegate(key,
                        groupBuckets,
                        GroupEvent.Type.GROUP_ADD_REQUESTED);
        simpleGroupStore.setDelegate(checkStoreGroupDelegate);
        // Testing storeGroup operation
        simpleGroupStore.storeGroupDescription(groupDesc);
        simpleGroupStore.unsetDelegate(checkStoreGroupDelegate);

        // Testing Group add operation failure
        Group createdGroup = simpleGroupStore.getGroup(D1, key);
        checkStoreGroupDelegate.verifyGroupId(createdGroup.id());

        GroupOperation groupAddOp = GroupOperation.
                createAddGroupOperation(createdGroup.id(),
                        createdGroup.type(),
                        createdGroup.buckets());
        InternalGroupStoreDelegate checkGroupAddFailureDelegate =
                new InternalGroupStoreDelegate(key,
                        groupBuckets,
                        GroupEvent.Type.GROUP_ADD_FAILED);
        simpleGroupStore.setDelegate(checkGroupAddFailureDelegate);
        simpleGroupStore.groupOperationFailed(D1, groupAddOp);


        // Testing Group modify operation failure
        simpleGroupStore.unsetDelegate(checkGroupAddFailureDelegate);
        GroupOperation groupModOp = GroupOperation.
                createModifyGroupOperation(createdGroup.id(),
                        createdGroup.type(),
                        createdGroup.buckets());
        InternalGroupStoreDelegate checkGroupModFailureDelegate =
                new InternalGroupStoreDelegate(key,
                        groupBuckets,
                        GroupEvent.Type.GROUP_UPDATE_FAILED);
        simpleGroupStore.setDelegate(checkGroupModFailureDelegate);
        simpleGroupStore.groupOperationFailed(D1, groupModOp);

        // Testing Group modify operation failure
        simpleGroupStore.unsetDelegate(checkGroupModFailureDelegate);
        GroupOperation groupDelOp = GroupOperation.
                createDeleteGroupOperation(createdGroup.id(),
                        createdGroup.type());
        InternalGroupStoreDelegate checkGroupDelFailureDelegate =
                new InternalGroupStoreDelegate(key,
                        groupBuckets,
                        GroupEvent.Type.GROUP_REMOVE_FAILED);
        simpleGroupStore.setDelegate(checkGroupDelFailureDelegate);
        simpleGroupStore.groupOperationFailed(D1, groupDelOp);
    }
}

