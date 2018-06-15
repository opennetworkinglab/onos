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

import com.google.common.collect.Lists;
import com.google.common.testing.EqualsTester;
import org.easymock.EasyMock;
import org.junit.Test;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.net.pi.model.DefaultPiPipeconf;
import org.onosproject.net.pi.model.PiActionId;
import org.onosproject.net.pi.model.PiActionParamId;
import org.onosproject.net.pi.model.PiMatchFieldId;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiPipeconfId;
import org.onosproject.net.pi.model.PiPipelineModel;
import org.onosproject.net.pi.model.PiTableId;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionGroupId;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.onosproject.net.pi.runtime.PiExactFieldMatch;
import org.onosproject.net.pi.runtime.PiMatchKey;
import org.onosproject.net.pi.runtime.PiTableEntry;
import org.onosproject.net.pi.runtime.PiTernaryFieldMatch;
import p4.v1.P4RuntimeOuterClass.Action;
import p4.v1.P4RuntimeOuterClass.TableEntry;

import java.net.URL;
import java.util.Collection;
import java.util.Random;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.onlab.util.ImmutableByteSequence.copyFrom;
import static org.onlab.util.ImmutableByteSequence.ofOnes;
import static org.onosproject.net.pi.model.PiPipeconf.ExtensionType.P4_INFO_TEXT;
import static org.onosproject.p4runtime.ctl.TableEntryEncoder.decode;
import static org.onosproject.p4runtime.ctl.TableEntryEncoder.encode;

/**
 * Test for P4 runtime table entry encoder.
 */
public class TableEntryEncoderTest {
    private static final String DOT = ".";
    private static final String TABLE_0 = "table0";
    private static final String TABLE_ECMP = "ecmp";
    private static final String SET_EGRESS_PORT = "set_egress_port";
    private static final String PORT = "port";
    private static final String HDR = "hdr";
    private static final String META = "meta";
    private static final String ETHERNET = "ethernet";
    private static final String DST_ADDR = "dstAddr";
    private static final String SRC_ADDR = "srcAddr";
    private static final String STANDARD_METADATA = "standard_metadata";
    private static final String ECMP_METADATA = "ecmp_metadata";
    private static final String INGRESS_PORT = "ingress_port";
    private static final String ETHER_TYPE = "etherType";
    private static final String ECMP_GROUP_ID = "ecmp_group_id";

    private final Random rand = new Random();
    private final URL p4InfoUrl = this.getClass().getResource("/test.p4info");

    private final PiPipeconf defaultPipeconf = DefaultPiPipeconf.builder()
            .withId(new PiPipeconfId("mock"))
            .withPipelineModel(EasyMock.niceMock(PiPipelineModel.class))
            .addExtension(P4_INFO_TEXT, p4InfoUrl)
            .build();

    private final P4InfoBrowser browser = PipeconfHelper.getP4InfoBrowser(defaultPipeconf);
    private final ImmutableByteSequence ethAddr = copyFrom(rand.nextInt()).fit(48);
    private final ImmutableByteSequence portValue = copyFrom((short) rand.nextInt());
    private final PiMatchFieldId ethDstAddrFieldId = PiMatchFieldId.of(HDR + DOT + ETHERNET + DOT + DST_ADDR);
    private final PiMatchFieldId ethSrcAddrFieldId = PiMatchFieldId.of(HDR + DOT + ETHERNET + DOT + SRC_ADDR);
    private final PiMatchFieldId inPortFieldId = PiMatchFieldId.of(STANDARD_METADATA + DOT + INGRESS_PORT);
    private final PiMatchFieldId ethTypeFieldId = PiMatchFieldId.of(HDR + DOT + ETHERNET + DOT + ETHER_TYPE);
    private final PiMatchFieldId ecmpGroupFieldId =
            PiMatchFieldId.of(META + DOT + ECMP_METADATA + DOT + ECMP_GROUP_ID);
    private final PiActionParamId portParamId = PiActionParamId.of(PORT);
    private final PiActionId outActionId = PiActionId.of(SET_EGRESS_PORT);
    private final PiTableId tableId = PiTableId.of(TABLE_0);
    private final PiTableId ecmpTableId = PiTableId.of(TABLE_ECMP);

    private final PiTableEntry piTableEntry = PiTableEntry
            .builder()
            .forTable(tableId)
            .withMatchKey(PiMatchKey.builder()
                                  .addFieldMatch(new PiTernaryFieldMatch(ethDstAddrFieldId, ethAddr, ofOnes(6)))
                                  .addFieldMatch(new PiTernaryFieldMatch(ethSrcAddrFieldId, ethAddr, ofOnes(6)))
                                  .addFieldMatch(new PiTernaryFieldMatch(inPortFieldId, portValue, ofOnes(2)))
                                  .addFieldMatch(new PiTernaryFieldMatch(ethTypeFieldId, portValue, ofOnes(2)))
                                  .build())
            .withAction(PiAction
                                .builder()
                                .withId(outActionId)
                                .withParameter(new PiActionParam(portParamId, portValue))
                                .build())
            .withPriority(1)
            .withCookie(2)
            .build();

