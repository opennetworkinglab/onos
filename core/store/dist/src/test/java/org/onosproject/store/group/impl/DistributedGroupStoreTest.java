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
package org.onosproject.store.group.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.testing.EqualsTester;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onosproject.cfg.ComponentConfigAdapter;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.GroupId;
import org.onosproject.mastership.MastershipServiceAdapter;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.group.DefaultGroup;
import org.onosproject.net.group.DefaultGroupBucket;
import org.onosproject.net.group.DefaultGroupDescription;
import org.onosproject.net.group.DefaultGroupKey;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.Group.GroupState;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupEvent;
import org.onosproject.net.group.GroupKey;
import org.onosproject.net.group.GroupOperation;
import org.onosproject.net.group.GroupStore;
import org.onosproject.net.group.GroupStoreDelegate;
import org.onosproject.net.group.GroupOperation.GroupMsgErrorCode;
import org.onosproject.store.cluster.messaging.ClusterCommunicationServiceAdapter;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.TestStorageService;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.onosproject.net.NetTestTools.APP_ID;
import static org.onosproject.net.NetTestTools.did;
import static org.onosproject.net.group.GroupDescription.Type.ALL;
import static org.onosproject.net.group.GroupDescription.Type.INDIRECT;
import static org.onosproject.net.group.GroupDescription.Type.SELECT;
import static org.onosproject.net.group.GroupStore.UpdateType.ADD;
import static org.onosproject.net.group.GroupStore.UpdateType.SET;
/**
 * Distributed group store test.
 */
public class DistributedGroupStoreTest {

    DeviceId deviceId1 = did("dev1");
    DeviceId deviceId2 = did("dev2");
    GroupId groupId1 = new GroupId(1);
    GroupId groupId2 = new GroupId(2);
    GroupId groupId3 = new GroupId(3);
    GroupKey groupKey1 = new DefaultGroupKey("abc".getBytes());
    GroupKey groupKey2 = new DefaultGroupKey("def".getBytes());
    GroupKey groupKey3 = new DefaultGroupKey("ghi".getBytes());

    TrafficTreatment treatment =
            DefaultTrafficTreatment.emptyTreatment();
    GroupBucket selectGroupBucket =
            DefaultGroupBucket.createSelectGroupBucket(treatment);
    GroupBucket failoverGroupBucket =
            DefaultGroupBucket.createFailoverGroupBucket(treatment,
                    PortNumber.IN_PORT, groupId1);

    GroupBuckets buckets = new GroupBuckets(ImmutableList.of(selectGroupBucket));
    GroupDescription groupDescription1 = new DefaultGroupDescription(
            deviceId1,
            ALL,
            buckets,
            groupKey1,
            groupId1.id(),
            APP_ID);
    GroupDescription groupDescription2 = new DefaultGroupDescription(
            deviceId2,
            INDIRECT,
            buckets,
            groupKey2,
            groupId2.id(),
            APP_ID);
    GroupDescription groupDescription3 = new DefaultGroupDescription(
            deviceId2,
            INDIRECT,
            buckets,
            groupKey3,
            groupId3.id(),
            APP_ID);

    DistributedGroupStore groupStoreImpl;
    GroupStore groupStore;
    ConsistentMap auditPendingReqQueue;

    static class MasterOfAll extends MastershipServiceAdapter {
        @Override
        public MastershipRole getLocalRole(DeviceId deviceId) {
            return MastershipRole.MASTER;
        }

        @Override
        public NodeId getMasterFor(DeviceId deviceId) {
            return new NodeId("foo");
        }
    }

    static class MasterNull extends MastershipServiceAdapter {
        @Override
        public MastershipRole getLocalRole(DeviceId deviceId) {
            return null;
        }

        @Override
        public NodeId getMasterFor(DeviceId deviceId) {
            return null;
        }
    }

    @Before
    public void setUp() throws Exception {
        groupStoreImpl = new DistributedGroupStore();
        groupStoreImpl.storageService = new TestStorageService();
        groupStoreImpl.clusterCommunicator = new ClusterCommunicationServiceAdapter();
        groupStoreImpl.mastershipService = new MasterOfAll();
        groupStoreImpl.cfgService = new ComponentConfigAdapter();
        groupStoreImpl.activate(null);
        groupStore = groupStoreImpl;
        auditPendingReqQueue =
                TestUtils.getField(groupStoreImpl, "auditPendingReqQueue");
    }

