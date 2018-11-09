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

package org.onosproject.incubator.net.virtual.impl;

import com.google.common.collect.Iterables;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onlab.packet.MacAddress;
import org.onlab.packet.MplsLabel;
import org.onosproject.TestApplicationId;
import org.onosproject.common.event.impl.TestEventDispatcher;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.GroupId;
import org.onosproject.event.EventDeliveryService;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualNetwork;
import org.onosproject.incubator.net.virtual.VirtualNetworkGroupStore;
import org.onosproject.incubator.net.virtual.VirtualNetworkStore;
import org.onosproject.incubator.net.virtual.impl.provider.VirtualProviderManager;
import org.onosproject.incubator.net.virtual.provider.AbstractVirtualProvider;
import org.onosproject.incubator.net.virtual.provider.VirtualGroupProvider;
import org.onosproject.incubator.net.virtual.provider.VirtualGroupProviderService;
import org.onosproject.incubator.net.virtual.provider.VirtualProviderRegistryService;
import org.onosproject.incubator.net.virtual.store.impl.DistributedVirtualNetworkStore;
import org.onosproject.incubator.net.virtual.store.impl.SimpleVirtualGroupStore;
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
import org.onosproject.net.group.GroupListener;
import org.onosproject.net.group.GroupOperation;
import org.onosproject.net.group.GroupOperations;
import org.onosproject.net.group.StoredGroupEntry;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.store.service.TestStorageService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.onosproject.incubator.net.virtual.impl.VirtualNetworkTestUtil.*;
import static org.onosproject.net.NetTestTools.injectEventDispatcher;

/**
 * Test codifying the virtual group service & group provider service contracts.
 */
public class VirtualNetworkGroupManagerTest {

    private VirtualNetworkManager manager;
    private DistributedVirtualNetworkStore virtualNetworkManagerStore;
    private ServiceDirectory testDirectory;
    private VirtualProviderManager providerRegistryService;

    private EventDeliveryService eventDeliveryService;

    private VirtualNetworkGroupManager groupManager1;
    private VirtualNetworkGroupManager groupManager2;

    private VirtualNetworkGroupStore groupStore;

    private TestGroupProvider provider = new TestGroupProvider();
    private VirtualGroupProviderService providerService1;
    private VirtualGroupProviderService providerService2;

    protected TestGroupListener listener1 = new TestGroupListener();
    protected TestGroupListener listener2 = new TestGroupListener();

    private VirtualNetwork vnet1;
    private VirtualNetwork vnet2;

    private ApplicationId appId;

    @Before
    public void setUp() throws Exception {
        virtualNetworkManagerStore = new DistributedVirtualNetworkStore();

        CoreService coreService = new TestCoreService();
        TestUtils.setField(virtualNetworkManagerStore, "coreService", coreService);
        TestUtils.setField(virtualNetworkManagerStore, "storageService", new TestStorageService());
        virtualNetworkManagerStore.activate();

        groupStore = new SimpleVirtualGroupStore();

        providerRegistryService = new VirtualProviderManager();
        providerRegistryService.registerProvider(provider);

        manager = new VirtualNetworkManager();
        manager.store = virtualNetworkManagerStore;
        TestUtils.setField(manager, "coreService", coreService);

        eventDeliveryService = new TestEventDispatcher();
        injectEventDispatcher(manager, eventDeliveryService);

        appId = new TestApplicationId("VirtualGroupManagerTest");

        testDirectory = new TestServiceDirectory()
                .add(VirtualNetworkStore.class, virtualNetworkManagerStore)
                .add(CoreService.class, coreService)
                .add(VirtualProviderRegistryService.class, providerRegistryService)
                .add(EventDeliveryService.class, eventDeliveryService)
                .add(VirtualNetworkGroupStore.class, groupStore);
        TestUtils.setField(manager, "serviceDirectory", testDirectory);

        manager.activate();

        vnet1 = setupVirtualNetworkTopology(manager, TID1);
        vnet2 = setupVirtualNetworkTopology(manager, TID2);

        groupManager1 = new VirtualNetworkGroupManager(manager, vnet1.id());
        groupManager2 = new VirtualNetworkGroupManager(manager, vnet2.id());
        groupManager1.addListener(listener1);
        groupManager2.addListener(listener2);

        providerService1 = (VirtualGroupProviderService)
                providerRegistryService.getProviderService(vnet1.id(),
                                                           VirtualGroupProvider.class);
        providerService2 = (VirtualGroupProviderService)
                providerRegistryService.getProviderService(vnet2.id(),
                                                           VirtualGroupProvider.class);
    }

