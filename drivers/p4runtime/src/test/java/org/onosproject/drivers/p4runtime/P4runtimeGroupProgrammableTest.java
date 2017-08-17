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

package org.onosproject.drivers.p4runtime;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.GroupId;
import org.onosproject.drivers.p4runtime.P4RuntimeGroupProgrammable.DefaultP4RuntimeGroupCookie;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverData;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.group.DefaultGroup;
import org.onosproject.net.group.DefaultGroupBucket;
import org.onosproject.net.group.DefaultGroupDescription;
import org.onosproject.net.group.DefaultGroupKey;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupKey;
import org.onosproject.net.group.GroupOperation;
import org.onosproject.net.group.GroupOperations;
import org.onosproject.net.group.GroupStore;
import org.onosproject.net.pi.model.DefaultPiPipeconf;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiPipeconfId;
import org.onosproject.net.pi.model.PiPipelineInterpreter;
import org.onosproject.net.pi.model.PiPipelineModel;
import org.onosproject.net.pi.runtime.PiActionProfileId;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionGroup;
import org.onosproject.net.pi.runtime.PiActionGroupMember;
import org.onosproject.net.pi.runtime.PiActionGroupMemberId;
import org.onosproject.net.pi.runtime.PiActionId;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.onosproject.net.pi.runtime.PiActionParamId;
import org.onosproject.net.pi.runtime.PiPipeconfService;
import org.onosproject.net.pi.runtime.PiTableAction;
import org.onosproject.net.pi.runtime.PiTableId;
import org.onosproject.p4runtime.api.P4RuntimeClient;
import org.onosproject.p4runtime.api.P4RuntimeController;

import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;
import static org.onosproject.net.group.GroupDescription.Type.SELECT;
import static org.onosproject.net.pi.model.PiPipeconf.ExtensionType.P4_INFO_TEXT;
import static org.onosproject.p4runtime.api.P4RuntimeClient.WriteOperationType.DELETE;
import static org.onosproject.p4runtime.api.P4RuntimeClient.WriteOperationType.INSERT;

public class P4runtimeGroupProgrammableTest {
    private static final String P4INFO_PATH = "/default.p4info";
    private static final DeviceId DEVICE_ID = DeviceId.deviceId("device:p4runtime:1");
    private static final PiPipeconfId PIPECONF_ID = new PiPipeconfId("p4runtime-mock-pipeconf");
    private static final PiPipeconf PIPECONF = buildPipeconf();
    private static final PiTableId ECMP_TABLE_ID = PiTableId.of("ecmp");
    private static final PiActionProfileId ACT_PROF_ID = PiActionProfileId.of("ecmp_selector");
    private static final ApplicationId APP_ID = TestApplicationId.create("P4runtimeGroupProgrammableTest");
    private static final GroupId GROUP_ID = GroupId.valueOf(1);
    private static final PiActionId EGRESS_PORT_ACTION_ID = PiActionId.of("set_egress_port");
    private static final PiActionParamId PORT_PARAM_ID = PiActionParamId.of("port");
    private static final List<GroupBucket> BUCKET_LIST = ImmutableList.of(
            outputBucket(1),
            outputBucket(2),
            outputBucket(3)
    );
    private static final DefaultP4RuntimeGroupCookie COOKIE =
            new DefaultP4RuntimeGroupCookie(ECMP_TABLE_ID, ACT_PROF_ID, GROUP_ID.id());
    private static final GroupKey GROUP_KEY =
            new DefaultGroupKey(P4RuntimeGroupProgrammable.KRYO.serialize(COOKIE));
    private static final GroupBuckets BUCKETS = new GroupBuckets(BUCKET_LIST);
    private static final GroupDescription GROUP_DESC =
            new DefaultGroupDescription(DEVICE_ID,
                                        SELECT,
                                        BUCKETS,
                                        GROUP_KEY,
                                        GROUP_ID.id(),
                                        APP_ID);
    private static final Group GROUP = new DefaultGroup(GROUP_ID, GROUP_DESC);
    private static final int DEFAULT_MEMBER_WEIGHT = 1;
    private static final int BASE_MEM_ID = 65535;
    private static final Collection<PiActionGroupMember> EXPECTED_MEMBERS =
            ImmutableSet.of(
                    outputMember(1),
                    outputMember(2),
                    outputMember(3)
            );

