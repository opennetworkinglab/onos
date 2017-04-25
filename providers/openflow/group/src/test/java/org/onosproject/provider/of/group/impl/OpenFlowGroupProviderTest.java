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
package org.onosproject.provider.of.group.impl;

import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.core.GroupId;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.group.DefaultGroupBucket;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupOperation;
import org.onosproject.net.group.GroupOperations;
import org.onosproject.net.group.GroupProvider;
import org.onosproject.net.group.GroupProviderRegistry;
import org.onosproject.net.group.GroupProviderService;
import org.onosproject.net.provider.AbstractProviderService;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.openflow.controller.Dpid;
import org.onosproject.openflow.controller.OpenFlowController;
import org.onosproject.openflow.controller.OpenFlowEventListener;
import org.onosproject.openflow.controller.OpenFlowMessageListener;
import org.onosproject.openflow.controller.OpenFlowSwitch;
import org.onosproject.openflow.controller.OpenFlowSwitchListener;
import org.onosproject.openflow.controller.PacketListener;
import org.onosproject.openflow.controller.RoleState;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFGroupDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFGroupMod;
import org.projectfloodlight.openflow.protocol.OFGroupModFailedCode;
import org.projectfloodlight.openflow.protocol.OFGroupStatsReply;
import org.projectfloodlight.openflow.protocol.OFGroupType;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFMeterFeatures;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.errormsg.OFGroupModFailedErrorMsg;
import org.projectfloodlight.openflow.types.OFGroup;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.*;

public class OpenFlowGroupProviderTest {

    OpenFlowGroupProvider provider = new OpenFlowGroupProvider();
    private final OpenFlowController controller = new TestController();
    GroupProviderRegistry providerRegistry = new TestGroupProviderRegistry();
    GroupProviderService providerService;

    private DeviceId deviceId = DeviceId.deviceId("of:0000000000000001");
    private Dpid dpid1 = Dpid.dpid(deviceId.uri());

    @Before
    public void setUp() {
        provider.controller = controller;
        provider.providerRegistry = providerRegistry;
        provider.activate();
    }

    @Test
    public void basics() {
        assertNotNull("registration expected", providerService);
        assertEquals("incorrect provider", provider, providerService.provider());
    }

    @Test
    public void addGroup() {

        GroupId groupId = new GroupId(1);

        List<GroupBucket> bucketList = Lists.newArrayList();
        TrafficTreatment.Builder builder = DefaultTrafficTreatment.builder();
        builder.setOutput(PortNumber.portNumber(1));
        GroupBucket bucket =
                DefaultGroupBucket.createSelectGroupBucket(builder.build());
        bucketList.add(bucket);
        GroupBuckets buckets = new GroupBuckets(bucketList);

        List<GroupOperation> operationList = Lists.newArrayList();
        GroupOperation operation = GroupOperation.createAddGroupOperation(groupId,
                GroupDescription.Type.SELECT, buckets);
        operationList.add(operation);
        GroupOperations operations = new GroupOperations(operationList);

        provider.performGroupOperation(deviceId, operations);

        final Dpid dpid = Dpid.dpid(deviceId.uri());
        TestOpenFlowSwitch sw = (TestOpenFlowSwitch) controller.getSwitch(dpid);
        assertNotNull("Switch should not be nul", sw);
        assertNotNull("OFGroupMsg should not be null", sw.msg);

    }


    @Test
    public void groupModFailure() {
        TestOpenFlowGroupProviderService testProviderService =
                (TestOpenFlowGroupProviderService) providerService;

        GroupId groupId = new GroupId(1);
        List<GroupBucket> bucketList = Lists.newArrayList();
        TrafficTreatment.Builder builder = DefaultTrafficTreatment.builder();
        builder.setOutput(PortNumber.portNumber(1));
        GroupBucket bucket =
                DefaultGroupBucket.createSelectGroupBucket(builder.build());
        bucketList.add(bucket);
        GroupBuckets buckets = new GroupBuckets(bucketList);
        List<GroupOperation> operationList = Lists.newArrayList();
        GroupOperation operation = GroupOperation.createAddGroupOperation(groupId,
                GroupDescription.Type.SELECT, buckets);
        operationList.add(operation);
        GroupOperations operations = new GroupOperations(operationList);

        provider.performGroupOperation(deviceId, operations);

        OFGroupModFailedErrorMsg.Builder errorBuilder =
                OFFactories.getFactory(OFVersion.OF_13).errorMsgs().buildGroupModFailedErrorMsg();
        OFGroupMod.Builder groupBuilder = OFFactories.getFactory(OFVersion.OF_13).buildGroupModify();
        groupBuilder.setGroupType(OFGroupType.ALL);
        groupBuilder.setGroup(OFGroup.of(1));
        errorBuilder.setCode(OFGroupModFailedCode.GROUP_EXISTS);
        errorBuilder.setXid(provider.getXidAndAdd(0) - 1);

        controller.processPacket(dpid1, errorBuilder.build());

        assertNotNull("Operation failed should not be null",
                testProviderService.failedOperation);
    }


