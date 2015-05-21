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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Link;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.intent.FlowRuleIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentCompiler;
import org.onosproject.net.intent.IntentExtensionService;
import org.onosproject.net.intent.OpticalPathIntent;
import org.onosproject.net.resource.link.LinkResourceAllocations;
import org.onosproject.net.resource.link.LinkResourceService;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@Component(immediate = true)
public class OpticalPathIntentCompiler implements IntentCompiler<OpticalPathIntent> {

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentExtensionService intentManager;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkResourceService resourceService;

    private ApplicationId appId;

    @Activate
    public void activate() {
        appId = coreService.registerApplication("org.onosproject.net.intent");
        intentManager.registerCompiler(OpticalPathIntent.class, this);
    }

    @Deactivate
    public void deactivate() {
        intentManager.unregisterCompiler(OpticalPathIntent.class);
    }

    @Override
    public List<Intent> compile(OpticalPathIntent intent, List<Intent> installable,
                                Set<LinkResourceAllocations> resources) {
        return Collections.singletonList(
                new FlowRuleIntent(appId, createRules(intent), intent.resources()));
    }

    private List<FlowRule> createRules(OpticalPathIntent intent) {
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
        selectorBuilder.matchInPort(intent.src().port());

        List<FlowRule> rules = new LinkedList<>();
        ConnectPoint current = intent.src();

        for (Link link : intent.path().links()) {
            TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();
            treatmentBuilder.add(Instructions.modL0Lambda(intent.lambda()));
            treatmentBuilder.setOutput(link.src().port());

            FlowRule rule = DefaultFlowRule.builder()
                    .forDevice(current.deviceId())
                    .withSelector(selectorBuilder.build())
                    .withTreatment(treatmentBuilder.build())
                    .withPriority(100)
                    .fromApp(appId)
                    .makePermanent()
                    .build();

            rules.add(rule);

            current = link.dst();
            selectorBuilder.matchInPort(link.dst().port());
            selectorBuilder.add(Criteria.matchLambda(intent.lambda()));
        }

        // Build the egress ROADM rule
        TrafficTreatment.Builder treatmentLast = DefaultTrafficTreatment.builder();
        treatmentLast.setOutput(intent.dst().port());

        FlowRule rule = new DefaultFlowRule.Builder()
                .forDevice(intent.dst().deviceId())
                .withSelector(selectorBuilder.build())
                .withTreatment(treatmentLast.build())
                .withPriority(100)
                .fromApp(appId)
                .makePermanent()
                .build();
        rules.add(rule);

        return rules;
    }
}
