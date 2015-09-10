/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.net.group.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.onosproject.net.NetTestTools.injectEventDispatcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.MacAddress;
import org.onlab.packet.MplsLabel;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.core.DefaultGroupId;
import org.onosproject.core.GroupId;
import org.onosproject.common.event.impl.TestEventDispatcher;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.impl.DeviceManager;
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
import org.onosproject.net.group.GroupListener;
import org.onosproject.net.group.GroupOperation;
import org.onosproject.net.group.GroupOperations;
import org.onosproject.net.group.GroupProvider;
import org.onosproject.net.group.GroupProviderRegistry;
import org.onosproject.net.group.GroupProviderService;
import org.onosproject.net.group.GroupService;
import org.onosproject.net.group.StoredGroupEntry;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.store.trivial.SimpleGroupStore;

import com.google.common.collect.Iterables;

/**
 * Test codifying the group service & group provider service contracts.
 */
public class GroupManagerTest {

    private static final ProviderId PID = new ProviderId("of", "groupfoo");
    private static final DeviceId DID = DeviceId.deviceId("of:001");

    private GroupManager mgr;
    private GroupService groupService;
    private GroupProviderRegistry providerRegistry;
    private TestGroupListener internalListener = new TestGroupListener();
    private GroupListener listener = internalListener;
    private TestGroupProvider internalProvider;
    private GroupProvider provider;
    private GroupProviderService providerService;
    private ApplicationId appId;

    @Before
    public void setUp() {
        mgr = new GroupManager();
        groupService = mgr;
        mgr.deviceService = new DeviceManager();
        mgr.store = new SimpleGroupStore();
        injectEventDispatcher(mgr, new TestEventDispatcher());
        providerRegistry = mgr;

        mgr.activate();
        mgr.addListener(listener);

        internalProvider = new TestGroupProvider(PID);
        provider = internalProvider;
        providerService = providerRegistry.register(provider);
        appId = new DefaultApplicationId(2, "org.groupmanager.test");
        assertTrue("provider should be registered",
                   providerRegistry.getProviders().contains(provider.id()));
    }

    @After
    public void tearDown() {
        providerRegistry.unregister(provider);
        assertFalse("provider should not be registered",
                    providerRegistry.getProviders().contains(provider.id()));
        mgr.removeListener(listener);
        mgr.deactivate();
        injectEventDispatcher(mgr, null);
    }

    /**
     * Tests group service north bound and south bound interfaces.
     * The following operations are tested:
     * a)Tests group creation before the device group AUDIT completes
     * b)Tests initial device group AUDIT process
     * c)Tests deletion process of any extraneous groups
     * d)Tests execution of any pending group creation requests
     * after the device group AUDIT completes
     * e)Tests re-apply process of any missing groups
     * f)Tests event notifications after receiving confirmation for
     * any operations from data plane
     * g)Tests group bucket modifications (additions and deletions)
     * h)Tests group deletion
     */
    @Test
    public void testGroupService() {
        // Test Group creation before AUDIT process
        testGroupCreationBeforeAudit();

        // Test initial group audit process
        testInitialAuditWithPendingGroupRequests();

        // Test audit with extraneous and missing groups
        testAuditWithExtraneousMissingGroups();

        // Test audit with confirmed groups
        testAuditWithConfirmedGroups();

        // Test group add bucket operations
        testAddBuckets();

        // Test group remove bucket operations
        testRemoveBuckets();

        // Test group remove operations
        testRemoveGroup();
    }

    // Test Group creation before AUDIT process
    private void testGroupCreationBeforeAudit() {
        PortNumber[] ports1 = {PortNumber.portNumber(31),
                               PortNumber.portNumber(32)};
        PortNumber[] ports2 = {PortNumber.portNumber(41),
                               PortNumber.portNumber(42)};
        GroupKey key = new DefaultGroupKey("group1BeforeAudit".getBytes());
        List<GroupBucket> buckets = new ArrayList<>();
        List<PortNumber> outPorts = new ArrayList<>();
        outPorts.addAll(Arrays.asList(ports1));
        outPorts.addAll(Arrays.asList(ports2));
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
        GroupDescription newGroupDesc = new DefaultGroupDescription(DID,
                                                                    Group.Type.SELECT,
                                                                    groupBuckets,
                                                                    key,
                                                                    null,
                                                                    appId);
        groupService.addGroup(newGroupDesc);
        internalProvider.validate(DID, null);
        assertEquals(null, groupService.getGroup(DID, key));
        assertEquals(0, Iterables.size(groupService.getGroups(DID, appId)));
    }

