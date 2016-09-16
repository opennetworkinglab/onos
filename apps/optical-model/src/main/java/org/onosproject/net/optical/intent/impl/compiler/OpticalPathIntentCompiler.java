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
package org.onosproject.net.optical.intent.impl.compiler;

import com.google.common.collect.Lists;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@Component(immediate = true)
public class OpticalPathIntentCompiler implements IntentCompiler<OpticalPathIntent> {

    private static final Logger log = LoggerFactory.getLogger(OpticalPathIntentCompiler.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentExtensionService intentManager;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

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
    public List<Intent> compile(OpticalPathIntent intent, List<Intent> installable) {
        log.debug("Compiling optical path intent between {} and {}", intent.src(), intent.dst());

        // Create rules for forward and reverse path
        List<FlowRule> rules = createRules(intent);
        if (intent.isBidirectional()) {
            rules.addAll(createReverseRules(intent));
        }

        return Collections.singletonList(new FlowRuleIntent(appId, rules, intent.resources()));
    }

    /**
     * Create rules for the forward path of the intent.
     *
     * @param intent the intent
     * @return list of flow rules
     */
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
                    .withPriority(intent.priority())
                    .fromApp(appId)
                    .makePermanent()
                    .build();

            rules.add(rule);

            current = link.dst();
            selectorBuilder.matchInPort(link.dst().port());
            selectorBuilder.add(Criteria.matchLambda(intent.lambda()));
            selectorBuilder.add(Criteria.matchOchSignalType(intent.signalType()));
        }

        // Build the egress ROADM rule
        TrafficTreatment.Builder treatmentLast = DefaultTrafficTreatment.builder();
        treatmentLast.setOutput(intent.dst().port());

        FlowRule rule = new DefaultFlowRule.Builder()
                .forDevice(intent.dst().deviceId())
                .withSelector(selectorBuilder.build())
                .withTreatment(treatmentLast.build())
                .withPriority(intent.priority())
                .fromApp(appId)
                .makePermanent()
                .build();
        rules.add(rule);

        return rules;
    }

    /**
     * Create rules for the reverse path of the intent.
     *
     * @param intent the intent
     * @return list of flow rules
     */
    private List<FlowRule> createReverseRules(OpticalPathIntent intent) {
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
        selectorBuilder.matchInPort(intent.dst().port());

        List<FlowRule> rules = new LinkedList<>();
        ConnectPoint current = intent.dst();

        for (Link link : Lists.reverse(intent.path().links())) {
            TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();
            treatmentBuilder.add(Instructions.modL0Lambda(intent.lambda()));
            treatmentBuilder.setOutput(link.dst().port());

            FlowRule rule = DefaultFlowRule.builder()
                    .forDevice(current.deviceId())
                    .withSelector(selectorBuilder.build())
                    .withTreatment(treatmentBuilder.build())
                    .withPriority(intent.priority())
                    .fromApp(appId)
                    .makePermanent()
                    .build();

            rules.add(rule);

            current = link.src();
            selectorBuilder.matchInPort(link.src().port());
            selectorBuilder.add(Criteria.matchLambda(intent.lambda()));
            selectorBuilder.add(Criteria.matchOchSignalType(intent.signalType()));
        }

        // Build the egress ROADM rule
        TrafficTreatment.Builder treatmentLast = DefaultTrafficTreatment.builder();
        treatmentLast.setOutput(intent.src().port());

        FlowRule rule = new DefaultFlowRule.Builder()
                .forDevice(intent.src().deviceId())
                .withSelector(selectorBuilder.build())
                .withTreatment(treatmentLast.build())
                .withPriority(intent.priority())
                .fromApp(appId)
                .makePermanent()
                .build();
        rules.add(rule);

        return rules;
    }
}
