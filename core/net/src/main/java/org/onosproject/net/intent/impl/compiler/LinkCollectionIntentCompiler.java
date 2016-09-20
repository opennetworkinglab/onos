/*
 * Copyright 2015-present Open Networking Laboratory
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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.intent.FlowRuleIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentCompiler;
import org.onosproject.net.intent.LinkCollectionIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Compiler to produce flow rules from link collections.
 */
@Component(immediate = true)
public class LinkCollectionIntentCompiler
        extends LinkCollectionCompiler<FlowRule>
        implements IntentCompiler<LinkCollectionIntent> {

    private static Logger log = LoggerFactory.getLogger(LinkCollectionIntentCompiler.class);


    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentConfigurableRegistrator registrator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    private ApplicationId appId;

    @Activate
    public void activate() {
        appId = coreService.registerApplication("org.onosproject.net.intent");
        registrator.registerCompiler(LinkCollectionIntent.class, this, false);
    }

    @Deactivate
    public void deactivate() {
        registrator.unregisterCompiler(LinkCollectionIntent.class, false);
    }

    @Override
    public List<Intent> compile(LinkCollectionIntent intent, List<Intent> installable) {

        SetMultimap<DeviceId, PortNumber> inputPorts = HashMultimap.create();
        SetMultimap<DeviceId, PortNumber> outputPorts = HashMultimap.create();

        computePorts(intent, inputPorts, outputPorts);

        List<FlowRule> rules = new ArrayList<>();
        for (DeviceId deviceId: outputPorts.keySet()) {
            rules.addAll(createRules(intent, deviceId, inputPorts.get(deviceId), outputPorts.get(deviceId)));
        }
        return Collections.singletonList(new FlowRuleIntent(appId, rules, intent.resources()));
    }

    @Override
    protected List<FlowRule> createRules(LinkCollectionIntent intent, DeviceId deviceId,
                                       Set<PortNumber> inPorts, Set<PortNumber> outPorts) {

        Set<PortNumber> ingressPorts = Sets.newHashSet();
        Set<PortNumber> egressPorts = Sets.newHashSet();

        computePorts(intent, deviceId, ingressPorts, egressPorts);

        List<FlowRule> rules = new ArrayList<>(inPorts.size());
        Set<PortNumber> copyIngressPorts = ImmutableSet.copyOf(ingressPorts);
        Set<PortNumber> copyEgressPorts = ImmutableSet.copyOf(egressPorts);

        inPorts.forEach(inport -> {
                ForwardingInstructions instructions = this.createForwardingInstructions(intent,
                                                                                        inport,
                                                                                        deviceId,
                                                                                        outPorts,
                                                                                        copyIngressPorts,
                                                                                        copyEgressPorts);
                FlowRule rule = DefaultFlowRule.builder()
                        .forDevice(deviceId)
                        .withSelector(instructions.selector())
                        .withTreatment(instructions.treatment())
                        .withPriority(intent.priority())
                        .fromApp(appId)
                        .makePermanent()
                        .build();
                rules.add(rule);
            }
        );

        return rules;
    }
}
