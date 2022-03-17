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

package org.onosproject.p4runtime.ctl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.internal.AbstractServerImplBuilder;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.net.DeviceId;
import org.onosproject.net.pi.model.DefaultPiPipeconf;
import org.onosproject.net.pi.model.PiActionId;
import org.onosproject.net.pi.model.PiActionParamId;
import org.onosproject.net.pi.model.PiActionProfileId;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiPipeconfId;
import org.onosproject.net.pi.model.PiPipelineModel;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.onosproject.net.pi.runtime.PiActionProfileGroup;
import org.onosproject.net.pi.runtime.PiActionProfileGroupId;
import org.onosproject.net.pi.runtime.PiActionProfileMember;
import org.onosproject.net.pi.runtime.PiActionProfileMemberId;
import org.onosproject.p4runtime.api.P4RuntimeWriteClient;
import org.onosproject.p4runtime.ctl.client.P4RuntimeClientImpl;
import org.onosproject.p4runtime.ctl.controller.P4RuntimeControllerImpl;
import p4.v1.P4RuntimeOuterClass.ActionProfileGroup;
import p4.v1.P4RuntimeOuterClass.ActionProfileMember;
import p4.v1.P4RuntimeOuterClass.Entity;
import p4.v1.P4RuntimeOuterClass.Uint128;
import p4.v1.P4RuntimeOuterClass.Update;
import p4.v1.P4RuntimeOuterClass.WriteRequest;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.easymock.EasyMock.niceMock;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.onosproject.net.pi.model.PiPipeconf.ExtensionType.P4_INFO_TEXT;
import static p4.v1.P4RuntimeOuterClass.Action;
import static p4.v1.P4RuntimeOuterClass.ReadResponse;

/**
 * Tests for P4 Runtime Action Profile Group support.
 */
public class P4RuntimeGroupTest {
    private static final String PIPECONF_ID = "p4runtime-mock-pipeconf";
    private static final String P4INFO_PATH = "/test.p4info";
    private static final PiPipeconf PIPECONF = buildPipeconf();
    private static final int P4_INFO_ACT_PROF_ID = 285227860;
    private static final PiActionProfileId ACT_PROF_ID = PiActionProfileId.of("ecmp_selector");
    private static final PiActionProfileGroupId GROUP_ID = PiActionProfileGroupId.of(1);
    private static final int DEFAULT_MEMBER_WEIGHT = 1;
    private static final PiActionId EGRESS_PORT_ACTION_ID = PiActionId.of("set_egress_port");
    private static final PiActionParamId PORT_PARAM_ID = PiActionParamId.of("port");
    private static final int BASE_MEM_ID = 65535;
    private static final List<Integer> MEMBER_IDS = ImmutableList.of(65536, 65537, 65538);
    private static final List<PiActionProfileMember> GROUP_MEMBER_INSTANCES =
            Lists.newArrayList(
                    outputMember((short) 1),
                    outputMember((short) 2),
                    outputMember((short) 3)
            );
    private static final List<PiActionProfileGroup.WeightedMember> GROUP_WEIGHTED_MEMBERS =
            GROUP_MEMBER_INSTANCES.stream()
                    .map(m -> new PiActionProfileGroup.WeightedMember(m, DEFAULT_MEMBER_WEIGHT))
                    .collect(Collectors.toList());
    private static final PiActionProfileGroup GROUP = PiActionProfileGroup.builder()
            .withId(GROUP_ID)
            .addMembers(GROUP_MEMBER_INSTANCES)
            .withActionProfileId(ACT_PROF_ID)
            .build();
    private static final DeviceId DEVICE_ID = DeviceId.deviceId("device:p4runtime:1");
    private static final int P4_DEVICE_ID = 1;
    private static final int SET_EGRESS_PORT_ID = 16794308;
    private static final String GRPC_SERVER_NAME = "P4RuntimeGroupTest";
    private static final long DEFAULT_TIMEOUT_TIME = 10;
    private static final Uint128 DEFAULT_ELECTION_ID = Uint128.getDefaultInstance();

    private org.onosproject.p4runtime.ctl.client.P4RuntimeClientImpl client;
    private P4RuntimeControllerImpl controller;
    private static MockP4RuntimeServer p4RuntimeServerImpl = new MockP4RuntimeServer();
    private static Server grpcServer;
    private static ManagedChannel grpcChannel;

    private static PiActionProfileMember outputMember(short portNum) {
        PiActionParam param = new PiActionParam(PORT_PARAM_ID,
                                                ImmutableByteSequence.copyFrom(portNum));
        PiAction piAction = PiAction.builder()
                .withId(EGRESS_PORT_ACTION_ID)
                .withParameter(param).build();

        return PiActionProfileMember.builder()
                .forActionProfile(ACT_PROF_ID)
                .withAction(piAction)
                .withId(PiActionProfileMemberId.of(BASE_MEM_ID + portNum))
                .build();
    }

