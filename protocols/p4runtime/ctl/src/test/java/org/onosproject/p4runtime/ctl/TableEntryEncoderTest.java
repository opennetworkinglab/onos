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

package org.onosproject.p4runtime.ctl;

import com.google.common.collect.Lists;
import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.bmv2.model.Bmv2PipelineModelParser;
import org.onosproject.net.pi.model.DefaultPiPipeconf;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiPipeconfId;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionId;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.onosproject.net.pi.runtime.PiActionParamId;
import org.onosproject.net.pi.runtime.PiHeaderFieldId;
import org.onosproject.net.pi.runtime.PiTableEntry;
import org.onosproject.net.pi.runtime.PiTableId;
import org.onosproject.net.pi.runtime.PiTernaryFieldMatch;
import p4.P4RuntimeOuterClass.Action;
import p4.P4RuntimeOuterClass.TableEntry;

import java.net.URL;
import java.util.Collection;
import java.util.Random;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.onlab.util.ImmutableByteSequence.copyFrom;
import static org.onlab.util.ImmutableByteSequence.fit;
import static org.onosproject.net.pi.model.PiPipeconf.ExtensionType.BMV2_JSON;
import static org.onosproject.net.pi.model.PiPipeconf.ExtensionType.P4_INFO_TEXT;
import static org.onosproject.p4runtime.ctl.TableEntryEncoder.decode;
import static org.onosproject.p4runtime.ctl.TableEntryEncoder.encode;

public class TableEntryEncoderTest {

    private static final String TABLE_0 = "table0";
    private static final String SET_EGRESS_PORT = "set_egress_port";
    private static final String PORT = "port";
    private static final String ETHERNET = "ethernet";
    private static final String DST_ADDR = "dstAddr";
    private static final String SRC_ADDR = "srcAddr";
    private static final String STANDARD_METADATA = "standard_metadata";
    private static final String INGRESS_PORT = "ingress_port";
    private static final String ETHER_TYPE = "etherType";

    private final Random rand = new Random();
    private final URL p4InfoUrl = this.getClass().getResource("/default.p4info");
    private final URL jsonUrl = this.getClass().getResource("/default.json");

    private final PiPipeconf defaultPipeconf = DefaultPiPipeconf.builder()
            .withId(new PiPipeconfId("mock"))
            .withPipelineModel(Bmv2PipelineModelParser.parse(jsonUrl))
            .addExtension(P4_INFO_TEXT, p4InfoUrl)
            .addExtension(BMV2_JSON, jsonUrl)
            .build();

    private final P4InfoBrowser browser = PipeconfHelper.getP4InfoBrowser(defaultPipeconf);
    private final ImmutableByteSequence ethAddr = fit(copyFrom(rand.nextInt()), 48);
    private final ImmutableByteSequence portValue = copyFrom((short) rand.nextInt());
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
            .withFieldMatch(new PiTernaryFieldMatch(ethDstAddrFieldId, ethAddr, ImmutableByteSequence.ofOnes(6)))
            .withFieldMatch(new PiTernaryFieldMatch(ethSrcAddrFieldId, ethAddr, ImmutableByteSequence.ofOnes(6)))
            .withFieldMatch(new PiTernaryFieldMatch(inPortFieldId, portValue, ImmutableByteSequence.ofOnes(2)))
            .withFieldMatch(new PiTernaryFieldMatch(ethTypeFieldId, portValue, ImmutableByteSequence.ofOnes(2)))
            .withAction(PiAction
                                .builder()
                                .withId(outActionId)
                                .withParameter(new PiActionParam(portParamId, portValue))
                                .build())
            .withPriority(1)
            .withCookie(2)
            .build();

    public TableEntryEncoderTest() throws ImmutableByteSequence.ByteSequenceTrimException {
    }