    @After
    public void tearDown() {
        providerRegistryService.unregisterProvider(provider);
        assertFalse("provider should not be registered",
                    providerRegistryService.getProviders().contains(provider.id()));
        groupManager1.removeListener(listener1);
        groupManager2.removeListener(listener2);

        manager.deactivate();
        virtualNetworkManagerStore.deactivate();
    }

    /**
     * Tests group creation before the device group AUDIT completes.
     */
    @Test
    public void testGroupServiceBasics() {
        // Test Group creation before AUDIT process
        testGroupCreationBeforeAudit(vnet1.id(), VDID1);
        testGroupCreationBeforeAudit(vnet2.id(), VDID1);
    }

    /**
     * Tests initial device group AUDIT process.
     */
    @Test
    public void testGroupServiceInitialAudit() {
        // Test Group creation before AUDIT process
        testGroupCreationBeforeAudit(vnet1.id(), VDID1);
        testGroupCreationBeforeAudit(vnet2.id(), VDID1);
        // Test initial group audit process
        testInitialAuditWithPendingGroupRequests(vnet1.id(), VDID1);
        testInitialAuditWithPendingGroupRequests(vnet2.id(), VDID1);
    }

    /**
     * Tests deletion process of any extraneous groups.
     */
    @Test
    public void testGroupServiceAuditExtraneous() {
        // Test Group creation before AUDIT process
        testGroupCreationBeforeAudit(vnet1.id(), VDID1);
        testGroupCreationBeforeAudit(vnet2.id(), VDID1);

        // Test audit with extraneous and missing groups
        testAuditWithExtraneousMissingGroups(vnet1.id(), VDID1);
        testAuditWithExtraneousMissingGroups(vnet2.id(), VDID1);
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
        testGroupCreationBeforeAudit(vnet1.id(), VDID1);
        testGroupCreationBeforeAudit(vnet2.id(), VDID1);

        // Test audit with extraneous and missing groups
        testAuditWithExtraneousMissingGroups(vnet1.id(), VDID1);
        testAuditWithExtraneousMissingGroups(vnet2.id(), VDID1);

        // Test audit with confirmed groups
        testAuditWithConfirmedGroups(vnet1.id(), VDID1);
        testAuditWithConfirmedGroups(vnet2.id(), VDID1);
    }

    /**
     * Tests group Purge Operation.
     */
    @Test
    public void testPurgeGroups() {
        // Tests for virtual network 1
        // Test Group creation before AUDIT process
        testGroupCreationBeforeAudit(vnet1.id(), VDID1);
        testAuditWithExtraneousMissingGroups(vnet1.id(), VDID1);
        // Test group add bucket operations
        testAddBuckets(vnet1.id(), VDID1);
        // Test group Purge operations
        testPurgeGroupEntry(vnet1.id(), VDID1);

        // Tests for virtual network 2
        // Test Group creation before AUDIT process
        testGroupCreationBeforeAudit(vnet2.id(), VDID1);
        testAuditWithExtraneousMissingGroups(vnet2.id(), VDID1);
        // Test group add bucket operations
        testAddBuckets(vnet2.id(), VDID1);
        // Test group Purge operations
        testPurgeGroupEntry(vnet2.id(), VDID1);
    }

