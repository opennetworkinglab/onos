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

import com.google.common.collect.ImmutableList;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.FlowRuleIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentCompiler;
import org.onosproject.net.intent.PathIntent;
import org.onosproject.net.resource.ResourceService;
import org.onosproject.net.resource.impl.LabelAllocator;
import org.slf4j.Logger;

import java.util.LinkedList;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;


@Component(immediate = true)
public class PathIntentCompiler
        extends PathCompiler<FlowRule>
        implements IntentCompiler<PathIntent>,
        PathCompiler.PathCompilerCreateFlow<FlowRule> {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentConfigurableRegistrator registrator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ResourceService resourceService;

    private ApplicationId appId;

    @Activate
    public void activate() {
        appId = coreService.registerApplication("org.onosproject.net.intent");
        registrator.registerCompiler(PathIntent.class, this, false);
        labelAllocator = new LabelAllocator(resourceService);
    }

    @Deactivate
    public void deactivate() {
        registrator.unregisterCompiler(PathIntent.class, false);
    }

    @Override
    public List<Intent> compile(PathIntent intent, List<Intent> installable) {

        List<FlowRule> rules = new LinkedList<>();
        List<DeviceId> devices = new LinkedList<>();
        compile(this, intent, rules, devices);


        return ImmutableList.of(new FlowRuleIntent(appId,
                                                   intent.key(),
                                                   rules,
                                                   intent.resources(),
                                                   intent.type(),
                                                   intent.resourceGroup()
        ));
    }

    @Override
    public Logger log() {
        return log;
    }

    @Override
    public ResourceService resourceService() {
        return resourceService;
    }


    @Override
    public void createFlow(TrafficSelector originalSelector, TrafficTreatment originalTreatment,
                           ConnectPoint ingress, ConnectPoint egress,
                           int priority, boolean applyTreatment,
                           List<FlowRule> rules,
                           List<DeviceId> devices) {

        TrafficSelector selector = DefaultTrafficSelector.builder(originalSelector)
                .matchInPort(ingress.port())
                .build();

        TrafficTreatment.Builder treatmentBuilder;
        if (applyTreatment) {
            treatmentBuilder = DefaultTrafficTreatment.builder(originalTreatment);
        } else {
            treatmentBuilder = DefaultTrafficTreatment.builder();
        }
        TrafficTreatment treatment = treatmentBuilder.setOutput(egress.port()).build();

        rules.add(DefaultFlowRule.builder()
                .forDevice(ingress.deviceId())
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(priority)
                .fromApp(appId)
                .makePermanent()
                .build());

    }
}