    @Test
    public void groupStatsEvent() {
        TestOpenFlowGroupProviderService testProviderService =
                (TestOpenFlowGroupProviderService) providerService;

        OFGroupStatsReply.Builder rep1 =
                OFFactories.getFactory(OFVersion.OF_13).buildGroupStatsReply();
        rep1.setXid(1);
        controller.processPacket(dpid1, rep1.build());
        OFGroupDescStatsReply.Builder rep2 =
                OFFactories.getFactory(OFVersion.OF_13).buildGroupDescStatsReply();
        assertNull("group entries is not set yet", testProviderService.getGroupEntries());

        rep2.setXid(2);
        controller.processPacket(dpid1, rep2.build());
        assertNotNull("group entries should be set", testProviderService.getGroupEntries());
    }



    @After
    public void tearDown() {
        provider.deactivate();
        provider.providerRegistry = null;
        provider.controller = null;
    }

    private class TestOpenFlowGroupProviderService
            extends AbstractProviderService<GroupProvider>
            implements GroupProviderService {

        Collection<Group> groups = null;
        GroupOperation failedOperation = null;

        protected TestOpenFlowGroupProviderService(GroupProvider provider) {
            super(provider);
        }

        @Override
        public void groupOperationFailed(DeviceId deviceId, GroupOperation operation) {
            this.failedOperation = operation;
        }

        @Override
        public void pushGroupMetrics(DeviceId deviceId, Collection<Group> groupEntries) {
            this.groups = groupEntries;
        }

        @Override
        public void notifyOfFailovers(Collection<Group> groups) {
        }

        public Collection<Group> getGroupEntries() {
            return groups;
        }
    }

    private class TestController implements OpenFlowController {

        OpenFlowEventListener eventListener = null;
        List<OpenFlowSwitch> switches = Lists.newArrayList();

        public TestController() {
            OpenFlowSwitch testSwitch = new TestOpenFlowSwitch();
            switches.add(testSwitch);
        }

        @Override
        public void addListener(OpenFlowSwitchListener listener) {
        }

        @Override
        public void removeListener(OpenFlowSwitchListener listener) {

        }

        @Override
        public void addMessageListener(OpenFlowMessageListener listener) {

        }

        @Override
        public void removeMessageListener(OpenFlowMessageListener listener) {

        }

        @Override
        public void addPacketListener(int priority, PacketListener listener) {

        }

        @Override
        public void removePacketListener(PacketListener listener) {

        }

        @Override
        public void addEventListener(OpenFlowEventListener listener) {
            this.eventListener = listener;
        }

        @Override
        public void removeEventListener(OpenFlowEventListener listener) {

        }

        @Override
        public void write(Dpid dpid, OFMessage msg) {

        }

        @Override
        public CompletableFuture<OFMessage> writeResponse(Dpid dpid, OFMessage msg) {
            return null;
        }

        @Override
        public void processPacket(Dpid dpid, OFMessage msg) {
            eventListener.handleMessage(dpid, msg);
        }

        @Override
        public void setRole(Dpid dpid, RoleState role) {

        }

        @Override
        public Iterable<OpenFlowSwitch> getSwitches() {
            return switches;
        }

        @Override
        public Iterable<OpenFlowSwitch> getMasterSwitches() {
            return null;
        }

        @Override
        public Iterable<OpenFlowSwitch> getEqualSwitches() {
            return null;
        }

        @Override
        public OpenFlowSwitch getSwitch(Dpid dpid) {
            return switches.get(0);
        }

        @Override
        public OpenFlowSwitch getMasterSwitch(Dpid dpid) {
            return null;
        }

        @Override
        public OpenFlowSwitch getEqualSwitch(Dpid dpid) {
            return null;
        }
    }

    private class TestGroupProviderRegistry implements GroupProviderRegistry {

        @Override
        public GroupProviderService register(GroupProvider provider) {
            providerService = new TestOpenFlowGroupProviderService(provider);
            return providerService;
        }

        @Override
        public void unregister(GroupProvider provider) {
        }

        @Override
        public Set<ProviderId> getProviders() {
            return null;
        }
    }

    private class TestOpenFlowSwitch implements OpenFlowSwitch {

        OFMessage msg = null;

        @Override
        public void sendMsg(OFMessage msg) {
            this.msg = msg;
        }

        @Override
        public void sendMsg(List<OFMessage> msgs) {

        }

        @Override
        public void handleMessage(OFMessage fromSwitch) {

        }

        @Override
        public void setRole(RoleState role) {

        }

        @Override
        public RoleState getRole() {
            return null;
        }

        @Override
        public List<OFPortDesc> getPorts() {
            return null;
        }

        @Override
        public OFMeterFeatures getMeterFeatures() {
            return null;
        }

        @Override
        public OFFactory factory() {
            return OFFactories.getFactory(OFVersion.OF_13);
        }

        @Override
        public String getStringId() {
            return null;
        }

        @Override
        public long getId() {
            return 0;
        }

        @Override
        public String manufacturerDescription() {
            return null;
        }

        @Override
        public String datapathDescription() {
            return null;
        }

        @Override
        public String hardwareDescription() {
            return null;
        }

        @Override
        public String softwareDescription() {
            return null;
        }

        @Override
        public String serialNumber() {
            return null;
        }

        @Override
        public boolean isConnected() {
            return false;
        }

        @Override
        public void disconnectSwitch() {

        }

        @Override
        public void returnRoleReply(RoleState requested, RoleState response) {

        }

        @Override
        public Device.Type deviceType() {
            return Device.Type.SWITCH;
        }

        @Override
        public String channelId() {
            return null;
        }
    }
}