    /**
     * Tests group bucket modifications (additions and deletions) and
     * Tests group deletion.
     */
    @Test
    public void testGroupServiceBuckets() {
        // Tests for virtual network 1
        // Test Group creation before AUDIT process
        testGroupCreationBeforeAudit(vnet1.id(), VDID1);

        testAuditWithExtraneousMissingGroups(vnet1.id(), VDID1);
        // Test group add bucket operations
        testAddBuckets(vnet1.id(), VDID1);

        // Test group remove bucket operations
        testRemoveBuckets(vnet1.id(), VDID1);

        // Test group remove operations
        testRemoveGroup(vnet1.id(), VDID1);

        // Tests for virtual network 2
        // Test Group creation before AUDIT process
        testGroupCreationBeforeAudit(vnet2.id(), VDID1);

        testAuditWithExtraneousMissingGroups(vnet2.id(), VDID1);
        // Test group add bucket operations
        testAddBuckets(vnet2.id(), VDID1);

        // Test group remove bucket operations
        testRemoveBuckets(vnet2.id(), VDID1);

        // Test group remove operations
        testRemoveGroup(vnet2.id(), VDID1);
    }

    /**
     * Tests group creation before the device group AUDIT completes with fallback
     * provider.
     */
    @Test
    public void testGroupServiceFallbackBasics() {
        // Test Group creation before AUDIT process
        testGroupCreationBeforeAudit(vnet1.id(), VDID2);
        testGroupCreationBeforeAudit(vnet2.id(), VDID2);
    }

    // Test Group creation before AUDIT process
    private void testGroupCreationBeforeAudit(NetworkId networkId, DeviceId deviceId) {
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
        VirtualNetworkGroupManager groupManager;
        if (networkId.id() == 1) {
            groupManager = groupManager1;
        } else {
            groupManager = groupManager2;
        }

        groupManager.addGroup(newGroupDesc);
        assertEquals(null, groupManager.getGroup(deviceId, key));
        assertEquals(0, Iterables.size(groupManager.getGroups(deviceId, appId)));
    }


    // Test initial AUDIT process with pending group requests
    private void testInitialAuditWithPendingGroupRequests(NetworkId networkId,
                                                          DeviceId deviceId) {
        VirtualNetworkGroupManager groupManager;
        VirtualGroupProviderService providerService;
        if (networkId.id() == 1) {
            groupManager = groupManager1;
            providerService = providerService1;
        } else {
            groupManager = groupManager2;
            providerService = providerService2;
        }

        PortNumber[] ports1 = {PortNumber.portNumber(31),
                PortNumber.portNumber(32)};
        PortNumber[] ports2 = {PortNumber.portNumber(41),
                PortNumber.portNumber(42)};
        GroupId gId1 = new GroupId(1);
        Group group1 = createSouthboundGroupEntry(gId1,
                                                  Arrays.asList(ports1),
                                                  0, deviceId);
        GroupId gId2 = new GroupId(2);
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
        Group createdGroup = groupManager.getGroup(deviceId, key);
        int createdGroupId = createdGroup.id().id();
        assertNotEquals(gId1.id().intValue(), createdGroupId);
        assertNotEquals(gId2.id().intValue(), createdGroupId);

        List<GroupOperation> expectedGroupOps = Arrays.asList(
                GroupOperation.createDeleteGroupOperation(gId1,
                                                          Group.Type.SELECT),
                GroupOperation.createAddGroupOperation(
                        createdGroup.id(),
                        Group.Type.SELECT,
                        createdGroup.buckets()));
        if (deviceId.equals(VDID1)) {
            provider.validate(networkId, deviceId, expectedGroupOps);
        }
    }

