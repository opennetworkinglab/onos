/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.flowanalyzer;

import org.junit.Ignore;
import org.junit.Test;

import org.onosproject.core.DefaultApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.topology.TopologyService;

import java.util.Arrays;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;


/**
 * Created by nikcheerla on 7/20/15.
 */
public class FlowAnalyzerTest {

    FlowRuleService flowRuleService = new MockFlowRuleService();
    TopologyService topologyService;
    MockLinkService linkService = new MockLinkService();

    @Test
    @Ignore("This needs to be reworked to be more robust")
    public void basic() {
        flowRuleService = new MockFlowRuleService();
        flowRuleService.applyFlowRules(genFlow("ATL-001", 110, 90));
        flowRuleService.applyFlowRules(genFlow("ATL-001", 110, 100));
        flowRuleService.applyFlowRules(genFlow("ATL-001", 110, 150));
        flowRuleService.applyFlowRules(genFlow("ATL-002", 80, 70));
        flowRuleService.applyFlowRules(genFlow("ATL-003", 120, 130));
        flowRuleService.applyFlowRules(genFlow("ATL-004", 50));
        flowRuleService.applyFlowRules(genFlow("ATL-005", 140, 10));

        linkService.addLink("H00:00:00:00:00:0660", 160, "ATL-005", 140);
        linkService.addLink("ATL-005", 10, "ATL-004", 40);
        linkService.addLink("ATL-004", 50, "ATL-002", 80);
        linkService.addLink("ATL-002", 70, "ATL-001", 110);
        linkService.addLink("ATL-001", 150, "H00:00:00:00:00:0770", 170);
        linkService.addLink("ATL-001", 90, "ATL-004", 30);
        linkService.addLink("ATL-001", 100, "ATL-003", 120);
        linkService.addLink("ATL-003", 130, "ATL-005", 20);

        topologyService = new MockTopologyService(linkService.createdGraph);

        FlowAnalyzer flowAnalyzer = new FlowAnalyzer();
        flowAnalyzer.flowRuleService = flowRuleService;
        flowAnalyzer.linkService = linkService;
        flowAnalyzer.topologyService = topologyService;

        String labels = flowAnalyzer.analysisOutput();
        String correctOutput = "Flow Rule: Device: atl-005, [IN_PORT{port=140}], [OUTPUT{port=10}]\n" +
                "Analysis: Cleared!\n" +
                "\n" +
                "Flow Rule: Device: atl-003, [IN_PORT{port=120}], [OUTPUT{port=130}]\n" +
                "Analysis: Black Hole!\n" +
                "\n" +
                "Flow Rule: Device: atl-001, [IN_PORT{port=110}], [OUTPUT{port=90}]\n" +
                "Analysis: Cycle Critical Point!\n" +
                "\n" +
                "Flow Rule: Device: atl-004, [], [OUTPUT{port=50}]\n" +
                "Analysis: Cycle!\n" +
                "\n" +
                "Flow Rule: Device: atl-001, [IN_PORT{port=110}], [OUTPUT{port=150}]\n" +
                "Analysis: Cleared!\n" +
                "\n" +
                "Flow Rule: Device: atl-001, [IN_PORT{port=110}], [OUTPUT{port=100}]\n" +
                "Analysis: Black Hole!\n" +
                "\n" +
                "Flow Rule: Device: atl-002, [IN_PORT{port=80}], [OUTPUT{port=70}]\n" +
                "Analysis: Cycle!\n";
        assertEquals("Wrong labels", new TreeSet(Arrays.asList(labels.replaceAll("\\s+", "").split("!"))),
                     new TreeSet(Arrays.asList(correctOutput.replaceAll("\\s+", "").split("!"))));
    }

    public FlowRule genFlow(String d, long inPort, long outPort) {
        DeviceId device = DeviceId.deviceId(d);
        TrafficSelector ts = DefaultTrafficSelector.builder().matchInPort(PortNumber.portNumber(inPort)).build();
        TrafficTreatment tt = DefaultTrafficTreatment.builder()
                .add(Instructions.createOutput(PortNumber.portNumber(outPort))).build();
        return DefaultFlowRule.builder()
            .forDevice(device)
            .withSelector(ts)
            .withTreatment(tt)
            .withPriority(1)
            .fromApp(new DefaultApplicationId(5000, "of"))
            .withHardTimeout(50000)
            .makePermanent()
            .build();
    }
    public FlowRule genFlow(String d, long outPort) {
        DeviceId device = DeviceId.deviceId(d);
        TrafficSelector ts = DefaultTrafficSelector.builder().build();
        TrafficTreatment tt = DefaultTrafficTreatment.builder()
                .add(Instructions.createOutput(PortNumber.portNumber(outPort))).build();
        return DefaultFlowRule.builder()
            .forDevice(device)
            .withSelector(ts)
            .withTreatment(tt)
            .withPriority(1)
            .fromApp(new DefaultApplicationId(5000, "of"))
            .withHardTimeout(50000)
            .makePermanent()
            .build();
    }

}