    private static PiPipeconf buildPipeconf() {
        final URL p4InfoUrl = P4RuntimeGroupTest.class.getResource(P4INFO_PATH);
        return DefaultPiPipeconf.builder()
                .withId(new PiPipeconfId(PIPECONF_ID))
                .withPipelineModel(EasyMock.niceMock(PiPipelineModel.class))
                .addExtension(P4_INFO_TEXT, p4InfoUrl)
                .build();
    }

    @BeforeClass
    public static void globalSetup() throws IOException {
        AbstractServerImplBuilder builder = InProcessServerBuilder
                .forName(GRPC_SERVER_NAME).directExecutor();
        builder.addService(p4RuntimeServerImpl);
        grpcServer = builder.build().start();
        grpcChannel = InProcessChannelBuilder.forName(GRPC_SERVER_NAME)
                .directExecutor()
                .build();
    }

    @AfterClass
    public static void globalTearDown() {
        grpcServer.shutdown();
        grpcChannel.shutdown();
    }


    @Before
    public void setup() {
        controller = niceMock(org.onosproject.p4runtime.ctl.controller.P4RuntimeControllerImpl.class);
        client = new P4RuntimeClientImpl(
                DEVICE_ID, grpcChannel, controller, new MockPipeconfService(),
                new MockMasterElectionIdStore());
    }

    @After
    public void teardown() {
        client.shutdown();
    }

    @Test
    public void testInvalidPiActionProfileMember() {
        PiActionParam param = new PiActionParam(PORT_PARAM_ID, "invalidString");
        PiAction piAction = PiAction.builder()
                .withId(EGRESS_PORT_ACTION_ID)
                .withParameter(param).build();
        PiActionProfileMember actionProfileMember = PiActionProfileMember.builder()
                .forActionProfile(ACT_PROF_ID)
                .withAction(piAction)
                .withId(PiActionProfileMemberId.of(BASE_MEM_ID + 1))
                .build();
        P4RuntimeWriteClient.WriteRequest writeRequest = client.write(P4_DEVICE_ID, PIPECONF);
        writeRequest.insert(actionProfileMember);
        P4RuntimeWriteClient.WriteResponse response = writeRequest.submitSync();

        assertEquals(false, response.isSuccess());
        assertEquals(1, response.all().size());
        assertEquals("Wrong size for param 'port' of action 'set_egress_port', " +
                        "expected no more than 2 bytes, but found 13",
                response.all().iterator().next().explanation());
    }

    @Test
    public void testInsertPiActionProfileGroup() throws Exception {
        CompletableFuture<Void> complete = p4RuntimeServerImpl.expectRequests(1);
        client.write(P4_DEVICE_ID, PIPECONF).insert(GROUP).submitSync();
        assertTrue(client.write(P4_DEVICE_ID, PIPECONF).insert(GROUP).submitSync().isSuccess());
        complete.get(DEFAULT_TIMEOUT_TIME, TimeUnit.SECONDS);
        WriteRequest result = p4RuntimeServerImpl.getWriteReqs().get(0);
        assertEquals(1, result.getDeviceId());
        assertEquals(1, result.getUpdatesCount());
        assertEquals(DEFAULT_ELECTION_ID, result.getElectionId());

        Update update = result.getUpdatesList().get(0);
        assertEquals(Update.Type.INSERT, update.getType());

        Entity entity = update.getEntity();
        ActionProfileGroup actionProfileGroup = entity.getActionProfileGroup();
        assertNotNull(actionProfileGroup);

        assertEquals(P4_INFO_ACT_PROF_ID, actionProfileGroup.getActionProfileId());
        assertEquals(3, actionProfileGroup.getMembersCount());
        List<ActionProfileGroup.Member> members = actionProfileGroup.getMembersList();

        for (ActionProfileGroup.Member member : members) {
            // XXX: We can't guarantee the order of member, just make sure we
            // have these member ids
            assertTrue(MEMBER_IDS.contains(member.getMemberId()));
            assertEquals(DEFAULT_MEMBER_WEIGHT, member.getWeight());
        }
    }

