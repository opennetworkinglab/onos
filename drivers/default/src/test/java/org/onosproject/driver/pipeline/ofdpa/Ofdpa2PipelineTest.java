/*
 * Copyright 2019-present Open Networking Foundation
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

package org.onosproject.driver.pipeline.ofdpa;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpPrefix;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.NetTestTools;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleServiceAdapter;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;

import java.util.List;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;
import static org.onlab.junit.TestTools.assertAfter;

/**
 * Tests for Broadcom's OF-DPA v2.0 TTP.
 */
public class Ofdpa2PipelineTest {

    // Pipeliner and accumulator parameters
    private Ofdpa2Pipeline ofdpa2Pipeline;
    private static final int MAX_FWD = 2;
    private static final int MAX_BATCH = 1000;
    private static final int MAX_IDLE = 1000;
    private static final int WAIT_TIME = 50;
    // Forwarding objectives to be tested
    private static final int PRIORITY = 1000;
    private static final ApplicationId APP_ID = new DefaultApplicationId(1, "org.onosproject.test");
    private ForwardingObjective versatileFwd = DefaultForwardingObjective.builder()
            .fromApp(APP_ID)
            .withSelector(NetTestTools.emptySelector())
            .withTreatment(NetTestTools.emptyTreatment())
            .withPriority(PRIORITY)
            .withFlag(ForwardingObjective.Flag.VERSATILE)
            .add();
    private static final TrafficSelector IP_SELECTOR = DefaultTrafficSelector.builder()
            .matchEthType(Ethernet.TYPE_IPV4)
            .matchIPDst(IpPrefix.valueOf("10.0.0.0/24"))
            .build();
    private static final TrafficTreatment CTRL_TREATMENT = DefaultTrafficTreatment.builder()
            .punt()
            .build();
    private ForwardingObjective specificFwd = DefaultForwardingObjective
            .builder()
            .fromApp(APP_ID)
            .withSelector(IP_SELECTOR)
            .withTreatment(CTRL_TREATMENT)
            .withPriority(PRIORITY)
            .withFlag(ForwardingObjective.Flag.SPECIFIC)
            .add();
    private static final DeviceId DEV1 = DeviceId.deviceId("of:1");
    // Test flow rule service
    private TestFlowRuleService testFlowRuleService;

    @Before
    public void setUp() {
        ofdpa2Pipeline = new Ofdpa2Pipeline();
        ofdpa2Pipeline.setupAccumulatorForTests(MAX_FWD,
                                                MAX_BATCH,
                                                MAX_IDLE);
        ofdpa2Pipeline.deviceId = DEV1;
        testFlowRuleService = new TestFlowRuleService();
        ofdpa2Pipeline.flowRuleService = testFlowRuleService;
    }

    @Test
    public void verifyAccumulation() {
        // Versatile are not accumulated
        ofdpa2Pipeline.forward(versatileFwd);
        assertAfter(WAIT_TIME, WAIT_TIME * 2, () -> assertEquals(1, testFlowRuleService.fops.size()));
        // Specific are delayed for MAX_IDLE
        ofdpa2Pipeline.forward(specificFwd);
        assertAfter(WAIT_TIME, WAIT_TIME * 2, () -> assertEquals(1, testFlowRuleService.fops.size()));
        assertAfter(WAIT_TIME,  MAX_IDLE + WAIT_TIME, () -> assertEquals(2, testFlowRuleService.fops.size()));
    }

    // Simplified version of the FlowRuleService
    private class TestFlowRuleService extends FlowRuleServiceAdapter {

        List<FlowRuleOperations> fops = Lists.newArrayList();

        @Override
        public void apply(FlowRuleOperations ops) {
            fops.add(ops);
        }

    }

}