/*
 * Copyright 2018-present Open Networking Foundation
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
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.runtime.PiMatchKey;
import org.onosproject.net.pi.runtime.PiTableEntry;
import org.onosproject.net.pi.runtime.PiTernaryFieldMatch;
import org.onosproject.pipelines.basic.PipeconfLoader;

import java.util.Optional;
import java.util.Random;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.onosproject.pipelines.basic.BasicConstants.HDR_HDR_ETHERNET_DST_ADDR;
import static org.onosproject.pipelines.basic.BasicConstants.HDR_HDR_ETHERNET_ETHER_TYPE;
import static org.onosproject.pipelines.basic.BasicConstants.HDR_HDR_ETHERNET_SRC_ADDR;
import static org.onosproject.pipelines.basic.BasicConstants.HDR_STANDARD_METADATA_INGRESS_PORT;
import static org.onosproject.pipelines.basic.BasicConstants.INGRESS_TABLE0_CONTROL_TABLE0;

/**
 * Test for {@link PiFlowRuleTranslatorImpl}.
 */
@SuppressWarnings("ConstantConditions")
public class PiFlowRuleTranslatorImplTest {
    private static final short IN_PORT_MASK = 0x01ff; // 9-bit mask
    private static final short ETH_TYPE_MASK = (short) 0xffff;
    private static final DeviceId DEVICE_ID = DeviceId.deviceId("device:dummy:1");

    private Random random = new Random();
    private PiPipeconf pipeconf;

    @Before
    public void setUp() {
        pipeconf = PipeconfLoader.BASIC_PIPECONF;
    }

    @Test
    public void testTranslateFlowRules() throws Exception {

        ApplicationId appId = new DefaultApplicationId(1, "test");
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

        TrafficSelector emptySelector = DefaultTrafficSelector
                .builder().build();

        TrafficTreatment outPort2 = DefaultTrafficTreatment
                .builder()
                .setOutput(PortNumber.portNumber(outPort))
                .build();

        FlowRule rule1 = DefaultFlowRule.builder()
                .forDevice(DEVICE_ID)
                .forTable(INGRESS_TABLE0_CONTROL_TABLE0)
                .fromApp(appId)
                .withSelector(matchInPort1)
                .withTreatment(outPort2)
                .makeTemporary(timeout)
                .withPriority(priority)
                .build();

        FlowRule rule2 = DefaultFlowRule.builder()
                .forDevice(DEVICE_ID)
                .forTable(INGRESS_TABLE0_CONTROL_TABLE0)
                .fromApp(appId)
                .withSelector(matchInPort1)
                .withTreatment(outPort2)
                .makeTemporary(timeout)
                .withPriority(priority)
                .build();

        FlowRule defActionRule = DefaultFlowRule.builder()
                .forDevice(DEVICE_ID)
                .forTable(INGRESS_TABLE0_CONTROL_TABLE0)
                .fromApp(appId)
                .withSelector(emptySelector)
                .withTreatment(outPort2)
                .makeTemporary(timeout)
                .withPriority(priority)
                .build();

        PiTableEntry entry1 = PiFlowRuleTranslatorImpl.translate(rule1, pipeconf, null);
        PiTableEntry entry2 = PiFlowRuleTranslatorImpl.translate(rule2, pipeconf, null);
        PiTableEntry defActionEntry = PiFlowRuleTranslatorImpl.translate(defActionRule, pipeconf, null);

        // check equality, i.e. same rules must produce same entries
        new EqualsTester()
                .addEqualityGroup(rule1, rule2)
                .addEqualityGroup(entry1, entry2)
                .testEquals();

        // parse values stored in entry1
        PiTernaryFieldMatch inPortParam = (PiTernaryFieldMatch) entry1.matchKey()
                .fieldMatch(HDR_STANDARD_METADATA_INGRESS_PORT).get();
        PiTernaryFieldMatch ethDstParam = (PiTernaryFieldMatch) entry1.matchKey()
                .fieldMatch(HDR_HDR_ETHERNET_DST_ADDR).get();
        PiTernaryFieldMatch ethSrcParam = (PiTernaryFieldMatch) entry1.matchKey()
                .fieldMatch(HDR_HDR_ETHERNET_SRC_ADDR).get();
        PiTernaryFieldMatch ethTypeParam = (PiTernaryFieldMatch) entry1.matchKey()
                .fieldMatch(HDR_HDR_ETHERNET_ETHER_TYPE).get();
        Optional<Double> expectedTimeout = pipeconf.pipelineModel()
                .table(INGRESS_TABLE0_CONTROL_TABLE0).get().supportsAging()
                ? Optional.of((double) rule1.timeout()) : Optional.empty();

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
        // FIXME: re-enable when P4Runtime priority handling will be moved out of transltion service
        // see PiFlowRuleTranslatorImpl
        // assertThat("Incorrect priority value",
        //            entry1.priority().get(), is(equalTo(MAX_PI_PRIORITY - rule1.priority())));
        assertThat("Incorrect timeout value",
                   entry1.timeout(), is(equalTo(expectedTimeout)));
        assertThat("Match key should be empty",
                   defActionEntry.matchKey(), is(equalTo(PiMatchKey.EMPTY)));
        assertThat("Priority should not be set", !defActionEntry.priority().isPresent());
    }
}