    private P4RuntimeGroupProgrammable programmable;
    private DriverHandler driverHandler;
    private DriverData driverData;
    private P4RuntimeController controller;
    private P4RuntimeClient client;
    private PiPipeconfService piPipeconfService;
    private DeviceService deviceService;
    private Device device;
    private GroupStore groupStore;

    private static PiPipeconf buildPipeconf() {
        final URL p4InfoUrl = P4runtimeGroupProgrammableTest.class.getResource(P4INFO_PATH);
        return DefaultPiPipeconf.builder()
                .withId(PIPECONF_ID)
                .withPipelineModel(niceMock(PiPipelineModel.class))
                .addExtension(P4_INFO_TEXT, p4InfoUrl)
                .build();
    }

    private static GroupBucket outputBucket(int portNum) {
        ImmutableByteSequence paramVal = ImmutableByteSequence.copyFrom(portNum);
        PiActionParam param = new PiActionParam(PiActionParamId.of(PORT_PARAM_ID.name()), paramVal);
        PiTableAction action = PiAction.builder().withId(EGRESS_PORT_ACTION_ID).withParameter(param).build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .add(Instructions.piTableAction(action))
                .build();

        return DefaultGroupBucket.createSelectGroupBucket(treatment);
    }

    private static PiActionGroupMember outputMember(int portNum) {
        PiActionParam param = new PiActionParam(PORT_PARAM_ID,
                                                ImmutableByteSequence.copyFrom(portNum));
        PiAction piAction = PiAction.builder()
                .withId(EGRESS_PORT_ACTION_ID)
                .withParameter(param).build();

        return PiActionGroupMember.builder()
                .withAction(piAction)
                .withId(PiActionGroupMemberId.of(BASE_MEM_ID + portNum))
                .withWeight(DEFAULT_MEMBER_WEIGHT)
                .build();
    }

    @Before
    public void setup() {
        driverHandler = EasyMock.niceMock(DriverHandler.class);
        driverData = EasyMock.niceMock(DriverData.class);
        controller = EasyMock.niceMock(P4RuntimeController.class);
        client = EasyMock.niceMock(P4RuntimeClient.class);
        piPipeconfService = EasyMock.niceMock(PiPipeconfService.class);
        deviceService = EasyMock.niceMock(DeviceService.class);
        device = EasyMock.niceMock(Device.class);
        groupStore = EasyMock.niceMock(GroupStore.class);

        expect(controller.hasClient(DEVICE_ID)).andReturn(true).anyTimes();
        expect(controller.getClient(DEVICE_ID)).andReturn(client).anyTimes();
        expect(device.is(PiPipelineInterpreter.class)).andReturn(true).anyTimes();
        expect(device.id()).andReturn(DEVICE_ID).anyTimes();
        expect(deviceService.getDevice(DEVICE_ID)).andReturn(device).anyTimes();
        expect(driverData.deviceId()).andReturn(DEVICE_ID).anyTimes();
        expect(groupStore.getGroup(DEVICE_ID, GROUP_ID)).andReturn(GROUP).anyTimes();
        expect(piPipeconfService.ofDevice(DEVICE_ID)).andReturn(Optional.of(PIPECONF_ID)).anyTimes();
        expect(piPipeconfService.getPipeconf(PIPECONF_ID)).andReturn(Optional.of(PIPECONF)).anyTimes();
        expect(driverHandler.data()).andReturn(driverData).anyTimes();
        expect(driverHandler.get(P4RuntimeController.class)).andReturn(controller).anyTimes();
        expect(driverHandler.get(PiPipeconfService.class)).andReturn(piPipeconfService).anyTimes();
        expect(driverHandler.get(DeviceService.class)).andReturn(deviceService).anyTimes();
        expect(driverHandler.get(GroupStore.class)).andReturn(groupStore).anyTimes();

        programmable = new P4RuntimeGroupProgrammable();
        programmable.setHandler(driverHandler);
        programmable.setData(driverData);
        EasyMock.replay(driverHandler, driverData, controller, piPipeconfService,
                        deviceService, device, groupStore);
    }