    // Test AUDIT process with extraneous groups and missing groups
    private void testAuditWithExtraneousMissingGroups(NetworkId networkId,
                                                      DeviceId deviceId) {
        VirtualNetworkGroupManager groupManager;
        VirtualGroupProviderService providerService;
        if (networkId.id() == 1) {
            groupManager = groupManager1;
            providerService = providerService1;
        } else {
            groupManager = groupManager2;
            providerService = providerService2;
        }

        PortNumber[] ports1 = {PortNumber.portNumber(31),
                PortNumber.portNumber(32)};
        PortNumber[] ports2 = {PortNumber.portNumber(41),
                PortNumber.portNumber(42)};
        GroupId gId1 = new GroupId(1);
        Group group1 = createSouthboundGroupEntry(gId1,
                                                  Arrays.asList(ports1),
                                                  0, deviceId);
        GroupId gId2 = new GroupId(2);
        Group group2 = createSouthboundGroupEntry(gId2,
                                                  Arrays.asList(ports2),
                                                  0, deviceId);
        List<Group> groupEntries = Arrays.asList(group1, group2);
        providerService.pushGroupMetrics(deviceId, groupEntries);
        GroupKey key = new DefaultGroupKey("group1BeforeAudit".getBytes());
        Group createdGroup = groupManager.getGroup(deviceId, key);
        List<GroupOperation> expectedGroupOps = Arrays.asList(
                GroupOperation.createDeleteGroupOperation(gId1,
                                                          Group.Type.SELECT),
                GroupOperation.createDeleteGroupOperation(gId2,
                                                          Group.Type.SELECT),
                GroupOperation.createAddGroupOperation(createdGroup.id(),
                                                       Group.Type.SELECT,
                                                       createdGroup.buckets()));
        if (deviceId.equals(VDID1)) {
            provider.validate(networkId, deviceId, expectedGroupOps);
        }
    }

