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

package org.onosproject.p4runtime.test;

import com.google.common.collect.Lists;
import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.NettyChannelBuilder;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.bmv2.model.Bmv2PipelineModelParser;
import org.onosproject.drivers.bmv2.Bmv2DefaultInterpreter;
import org.onosproject.grpc.ctl.GrpcControllerImpl;
import org.onosproject.net.DeviceId;
import org.onosproject.net.pi.model.DefaultPiPipeconf;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiPipeconfId;
import org.onosproject.net.pi.model.PiPipelineInterpreter;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionId;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.onosproject.net.pi.runtime.PiActionParamId;
import org.onosproject.net.pi.runtime.PiHeaderFieldId;
import org.onosproject.net.pi.runtime.PiMatchKey;
import org.onosproject.net.pi.runtime.PiPacketMetadata;
import org.onosproject.net.pi.runtime.PiPacketMetadataId;
import org.onosproject.net.pi.runtime.PiPacketOperation;
import org.onosproject.net.pi.runtime.PiTableEntry;
import org.onosproject.net.pi.runtime.PiTableId;
import org.onosproject.net.pi.runtime.PiTernaryFieldMatch;
import org.onosproject.p4runtime.api.P4RuntimeClient;
import org.onosproject.p4runtime.ctl.P4RuntimeClientImpl;
import org.onosproject.p4runtime.ctl.P4RuntimeControllerImpl;
import p4.P4RuntimeGrpc;
import p4.P4RuntimeOuterClass;

import java.net.URL;
import java.util.concurrent.ExecutionException;

import static org.onlab.util.ImmutableByteSequence.*;
import static org.onosproject.net.pi.model.PiPipeconf.ExtensionType.BMV2_JSON;
import static org.onosproject.net.pi.model.PiPipeconf.ExtensionType.P4_INFO_TEXT;
import static org.onosproject.net.pi.runtime.PiPacketOperation.Type.PACKET_OUT;
import static p4.P4RuntimeOuterClass.ActionProfileGroup.Type.SELECT;
import static p4.P4RuntimeOuterClass.Update.Type.INSERT;

/**
 * Class used for quick testing of P4Runtime with real devices. To be removed before release.
 */
public class P4RuntimeTest {

    private static final String GRPC_SERVER_ADDR = "192.168.56.102";
    private static final int GRPC_SERVER_PORT = 55044;

    private static final String TABLE_0 = "table0";
    private static final String SET_EGRESS_PORT = "set_egress_port";
    private static final String PORT = "port";
    private static final String ETHERNET = "ethernet";
    private static final String DST_ADDR = "dstAddr";
    private static final String SRC_ADDR = "srcAddr";
    private static final String STANDARD_METADATA = "standard_metadata";
    private static final String INGRESS_PORT = "ingress_port";
    private static final String ETHER_TYPE = "etherType";


    private final URL p4InfoUrl = this.getClass().getResource("/bmv2/default.p4info");
    private final URL jsonUrl = this.getClass().getResource("/bmv2/default.json");

    private final PiPipeconf bmv2DefaultPipeconf = DefaultPiPipeconf.builder()
            .withId(new PiPipeconfId("mock-p4runtime"))
            .withPipelineModel(Bmv2PipelineModelParser.parse(jsonUrl))
            .addBehaviour(PiPipelineInterpreter.class, Bmv2DefaultInterpreter.class)
            .addExtension(P4_INFO_TEXT, p4InfoUrl)
            .addExtension(BMV2_JSON, jsonUrl)
            .build();
    private final P4RuntimeControllerImpl controller = new P4RuntimeControllerImpl();
    private final GrpcControllerImpl grpcController = new GrpcControllerImpl();
    private final DeviceId deviceId = DeviceId.deviceId("dummy:1");
    private final ManagedChannelBuilder channelBuilder = NettyChannelBuilder
            .forAddress(GRPC_SERVER_ADDR, GRPC_SERVER_PORT)
            .usePlaintext(true);
    private P4RuntimeClientImpl client;

