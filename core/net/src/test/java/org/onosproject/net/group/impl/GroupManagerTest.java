/*
 * Copyright 2015-present Open Networking Laboratory
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.MacAddress;
import org.onlab.packet.MplsLabel;
import org.onosproject.cfg.ComponentConfigAdapter;
import org.onosproject.common.event.impl.TestEventDispatcher;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.core.DefaultGroupId;
import org.onosproject.core.GroupId;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.driver.DefaultDriver;
import org.onosproject.net.driver.impl.DriverManager;
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
import org.onosproject.net.group.GroupProgrammable;
import org.onosproject.net.group.GroupProvider;
import org.onosproject.net.group.GroupProviderRegistry;
import org.onosproject.net.group.GroupProviderService;
import org.onosproject.net.group.GroupService;
import org.onosproject.net.group.StoredGroupEntry;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.store.trivial.SimpleGroupStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.onosproject.net.NetTestTools.injectEventDispatcher;

/**
 * Test codifying the group service & group provider service contracts.
 */
public class GroupManagerTest {

    private static final ProviderId PID = new ProviderId("of", "groupfoo");
    private static final DeviceId DID = DeviceId.deviceId("of:001");
    private static final ProviderId FOO_PID = new ProviderId("foo", "foo");
    private static final DeviceId FOO_DID = DeviceId.deviceId("foo:002");

    private static final DefaultAnnotations ANNOTATIONS =
            DefaultAnnotations.builder().set(AnnotationKeys.DRIVER, "foo").build();

    private static final Device FOO_DEV =
            new DefaultDevice(FOO_PID, FOO_DID, Device.Type.SWITCH, "", "", "", "", null, ANNOTATIONS);

    private GroupManager mgr;
    private GroupService groupService;
    private GroupProviderRegistry providerRegistry;
    private TestGroupListener internalListener = new TestGroupListener();
    private GroupListener listener = internalListener;
    private TestGroupProvider internalProvider;
    private GroupProvider provider;
    private GroupProviderService providerService;
    private ApplicationId appId;
    private TestDriverManager driverService;