    @Test
    public void testP4InfoBrowser() throws Exception {

        P4InfoBrowser browser = PipeconfHelper.getP4InfoBrowser(defaultPipeconf);

        assertThat(browser.tables().hasName(TABLE_0), is(true));
        assertThat(browser.actions().hasName(SET_EGRESS_PORT), is(true));

        int tableId = browser.tables().getByName(TABLE_0).getPreamble().getId();
        int actionId = browser.actions().getByName(SET_EGRESS_PORT).getPreamble().getId();

        assertThat(browser.matchFields(tableId).hasName(STANDARD_METADATA + "." + INGRESS_PORT), is(true));
        assertThat(browser.actionParams(actionId).hasName(PORT), is(true));

        // TODO: improve, assert browsing other entities (counters, meters, etc.)
    }

    @Test
    public void testTableEntryEncoder()
            throws P4InfoBrowser.NotFoundException, ImmutableByteSequence.ByteSequenceTrimException {

        Collection<TableEntry> result = encode(Lists.newArrayList(piTableEntry), defaultPipeconf);
        assertThat(result, hasSize(1));

        TableEntry tableEntryMsg = result.iterator().next();

        Collection<PiTableEntry> decodedResults = decode(Lists.newArrayList(tableEntryMsg), defaultPipeconf);
        PiTableEntry decodedPiTableEntry = decodedResults.iterator().next();

        // Test equality for decoded entry.
        new EqualsTester()
                .addEqualityGroup(piTableEntry, decodedPiTableEntry)
                .testEquals();

        // Table ID.
        int p4InfoTableId = browser.tables().getByName(tableId.id()).getPreamble().getId();
        int encodedTableId = tableEntryMsg.getTableId();
        assertThat(encodedTableId, is(p4InfoTableId));

        // Ternary match.
        byte[] encodedTernaryMatchValue = tableEntryMsg.getMatch(0).getTernary().getValue().toByteArray();
        assertThat(encodedTernaryMatchValue, is(ethAddr.asArray()));

        Action actionMsg = tableEntryMsg.getAction().getAction();

        // Action ID.
        int p4InfoActionId = browser.actions().getByName(outActionId.name()).getPreamble().getId();
        int encodedActionId = actionMsg.getActionId();
        assertThat(encodedActionId, is(p4InfoActionId));

        // Action param ID.
        int p4InfoActionParamId = browser.actionParams(p4InfoActionId).getByName(portParamId.name()).getId();
        int encodedActionParamId = actionMsg.getParams(0).getParamId();
        assertThat(encodedActionParamId, is(p4InfoActionParamId));

        // Action param value.
        byte[] encodedActionParam = actionMsg.getParams(0).getValue().toByteArray();
        assertThat(encodedActionParam, is(portValue.asArray()));

        // TODO: improve, assert other field match types (ternary, LPM)
    }

//    @Test
//    public void testRuntime() throws ExecutionException, InterruptedException {
//
//        // FIXME: remove me.
//
//        P4RuntimeControllerImpl controller = new P4RuntimeControllerImpl();
//        GrpcControllerImpl grpcController = new GrpcControllerImpl();
//        controller.grpcController = grpcController;
//        GrpcControllerImpl.ENABLE_MESSAGE_LOG = true;
//        grpcController.activate();
//        DeviceId deviceId = DeviceId.deviceId("dummy:1");
//
//        ManagedChannelBuilder channelBuilder = NettyChannelBuilder
//                .forAddress("192.168.56.102", 55044)
//                .usePlaintext(true);
//
//        assert (controller.createClient(deviceId, 1, channelBuilder));
//
//        P4RuntimeClient client = controller.getClient(deviceId);
//
//        assert(client.setPipelineConfig(defaultPipeconf, PiPipeconf.ExtensionType.BMV2_JSON).get());
//
//        assert(client.initStreamChannel().get());
//
//        assert(client.dumpTable(PiTableId.of(TABLE_0), defaultPipeconf).get().size() == 0);
//
//        assert(client.writeTableEntries(Lists.newArrayList(piTableEntry), INSERT, defaultPipeconf).get());
//
//        assert(client.dumpTable(PiTableId.of(TABLE_0), defaultPipeconf).get().size() == 1);
//    }
}