    private final PiTableEntry piTableEntryWithoutAction = PiTableEntry
            .builder()
            .forTable(tableId)
            .withMatchKey(PiMatchKey.builder()
                                  .addFieldMatch(new PiTernaryFieldMatch(ethDstAddrFieldId, ethAddr, ofOnes(6)))
                                  .addFieldMatch(new PiTernaryFieldMatch(ethSrcAddrFieldId, ethAddr, ofOnes(6)))
                                  .addFieldMatch(new PiTernaryFieldMatch(inPortFieldId, portValue, ofOnes(2)))
                                  .addFieldMatch(new PiTernaryFieldMatch(ethTypeFieldId, portValue, ofOnes(2)))
                                  .build())
            .withPriority(1)
            .withCookie(2)
            .build();

    private final PiTableEntry piTableEntryWithGroupAction = PiTableEntry
            .builder()
            .forTable(ecmpTableId)
            .withMatchKey(PiMatchKey.builder()
                                  .addFieldMatch(new PiExactFieldMatch(ecmpGroupFieldId, ofOnes(1)))
                                  .build())
            .withAction(PiActionGroupId.of(1))
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

        assertThat(browser.matchFields(tableId).hasName(STANDARD_METADATA + DOT + INGRESS_PORT), is(true));
        assertThat(browser.actionParams(actionId).hasName(PORT), is(true));

        // TODO: improve, assert browsing other entities (counters, meters, etc.)
    }

    @Test
    public void testTableEntryEncoder() throws Exception {

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
        int p4InfoActionId = browser.actions().getByName(outActionId.toString()).getPreamble().getId();
        int encodedActionId = actionMsg.getActionId();
        assertThat(encodedActionId, is(p4InfoActionId));

        // Action param ID.
        int p4InfoActionParamId = browser.actionParams(p4InfoActionId).getByName(portParamId.toString()).getId();
        int encodedActionParamId = actionMsg.getParams(0).getParamId();
        assertThat(encodedActionParamId, is(p4InfoActionParamId));

        // Action param value.
        byte[] encodedActionParam = actionMsg.getParams(0).getValue().toByteArray();
        assertThat(encodedActionParam, is(portValue.asArray()));

        // TODO: improve, assert other field match types (ternary, LPM)
    }

    @Test
    public void testActopProfileGroup() throws Exception {
        Collection<TableEntry> result = encode(Lists.newArrayList(piTableEntryWithGroupAction), defaultPipeconf);
        assertThat(result, hasSize(1));

        TableEntry tableEntryMsg = result.iterator().next();

        Collection<PiTableEntry> decodedResults = decode(Lists.newArrayList(tableEntryMsg), defaultPipeconf);
        PiTableEntry decodedPiTableEntry = decodedResults.iterator().next();

        // Test equality for decoded entry.
        new EqualsTester()
                .addEqualityGroup(piTableEntryWithGroupAction, decodedPiTableEntry)
                .testEquals();

        // Table ID.
        int p4InfoTableId = browser.tables().getByName(ecmpTableId.id()).getPreamble().getId();
        int encodedTableId = tableEntryMsg.getTableId();
        assertThat(encodedTableId, is(p4InfoTableId));

        // Exact match.
        byte[] encodedTernaryMatchValue = tableEntryMsg.getMatch(0).getExact().getValue().toByteArray();
        assertThat(encodedTernaryMatchValue, is(new byte[]{(byte) 0xff}));

        // Action profile group id
        int actionProfileGroupId = tableEntryMsg.getAction().getActionProfileGroupId();
        assertThat(actionProfileGroupId, is(1));
    }

    @Test
    public void testEncodeWithNoAction() throws Exception {
        Collection<TableEntry> result = encode(Lists.newArrayList(piTableEntryWithoutAction), defaultPipeconf);
        assertThat(result, hasSize(1));

        TableEntry tableEntryMsg = result.iterator().next();

        Collection<PiTableEntry> decodedResults = decode(Lists.newArrayList(tableEntryMsg), defaultPipeconf);
        PiTableEntry decodedPiTableEntry = decodedResults.iterator().next();

        // Test equality for decoded entry.
        new EqualsTester()
                .addEqualityGroup(piTableEntryWithoutAction, decodedPiTableEntry)
                .testEquals();

        // Table ID.
        int p4InfoTableId = browser.tables().getByName(tableId.id()).getPreamble().getId();
        int encodedTableId = tableEntryMsg.getTableId();
        assertThat(encodedTableId, is(p4InfoTableId));

        // Ternary match.
        byte[] encodedTernaryMatchValue = tableEntryMsg.getMatch(0).getTernary().getValue().toByteArray();
        assertThat(encodedTernaryMatchValue, is(ethAddr.asArray()));

        // no action
        assertThat(tableEntryMsg.hasAction(), is(false));

        // TODO: improve, assert other field match types (ternary, LPM)
    }
}
