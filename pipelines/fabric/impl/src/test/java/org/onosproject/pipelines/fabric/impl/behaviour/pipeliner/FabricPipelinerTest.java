/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.pipelines.fabric.impl.behaviour.pipeliner;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ethernet;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.PiCriterion;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.onosproject.pipelines.fabric.FabricConstants;
import org.onosproject.pipelines.fabric.impl.behaviour.FabricCapabilities;

import java.io.IOException;
import java.util.Optional;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertTrue;
import static org.onosproject.pipelines.fabric.impl.behaviour.Constants.PORT_TYPE_INTERNAL;

public class FabricPipelinerTest {

    private static final ApplicationId APP_ID = TestApplicationId.create("FabricPipelinerTest");
    private static final DeviceId DEVICE_ID = DeviceId.deviceId("device:1");
    private static final int DEFAULT_FLOW_PRIORITY = 100;
    private static final long CPU_PORT = 320;
    private static final byte FWD_IPV4_ROUTING = 2;
    private static final int DEFAULT_VLAN = 4094;
    public static final byte[] ONE = new byte[]{1};
    public static final byte[] ZERO = new byte[]{0};

    private FabricPipeliner pipeliner;
    private FlowRuleService flowRuleService;

    @Before
    public void setup() throws IOException {
        FabricCapabilities capabilities = createMock(FabricCapabilities.class);
        expect(capabilities.cpuPort()).andReturn(Optional.of(CPU_PORT)).anyTimes();
        replay(capabilities);

        // Services mock
        flowRuleService = createMock(FlowRuleService.class);

        pipeliner = new FabricPipeliner(capabilities);
        pipeliner.flowRuleService = flowRuleService;
        pipeliner.appId = APP_ID;
        pipeliner.deviceId = DEVICE_ID;
    }

    @Test
    public void testInitializePipeline() {
        final Capture<FlowRule> capturedCpuIgVlanRule = newCapture(CaptureType.ALL);
        final Capture<FlowRule> capturedCpuFwdClsRule = newCapture(CaptureType.ALL);

        // ingress_port_vlan table for cpu port
        final TrafficSelector cpuIgVlanSelector = DefaultTrafficSelector.builder()
                .add(Criteria.matchInPort(PortNumber.portNumber(CPU_PORT)))
                .add(PiCriterion.builder()
                        .matchExact(FabricConstants.HDR_VLAN_IS_VALID, ZERO)
                        .build())
                .build();
        final TrafficTreatment cpuIgVlanTreatment = DefaultTrafficTreatment.builder()
                .piTableAction(PiAction.builder()
                        .withId(FabricConstants.FABRIC_INGRESS_FILTERING_PERMIT_WITH_INTERNAL_VLAN)
                        .withParameter(new PiActionParam(FabricConstants.VLAN_ID, DEFAULT_VLAN))
                        .withParameter(new PiActionParam(FabricConstants.PORT_TYPE, PORT_TYPE_INTERNAL))
                        .build())
                .build();
        final FlowRule expectedCpuIgVlanRule = DefaultFlowRule.builder()
                .withSelector(cpuIgVlanSelector)
                .withTreatment(cpuIgVlanTreatment)
                .forTable(FabricConstants.FABRIC_INGRESS_FILTERING_INGRESS_PORT_VLAN)
                .makePermanent()
                .withPriority(DEFAULT_FLOW_PRIORITY)
                .forDevice(DEVICE_ID)
                .fromApp(APP_ID)
                .build();

        final TrafficSelector cpuFwdClsSelector = DefaultTrafficSelector.builder()
                .matchInPort(PortNumber.portNumber(CPU_PORT))
                .matchPi(PiCriterion.builder()
                        .matchExact(FabricConstants.HDR_IP_ETH_TYPE, Ethernet.TYPE_IPV4)
                        .build())
                .build();
        final TrafficTreatment cpuFwdClsTreatment = DefaultTrafficTreatment.builder()
                .piTableAction(PiAction.builder()
                        .withId(FabricConstants.FABRIC_INGRESS_FILTERING_SET_FORWARDING_TYPE)
                        .withParameter(new PiActionParam(FabricConstants.FWD_TYPE, FWD_IPV4_ROUTING))
                        .build())
                .build();
        final FlowRule expectedCpuFwdClsRule = DefaultFlowRule.builder()
                .withSelector(cpuFwdClsSelector)
                .withTreatment(cpuFwdClsTreatment)
                .forTable(FabricConstants.FABRIC_INGRESS_FILTERING_FWD_CLASSIFIER)
                .makePermanent()
                .withPriority(DEFAULT_FLOW_PRIORITY)
                .forDevice(DEVICE_ID)
                .fromApp(APP_ID)
                .build();
        flowRuleService.applyFlowRules(
                capture(capturedCpuIgVlanRule),
                capture(capturedCpuFwdClsRule));

        replay(flowRuleService);
        pipeliner.initializePipeline();

        assertTrue(expectedCpuIgVlanRule.exactMatch(capturedCpuIgVlanRule.getValue()));
        assertTrue(expectedCpuFwdClsRule.exactMatch(capturedCpuFwdClsRule.getValue()));

        verify(flowRuleService);
        reset(flowRuleService);
    }
}