    private final ImmutableByteSequence ethAddr = fit(copyFrom(1), 48);
    private final ImmutableByteSequence portValue = copyFrom((short) 1);
    private final PiHeaderFieldId ethDstAddrFieldId = PiHeaderFieldId.of(ETHERNET, DST_ADDR);
    private final PiHeaderFieldId ethSrcAddrFieldId = PiHeaderFieldId.of(ETHERNET, SRC_ADDR);
    private final PiHeaderFieldId inPortFieldId = PiHeaderFieldId.of(STANDARD_METADATA, INGRESS_PORT);
    private final PiHeaderFieldId ethTypeFieldId = PiHeaderFieldId.of(ETHERNET, ETHER_TYPE);
    private final PiActionParamId portParamId = PiActionParamId.of(PORT);
    private final PiActionId outActionId = PiActionId.of(SET_EGRESS_PORT);
    private final PiTableId tableId = PiTableId.of(TABLE_0);

    private final PiTableEntry piTableEntry = PiTableEntry
            .builder()
            .forTable(tableId)
            .withMatchKey(PiMatchKey.builder()
                                  .addFieldMatch(new PiTernaryFieldMatch(ethDstAddrFieldId, ethAddr, ofZeros(6)))
                                  .addFieldMatch(new PiTernaryFieldMatch(ethSrcAddrFieldId, ethAddr, ofZeros(6)))
                                  .addFieldMatch(new PiTernaryFieldMatch(inPortFieldId, portValue, ofZeros(2)))
                                  .addFieldMatch(new PiTernaryFieldMatch(ethTypeFieldId, portValue, ofZeros(2)))
                                  .build())
            .withAction(PiAction
                                .builder()
                                .withId(outActionId)
                                .withParameter(new PiActionParam(portParamId, portValue))
                                .build())
            .withPriority(1)
            .withCookie(2)
            .build();

    public P4RuntimeTest() throws ImmutableByteSequence.ByteSequenceTrimException {
    }

    @Before
    public void setUp() throws Exception {
        controller.grpcController = grpcController;
        GrpcControllerImpl.enableMessageLog = true;
        grpcController.activate();
    }

    private void createClient()
            throws ExecutionException, InterruptedException, PiPipelineInterpreter.PiInterpreterException,
            IllegalAccessException, InstantiationException {

        assert (controller.createClient(deviceId, 1, channelBuilder));

        client = (P4RuntimeClientImpl) controller.getClient(deviceId);
    }

    private void setPipelineConfig(PiPipeconf pipeconf, PiPipeconf.ExtensionType extensionType)
            throws ExecutionException, InterruptedException, PiPipelineInterpreter.PiInterpreterException,
            IllegalAccessException, InstantiationException {

        assert (client.setPipelineConfig(pipeconf, extensionType).get());
        assert (client.initStreamChannel().get());
    }

    private void testActionProfile(int actionProfileId) {

        P4RuntimeGrpc.P4RuntimeBlockingStub stub = client.blockingStub();

        P4RuntimeOuterClass.ActionProfileMember profileMemberMsg = P4RuntimeOuterClass.ActionProfileMember.newBuilder()
                .setActionProfileId(actionProfileId)
                .setMemberId(1)
                .setAction(P4RuntimeOuterClass.Action.newBuilder()
                                   .setActionId(16793508)
                                   .build())
                .build();

        P4RuntimeOuterClass.ActionProfileGroup groupMsg = P4RuntimeOuterClass.ActionProfileGroup.newBuilder()
                .setActionProfileId(actionProfileId)
                .setGroupId(1)
                .setType(SELECT)
                .addMembers(P4RuntimeOuterClass.ActionProfileGroup.Member.newBuilder()
                                    .setMemberId(1)
                                    .setWeight(1)
                                    .build())
                .setMaxSize(3)
                .build();

        P4RuntimeOuterClass.WriteRequest writeRequest = P4RuntimeOuterClass.WriteRequest.newBuilder()
                .setDeviceId(client.p4DeviceId())
                .addUpdates(P4RuntimeOuterClass.Update.newBuilder()
                                    .setType(INSERT)
                                    .setEntity(P4RuntimeOuterClass.Entity.newBuilder()
                                                       .setActionProfileGroup(groupMsg)
                                                       .build())
                                    .build())
                .addUpdates(P4RuntimeOuterClass.Update.newBuilder()
                                    .setType(INSERT)
                                    .setEntity(P4RuntimeOuterClass.Entity.newBuilder()
                                                       .setActionProfileMember(profileMemberMsg)
                                                       .build())
                                    .build())
                .build();

        stub.write(writeRequest);
    }

