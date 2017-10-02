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

package org.onosproject.net.pi.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.MacAddress;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.TestApplicationId;
import org.onosproject.bmv2.model.Bmv2PipelineModelParser;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.core.GroupId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.group.DefaultGroup;
import org.onosproject.net.group.DefaultGroupBucket;
import org.onosproject.net.group.DefaultGroupDescription;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.pi.model.DefaultPiPipeconf;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiPipeconfId;
import org.onosproject.net.pi.model.PiPipelineInterpreter;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionGroup;
import org.onosproject.net.pi.runtime.PiActionGroupMember;
import org.onosproject.net.pi.runtime.PiActionGroupMemberId;
import org.onosproject.net.pi.runtime.PiActionId;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.onosproject.net.pi.runtime.PiActionParamId;
import org.onosproject.net.pi.runtime.PiActionProfileId;
import org.onosproject.net.pi.runtime.PiGroupKey;
import org.onosproject.net.pi.runtime.PiTableAction;
import org.onosproject.net.pi.runtime.PiTableEntry;
import org.onosproject.net.pi.runtime.PiTableId;
import org.onosproject.net.pi.runtime.PiTernaryFieldMatch;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.onlab.util.ImmutableByteSequence.copyFrom;
import static org.onlab.util.ImmutableByteSequence.fit;
import static org.onosproject.net.group.GroupDescription.Type.SELECT;
import static org.onosproject.net.pi.impl.MockInterpreter.*;
import static org.onosproject.net.pi.impl.PiFlowRuleTranslator.MAX_PI_PRIORITY;

/**
 * Tests for {@link PiFlowRuleTranslator}.
 */
@SuppressWarnings("ConstantConditions")
public class PiTranslatorServiceTest {

    private static final String BMV2_JSON_PATH = "/org/onosproject/net/pi/impl/default.json";
    private static final short IN_PORT_MASK = 0x01ff; // 9-bit mask
    private static final short ETH_TYPE_MASK = (short) 0xffff;
    private static final DeviceId DEVICE_ID = DeviceId.deviceId("device:dummy:1");
    private static final ApplicationId APP_ID = TestApplicationId.create("dummy");
    private static final PiTableId ECMP_TABLE_ID = PiTableId.of("ecmp");
    private static final PiActionProfileId ACT_PROF_ID = PiActionProfileId.of("ecmp_selector");
    private static final GroupId GROUP_ID = GroupId.valueOf(1);
    private static final PiActionId EGRESS_PORT_ACTION_ID = PiActionId.of("set_egress_port");
    private static final int PORT_BITWIDTH = 9;
    private static final PiActionParamId PORT_PARAM_ID = PiActionParamId.of("port");
    private static final List<GroupBucket> BUCKET_LIST = ImmutableList.of(outputBucket(1),
                                                                          outputBucket(2),
                                                                          outputBucket(3)
    );
    private static final PiGroupKey GROUP_KEY = new PiGroupKey(ECMP_TABLE_ID, ACT_PROF_ID, GROUP_ID.id());
    private static final GroupBuckets BUCKETS = new GroupBuckets(BUCKET_LIST);
    private static final GroupDescription GROUP_DESC =
            new DefaultGroupDescription(DEVICE_ID, SELECT, BUCKETS, GROUP_KEY, GROUP_ID.id(), APP_ID);
    private static final Group GROUP = new DefaultGroup(GROUP_ID, GROUP_DESC);
    private static final int DEFAULT_MEMBER_WEIGHT = 1;
    private static final int BASE_MEM_ID = 65535;
    private Collection<PiActionGroupMember> expectedMembers;

    private Random random = new Random();
    private PiPipeconf pipeconf;

    @Before
    public void setUp() throws Exception {
        pipeconf = DefaultPiPipeconf.builder()
                .withId(new PiPipeconfId("mock-pipeconf"))
                .withPipelineModel(Bmv2PipelineModelParser.parse(this.getClass().getResource(BMV2_JSON_PATH)))
                .addBehaviour(PiPipelineInterpreter.class, MockInterpreter.class)
                .build();

        expectedMembers = ImmutableSet.of(outputMember(1),
                                          outputMember(2),
                                          outputMember(3));
    }