    // Test initial AUDIT process with pending group requests
    private void testInitialAuditWithPendingGroupRequests() {
        PortNumber[] ports1 = {PortNumber.portNumber(31),
                               PortNumber.portNumber(32)};
        PortNumber[] ports2 = {PortNumber.portNumber(41),
                               PortNumber.portNumber(42)};
        GroupId gId1 = new DefaultGroupId(1);
        Group group1 = createSouthboundGroupEntry(gId1,
                                                  Arrays.asList(ports1),
                                                  0);
        GroupId gId2 = new DefaultGroupId(2);
        // Non zero reference count will make the group manager to queue
        // the extraneous groups until reference count is zero.
        Group group2 = createSouthboundGroupEntry(gId2,
                                                  Arrays.asList(ports2),
                                                  2);
        List<Group> groupEntries = Arrays.asList(group1, group2);
        providerService.pushGroupMetrics(DID, groupEntries);
        // First group metrics would trigger the device audit completion
        // post which all pending group requests are also executed.
        GroupKey key = new DefaultGroupKey("group1BeforeAudit".getBytes());
        Group createdGroup = groupService.getGroup(DID, key);
        int createdGroupId = createdGroup.id().id();
        assertNotEquals(gId1.id(), createdGroupId);
        assertNotEquals(gId2.id(), createdGroupId);

        List<GroupOperation> expectedGroupOps = Arrays.asList(
                            GroupOperation.createDeleteGroupOperation(gId1,
                                                          Group.Type.SELECT),
                            GroupOperation.createAddGroupOperation(
                                           createdGroup.id(),
                                           Group.Type.SELECT,
                                           createdGroup.buckets()));
        internalProvider.validate(DID, expectedGroupOps);
    }

    // Test AUDIT process with extraneous groups and missing groups
    private void testAuditWithExtraneousMissingGroups() {
        PortNumber[] ports1 = {PortNumber.portNumber(31),
                               PortNumber.portNumber(32)};
        PortNumber[] ports2 = {PortNumber.portNumber(41),
                               PortNumber.portNumber(42)};
        GroupId gId1 = new DefaultGroupId(1);
        Group group1 = createSouthboundGroupEntry(gId1,
                                            Arrays.asList(ports1),
                                            0);
        GroupId gId2 = new DefaultGroupId(2);
        Group group2 = createSouthboundGroupEntry(gId2,
                                            Arrays.asList(ports2),
                                            0);
        List<Group> groupEntries = Arrays.asList(group1, group2);
        providerService.pushGroupMetrics(DID, groupEntries);
        GroupKey key = new DefaultGroupKey("group1BeforeAudit".getBytes());
        Group createdGroup = groupService.getGroup(DID, key);
        List<GroupOperation> expectedGroupOps = Arrays.asList(
                GroupOperation.createDeleteGroupOperation(gId1,
                                                          Group.Type.SELECT),
                GroupOperation.createDeleteGroupOperation(gId2,
                                                          Group.Type.SELECT),
                GroupOperation.createAddGroupOperation(createdGroup.id(),
                                                       Group.Type.SELECT,
                                                       createdGroup.buckets()));
        internalProvider.validate(DID, expectedGroupOps);
    }

    // Test AUDIT with confirmed groups
    private void testAuditWithConfirmedGroups() {
        GroupKey key = new DefaultGroupKey("group1BeforeAudit".getBytes());
        Group createdGroup = groupService.getGroup(DID, key);
        createdGroup = new DefaultGroup(createdGroup.id(),
                                        DID,
                                        Group.Type.SELECT,
                                        createdGroup.buckets());
        List<Group> groupEntries = Collections.singletonList(createdGroup);
        providerService.pushGroupMetrics(DID, groupEntries);
        internalListener.validateEvent(Collections.singletonList(GroupEvent.Type.GROUP_ADDED));
    }