    /**
     * Test init function.
     */
    @Test
    public void testInit() {
        programmable.init();
    }

    /**
     * Test add group with buckets.
     */
    @Test
    public void testAddGroup() {
        List<GroupOperation> ops = Lists.newArrayList();
        ops.add(GroupOperation.createAddGroupOperation(GROUP_ID, SELECT, BUCKETS));
        GroupOperations groupOps = new GroupOperations(ops);
        CompletableFuture<Boolean> completeTrue = new CompletableFuture<>();
        completeTrue.complete(true);

        Capture<PiActionGroup> groupCapture1 = EasyMock.newCapture();
        expect(client.writeActionGroup(EasyMock.capture(groupCapture1), EasyMock.eq(INSERT), EasyMock.eq(PIPECONF)))
                .andReturn(completeTrue).anyTimes();

        Capture<PiActionGroup> groupCapture2 = EasyMock.newCapture();
        Capture<Collection<PiActionGroupMember>> membersCapture = EasyMock.newCapture();
        expect(client.writeActionGroupMembers(EasyMock.capture(groupCapture2),
                                                       EasyMock.capture(membersCapture),
                                                       EasyMock.eq(INSERT),
                                                       EasyMock.eq(PIPECONF)))
                .andReturn(completeTrue).anyTimes();

        EasyMock.replay(client);
        programmable.performGroupOperation(DEVICE_ID, groupOps);

        // verify group installed by group programmable
        PiActionGroup group1 = groupCapture1.getValue();
        PiActionGroup group2 = groupCapture2.getValue();
        assertEquals("Groups should be equal", group1, group2);
        assertEquals(GROUP_ID.id(), group1.id().id());
        assertEquals(PiActionGroup.Type.SELECT, group1.type());
        assertEquals(ACT_PROF_ID, group1.actionProfileId());

        // members installed
        Collection<PiActionGroupMember> members = group1.members();
        assertEquals(3, members.size());

        Assert.assertTrue(EXPECTED_MEMBERS.containsAll(members));
        Assert.assertTrue(members.containsAll(EXPECTED_MEMBERS));
    }

    /**
     * Test remove group with buckets.
     */
    @Test
    public void testDelGroup() {
        List<GroupOperation> ops = Lists.newArrayList();
        ops.add(GroupOperation.createDeleteGroupOperation(GROUP_ID, SELECT));
        GroupOperations groupOps = new GroupOperations(ops);
        CompletableFuture<Boolean> completeTrue = new CompletableFuture<>();
        completeTrue.complete(true);

        Capture<PiActionGroup> groupCapture1 = EasyMock.newCapture();
        expect(client.writeActionGroup(EasyMock.capture(groupCapture1), EasyMock.eq(DELETE), EasyMock.eq(PIPECONF)))
                .andReturn(completeTrue).anyTimes();

        Capture<PiActionGroup> groupCapture2 = EasyMock.newCapture();
        Capture<Collection<PiActionGroupMember>> membersCapture = EasyMock.newCapture();
        expect(client.writeActionGroupMembers(EasyMock.capture(groupCapture2),
                                                       EasyMock.capture(membersCapture),
                                                       EasyMock.eq(DELETE),
                                                       EasyMock.eq(PIPECONF)))
                .andReturn(completeTrue).anyTimes();

        EasyMock.replay(client);
        programmable.performGroupOperation(DEVICE_ID, groupOps);

        // verify group installed by group programmable
        PiActionGroup group1 = groupCapture1.getValue();
        PiActionGroup group2 = groupCapture2.getValue();
        assertEquals("Groups should be equal", group1, group2);
        assertEquals(GROUP_ID.id(), group1.id().id());
        assertEquals(PiActionGroup.Type.SELECT, group1.type());
        assertEquals(ACT_PROF_ID, group1.actionProfileId());

        // members installed
        Collection<PiActionGroupMember> members = group1.members();
        assertEquals(3, members.size());

        Assert.assertTrue(EXPECTED_MEMBERS.containsAll(members));
        Assert.assertTrue(members.containsAll(EXPECTED_MEMBERS));
    }
}