    @After
    public void tearDown() throws Exception {
        groupStoreImpl.deactivate();
    }

    /**
     * Tests the initial state of the store.
     */
    @Test
    public void testEmptyStore() {
        assertThat(groupStore.getGroupCount(deviceId1), is(0));
        assertThat(groupStore.getGroup(deviceId1, groupId1), nullValue());
        assertThat(groupStore.getGroup(deviceId1, groupKey1), nullValue());
    }

    /**
     * Tests adding a pending group.
     */
    @Test
    public void testAddPendingGroup() throws Exception {
        // Make sure the pending list starts out empty
        assertThat(auditPendingReqQueue.size(), is(0));

        // Add a new pending group. Make sure that the store remains empty
        groupStore.storeGroupDescription(groupDescription1);
        assertThat(groupStore.getGroupCount(deviceId1), is(0));
        assertThat(groupStore.getGroup(deviceId1, groupId1), nullValue());
        assertThat(groupStore.getGroup(deviceId1, groupKey1), nullValue());

        // Make sure the group is pending
        assertThat(auditPendingReqQueue.size(), is(1));

        groupStore.deviceInitialAuditCompleted(deviceId1, true);

        // Make sure the group isn't pending anymore
        assertThat(auditPendingReqQueue.size(), is(0));
    }


    /**
     * Tests adding and removing a group.
     */
    @Test
    public void testAddRemoveGroup() throws Exception {
        groupStore.deviceInitialAuditCompleted(deviceId1, true);
        assertThat(groupStore.deviceInitialAuditStatus(deviceId1), is(true));

        // Make sure the pending list starts out empty
        assertThat(auditPendingReqQueue.size(), is(0));

        groupStore.storeGroupDescription(groupDescription1);
        assertThat(groupStore.getGroupCount(deviceId1), is(1));
        assertThat(groupStore.getGroup(deviceId1, groupId1), notNullValue());
        assertThat(groupStore.getGroup(deviceId1, groupKey1), notNullValue());

        // Make sure that nothing is pending
        assertThat(auditPendingReqQueue.size(), is(0));

        Group groupById = groupStore.getGroup(deviceId1, groupId1);
        Group groupByKey = groupStore.getGroup(deviceId1, groupKey1);
        assertThat(groupById, notNullValue());
        assertThat(groupByKey, notNullValue());
        assertThat(groupById, is(groupByKey));
        assertThat(groupById.deviceId(), is(did("dev1")));

        groupStore.removeGroupEntry(groupById);

        assertThat(groupStore.getGroupCount(deviceId1), is(0));
        assertThat(groupStore.getGroup(deviceId1, groupId1), nullValue());
        assertThat(groupStore.getGroup(deviceId1, groupKey1), nullValue());

        // Make sure that nothing is pending
        assertThat(auditPendingReqQueue.size(), is(0));
    }

    /**
     * Tests removing all groups on the given device.
     */
    @Test
    public void testRemoveGroupOnDevice() throws Exception {
        groupStore.deviceInitialAuditCompleted(deviceId1, true);
        assertThat(groupStore.deviceInitialAuditStatus(deviceId1), is(true));
        groupStore.deviceInitialAuditCompleted(deviceId2, true);
        assertThat(groupStore.deviceInitialAuditStatus(deviceId2), is(true));

        // Make sure the pending list starts out empty
        assertThat(auditPendingReqQueue.size(), is(0));

        groupStore.storeGroupDescription(groupDescription1);
        groupStore.storeGroupDescription(groupDescription2);
        groupStore.storeGroupDescription(groupDescription3);
        assertThat(groupStore.getGroupCount(deviceId1), is(1));
        assertThat(groupStore.getGroupCount(deviceId2), is(2));

        groupStore.purgeGroupEntry(deviceId2);
        assertThat(groupStore.getGroupCount(deviceId1), is(1));
        assertThat(groupStore.getGroupCount(deviceId2), is(0));

        groupStore.purgeGroupEntries();
        assertThat(groupStore.getGroupCount(deviceId1), is(0));
        assertThat(groupStore.getGroupCount(deviceId2), is(0));
    }

