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
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.core.DefaultGroupId;
import org.onosproject.core.GroupId;
import org.onosproject.event.impl.TestEventDispatcher;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.group.DefaultGroup;
import org.onosproject.net.group.DefaultGroupBucket;
import org.onosproject.net.group.DefaultGroupDescription;
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
import org.onosproject.store.trivial.impl.SimpleGroupStore;

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
        mgr.store = new SimpleGroupStore();
        mgr.eventDispatcher = new TestEventDispatcher();
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
        mgr.eventDispatcher = null;
    }

    private class TestGroupKey implements GroupKey {
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
        PortNumber[] ports1 = {PortNumber.portNumber(31),
                               PortNumber.portNumber(32)};
        PortNumber[] ports2 = {PortNumber.portNumber(41),
                               PortNumber.portNumber(42)};
        // Test Group creation before AUDIT process
        TestGroupKey key = new TestGroupKey("group1BeforeAudit");
        List<GroupBucket> buckets = new ArrayList<GroupBucket>();
        List<PortNumber> outPorts = new ArrayList<PortNumber>();
        outPorts.addAll(Arrays.asList(ports1));
        outPorts.addAll(Arrays.asList(ports2));
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
        GroupDescription newGroupDesc = new DefaultGroupDescription(DID,
                                                                    Group.Type.SELECT,
                                                                    groupBuckets,
                                                                    key,
                                                                    appId);
        groupService.addGroup(newGroupDesc);
        internalProvider.validate(DID, null);
        assertEquals(null, groupService.getGroup(DID, key));
        assertEquals(0, Iterables.size(groupService.getGroups(DID, appId)));

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
        // First group metrics would trigger the device audit completion
        // post which all pending group requests are also executed.
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
                                           groupBuckets));
        internalProvider.validate(DID, expectedGroupOps);

        group1 = createSouthboundGroupEntry(gId1,
                                            Arrays.asList(ports1),
                                            0);
        group2 = createSouthboundGroupEntry(gId2,
                                            Arrays.asList(ports2),
                                            0);
        groupEntries = Arrays.asList(group1, group2);
        providerService.pushGroupMetrics(DID, groupEntries);
        expectedGroupOps = Arrays.asList(
                GroupOperation.createDeleteGroupOperation(gId1,
                                                          Group.Type.SELECT),
                GroupOperation.createDeleteGroupOperation(gId2,
                                                          Group.Type.SELECT),
                GroupOperation.createAddGroupOperation(createdGroup.id(),
                                                       Group.Type.SELECT,
                                                       groupBuckets));
        internalProvider.validate(DID, expectedGroupOps);

        createdGroup = new DefaultGroup(createdGroup.id(),
                                        DID,
                                        Group.Type.SELECT,
                                        groupBuckets);
        groupEntries = Arrays.asList(createdGroup);
        providerService.pushGroupMetrics(DID, groupEntries);
        internalListener.validateEvent(Arrays.asList(GroupEvent.Type.GROUP_ADDED));

        // Test group add bucket operations
        TestGroupKey addKey = new TestGroupKey("group1AddBuckets");
        PortNumber[] addPorts = {PortNumber.portNumber(51),
                                 PortNumber.portNumber(52)};
        outPorts.clear();
        outPorts.addAll(Arrays.asList(addPorts));
        List<GroupBucket> addBuckets = new ArrayList<GroupBucket>();
        for (PortNumber portNumber: outPorts) {
            TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
            tBuilder.setOutput(portNumber)
                    .setEthDst(MacAddress.valueOf("00:00:00:00:00:02"))
                    .setEthSrc(MacAddress.valueOf("00:00:00:00:00:01"))
                    .pushMpls()
                    .setMpls(106);
            addBuckets.add(DefaultGroupBucket.createSelectGroupBucket(
                                                        tBuilder.build()));
            buckets.add(DefaultGroupBucket.createSelectGroupBucket(
                                                        tBuilder.build()));
        }
        GroupBuckets groupAddBuckets = new GroupBuckets(addBuckets);
        groupService.addBucketsToGroup(DID,
                                       key,
                                       groupAddBuckets,
                                       addKey,
                                       appId);
        GroupBuckets updatedBuckets = new GroupBuckets(buckets);
        expectedGroupOps = Arrays.asList(
               GroupOperation.createModifyGroupOperation(createdGroup.id(),
                                                         Group.Type.SELECT,
                                                         updatedBuckets));
        internalProvider.validate(DID, expectedGroupOps);
        Group existingGroup = groupService.getGroup(DID, addKey);
        groupEntries = Arrays.asList(existingGroup);
        providerService.pushGroupMetrics(DID, groupEntries);
        internalListener.validateEvent(Arrays.asList(GroupEvent.Type.GROUP_UPDATED));

        // Test group remove bucket operations
        TestGroupKey removeKey = new TestGroupKey("group1RemoveBuckets");
        PortNumber[] removePorts = {PortNumber.portNumber(31),
                                 PortNumber.portNumber(32)};
        outPorts.clear();
        outPorts.addAll(Arrays.asList(removePorts));
        List<GroupBucket> removeBuckets = new ArrayList<GroupBucket>();
        for (PortNumber portNumber: outPorts) {
            TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
            tBuilder.setOutput(portNumber)
                    .setEthDst(MacAddress.valueOf("00:00:00:00:00:02"))
                    .setEthSrc(MacAddress.valueOf("00:00:00:00:00:01"))
                    .pushMpls()
                    .setMpls(106);
            removeBuckets.add(DefaultGroupBucket.createSelectGroupBucket(
                                                        tBuilder.build()));
            buckets.remove(DefaultGroupBucket.createSelectGroupBucket(
                                                        tBuilder.build()));
        }
        GroupBuckets groupRemoveBuckets = new GroupBuckets(removeBuckets);
        groupService.removeBucketsFromGroup(DID,
                                            addKey,
                                            groupRemoveBuckets,
                                            removeKey,
                                            appId);
        updatedBuckets = new GroupBuckets(buckets);
        expectedGroupOps = Arrays.asList(
               GroupOperation.createModifyGroupOperation(createdGroup.id(),
                                                         Group.Type.SELECT,
                                                         updatedBuckets));
        internalProvider.validate(DID, expectedGroupOps);
        existingGroup = groupService.getGroup(DID, removeKey);
        groupEntries = Arrays.asList(existingGroup);
        providerService.pushGroupMetrics(DID, groupEntries);
        internalListener.validateEvent(Arrays.asList(GroupEvent.Type.GROUP_UPDATED));

        // Test group remove operations
        groupService.removeGroup(DID, removeKey, appId);
        expectedGroupOps = Arrays.asList(
             GroupOperation.createDeleteGroupOperation(createdGroup.id(),
                                                       Group.Type.SELECT));
        internalProvider.validate(DID, expectedGroupOps);
        groupEntries = Collections.emptyList();
        providerService.pushGroupMetrics(DID, groupEntries);
        internalListener.validateEvent(Arrays.asList(GroupEvent.Type.GROUP_REMOVED));
    }

    private Group createSouthboundGroupEntry(GroupId gId,
                                             List<PortNumber> ports,
                                             long referenceCount) {
        List<PortNumber> outPorts = new ArrayList<PortNumber>();
        outPorts.addAll(ports);

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
        List<GroupOperation> groupOperations = new ArrayList<GroupOperation>();

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