    // Test group add bucket operations
    private void testAddBuckets() {
        GroupKey addKey = new DefaultGroupKey("group1AddBuckets".getBytes());

        GroupKey prevKey = new DefaultGroupKey("group1BeforeAudit".getBytes());
        Group createdGroup = groupService.getGroup(DID, prevKey);
        List<GroupBucket> buckets = new ArrayList<>();
        buckets.addAll(createdGroup.buckets().buckets());

        PortNumber[] addPorts = {PortNumber.portNumber(51),
                                 PortNumber.portNumber(52)};
        List<PortNumber> outPorts;
        outPorts = new ArrayList<>();
        outPorts.addAll(Arrays.asList(addPorts));
        List<GroupBucket> addBuckets;
        addBuckets = new ArrayList<>();
        for (PortNumber portNumber: outPorts) {
            TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
            tBuilder.setOutput(portNumber)
                    .setEthDst(MacAddress.valueOf("00:00:00:00:00:02"))
                    .setEthSrc(MacAddress.valueOf("00:00:00:00:00:01"))
                    .pushMpls()
                    .setMpls(MplsLabel.mplsLabel(106));
            addBuckets.add(DefaultGroupBucket.createSelectGroupBucket(
                                                        tBuilder.build()));
            buckets.add(DefaultGroupBucket.createSelectGroupBucket(
                                                        tBuilder.build()));
        }
        GroupBuckets groupAddBuckets = new GroupBuckets(addBuckets);
        groupService.addBucketsToGroup(DID,
                                       prevKey,
                                       groupAddBuckets,
                                       addKey,
                                       appId);
        GroupBuckets updatedBuckets = new GroupBuckets(buckets);
        List<GroupOperation> expectedGroupOps = Collections.singletonList(
                GroupOperation.createModifyGroupOperation(createdGroup.id(),
                        Group.Type.SELECT,
                        updatedBuckets));
        internalProvider.validate(DID, expectedGroupOps);
        Group existingGroup = groupService.getGroup(DID, addKey);
        List<Group> groupEntries = Collections.singletonList(existingGroup);
        providerService.pushGroupMetrics(DID, groupEntries);
        internalListener.validateEvent(Collections.singletonList(GroupEvent.Type.GROUP_UPDATED));
    }

    // Test group remove bucket operations
    private void testRemoveBuckets() {
        GroupKey removeKey = new DefaultGroupKey("group1RemoveBuckets".getBytes());

        GroupKey prevKey = new DefaultGroupKey("group1AddBuckets".getBytes());
        Group createdGroup = groupService.getGroup(DID, prevKey);
        List<GroupBucket> buckets = new ArrayList<>();
        buckets.addAll(createdGroup.buckets().buckets());

        PortNumber[] removePorts = {PortNumber.portNumber(31),
                                 PortNumber.portNumber(32)};
        List<PortNumber> outPorts = new ArrayList<>();
        outPorts.addAll(Arrays.asList(removePorts));
        List<GroupBucket> removeBuckets = new ArrayList<>();
        for (PortNumber portNumber: outPorts) {
            TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
            tBuilder.setOutput(portNumber)
                    .setEthDst(MacAddress.valueOf("00:00:00:00:00:02"))
                    .setEthSrc(MacAddress.valueOf("00:00:00:00:00:01"))
                    .pushMpls()
                    .setMpls(MplsLabel.mplsLabel(106));
            removeBuckets.add(DefaultGroupBucket.createSelectGroupBucket(
                                                        tBuilder.build()));
            buckets.remove(DefaultGroupBucket.createSelectGroupBucket(
                                                        tBuilder.build()));
        }
        GroupBuckets groupRemoveBuckets = new GroupBuckets(removeBuckets);
        groupService.removeBucketsFromGroup(DID,
                                            prevKey,
                                            groupRemoveBuckets,
                                            removeKey,
                                            appId);
        GroupBuckets updatedBuckets = new GroupBuckets(buckets);
        List<GroupOperation> expectedGroupOps = Collections.singletonList(
                GroupOperation.createModifyGroupOperation(createdGroup.id(),
                        Group.Type.SELECT,
                        updatedBuckets));
        internalProvider.validate(DID, expectedGroupOps);
        Group existingGroup = groupService.getGroup(DID, removeKey);
        List<Group> groupEntries = Collections.singletonList(existingGroup);
        providerService.pushGroupMetrics(DID, groupEntries);
        internalListener.validateEvent(Collections.singletonList(GroupEvent.Type.GROUP_UPDATED));
    }

    // Test group remove operations
    private void testRemoveGroup() {
        GroupKey currKey = new DefaultGroupKey("group1RemoveBuckets".getBytes());
        Group existingGroup = groupService.getGroup(DID, currKey);
        groupService.removeGroup(DID, currKey, appId);
        List<GroupOperation> expectedGroupOps = Collections.singletonList(
                GroupOperation.createDeleteGroupOperation(existingGroup.id(),
                        Group.Type.SELECT));
        internalProvider.validate(DID, expectedGroupOps);
        List<Group> groupEntries = Collections.emptyList();
        providerService.pushGroupMetrics(DID, groupEntries);
        internalListener.validateEvent(Collections.singletonList(GroupEvent.Type.GROUP_REMOVED));
    }