    /**
     * Tests adding and removing a group.
     */
    @Test
    public void testRemoveGroupDescription() throws Exception {
        groupStore.deviceInitialAuditCompleted(deviceId1, true);

        groupStore.storeGroupDescription(groupDescription1);

        groupStore.deleteGroupDescription(deviceId1, groupKey1);

        // Group should still be there, marked for removal
        assertThat(groupStore.getGroupCount(deviceId1), is(1));
        Group queriedGroup = groupStore.getGroup(deviceId1, groupId1);
        assertThat(queriedGroup.state(), is(Group.GroupState.PENDING_DELETE));

    }

    /**
     * Tests pushing group metrics.
     */
    @Test
    public void testPushGroupMetrics() {
        groupStore.deviceInitialAuditCompleted(deviceId1, true);
        groupStore.deviceInitialAuditCompleted(deviceId2, true);

        GroupDescription groupDescription3 = new DefaultGroupDescription(
                deviceId1,
                SELECT,
                buckets,
                new DefaultGroupKey("aaa".getBytes()),
                null,
                APP_ID);

        groupStore.storeGroupDescription(groupDescription1);
        groupStore.storeGroupDescription(groupDescription2);
        groupStore.storeGroupDescription(groupDescription3);
        Group group1 = groupStore.getGroup(deviceId1, groupId1);

        assertThat(group1, instanceOf(DefaultGroup.class));
        DefaultGroup defaultGroup1 = (DefaultGroup) group1;
        defaultGroup1.setPackets(55L);
        defaultGroup1.setBytes(66L);
        groupStore.pushGroupMetrics(deviceId1, ImmutableList.of(group1));

        // Make sure the group was updated.

        Group requeryGroup1 = groupStore.getGroup(deviceId1, groupId1);
        assertThat(requeryGroup1.packets(), is(55L));
        assertThat(requeryGroup1.bytes(), is(66L));

    }

    class TestDelegate implements GroupStoreDelegate {
        private List<GroupEvent> eventsSeen = new LinkedList<>();
        @Override
        public void notify(GroupEvent event) {
            eventsSeen.add(event);
        }

        public List<GroupEvent> eventsSeen() {
            return eventsSeen;
        }

        public void resetEvents() {
            eventsSeen.clear();
        }
    }

    /**
     * Tests group operation failed interface.
     */
    @Test
    public void testGroupOperationFailed() {
        TestDelegate delegate = new TestDelegate();
        groupStore.setDelegate(delegate);
        groupStore.deviceInitialAuditCompleted(deviceId1, true);
        groupStore.deviceInitialAuditCompleted(deviceId2, true);

        groupStore.storeGroupDescription(groupDescription1);
        groupStore.storeGroupDescription(groupDescription2);

        List<GroupEvent> eventsAfterAdds = delegate.eventsSeen();
        assertThat(eventsAfterAdds, hasSize(2));
        eventsAfterAdds.forEach(event -> assertThat(event.type(), is(GroupEvent.Type.GROUP_ADD_REQUESTED)));
        delegate.resetEvents();

        GroupOperation opAdd =
                GroupOperation.createAddGroupOperation(groupId1,
                        INDIRECT,
                        buckets);
        groupStore.groupOperationFailed(deviceId1, opAdd);

        List<GroupEvent> eventsAfterAddFailed = delegate.eventsSeen();
        assertThat(eventsAfterAddFailed, hasSize(2));
        assertThat(eventsAfterAddFailed.get(0).type(),
                is(GroupEvent.Type.GROUP_ADD_FAILED));
        assertThat(eventsAfterAddFailed.get(1).type(),
                is(GroupEvent.Type.GROUP_REMOVED));
        delegate.resetEvents();

        GroupOperation opModify =
                GroupOperation.createModifyGroupOperation(groupId2,
                        INDIRECT,
                        buckets);
        groupStore.groupOperationFailed(deviceId2, opModify);
        List<GroupEvent> eventsAfterModifyFailed = delegate.eventsSeen();
        assertThat(eventsAfterModifyFailed, hasSize(1));
        assertThat(eventsAfterModifyFailed.get(0).type(),
                is(GroupEvent.Type.GROUP_UPDATE_FAILED));
        delegate.resetEvents();

        GroupOperation opDelete =
                GroupOperation.createDeleteGroupOperation(groupId2,
                        INDIRECT);
        groupStore.groupOperationFailed(deviceId2, opDelete);
        List<GroupEvent> eventsAfterDeleteFailed = delegate.eventsSeen();
        assertThat(eventsAfterDeleteFailed, hasSize(1));
        assertThat(eventsAfterDeleteFailed.get(0).type(),
                is(GroupEvent.Type.GROUP_REMOVE_FAILED));
        delegate.resetEvents();
    }