    @Before
    public void setUp() {
        mgr = new GroupManager();
        groupService = mgr;
        //mgr.deviceService = new DeviceManager();
        mgr.deviceService = new TestDeviceService();
        mgr.cfgService = new ComponentConfigAdapter();
        mgr.store = new SimpleGroupStore();
        injectEventDispatcher(mgr, new TestEventDispatcher());
        providerRegistry = mgr;

        mgr.activate(null);
        mgr.addListener(listener);

        driverService = new TestDriverManager();
        driverService.addDriver(new DefaultDriver("foo", ImmutableList.of(), "", "", "",
                                                  ImmutableMap.of(GroupProgrammable.class,
                                                                  TestGroupProgrammable.class),
                                                  ImmutableMap.of()));

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
     * Tests group creation before the device group AUDIT completes.
     */
    @Test
    public void testGroupServiceBasics() {
        // Test Group creation before AUDIT process
        testGroupCreationBeforeAudit(DID);
    }

    /**
     * Tests initial device group AUDIT process.
     */
    @Test
    public void testGroupServiceInitialAudit() {
        // Test Group creation before AUDIT process
        testGroupCreationBeforeAudit(DID);
        // Test initial group audit process
        testInitialAuditWithPendingGroupRequests(DID);
    }

    /**
     * Tests deletion process of any extraneous groups.
     */
    @Test
    public void testGroupServiceAuditExtraneous() {
        // Test Group creation before AUDIT process
        testGroupCreationBeforeAudit(DID);

        // Test audit with extraneous and missing groups
        testAuditWithExtraneousMissingGroups(DID);
    }

    /**
     * Tests re-apply process of any missing groups tests execution of
     * any pending group creation request after the device group AUDIT completes
     * and tests event notifications after receiving confirmation for any
     * operations from data plane.
     */
    @Test
    public void testGroupServiceAuditConfirmed() {
        // Test Group creation before AUDIT process
        testGroupCreationBeforeAudit(DID);

        // Test audit with extraneous and missing groups
        testAuditWithExtraneousMissingGroups(DID);

        // Test audit with confirmed groups
        testAuditWithConfirmedGroups(DID);
    }

    /**
     * Tests group Purge Operation.
     */
    @Test
    public void testPurgeGroups() {
        //Test Group creation before AUDIT process
        testGroupCreationBeforeAudit(DID);
        programmableTestCleanUp();
        testAuditWithExtraneousMissingGroups(DID);
        // Test group add bucket operations
        testAddBuckets(DID);
        // Test group Purge operations
        testPurgeGroupEntry(DID);
    }

    /**
     * Tests group bucket modifications (additions and deletions) and
     * Tests group deletion.
     */
    @Test
    public void testGroupServiceBuckets() {
        // Test Group creation before AUDIT process
        testGroupCreationBeforeAudit(DID);
        programmableTestCleanUp();

        testAuditWithExtraneousMissingGroups(DID);
        // Test group add bucket operations
        testAddBuckets(DID);

        // Test group remove bucket operations
        testRemoveBuckets(DID);

        // Test group remove operations
        testRemoveGroup(DID);
    }

    /**
     * Tests group creation before the device group AUDIT completes with fallback
     * provider.
     */
    @Test
    public void testGroupServiceFallbackBasics() {
        // Test Group creation before AUDIT process
        testGroupCreationBeforeAudit(FOO_DID);
        programmableTestCleanUp();

    }

    /**
     * Tests initial device group AUDIT process with fallback provider.
     */
    @Test
    public void testGroupServiceFallbackInitialAudit() {
        // Test Group creation before AUDIT process
        testGroupCreationBeforeAudit(FOO_DID);
        programmableTestCleanUp();
        // Test initial group audit process
        testInitialAuditWithPendingGroupRequests(FOO_DID);
    }

    /**
     * Tests deletion process of any extraneous groups with fallback provider.
     */
    @Test
    public void testGroupServiceFallbackAuditExtraneous() {
        // Test Group creation before AUDIT process
        testGroupCreationBeforeAudit(FOO_DID);
        programmableTestCleanUp();

        // Test audit with extraneous and missing groups
        testAuditWithExtraneousMissingGroups(FOO_DID);
    }

    /**
     * Tests re-apply process of any missing groups tests execution of
     * any pending group creation request after the device group AUDIT completes
     * and tests event notifications after receiving confirmation for any
     * operations from data plane with fallback provider.
     */
    @Test
    public void testGroupServiceFallbackAuditConfirmed() {
        // Test Group creation before AUDIT process
        testGroupCreationBeforeAudit(FOO_DID);
        programmableTestCleanUp();

        // Test audit with extraneous and missing groups
        testAuditWithExtraneousMissingGroups(FOO_DID);

        // Test audit with confirmed groups
        testAuditWithConfirmedGroups(FOO_DID);
    }

    /**
     * Tests group bucket modifications (additions and deletions) and
     * Tests group deletion with fallback provider.
     */
    @Test
    public void testGroupServiceFallbackBuckets() {
        // Test Group creation before AUDIT process
        testGroupCreationBeforeAudit(FOO_DID);
        programmableTestCleanUp();

        testAuditWithExtraneousMissingGroups(FOO_DID);
        // Test group add bucket operations
        testAddBuckets(FOO_DID);

        // Test group remove bucket operations
        testRemoveBuckets(FOO_DID);

        // Test group remove operations
        testRemoveGroup(FOO_DID);
    }

    private void programmableTestCleanUp() {
        groupOperations.clear();
        lastDeviceIdProgrammable = null;
    }

    // Test Group creation before AUDIT process
    private void testGroupCreationBeforeAudit(DeviceId deviceId) {
        PortNumber[] ports1 = {PortNumber.portNumber(31),
                PortNumber.portNumber(32)};
        PortNumber[] ports2 = {PortNumber.portNumber(41),
                PortNumber.portNumber(42)};
        GroupKey key = new DefaultGroupKey("group1BeforeAudit".getBytes());
        List<GroupBucket> buckets = new ArrayList<>();
        List<PortNumber> outPorts = new ArrayList<>();
        outPorts.addAll(Arrays.asList(ports1));
        outPorts.addAll(Arrays.asList(ports2));
        for (PortNumber portNumber : outPorts) {
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
        GroupDescription newGroupDesc = new DefaultGroupDescription(deviceId,
                                                                    Group.Type.SELECT,
                                                                    groupBuckets,
                                                                    key,
                                                                    null,
                                                                    appId);
        groupService.addGroup(newGroupDesc);
        assertEquals(null, groupService.getGroup(deviceId, key));
        assertEquals(0, Iterables.size(groupService.getGroups(deviceId, appId)));
    }

    // Test initial AUDIT process with pending group requests
    private void testInitialAuditWithPendingGroupRequests(DeviceId deviceId) {
        PortNumber[] ports1 = {PortNumber.portNumber(31),
                PortNumber.portNumber(32)};
        PortNumber[] ports2 = {PortNumber.portNumber(41),
                PortNumber.portNumber(42)};
        GroupId gId1 = new DefaultGroupId(1);
        Group group1 = createSouthboundGroupEntry(gId1,
                                                  Arrays.asList(ports1),
                                                  0, deviceId);
        GroupId gId2 = new DefaultGroupId(2);
        // Non zero reference count will make the group manager to queue
        // the extraneous groups until reference count is zero.
        Group group2 = createSouthboundGroupEntry(gId2,
                                                  Arrays.asList(ports2),
                                                  2, deviceId);
        List<Group> groupEntries = Arrays.asList(group1, group2);
        providerService.pushGroupMetrics(deviceId, groupEntries);
        // First group metrics would trigger the device audit completion
        // post which all pending group requests are also executed.
        GroupKey key = new DefaultGroupKey("group1BeforeAudit".getBytes());
        Group createdGroup = groupService.getGroup(deviceId, key);
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
        if (deviceId.equals(DID)) {
            internalProvider.validate(deviceId, expectedGroupOps);
        } else {
            this.validate(deviceId, expectedGroupOps);
        }
    }

    // Test AUDIT process with extraneous groups and missing groups
    private void testAuditWithExtraneousMissingGroups(DeviceId deviceId) {
        PortNumber[] ports1 = {PortNumber.portNumber(31),
                PortNumber.portNumber(32)};
        PortNumber[] ports2 = {PortNumber.portNumber(41),
                PortNumber.portNumber(42)};
        GroupId gId1 = new DefaultGroupId(1);
        Group group1 = createSouthboundGroupEntry(gId1,
                                                  Arrays.asList(ports1),
                                                  0, deviceId);
        GroupId gId2 = new DefaultGroupId(2);
        Group group2 = createSouthboundGroupEntry(gId2,
                                                  Arrays.asList(ports2),
                                                  0, deviceId);
        List<Group> groupEntries = Arrays.asList(group1, group2);
        providerService.pushGroupMetrics(deviceId, groupEntries);
        GroupKey key = new DefaultGroupKey("group1BeforeAudit".getBytes());
        Group createdGroup = groupService.getGroup(deviceId, key);
        List<GroupOperation> expectedGroupOps = Arrays.asList(
                GroupOperation.createDeleteGroupOperation(gId1,
                                                          Group.Type.SELECT),
                GroupOperation.createDeleteGroupOperation(gId2,
                                                          Group.Type.SELECT),
                GroupOperation.createAddGroupOperation(createdGroup.id(),
                                                       Group.Type.SELECT,
                                                       createdGroup.buckets()));
        if (deviceId.equals(DID)) {
            internalProvider.validate(deviceId, expectedGroupOps);
        } else {
            this.validate(deviceId, expectedGroupOps);
        }
    }

    // Test AUDIT with confirmed groups
    private void testAuditWithConfirmedGroups(DeviceId deviceId) {
        GroupKey key = new DefaultGroupKey("group1BeforeAudit".getBytes());
        Group createdGroup = groupService.getGroup(deviceId, key);
        createdGroup = new DefaultGroup(createdGroup.id(),
                                        deviceId,
                                        Group.Type.SELECT,
                                        createdGroup.buckets());
        List<Group> groupEntries = Collections.singletonList(createdGroup);
        providerService.pushGroupMetrics(deviceId, groupEntries);
        internalListener.validateEvent(Collections.singletonList(GroupEvent.Type.GROUP_ADDED));
    }

    // Test group add bucket operations
    private void testAddBuckets(DeviceId deviceId) {
        GroupKey addKey = new DefaultGroupKey("group1AddBuckets".getBytes());

        GroupKey prevKey = new DefaultGroupKey("group1BeforeAudit".getBytes());
        Group createdGroup = groupService.getGroup(deviceId, prevKey);
        List<GroupBucket> buckets = new ArrayList<>();
        buckets.addAll(createdGroup.buckets().buckets());

        PortNumber[] addPorts = {PortNumber.portNumber(51),
                PortNumber.portNumber(52)};
        List<PortNumber> outPorts;
        outPorts = new ArrayList<>();
        outPorts.addAll(Arrays.asList(addPorts));
        List<GroupBucket> addBuckets;
        addBuckets = new ArrayList<>();
        for (PortNumber portNumber : outPorts) {
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
        groupService.addBucketsToGroup(deviceId,
                                       prevKey,
                                       groupAddBuckets,
                                       addKey,
                                       appId);
        GroupBuckets updatedBuckets = new GroupBuckets(buckets);
        List<GroupOperation> expectedGroupOps = Collections.singletonList(
                GroupOperation.createModifyGroupOperation(createdGroup.id(),
                                                          Group.Type.SELECT,
                                                          updatedBuckets));
        if (deviceId.equals(DID)) {
            internalProvider.validate(deviceId, expectedGroupOps);
        } else {
            this.validate(deviceId, expectedGroupOps);
        }
        Group existingGroup = groupService.getGroup(deviceId, addKey);
        List<Group> groupEntries = Collections.singletonList(existingGroup);
        providerService.pushGroupMetrics(deviceId, groupEntries);
        internalListener.validateEvent(Collections.singletonList(GroupEvent.Type.GROUP_UPDATED));
    }

    // Test group remove bucket operations
    private void testRemoveBuckets(DeviceId deviceId) {
        GroupKey removeKey = new DefaultGroupKey("group1RemoveBuckets".getBytes());

        GroupKey prevKey = new DefaultGroupKey("group1AddBuckets".getBytes());
        Group createdGroup = groupService.getGroup(deviceId, prevKey);
        List<GroupBucket> buckets = new ArrayList<>();
        buckets.addAll(createdGroup.buckets().buckets());

        PortNumber[] removePorts = {PortNumber.portNumber(31),
                PortNumber.portNumber(32)};
        List<PortNumber> outPorts = new ArrayList<>();
        outPorts.addAll(Arrays.asList(removePorts));
        List<GroupBucket> removeBuckets = new ArrayList<>();
        for (PortNumber portNumber : outPorts) {
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
        groupService.removeBucketsFromGroup(deviceId,
                                            prevKey,
                                            groupRemoveBuckets,
                                            removeKey,
                                            appId);
        GroupBuckets updatedBuckets = new GroupBuckets(buckets);
        List<GroupOperation> expectedGroupOps = Collections.singletonList(
                GroupOperation.createModifyGroupOperation(createdGroup.id(),
                                                          Group.Type.SELECT,
                                                          updatedBuckets));
        if (deviceId.equals(DID)) {
            internalProvider.validate(deviceId, expectedGroupOps);
        } else {
            this.validate(deviceId, expectedGroupOps);
        }
        Group existingGroup = groupService.getGroup(deviceId, removeKey);
        List<Group> groupEntries = Collections.singletonList(existingGroup);
        providerService.pushGroupMetrics(deviceId, groupEntries);
        internalListener.validateEvent(Collections.singletonList(GroupEvent.Type.GROUP_UPDATED));
    }

    // Test purge group entry operations
    private void testPurgeGroupEntry(DeviceId deviceId) {
        assertEquals(1, Iterables.size(groupService.getGroups(deviceId, appId)));
        groupService.purgeGroupEntries(deviceId);
        assertEquals(0, Iterables.size(groupService.getGroups(deviceId, appId)));
    }

    // Test group remove operations
    private void testRemoveGroup(DeviceId deviceId) {
        GroupKey currKey = new DefaultGroupKey("group1RemoveBuckets".getBytes());
        Group existingGroup = groupService.getGroup(deviceId, currKey);
        groupService.removeGroup(deviceId, currKey, appId);
        List<GroupOperation> expectedGroupOps = Collections.singletonList(
                GroupOperation.createDeleteGroupOperation(existingGroup.id(),
                                                          Group.Type.SELECT));
        if (deviceId.equals(DID)) {
            internalProvider.validate(deviceId, expectedGroupOps);
        } else {
            this.validate(deviceId, expectedGroupOps);
        }
        List<Group> groupEntries = Collections.emptyList();
        providerService.pushGroupMetrics(deviceId, groupEntries);
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
        groupOperationFaliure(DID);
    }

    /**
     * Test GroupOperationFailure function in Group Manager
     * with fallback provider.
     * a)GroupAddFailure
     * b)GroupUpdateFailure
     * c)GroupRemoteFailure
     */
    @Test
    public void testGroupOperationFailureFallBack() {
        groupOperationFaliure(FOO_DID);
    }

    private void groupOperationFaliure(DeviceId deviceId) {
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
        for (PortNumber portNumber : outPorts) {
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
        GroupDescription newGroupDesc = new DefaultGroupDescription(deviceId,
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
                                                  0, deviceId);
        GroupId gId2 = new DefaultGroupId(2);
        // Non zero reference count will make the group manager to queue
        // the extraneous groups until reference count is zero.
        Group group2 = createSouthboundGroupEntry(gId2,
                                                  Arrays.asList(ports2),
                                                  2, deviceId);
        List<Group> groupEntries = Arrays.asList(group1, group2);
        providerService.pushGroupMetrics(deviceId, groupEntries);
        Group createdGroup = groupService.getGroup(deviceId, key);

        // Group Add failure test
        GroupOperation groupAddOp = GroupOperation.
                createAddGroupOperation(createdGroup.id(),
                                        createdGroup.type(),
                                        createdGroup.buckets());
        providerService.groupOperationFailed(deviceId, groupAddOp);
        internalListener.validateEvent(Collections.singletonList(GroupEvent.Type.GROUP_ADD_FAILED));

        // Group Mod failure test
        groupService.addGroup(newGroupDesc);
        createdGroup = groupService.getGroup(deviceId, key);
        assertNotNull(createdGroup);

        GroupOperation groupModOp = GroupOperation.
                createModifyGroupOperation(createdGroup.id(),
                                           createdGroup.type(),
                                           createdGroup.buckets());
        providerService.groupOperationFailed(deviceId, groupModOp);
        internalListener.validateEvent(Collections.singletonList(GroupEvent.Type.GROUP_UPDATE_FAILED));

        // Group Delete failure test
        groupService.addGroup(newGroupDesc);
        createdGroup = groupService.getGroup(deviceId, key);
        assertNotNull(createdGroup);

        GroupOperation groupDelOp = GroupOperation.
                createDeleteGroupOperation(createdGroup.id(),
                                           createdGroup.type());
        providerService.groupOperationFailed(deviceId, groupDelOp);
        internalListener.validateEvent(Collections.singletonList(GroupEvent.Type.GROUP_REMOVE_FAILED));
    }

    private Group createSouthboundGroupEntry(GroupId gId,
                                             List<PortNumber> ports,
                                             long referenceCount, DeviceId deviceId) {
        List<PortNumber> outPorts = new ArrayList<>();
        outPorts.addAll(ports);

        List<GroupBucket> buckets = new ArrayList<>();
        for (PortNumber portNumber : outPorts) {
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
                gId, deviceId, Group.Type.SELECT, groupBuckets);
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

    private static class TestDeviceService extends DeviceServiceAdapter {
        @Override
        public int getDeviceCount() {
            return 1;
        }

        @Override
        public Iterable<Device> getDevices() {
            return ImmutableList.of(FOO_DEV);
        }

        @Override
        public Iterable<Device> getAvailableDevices() {
            return getDevices();
        }

        @Override
        public Device getDevice(DeviceId deviceId) {
            return FOO_DEV;
        }
    }

    private class TestDriverManager extends DriverManager {
        TestDriverManager() {
            this.deviceService = mgr.deviceService;
            activate();
        }
    }

    private static DeviceId lastDeviceIdProgrammable;
    private static List<GroupOperation> groupOperations = new ArrayList<>();

    public static class TestGroupProgrammable extends AbstractHandlerBehaviour implements GroupProgrammable {
        @Override
        public void performGroupOperation(DeviceId deviceId, GroupOperations groupOps) {
            lastDeviceIdProgrammable = deviceId;
            groupOperations.addAll(groupOps.operations());
        }
    }

    public void validate(DeviceId expectedDeviceId,
                         List<GroupOperation> expectedGroupOps) {
        if (expectedGroupOps == null) {
            assertTrue("events generated", groupOperations.isEmpty());
            return;
        }

        assertEquals(lastDeviceIdProgrammable, expectedDeviceId);
        assertTrue(groupOperations.containsAll(expectedGroupOps) &&
                   expectedGroupOps.containsAll(groupOperations));

        groupOperations.clear();
        lastDeviceIdProgrammable = null;
    }
}