    // Test AUDIT with confirmed groups
    private void testAuditWithConfirmedGroups(NetworkId networkId,
                                              DeviceId deviceId) {
        VirtualNetworkGroupManager groupManager;
        VirtualGroupProviderService providerService;
        TestGroupListener listener;

        if (networkId.id() == 1) {
            groupManager = groupManager1;
            providerService = providerService1;
            listener = listener1;
        } else {
            groupManager = groupManager2;
            providerService = providerService2;
            listener = listener2;
        }

        GroupKey key = new DefaultGroupKey("group1BeforeAudit".getBytes());
        Group createdGroup = groupManager.getGroup(deviceId, key);
        createdGroup = new DefaultGroup(createdGroup.id(),
                                        deviceId,
                                        Group.Type.SELECT,
                                        createdGroup.buckets());
        List<Group> groupEntries = Collections.singletonList(createdGroup);
        providerService.pushGroupMetrics(deviceId, groupEntries);
        listener.validateEvent(Collections.singletonList(GroupEvent.Type.GROUP_ADDED));
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

    // Test group add bucket operations
    private void testAddBuckets(NetworkId networkId, DeviceId deviceId) {
        VirtualNetworkGroupManager groupManager;
        VirtualGroupProviderService providerService;
        TestGroupListener listener;

        if (networkId.id() == 1) {
            groupManager = groupManager1;
            providerService = providerService1;
            listener = listener1;
        } else {
            groupManager = groupManager2;
            providerService = providerService2;
            listener = listener2;
        }

        GroupKey addKey = new DefaultGroupKey("group1AddBuckets".getBytes());

        GroupKey prevKey = new DefaultGroupKey("group1BeforeAudit".getBytes());
        Group createdGroup = groupManager.getGroup(deviceId, prevKey);
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
        groupManager.addBucketsToGroup(deviceId,
                                       prevKey,
                                       groupAddBuckets,
                                       addKey,
                                       appId);
        GroupBuckets updatedBuckets = new GroupBuckets(buckets);
        List<GroupOperation> expectedGroupOps = Collections.singletonList(
                GroupOperation.createModifyGroupOperation(createdGroup.id(),
                                                          Group.Type.SELECT,
                                                          updatedBuckets));
        if (deviceId.equals(VDID1)) {
            provider.validate(networkId, deviceId, expectedGroupOps);
        }

        Group existingGroup = groupManager.getGroup(deviceId, addKey);
        List<Group> groupEntries = Collections.singletonList(existingGroup);
        providerService.pushGroupMetrics(deviceId, groupEntries);
        listener.validateEvent(Collections.singletonList(GroupEvent.Type.GROUP_UPDATED));
    }

    // Test purge group entry operations
    private void testPurgeGroupEntry(NetworkId networkId, DeviceId deviceId) {
        VirtualNetworkGroupManager groupManager;
        if (networkId.id() == 1) {
            groupManager = groupManager1;
        } else {
            groupManager = groupManager2;
        }

        assertEquals(1, Iterables.size(groupManager.getGroups(deviceId, appId)));
        groupManager.purgeGroupEntries(deviceId);
        assertEquals(0, Iterables.size(groupManager.getGroups(deviceId, appId)));
    }

    // Test group remove bucket operations
    private void testRemoveBuckets(NetworkId networkId, DeviceId deviceId) {
        VirtualNetworkGroupManager groupManager;
        VirtualGroupProviderService providerService;
        TestGroupListener listener;

        if (networkId.id() == 1) {
            groupManager = groupManager1;
            providerService = providerService1;
            listener = listener1;
        } else {
            groupManager = groupManager2;
            providerService = providerService2;
            listener = listener2;
        }

        GroupKey removeKey = new DefaultGroupKey("group1RemoveBuckets".getBytes());

        GroupKey prevKey = new DefaultGroupKey("group1AddBuckets".getBytes());
        Group createdGroup = groupManager.getGroup(deviceId, prevKey);
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
        groupManager.removeBucketsFromGroup(deviceId,
                                            prevKey,
                                            groupRemoveBuckets,
                                            removeKey,
                                            appId);
        GroupBuckets updatedBuckets = new GroupBuckets(buckets);
        List<GroupOperation> expectedGroupOps = Collections.singletonList(
                GroupOperation.createModifyGroupOperation(createdGroup.id(),
                                                          Group.Type.SELECT,
                                                          updatedBuckets));
        if (deviceId.equals(VDID1)) {
            provider.validate(networkId, deviceId, expectedGroupOps);
        }

        Group existingGroup = groupManager.getGroup(deviceId, removeKey);
        List<Group> groupEntries = Collections.singletonList(existingGroup);
        providerService.pushGroupMetrics(deviceId, groupEntries);
        listener.validateEvent(Collections.singletonList(GroupEvent.Type.GROUP_UPDATED));
    }

    // Test group remove operations
    private void testRemoveGroup(NetworkId networkId, DeviceId deviceId) {
        VirtualNetworkGroupManager groupManager;
        VirtualGroupProviderService providerService;
        TestGroupListener listener;

        if (networkId.id() == 1) {
            groupManager = groupManager1;
            providerService = providerService1;
            listener = listener1;
        } else {
            groupManager = groupManager2;
            providerService = providerService2;
            listener = listener2;
        }

        GroupKey currKey = new DefaultGroupKey("group1RemoveBuckets".getBytes());
        Group existingGroup = groupManager.getGroup(deviceId, currKey);
        groupManager.removeGroup(deviceId, currKey, appId);
        List<GroupOperation> expectedGroupOps = Collections.singletonList(
                GroupOperation.createDeleteGroupOperation(existingGroup.id(),
                                                          Group.Type.SELECT));
        if (deviceId.equals(VDID1)) {
            provider.validate(networkId, deviceId, expectedGroupOps);
        }

        List<Group> groupEntries = Collections.emptyList();
        providerService.pushGroupMetrics(deviceId, groupEntries);
        listener.validateEvent(Collections.singletonList(GroupEvent.Type.GROUP_REMOVED));
    }

    private class TestGroupProvider extends AbstractVirtualProvider
            implements VirtualGroupProvider {
        NetworkId lastNetworkId;
        DeviceId lastDeviceId;
        List<GroupOperation> groupOperations = new ArrayList<>();

        protected TestGroupProvider() {
            super(new ProviderId("test", "org.onosproject.virtual.testprovider"));
        }

        @Override
        public void performGroupOperation(NetworkId networkId, DeviceId deviceId,
                                          GroupOperations groupOps) {
            lastNetworkId = networkId;
            lastDeviceId = deviceId;
            groupOperations.addAll(groupOps.operations());
        }

        public void validate(NetworkId expectedNetworkId, DeviceId expectedDeviceId,
                             List<GroupOperation> expectedGroupOps) {
            if (expectedGroupOps == null) {
                assertTrue("events generated", groupOperations.isEmpty());
                return;
            }

            assertEquals(lastNetworkId, expectedNetworkId);
            assertEquals(lastDeviceId, expectedDeviceId);
            assertTrue((this.groupOperations.containsAll(expectedGroupOps) &&
                    expectedGroupOps.containsAll(groupOperations)));

            groupOperations.clear();
            lastDeviceId = null;
            lastNetworkId = null;
        }
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
}