    /**
     * Tests group operation failed interface, with error codes for failures.
     */
    @Test
    public void testGroupOperationFailedWithErrorCode() {
        TestDelegate delegate = new TestDelegate();
        groupStore.setDelegate(delegate);
        groupStore.deviceInitialAuditCompleted(deviceId1, true);
        groupStore.storeGroupDescription(groupDescription1);
        groupStore.deviceInitialAuditCompleted(deviceId2, true);
        groupStore.storeGroupDescription(groupDescription2);

        List<GroupEvent> eventsAfterAdds = delegate.eventsSeen();
        assertThat(eventsAfterAdds, hasSize(2));
        eventsAfterAdds.forEach(event -> assertThat(event
                .type(), is(GroupEvent.Type.GROUP_ADD_REQUESTED)));
        delegate.resetEvents();

        // test group exists
        GroupOperation opAdd = GroupOperation
                .createAddGroupOperation(groupId1, INDIRECT, buckets);
        GroupOperation addFailedExists = GroupOperation
                .createFailedGroupOperation(opAdd, GroupMsgErrorCode.GROUP_EXISTS);
        groupStore.groupOperationFailed(deviceId1, addFailedExists);

        List<GroupEvent> eventsAfterAddFailed = delegate.eventsSeen();
        assertThat(eventsAfterAddFailed, hasSize(2));
        assertThat(eventsAfterAddFailed.get(0).type(),
                   is(GroupEvent.Type.GROUP_ADDED));
        assertThat(eventsAfterAddFailed.get(1).type(),
                   is(GroupEvent.Type.GROUP_ADDED));
        Group g1 = groupStore.getGroup(deviceId1, groupId1);
        assertEquals(0, g1.failedRetryCount());
        delegate.resetEvents();

        // test invalid group
        Group g2 = groupStore.getGroup(deviceId2, groupId2);
        assertEquals(0, g2.failedRetryCount());
        assertEquals(GroupState.PENDING_ADD, g2.state());
        GroupOperation opAdd1 = GroupOperation
                .createAddGroupOperation(groupId2, INDIRECT, buckets);
        GroupOperation addFailedInvalid = GroupOperation
                .createFailedGroupOperation(opAdd1, GroupMsgErrorCode.INVALID_GROUP);

        groupStore.groupOperationFailed(deviceId2, addFailedInvalid);
        groupStore.pushGroupMetrics(deviceId2, ImmutableList.of());
        List<GroupEvent> eventsAfterAddFailed1 = delegate.eventsSeen();
        assertThat(eventsAfterAddFailed1, hasSize(1));
        assertThat(eventsAfterAddFailed.get(0).type(),
                   is(GroupEvent.Type.GROUP_ADD_REQUESTED));
        g2 = groupStore.getGroup(deviceId2, groupId2);
        assertEquals(1, g2.failedRetryCount());
        assertEquals(GroupState.PENDING_ADD_RETRY, g2.state());
        delegate.resetEvents();

        groupStore.groupOperationFailed(deviceId2, addFailedInvalid);
        groupStore.pushGroupMetrics(deviceId2, ImmutableList.of());
        List<GroupEvent> eventsAfterAddFailed2 = delegate.eventsSeen();
        assertThat(eventsAfterAddFailed2, hasSize(1));
        assertThat(eventsAfterAddFailed.get(0).type(),
                   is(GroupEvent.Type.GROUP_ADD_REQUESTED));
        g2 = groupStore.getGroup(deviceId2, groupId2);
        assertEquals(2, g2.failedRetryCount());
        assertEquals(GroupState.PENDING_ADD_RETRY, g2.state());
        delegate.resetEvents();

        groupStore.groupOperationFailed(deviceId2, addFailedInvalid);
        groupStore.pushGroupMetrics(deviceId2, ImmutableList.of());
        List<GroupEvent> eventsAfterAddFailed3 = delegate.eventsSeen();
        assertThat(eventsAfterAddFailed3, hasSize(2));
        assertThat(eventsAfterAddFailed.get(0).type(),
                   is(GroupEvent.Type.GROUP_ADD_FAILED));
        assertThat(eventsAfterAddFailed.get(1).type(),
                   is(GroupEvent.Type.GROUP_REMOVED));
        g2 = groupStore.getGroup(deviceId2, groupId2);
        assertEquals(null, g2);
        delegate.resetEvents();
    }

