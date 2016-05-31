/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.bmv2.ctl;

import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onlab.packet.MacAddress;
import org.onosproject.bmv2.api.context.Bmv2Configuration;
import org.onosproject.bmv2.api.context.Bmv2DeviceContext;
import org.onosproject.bmv2.api.context.Bmv2FlowRuleTranslator;
import org.onosproject.bmv2.api.context.Bmv2Interpreter;
import org.onosproject.bmv2.api.runtime.Bmv2TableEntry;
import org.onosproject.bmv2.api.runtime.Bmv2TernaryMatchParam;
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

import java.util.Random;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.onosproject.bmv2.ctl.Bmv2DefaultInterpreterImpl.TABLE0;

/**
 * Tests for {@link Bmv2FlowRuleTranslatorImpl}.
 */
public class Bmv2FlowRuleTranslatorImplTest {

    private Random random = new Random();
    private Bmv2Configuration configuration = Bmv2DeviceContextServiceImpl.loadDefaultConfiguration();
    private Bmv2Interpreter interpreter = new Bmv2DefaultInterpreterImpl();
    private Bmv2DeviceContext context = new Bmv2DeviceContext(configuration, interpreter);
    private Bmv2FlowRuleTranslator translator = new Bmv2FlowRuleTranslatorImpl();

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

        Bmv2TableEntry entry1 = translator.translate(rule1, context);
        Bmv2TableEntry entry2 = translator.translate(rule1, context);

        // check equality, i.e. same rules must produce same entries
        new EqualsTester()
                .addEqualityGroup(rule1, rule2)
                .addEqualityGroup(entry1, entry2)
                .testEquals();

        int numMatchParams = configuration.table(TABLE0).keys().size();
        // parse values stored in entry1
        Bmv2TernaryMatchParam inPortParam = (Bmv2TernaryMatchParam) entry1.matchKey().matchParams().get(0);
        Bmv2TernaryMatchParam ethDstParam = (Bmv2TernaryMatchParam) entry1.matchKey().matchParams().get(1);
        Bmv2TernaryMatchParam ethSrcParam = (Bmv2TernaryMatchParam) entry1.matchKey().matchParams().get(2);
        Bmv2TernaryMatchParam ethTypeParam = (Bmv2TernaryMatchParam) entry1.matchKey().matchParams().get(3);
        double expectedTimeout = (double) (configuration.table(TABLE0).hasTimeouts() ? rule1.timeout() : -1);

        // check that the number of parameters in the entry is the same as the number of table keys
        assertThat("Incorrect number of match parameters",
                   entry1.matchKey().matchParams().size(), is(equalTo(numMatchParams)));

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
                   entry1.priority(), is(equalTo(Integer.MAX_VALUE - rule1.priority())));
        assertThat("Incorrect timeout value",
                   entry1.timeout(), is(equalTo(expectedTimeout)));

    }
}