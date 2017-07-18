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

package org.onosproject.net.pi.impl;

import com.google.common.testing.EqualsTester;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.MacAddress;
import org.onosproject.bmv2.model.Bmv2PipelineModelParser;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.pi.model.DefaultPiPipeconf;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiPipeconfId;
import org.onosproject.net.pi.model.PiPipelineInterpreter;
import org.onosproject.net.pi.runtime.PiTableEntry;
import org.onosproject.net.pi.runtime.PiTernaryFieldMatch;

import java.util.Optional;
import java.util.Random;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.onosproject.net.pi.impl.MockInterpreter.*;

/**
 * Tests for {@link PiFlowRuleTranslator}.
 */
@SuppressWarnings("ConstantConditions")
public class PiFlowRuleTranslatorTest {

    private static final String BMV2_JSON_PATH = "/org/onosproject/net/pi/impl/default.json";

    private Random random = new Random();
    private PiPipeconf pipeconf;

    @Before
    public void setUp() throws Exception {
        pipeconf = DefaultPiPipeconf.builder()
                .withId(new PiPipeconfId("mock-pipeconf"))
                .withPipelineModel(Bmv2PipelineModelParser.parse(this.getClass().getResource(BMV2_JSON_PATH)))
                .addBehaviour(PiPipelineInterpreter.class, MockInterpreter.class)
                .build();
    }

    @Test
    public void testTranslate() throws Exception {

        DeviceId deviceId = DeviceId.NONE;
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
                .forDevice(deviceId)
                .forTable(tableId)
                .fromApp(appId)
                .withSelector(matchInPort1)
                .withTreatment(outPort2)
                .makeTemporary(timeout)
                .withPriority(priority)
                .build();

        FlowRule rule2 = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .forTable(tableId)
                .fromApp(appId)
                .withSelector(matchInPort1)
                .withTreatment(outPort2)
                .makeTemporary(timeout)
                .withPriority(priority)
                .build();

        PiTableEntry entry1 = PiFlowRuleTranslator.translateFlowRule(rule1, pipeconf, null);
        PiTableEntry entry2 = PiFlowRuleTranslator.translateFlowRule(rule1, pipeconf, null);

        // check equality, i.e. same rules must produce same entries
        new EqualsTester()
                .addEqualityGroup(rule1, rule2)
                .addEqualityGroup(entry1, entry2)
                .testEquals();

        int numMatchParams = pipeconf.pipelineModel().table(TABLE0).get().matchFields().size();
        // parse values stored in entry1
        PiTernaryFieldMatch inPortParam = (PiTernaryFieldMatch) entry1.fieldMatch(IN_PORT_ID).get();
        PiTernaryFieldMatch ethDstParam = (PiTernaryFieldMatch) entry1.fieldMatch(ETH_DST_ID).get();
        PiTernaryFieldMatch ethSrcParam = (PiTernaryFieldMatch) entry1.fieldMatch(ETH_SRC_ID).get();
        PiTernaryFieldMatch ethTypeParam = (PiTernaryFieldMatch) entry1.fieldMatch(ETH_TYPE_ID).get();
        Optional<Double> expectedTimeout = pipeconf.pipelineModel().table(TABLE0).get().supportsAging()
                ? Optional.of((double) rule1.timeout()) : Optional.empty();

        // check that the number of parameters in the entry is the same as the number of table keys
        assertThat("Incorrect number of match parameters",
                   entry1.fieldMatches().size(), is(equalTo(numMatchParams)));

        // check that values stored in entry are the same used for the flow rule
        assertThat("Incorrect inPort match param value",
                   inPortParam.value().asReadOnlyBuffer().getShort(), is(equalTo(inPort)));
        assertThat("Incorrect ethDestMac match param value",
                   ethDstParam.value().asArray(), is(equalTo(ethDstMac.toBytes())));
        assertThat("Incorrect ethSrcMac match param value",
                   ethSrcParam.value().asArray(), is(equalTo(ethSrcMac.toBytes())));
        assertThat("Incorrect ethType match param value",
                   ethTypeParam.value().asReadOnlyBuffer().getShort(), is(equalTo(ethType)));
        assertThat("Incorrect priority value",
                   entry1.priority().get(), is(equalTo(rule1.priority())));
        assertThat("Incorrect timeout value",
                   entry1.timeout(), is(equalTo(expectedTimeout)));

    }
}