    /**
     * Tests extraneous group operations.
     */
    @Test
    public void testExtraneousOperations() {
        ArrayList<Group> extraneous;
        groupStore.deviceInitialAuditCompleted(deviceId1, true);

        groupStore.storeGroupDescription(groupDescription1);
        Group group1 = groupStore.getGroup(deviceId1, groupId1);

        extraneous = Lists.newArrayList(groupStore.getExtraneousGroups(deviceId1));
        assertThat(extraneous, hasSize(0));

        groupStore.addOrUpdateExtraneousGroupEntry(group1);
        extraneous = Lists.newArrayList(groupStore.getExtraneousGroups(deviceId1));
        assertThat(extraneous, hasSize(1));

        groupStore.removeExtraneousGroupEntry(group1);
        extraneous = Lists.newArrayList(groupStore.getExtraneousGroups(deviceId1));
        assertThat(extraneous, hasSize(0));
    }

    /**
     * Tests updating of group descriptions.
     */
    @Test
    public void testUpdateGroupDescription() {

        GroupBuckets buckets =
                new GroupBuckets(ImmutableList.of(failoverGroupBucket, selectGroupBucket));

        groupStore.deviceInitialAuditCompleted(deviceId1, true);
        groupStore.storeGroupDescription(groupDescription1);

        GroupKey newKey = new DefaultGroupKey("123".getBytes());
        groupStore.updateGroupDescription(deviceId1,
                groupKey1,
                ADD,
                buckets,
                newKey);
        Group group1 = groupStore.getGroup(deviceId1, groupId1);
        assertThat(group1.appCookie(), is(newKey));
        assertThat(group1.buckets().buckets(), hasSize(2));

        short weight = 5;
        GroupBucket selectGroupBucketWithWeight =
                DefaultGroupBucket.createSelectGroupBucket(treatment, weight);
        buckets = new GroupBuckets(ImmutableList.of(failoverGroupBucket,
                selectGroupBucketWithWeight));

        groupStore.updateGroupDescription(deviceId1,
                newKey,
                ADD,
                buckets,
                newKey);

        group1 = groupStore.getGroup(deviceId1, groupId1);
        assertThat(group1.appCookie(), is(newKey));
        assertThat(group1.buckets().buckets(), hasSize(2));
        for (GroupBucket bucket : group1.buckets().buckets()) {
            if (bucket.type() == SELECT) {
                assertEquals(weight, bucket.weight());
            }
        }

        buckets = new GroupBuckets(ImmutableList.of(selectGroupBucketWithWeight));

        groupStore.updateGroupDescription(deviceId1,
                newKey,
                SET,
                buckets,
                newKey);

        group1 = groupStore.getGroup(deviceId1, groupId1);
        assertThat(group1.appCookie(), is(newKey));
        assertThat(group1.buckets().buckets(), hasSize(1));
        GroupBucket onlyBucket = group1.buckets().buckets().iterator().next();
        assertEquals(weight, onlyBucket.weight());
    }