    /**
     * Test GroupOperationFailure function in Group Manager.
     * a)GroupAddFailure
     * b)GroupUpdateFailure
     * c)GroupRemoteFailure
     */
    @Test
    public void testGroupOperationFailure() {
        PortNumber[] ports1 = {PortNumber.portNumber(31),
                PortNumber.portNumber(32)};
        PortNumber[] ports2 = {PortNumber.portNumber(41),
                PortNumber.portNumber(42)};
        // Test Group creation before AUDIT process
        GroupKey key = new DefaultGroupKey("group1BeforeAudit".getBytes());
        List<GroupBucket> buckets = new ArrayList<>();
        List<PortNumber> outPorts = new ArrayList<>();
        outPorts.addAll(Arrays.asList(ports1));
        outPorts.addAll(Arrays.asList(ports2));
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
        GroupDescription newGroupDesc = new DefaultGroupDescription(DID,
                Group.Type.SELECT,
                groupBuckets,
                key,
                null,
                appId);
        groupService.addGroup(newGroupDesc);

        // Test initial group audit process
        GroupId gId1 = new DefaultGroupId(1);
        Group group1 = createSouthboundGroupEntry(gId1,
                Arrays.asList(ports1),
                0);
        GroupId gId2 = new DefaultGroupId(2);
        // Non zero reference count will make the group manager to queue
        // the extraneous groups until reference count is zero.
        Group group2 = createSouthboundGroupEntry(gId2,
                Arrays.asList(ports2),
                2);
        List<Group> groupEntries = Arrays.asList(group1, group2);
        providerService.pushGroupMetrics(DID, groupEntries);
        Group createdGroup = groupService.getGroup(DID, key);

        // Group Add failure test
        GroupOperation groupAddOp = GroupOperation.
                createAddGroupOperation(createdGroup.id(),
                        createdGroup.type(),
                        createdGroup.buckets());
        providerService.groupOperationFailed(DID, groupAddOp);
        internalListener.validateEvent(Collections.singletonList(GroupEvent.Type.GROUP_ADD_FAILED));

        // Group Mod failure test
        groupService.addGroup(newGroupDesc);
        createdGroup = groupService.getGroup(DID, key);
        assertNotNull(createdGroup);

        GroupOperation groupModOp = GroupOperation.
                createModifyGroupOperation(createdGroup.id(),
                        createdGroup.type(),
                        createdGroup.buckets());
        providerService.groupOperationFailed(DID, groupModOp);
        internalListener.validateEvent(Collections.singletonList(GroupEvent.Type.GROUP_UPDATE_FAILED));

        // Group Delete failure test
        groupService.addGroup(newGroupDesc);
        createdGroup = groupService.getGroup(DID, key);
        assertNotNull(createdGroup);

        GroupOperation groupDelOp = GroupOperation.
                createDeleteGroupOperation(createdGroup.id(),
                        createdGroup.type());
        providerService.groupOperationFailed(DID, groupDelOp);
        internalListener.validateEvent(Collections.singletonList(GroupEvent.Type.GROUP_REMOVE_FAILED));

    }

    private Group createSouthboundGroupEntry(GroupId gId,
                                             List<PortNumber> ports,
                                             long referenceCount) {
        List<PortNumber> outPorts = new ArrayList<>();
        outPorts.addAll(ports);

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
        StoredGroupEntry group = new DefaultGroup(
                            gId, DID, Group.Type.SELECT, groupBuckets);
        group.setReferenceCount(referenceCount);
        return group;
    }

    private static class TestGroupListener implements GroupListener {
        final List<GroupEvent> events = new ArrayList<>();

        @Override
        public void event(GroupEvent event) {
            events.add(event);
        }

        public void validateEvent(List<GroupEvent.Type> expectedEvents) {
            int i = 0;
            System.err.println("events :" + events);
            for (GroupEvent e : events) {
                assertEquals("unexpected event", expectedEvents.get(i), e.type());
                i++;
            }
            assertEquals("mispredicted number of events",
                         expectedEvents.size(), events.size());
            events.clear();
        }
    }

    private class TestGroupProvider
                extends AbstractProvider implements GroupProvider {
        DeviceId lastDeviceId;
        List<GroupOperation> groupOperations = new ArrayList<>();

        protected TestGroupProvider(ProviderId id) {
            super(id);
        }

        @Override
        public void performGroupOperation(DeviceId deviceId,
                                          GroupOperations groupOps) {
            lastDeviceId = deviceId;
            groupOperations.addAll(groupOps.operations());
        }

        public void validate(DeviceId expectedDeviceId,
                             List<GroupOperation> expectedGroupOps) {
            if (expectedGroupOps == null) {
                assertTrue("events generated", groupOperations.isEmpty());
                return;
            }

            assertEquals(lastDeviceId, expectedDeviceId);
            assertTrue((this.groupOperations.containsAll(expectedGroupOps) &&
                    expectedGroupOps.containsAll(groupOperations)));

            groupOperations.clear();
            lastDeviceId = null;
        }

    }

}


