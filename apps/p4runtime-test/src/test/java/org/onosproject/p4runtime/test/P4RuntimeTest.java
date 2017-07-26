/*
 * Copyright 2017-present Open Networking Laboratory
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

import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.NettyChannelBuilder;
import org.junit.Ignore;
import org.junit.Test;
import org.onosproject.bmv2.model.Bmv2PipelineModelParser;
import org.onosproject.grpc.ctl.GrpcControllerImpl;
import org.onosproject.net.DeviceId;
import org.onosproject.net.pi.model.DefaultPiPipeconf;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiPipeconfId;
import org.onosproject.net.pi.model.PiPipelineInterpreter;
import org.onosproject.p4runtime.api.P4RuntimeClient;
import org.onosproject.p4runtime.ctl.P4RuntimeControllerImpl;

import java.net.URL;
import java.util.concurrent.ExecutionException;

import static org.onosproject.net.pi.model.PiPipeconf.ExtensionType.BMV2_JSON;
import static org.onosproject.net.pi.model.PiPipeconf.ExtensionType.P4_INFO_TEXT;

/**
 * Class used for quick testing of P4Runtime with real devices. To be removed before release.
 */
public class P4RuntimeTest {

    private final URL p4InfoUrl = this.getClass().getResource("/default.p4info");
    private final URL jsonUrl = this.getClass().getResource("/default.json");

    private final PiPipeconf bmv2DefaultPipeconf = DefaultPiPipeconf.builder()
            .withId(new PiPipeconfId("mock"))
            .withPipelineModel(Bmv2PipelineModelParser.parse(jsonUrl))
//            .addBehaviour(PiPipelineInterpreter.class, Bmv2DefaultInterpreter.class)
            .addExtension(P4_INFO_TEXT, p4InfoUrl)
            .addExtension(BMV2_JSON, jsonUrl)
            .build();

    @Test
    @Ignore
    public void testRuntime() throws ExecutionException, InterruptedException,
            PiPipelineInterpreter.PiInterpreterException, IllegalAccessException, InstantiationException {

        // FIXME: remove me.

        P4RuntimeControllerImpl controller = new P4RuntimeControllerImpl();
        GrpcControllerImpl grpcController = new GrpcControllerImpl();
        controller.grpcController = grpcController;
        GrpcControllerImpl.enableMessageLog = true;
        grpcController.activate();
        DeviceId deviceId = DeviceId.deviceId("dummy:1");

        ManagedChannelBuilder channelBuilder = NettyChannelBuilder
                .forAddress("192.168.56.102", 59975)
                .usePlaintext(true);

        assert (controller.createClient(deviceId, 1, channelBuilder));

        P4RuntimeClient client = controller.getClient(deviceId);

        assert (client.setPipelineConfig(bmv2DefaultPipeconf, PiPipeconf.ExtensionType.BMV2_JSON).get());

        assert (client.initStreamChannel().get());

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
}