    @Test
    public void testEqualsGroupStoreIdMapKey() {
        DistributedGroupStore.GroupStoreIdMapKey key1 =
            new DistributedGroupStore.GroupStoreIdMapKey(deviceId1, groupId1);
        DistributedGroupStore.GroupStoreIdMapKey sameAsKey1 =
                new DistributedGroupStore.GroupStoreIdMapKey(deviceId1, groupId1);
        DistributedGroupStore.GroupStoreIdMapKey key2 =
                new DistributedGroupStore.GroupStoreIdMapKey(deviceId2, groupId1);
        DistributedGroupStore.GroupStoreIdMapKey key3 =
                new DistributedGroupStore.GroupStoreIdMapKey(deviceId1, groupId2);

        new EqualsTester()
                .addEqualityGroup(key1, sameAsKey1)
                .addEqualityGroup(key2)
                .addEqualityGroup(key3)
                .testEquals();
    }

    @Test
    public void testEqualsGroupStoreKeyMapKey() {
        DistributedGroupStore.GroupStoreKeyMapKey key1 =
                new DistributedGroupStore.GroupStoreKeyMapKey(deviceId1, groupKey1);
        DistributedGroupStore.GroupStoreKeyMapKey sameAsKey1 =
                new DistributedGroupStore.GroupStoreKeyMapKey(deviceId1, groupKey1);
        DistributedGroupStore.GroupStoreKeyMapKey key2 =
                new DistributedGroupStore.GroupStoreKeyMapKey(deviceId2, groupKey1);
        DistributedGroupStore.GroupStoreKeyMapKey key3 =
                new DistributedGroupStore.GroupStoreKeyMapKey(deviceId1, groupKey2);

        new EqualsTester()
                .addEqualityGroup(key1, sameAsKey1)
                .addEqualityGroup(key2)
                .addEqualityGroup(key3)
                .testEquals();
    }

    @Test
    public void testEqualsGroupStoreMapKey() {
        DistributedGroupStore.GroupStoreMapKey key1 =
                new DistributedGroupStore.GroupStoreMapKey(deviceId1);
        DistributedGroupStore.GroupStoreMapKey sameAsKey1 =
                new DistributedGroupStore.GroupStoreMapKey(deviceId1);
        DistributedGroupStore.GroupStoreMapKey key2 =
                new DistributedGroupStore.GroupStoreMapKey(deviceId2);
        DistributedGroupStore.GroupStoreMapKey key3 =
                new DistributedGroupStore.GroupStoreMapKey(did("dev3"));

        new EqualsTester()
                .addEqualityGroup(key1, sameAsKey1)
                .addEqualityGroup(key2)
                .addEqualityGroup(key3)
                .testEquals();
    }

    @Test
    public void testMasterNull() throws Exception {
        groupStore.deviceInitialAuditCompleted(deviceId1, true);
        assertThat(groupStore.deviceInitialAuditStatus(deviceId1), is(true));
        // Make sure the pending list starts out empty
        assertThat(auditPendingReqQueue.size(), is(0));
        //Simulate master null
        groupStoreImpl.mastershipService = new MasterNull();
        //Add a group
        groupStore.storeGroupDescription(groupDescription1);
        assertThat(groupStore.getGroupCount(deviceId1), is(0));
        assertThat(groupStore.getGroup(deviceId1, groupId1), nullValue());
        assertThat(groupStore.getGroup(deviceId1, groupKey1), nullValue());
        //reset master
        groupStoreImpl.mastershipService = new MasterOfAll();
        // Master was null when the group add attempt is made.
        // So size of the pending list should be 1 now.
        assertThat(auditPendingReqQueue.size(), is(1));
        groupStore.deviceInitialAuditCompleted(deviceId1, true);
        //After the audit , the group should be removed from pending audit queue
        assertThat(auditPendingReqQueue.size(), is(0));
        //test whether the group is added to the store
        assertThat(groupStore.getGroupCount(deviceId1), is(1));
        assertThat(groupStore.getGroup(deviceId1, groupId1), notNullValue());
        assertThat(groupStore.getGroup(deviceId1, groupKey1), notNullValue());
    }


}