    @Test
    public void testInsertPiActionMembers() throws Exception {
        CompletableFuture<Void> complete = p4RuntimeServerImpl.expectRequests(1);
        assertTrue(client.write(P4_DEVICE_ID, PIPECONF).insert(GROUP_MEMBER_INSTANCES)
                           .submitSync().isSuccess());
        complete.get(DEFAULT_TIMEOUT_TIME, TimeUnit.SECONDS);
        WriteRequest result = p4RuntimeServerImpl.getWriteReqs().get(0);
        assertEquals(1, result.getDeviceId());
        assertEquals(3, result.getUpdatesCount());
        assertEquals(DEFAULT_ELECTION_ID, result.getElectionId());

        List<Update> updates = result.getUpdatesList();
        for (Update update : updates) {
            assertEquals(Update.Type.INSERT, update.getType());
            Entity entity = update.getEntity();
            ActionProfileMember member = entity.getActionProfileMember();
            assertNotNull(member);
            assertEquals(P4_INFO_ACT_PROF_ID, member.getActionProfileId());
            assertTrue(MEMBER_IDS.contains(member.getMemberId()));
            Action action = member.getAction();
            assertEquals(SET_EGRESS_PORT_ID, action.getActionId());
            assertEquals(1, action.getParamsCount());
            Action.Param param = action.getParamsList().get(0);
            assertEquals(1, param.getParamId());
            byte outPort = (byte) (member.getMemberId() - BASE_MEM_ID);
            ByteString bs = ByteString.copyFrom(new byte[]{outPort});
            assertEquals(bs, param.getValue());
        }
    }

    @Test
    public void testReadGroups() throws Exception {
        ActionProfileGroup.Builder group = ActionProfileGroup.newBuilder()
                .setGroupId(GROUP_ID.id())
                .setActionProfileId(P4_INFO_ACT_PROF_ID);

        MEMBER_IDS.forEach(id -> {
            ActionProfileGroup.Member member = ActionProfileGroup.Member.newBuilder()
                    .setMemberId(id)
                    .setWeight(DEFAULT_MEMBER_WEIGHT)
                    .build();
            group.addMembers(member);
        });

        List<ReadResponse> responses = Lists.newArrayList();
        responses.add(ReadResponse.newBuilder()
                              .addEntities(Entity.newBuilder().setActionProfileGroup(group))
                              .build()
        );

        p4RuntimeServerImpl.willReturnReadResult(responses);
        CompletableFuture<Void> complete = p4RuntimeServerImpl.expectRequests(1);
        Collection<PiActionProfileGroup> groups = client.read(P4_DEVICE_ID, PIPECONF)
                .actionProfileGroups(ACT_PROF_ID)
                .submitSync().all(PiActionProfileGroup.class);
        complete.get(DEFAULT_TIMEOUT_TIME, TimeUnit.SECONDS);
        assertEquals(1, groups.size());
        PiActionProfileGroup piActionGroup = groups.iterator().next();
        assertEquals(ACT_PROF_ID, piActionGroup.actionProfile());
        assertEquals(GROUP_ID, piActionGroup.id());
        assertEquals(3, piActionGroup.members().size());
        assertTrue(GROUP_WEIGHTED_MEMBERS.containsAll(piActionGroup.members()));
        assertTrue(piActionGroup.members().containsAll(GROUP_WEIGHTED_MEMBERS));
    }

    @Test
    public void testReadMembers() throws Exception {
        List<ActionProfileMember> members = Lists.newArrayList();

        MEMBER_IDS.forEach(id -> {
            byte outPort = (byte) (id - BASE_MEM_ID);
            ByteString bs = ByteString.copyFrom(new byte[]{0, outPort});
            Action.Param param = Action.Param.newBuilder()
                    .setParamId(1)
                    .setValue(bs)
                    .build();

            Action action = Action.newBuilder()
                    .setActionId(SET_EGRESS_PORT_ID)
                    .addParams(param)
                    .build();

            ActionProfileMember actProfMember =
                    ActionProfileMember.newBuilder()
                            .setActionProfileId(P4_INFO_ACT_PROF_ID)
                            .setMemberId(id)
                            .setAction(action)
                            .build();
            members.add(actProfMember);
        });

        List<ReadResponse> responses = Lists.newArrayList();
        responses.add(ReadResponse.newBuilder()
                              .addAllEntities(members.stream()
                                                      .map(m -> Entity.newBuilder()
                                                              .setActionProfileMember(m).build())
                                                      .collect(Collectors.toList()))
                              .build());

        p4RuntimeServerImpl.willReturnReadResult(responses);
        CompletableFuture<Void> complete = p4RuntimeServerImpl.expectRequests(1);
        Collection<PiActionProfileMember> piMembers = client.read(P4_DEVICE_ID, PIPECONF)
                .actionProfileMembers(ACT_PROF_ID).submitSync()
                .all(PiActionProfileMember.class);
        complete.get(DEFAULT_TIMEOUT_TIME, TimeUnit.SECONDS);
        assertEquals(3, piMembers.size());
        assertTrue(GROUP_MEMBER_INSTANCES.containsAll(piMembers));
        assertTrue(piMembers.containsAll(GROUP_MEMBER_INSTANCES));
    }
}