    @Test
    public void testTranslateFlowRules() throws Exception {

        ApplicationId appId = new DefaultApplicationId(1, "test");
        int tableId = 0;
        MacAddress ethDstMac = MacAddress.valueOf(random.nextLong());
        MacAddress ethSrcMac = MacAddress.valueOf(random.nextLong());
        short ethType = (short) (0x0000FFFF & random.nextInt());
        short outPort = (short) random.nextInt(65);
        short inPort = (short) random.nextInt(65);
        int timeout = random.nextInt(100);
        int priority = random.nextInt(100);

        TrafficSelector matchInPort1 = DefaultTrafficSelector
                .builder()
                .matchInPort(PortNumber.portNumber(inPort))
                .matchEthDst(ethDstMac)
                .matchEthSrc(ethSrcMac)
                .matchEthType(ethType)
                .build();

        TrafficTreatment outPort2 = DefaultTrafficTreatment
                .builder()
                .setOutput(PortNumber.portNumber(outPort))
                .build();

        FlowRule rule1 = DefaultFlowRule.builder()
                .forDevice(DEVICE_ID)
                .forTable(tableId)
                .fromApp(appId)
                .withSelector(matchInPort1)
                .withTreatment(outPort2)
                .makeTemporary(timeout)
                .withPriority(priority)
                .build();

        FlowRule rule2 = DefaultFlowRule.builder()
                .forDevice(DEVICE_ID)
                .forTable(tableId)
                .fromApp(appId)
                .withSelector(matchInPort1)
                .withTreatment(outPort2)
                .makeTemporary(timeout)
                .withPriority(priority)
                .build();

        PiTableEntry entry1 = PiFlowRuleTranslator.translate(rule1, pipeconf, null);
        PiTableEntry entry2 = PiFlowRuleTranslator.translate(rule1, pipeconf, null);

        // check equality, i.e. same rules must produce same entries
        new EqualsTester()
                .addEqualityGroup(rule1, rule2)
                .addEqualityGroup(entry1, entry2)
                .testEquals();

        int numMatchParams = pipeconf.pipelineModel().table(TABLE0).get().matchFields().size();
        // parse values stored in entry1
        PiTernaryFieldMatch inPortParam = (PiTernaryFieldMatch) entry1.matchKey().fieldMatch(IN_PORT_ID).get();
        PiTernaryFieldMatch ethDstParam = (PiTernaryFieldMatch) entry1.matchKey().fieldMatch(ETH_DST_ID).get();
        PiTernaryFieldMatch ethSrcParam = (PiTernaryFieldMatch) entry1.matchKey().fieldMatch(ETH_SRC_ID).get();
        PiTernaryFieldMatch ethTypeParam = (PiTernaryFieldMatch) entry1.matchKey().fieldMatch(ETH_TYPE_ID).get();
        Optional<Double> expectedTimeout = pipeconf.pipelineModel().table(TABLE0).get().supportsAging()
                ? Optional.of((double) rule1.timeout()) : Optional.empty();

        // check that the number of parameters in the entry is the same as the number of table keys
        assertThat("Incorrect number of match parameters",
                   entry1.matchKey().fieldMatches().size(), is(equalTo(numMatchParams)));

        // check that values stored in entry are the same used for the flow rule
        assertThat("Incorrect inPort match param value",
                   inPortParam.value().asReadOnlyBuffer().getShort(), is(equalTo(inPort)));
        assertThat("Incorrect inPort match param mask",
                   inPortParam.mask().asReadOnlyBuffer().getShort(), is(equalTo(IN_PORT_MASK)));
        assertThat("Incorrect ethDestMac match param value",
                   ethDstParam.value().asArray(), is(equalTo(ethDstMac.toBytes())));
        assertThat("Incorrect ethDestMac match param mask",
                   ethDstParam.mask().asArray(), is(equalTo(MacAddress.BROADCAST.toBytes())));
        assertThat("Incorrect ethSrcMac match param value",
                   ethSrcParam.value().asArray(), is(equalTo(ethSrcMac.toBytes())));
        assertThat("Incorrect ethSrcMac match param mask",
                   ethSrcParam.mask().asArray(), is(equalTo(MacAddress.BROADCAST.toBytes())));
        assertThat("Incorrect ethType match param value",
                   ethTypeParam.value().asReadOnlyBuffer().getShort(), is(equalTo(ethType)));
        assertThat("Incorrect ethType match param mask",
                   ethTypeParam.mask().asReadOnlyBuffer().getShort(), is(equalTo(ETH_TYPE_MASK)));
        assertThat("Incorrect priority value",
                   entry1.priority().get(), is(equalTo(MAX_PI_PRIORITY - rule1.priority())));
        assertThat("Incorrect timeout value",
                   entry1.timeout(), is(equalTo(expectedTimeout)));

    }

    private static GroupBucket outputBucket(int portNum) {
        ImmutableByteSequence paramVal = copyFrom(portNum);
        PiActionParam param = new PiActionParam(PiActionParamId.of(PORT_PARAM_ID.name()), paramVal);
        PiTableAction action = PiAction.builder().withId(EGRESS_PORT_ACTION_ID).withParameter(param).build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .add(Instructions.piTableAction(action))
                .build();
        return DefaultGroupBucket.createSelectGroupBucket(treatment);
    }

    private static PiActionGroupMember outputMember(int portNum)
            throws ImmutableByteSequence.ByteSequenceTrimException {
        PiActionParam param = new PiActionParam(PORT_PARAM_ID, fit(copyFrom(portNum), PORT_BITWIDTH));
        PiAction piAction = PiAction.builder()
                .withId(EGRESS_PORT_ACTION_ID)
                .withParameter(param).build();
        return PiActionGroupMember.builder()
                .withAction(piAction)
                .withId(PiActionGroupMemberId.of(BASE_MEM_ID + portNum))
                .withWeight(DEFAULT_MEMBER_WEIGHT)
                .build();
    }

    /**
     * Test add group with buckets.
     */
    @Test
    public void testTranslateGroups() throws Exception {

        PiActionGroup piGroup1 = PiGroupTranslator.translate(GROUP, pipeconf, null);
        PiActionGroup piGroup2 = PiGroupTranslator.translate(GROUP, pipeconf, null);

        new EqualsTester()
                .addEqualityGroup(piGroup1, piGroup2)
                .testEquals();

        assertThat("Group ID must be equal",
                   piGroup1.id().id(), is(equalTo(GROUP_ID.id())));
        assertThat("Group type must be SELECT",
                   piGroup1.type(), is(equalTo(PiActionGroup.Type.SELECT)));
        assertThat("Action profile ID must be equal",
                   piGroup1.actionProfileId(), is(equalTo(ACT_PROF_ID)));

        // members installed
        Collection<PiActionGroupMember> members = piGroup1.members();
        assertThat("The number of group members must be equal",
                   piGroup1.members().size(), is(expectedMembers.size()));
        assertThat("Group members must be equal",
                   members.containsAll(expectedMembers) && expectedMembers.containsAll(members));
    }
}
