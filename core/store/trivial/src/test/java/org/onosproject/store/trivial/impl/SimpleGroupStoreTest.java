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
package org.onosproject.store.trivial.impl;

import static org.junit.Assert.assertEquals;
import static org.onosproject.net.DeviceId.deviceId;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.core.GroupId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.group.DefaultGroupBucket;
import org.onosproject.net.group.DefaultGroupDescription;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupEvent;
import org.onosproject.net.group.GroupKey;
import org.onosproject.net.group.GroupStoreDelegate;
import org.onosproject.net.group.GroupStore.UpdateType;

/**
 * Test of the simple DeviceStore implementation.
 */
public class SimpleGroupStoreTest {

    private SimpleGroupStore simpleGroupStore;

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

    public class TestGroupKey implements GroupKey {
        private String groupId;

        public TestGroupKey(String id) {
            this.groupId = id;
        }

        public String id() {
            return this.groupId;
        }

        @Override
        public int hashCode() {
            return groupId.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof TestGroupKey) {
                return this.groupId.equals(((TestGroupKey) obj).id());
            }
            return false;
        }
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
            } else if (expectedEvent == GroupEvent.Type.GROUP_UPDATE_REQUESTED) {
                assertEquals(Group.GroupState.PENDING_UPDATE,
                             event.subject().state());
            } else if (expectedEvent == GroupEvent.Type.GROUP_REMOVE_REQUESTED) {
                assertEquals(Group.GroupState.PENDING_DELETE,
                             event.subject().state());
            } else if (expectedEvent == GroupEvent.Type.GROUP_REMOVED) {
                createdGroupId = event.subject().id();
                assertEquals(Group.GroupState.PENDING_DELETE,
                             event.subject().state());
            }
        }

        public void verifyGroupId(GroupId id) {
            assertEquals(createdGroupId, id);
        }
    }

    @Test
    public void testGroupStoreOperations() {
        ApplicationId appId =
                new DefaultApplicationId(2, "org.groupstore.test");
        TestGroupKey key = new TestGroupKey("group1");
        PortNumber[] ports = {PortNumber.portNumber(31),
                              PortNumber.portNumber(32)};
        List<PortNumber> outPorts = new ArrayList<PortNumber>();
        outPorts.add(ports[0]);
        outPorts.add(ports[1]);

        List<GroupBucket> buckets = new ArrayList<GroupBucket>();
        for (PortNumber portNumber: outPorts) {
            TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
            tBuilder.setOutput(portNumber)
                    .setEthDst(MacAddress.valueOf("00:00:00:00:00:02"))
                    .setEthSrc(MacAddress.valueOf("00:00:00:00:00:01"))
                    .pushMpls()
                    .setMpls(106);
            buckets.add(DefaultGroupBucket.createSelectGroupBucket(
                                                        tBuilder.build()));
        }
        GroupBuckets groupBuckets = new GroupBuckets(buckets);
        GroupDescription groupDesc = new DefaultGroupDescription(
                                                 D1,
                                                 Group.Type.SELECT,
                                                 groupBuckets,
                                                 key,
                                                 appId);
        InternalGroupStoreDelegate checkStoreGroupDelegate =
                new InternalGroupStoreDelegate(key,
                                               groupBuckets,
                                               GroupEvent.Type.GROUP_ADD_REQUESTED);
        simpleGroupStore.setDelegate(checkStoreGroupDelegate);
        /* Testing storeGroup operation */
        simpleGroupStore.storeGroupDescription(groupDesc);

        /* Testing getGroupCount operation */
        assertEquals(1, simpleGroupStore.getGroupCount(D1));

        /* Testing getGroup operation */
        Group createdGroup = simpleGroupStore.getGroup(D1, key);
        checkStoreGroupDelegate.verifyGroupId(createdGroup.id());

        /* Testing getGroups operation */
        Iterable<Group> createdGroups = simpleGroupStore.getGroups(D1);
        int groupCount = 0;
        for (Group group:createdGroups) {
            checkStoreGroupDelegate.verifyGroupId(group.id());
            groupCount++;
        }
        assertEquals(1, groupCount);
        simpleGroupStore.unsetDelegate(checkStoreGroupDelegate);

        /* Testing addOrUpdateGroupEntry operation from southbound */
        InternalGroupStoreDelegate addGroupEntryDelegate =
                new InternalGroupStoreDelegate(key,
                                               groupBuckets,
                                               GroupEvent.Type.GROUP_ADDED);
        simpleGroupStore.setDelegate(addGroupEntryDelegate);
        simpleGroupStore.addOrUpdateGroupEntry(createdGroup);
        simpleGroupStore.unsetDelegate(addGroupEntryDelegate);

        /* Testing updateGroupDescription for ADD operation from northbound */
        TestGroupKey addKey = new TestGroupKey("group1AddBuckets");
        PortNumber[] newNeighborPorts = {PortNumber.portNumber(41),
                                         PortNumber.portNumber(42)};
        List<PortNumber> newOutPorts = new ArrayList<PortNumber>();
        newOutPorts.add(newNeighborPorts[0]);
        newOutPorts.add(newNeighborPorts[1]);

        List<GroupBucket> toAddBuckets = new ArrayList<GroupBucket>();
        for (PortNumber portNumber: newOutPorts) {
            TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
            tBuilder.setOutput(portNumber)
                    .setEthDst(MacAddress.valueOf("00:00:00:00:00:03"))
                    .setEthSrc(MacAddress.valueOf("00:00:00:00:00:01"))
                    .pushMpls()
                    .setMpls(106);
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
        GroupDescription newGroupDesc = new DefaultGroupDescription(
                                                 D1,
                                                 Group.Type.SELECT,
                                                 toAddGroupBuckets,
                                                 addKey,
                                                 appId);
        simpleGroupStore.updateGroupDescription(D1,
                                                key,
                                                UpdateType.ADD,
                                                newGroupDesc);
        simpleGroupStore.unsetDelegate(updateGroupDescDelegate);

        /* Testing updateGroupDescription for REMOVE operation from northbound */
        TestGroupKey removeKey = new TestGroupKey("group1RemoveBuckets");
        List<GroupBucket> toRemoveBuckets = new ArrayList<GroupBucket>();
        toRemoveBuckets.add(updatedGroupBuckets.buckets().get(0));
        toRemoveBuckets.add(updatedGroupBuckets.buckets().get(1));
        GroupBuckets toRemoveGroupBuckets = new GroupBuckets(toRemoveBuckets);
        List<GroupBucket> remainingBuckets = new ArrayList<GroupBucket>();
        remainingBuckets.add(updatedGroupBuckets.buckets().get(2));
        remainingBuckets.add(updatedGroupBuckets.buckets().get(3));
        GroupBuckets remainingGroupBuckets = new GroupBuckets(remainingBuckets);
        InternalGroupStoreDelegate removeGroupDescDelegate =
                new InternalGroupStoreDelegate(removeKey,
                                               remainingGroupBuckets,
                                               GroupEvent.Type.GROUP_UPDATE_REQUESTED);
        simpleGroupStore.setDelegate(removeGroupDescDelegate);
        GroupDescription removeGroupDesc = new DefaultGroupDescription(
                                                        D1,
                                                        Group.Type.SELECT,
                                                        toRemoveGroupBuckets,
                                                        removeKey,
                                                        appId);
        simpleGroupStore.updateGroupDescription(D1,
                                                addKey,
                                                UpdateType.REMOVE,
                                                removeGroupDesc);
        simpleGroupStore.unsetDelegate(removeGroupDescDelegate);

        /* Testing getGroup operation */
        Group existingGroup = simpleGroupStore.getGroup(D1, removeKey);
        checkStoreGroupDelegate.verifyGroupId(existingGroup.id());

        /* Testing addOrUpdateGroupEntry operation from southbound */
        InternalGroupStoreDelegate updateGroupEntryDelegate =
                new InternalGroupStoreDelegate(removeKey,
                                               remainingGroupBuckets,
                                               GroupEvent.Type.GROUP_UPDATED);
        simpleGroupStore.setDelegate(updateGroupEntryDelegate);
        simpleGroupStore.addOrUpdateGroupEntry(existingGroup);
        simpleGroupStore.unsetDelegate(updateGroupEntryDelegate);

        /* Testing deleteGroupDescription operation from northbound */
        InternalGroupStoreDelegate deleteGroupDescDelegate =
                new InternalGroupStoreDelegate(removeKey,
                                               remainingGroupBuckets,
                                               GroupEvent.Type.GROUP_REMOVE_REQUESTED);
        simpleGroupStore.setDelegate(deleteGroupDescDelegate);
        simpleGroupStore.deleteGroupDescription(D1, removeKey);
        simpleGroupStore.unsetDelegate(deleteGroupDescDelegate);

        /* Testing removeGroupEntry operation from southbound */
        InternalGroupStoreDelegate removeGroupEntryDelegate =
                new InternalGroupStoreDelegate(removeKey,
                                               remainingGroupBuckets,
                                               GroupEvent.Type.GROUP_REMOVED);
        simpleGroupStore.setDelegate(removeGroupEntryDelegate);
        simpleGroupStore.removeGroupEntry(existingGroup);

        /* Testing getGroup operation */
        existingGroup = simpleGroupStore.getGroup(D1, removeKey);
        assertEquals(null, existingGroup);
        Iterable<Group> existingGroups  = simpleGroupStore.getGroups(D1);
        groupCount = 0;
        for (Group tmp:existingGroups) {
            /* To avoid warning */
            assertEquals(null, tmp);
            groupCount++;
        }
        assertEquals(0, groupCount);
        assertEquals(0, simpleGroupStore.getGroupCount(D1));

        simpleGroupStore.unsetDelegate(removeGroupEntryDelegate);

    }
}