    private void testPacketOut() throws IllegalAccessException, InstantiationException, ExecutionException,
            InterruptedException, ImmutableByteSequence.ByteSequenceTrimException {

        PiPacketOperation packetOperation = PiPacketOperation.builder()
                .withData(fit(copyFrom(1), 48 + 48 + 16))
                .withType(PACKET_OUT)
                .withMetadata(PiPacketMetadata.builder()
                                      .withId(PiPacketMetadataId.of("egress_port"))
                                      .withValue(fit(copyFrom(255), 9))
                                      .build())
                .build();

        assert (client.packetOut(packetOperation, bmv2DefaultPipeconf).get());

        Thread.sleep(5000);
    }

    private void testDumpTable(String tableName, PiPipeconf pipeconf) throws ExecutionException, InterruptedException {
        assert (client.dumpTable(PiTableId.of(tableName), pipeconf).get().size() == 0);
    }

    private void testAddEntry(PiPipeconf pipeconf) throws ExecutionException, InterruptedException {
        assert (client.writeTableEntries(Lists.newArrayList(piTableEntry), P4RuntimeClient.WriteOperationType.INSERT,
                                         pipeconf).get());
    }

    @Test
    @Ignore
    public void testBmv2() throws Exception {

        createClient();
        setPipelineConfig(bmv2DefaultPipeconf, BMV2_JSON);
        testPacketOut();

        // testPacketOut();

        // testActionProfile(285261835);
    }

    @Test
    @Ignore
    public void testBmv2AddEntry() throws Exception {
        createClient();
        testAddEntry(bmv2DefaultPipeconf);
        testDumpTable(TABLE_0, bmv2DefaultPipeconf);
    }

    @Test
    @Ignore
    public void testTofino() throws Exception {
        createClient();
        setPipelineConfig(bmv2DefaultPipeconf, null);
    }

// OLD STUFF
//        log.info("++++++++++++++++++++++++++++");
//
//        PiPipelineInterpreter interpreter = (PiPipelineInterpreter) defaultPipeconf
//                .implementation(PiPipelineInterpreter.class)
//                .orElse(null)
//                .newInstance();
//
//        TrafficTreatment t = DefaultTrafficTreatment.builder()
//                .setOutput(PortNumber.portNumber(830L)).build();
//        byte[] payload = new byte[1000];
////        payload[0] = 1;
//        Arrays.fill( payload, (byte) 1 );
//
//        OutboundPacket packet = new DefaultOutboundPacket(
//                deviceId, t, ByteBuffer.wrap(payload));
//
//
//        Collection<PiPacketOperation> operations = interpreter.mapOutboundPacket(packet,defaultPipeconf);
//        log.info("{}", operations);
//        operations.forEach(piPacketOperation -> {
//            try {
//                client.packetOut(piPacketOperation, defaultPipeconf).get();
//            } catch (InterruptedException | ExecutionException e) {
//               log.error("{}",e);
//            }
//        });

//        assert(client.dumpTable(PiTableId.of(TABLE_0), defaultPipeconf).get().size() == 0);

//        assert(client.writeTableEntries(Lists.newArrayList(piTableEntry), INSERT, defaultPipeconf).get());

//        assert(client.dumpTable(PiTableId.of(TABLE_0), defaultPipeconf).get().size() == 1);
}
