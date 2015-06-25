/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.net.intent.impl.compiler;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.FlowRuleIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentCompiler;
import org.onosproject.net.intent.IntentExtensionService;
import org.onosproject.net.intent.LinkCollectionIntent;
import org.onosproject.net.resource.link.LinkResourceAllocations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component(immediate = true)
public class LinkCollectionIntentCompiler implements IntentCompiler<LinkCollectionIntent> {

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentExtensionService intentManager;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    private ApplicationId appId;

    @Activate
    public void activate() {
        appId = coreService.registerApplication("org.onosproject.net.intent");
        intentManager.registerCompiler(LinkCollectionIntent.class, this);
    }

    @Deactivate
    public void deactivate() {
        intentManager.unregisterCompiler(LinkCollectionIntent.class);
    }

    @Override
    public List<Intent> compile(LinkCollectionIntent intent, List<Intent> installable,
                                Set<LinkResourceAllocations> resources) {
        SetMultimap<DeviceId, PortNumber> inputPorts = HashMultimap.create();
        SetMultimap<DeviceId, PortNumber> outputPorts = HashMultimap.create();

        for (Link link : intent.links()) {
            inputPorts.put(link.dst().deviceId(), link.dst().port());
            outputPorts.put(link.src().deviceId(), link.src().port());
        }

        for (ConnectPoint ingressPoint : intent.ingressPoints()) {
            inputPorts.put(ingressPoint.deviceId(), ingressPoint.port());
        }

        for (ConnectPoint egressPoint : intent.egressPoints()) {
            outputPorts.put(egressPoint.deviceId(), egressPoint.port());
        }

        List<FlowRule> rules = new ArrayList<>();
        for (DeviceId deviceId: outputPorts.keys()) {
            rules.addAll(createRules(intent, deviceId, inputPorts.get(deviceId), outputPorts.get(deviceId)));
        }
        return Collections.singletonList(new FlowRuleIntent(appId, rules, intent.resources()));
    }

    private List<FlowRule> createRules(LinkCollectionIntent intent, DeviceId deviceId,
                                       Set<PortNumber> inPorts, Set<PortNumber> outPorts) {
        Set<PortNumber> ingressPorts = intent.ingressPoints().stream()
                .filter(point -> point.deviceId().equals(deviceId))
                .map(ConnectPoint::port)
                .collect(Collectors.toSet());

        TrafficTreatment.Builder defaultTreatmentBuilder = DefaultTrafficTreatment.builder();
        outPorts.stream()
                .forEach(defaultTreatmentBuilder::setOutput);
        TrafficTreatment defaultTreatment = defaultTreatmentBuilder.build();

        TrafficTreatment.Builder ingressTreatmentBuilder = DefaultTrafficTreatment.builder(intent.treatment());
        outPorts.stream()
                .forEach(ingressTreatmentBuilder::setOutput);
        TrafficTreatment ingressTreatment = ingressTreatmentBuilder.build();

        List<FlowRule> rules = new ArrayList<>(inPorts.size());
        for (PortNumber inPort: inPorts) {
            TrafficSelector selector = DefaultTrafficSelector.builder(intent.selector()).matchInPort(inPort).build();
            TrafficTreatment treatment;
            if (ingressPorts.contains(inPort)) {
                treatment = ingressTreatment;
            } else {
                treatment = defaultTreatment;
            }

            FlowRule rule = DefaultFlowRule.builder()
                                .forDevice(deviceId)
                                .withSelector(selector)
                                .withTreatment(treatment)
                                .withPriority(intent.priority())
                                .fromApp(appId)
                                .makePermanent()
                                .build();
            rules.add(rule);
        }

        return rules;
    }
}
