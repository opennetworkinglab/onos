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
import org.onosproject.net.intent.FlowRuleIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentCompiler;
import org.onosproject.net.intent.IntentExtensionService;
import org.onosproject.net.intent.OpticalPathIntent;
import org.onosproject.net.intent.impl.IntentCompilationException;
import org.onosproject.net.resource.DefaultLinkResourceRequest;
import org.onosproject.net.resource.LambdaResource;
import org.onosproject.net.resource.LambdaResourceAllocation;
import org.onosproject.net.resource.LinkResourceAllocations;
import org.onosproject.net.resource.LinkResourceRequest;
import org.onosproject.net.resource.LinkResourceService;
import org.onosproject.net.resource.ResourceAllocation;
import org.onosproject.net.resource.ResourceType;
import org.onosproject.net.topology.TopologyService;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.onosproject.net.flow.DefaultTrafficTreatment.builder;

@Component(immediate = true)
public class OpticalPathIntentCompiler implements IntentCompiler<OpticalPathIntent> {

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentExtensionService intentManager;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkResourceService resourceService;

    private ApplicationId appId;

    static final short SIGNAL_TYPE = (short) 1;

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
        LinkResourceAllocations allocations = assignWavelength(intent);

        return Collections.singletonList(
                new FlowRuleIntent(appId, createRules(intent, allocations), intent.resources()));
    }

    private LinkResourceAllocations assignWavelength(OpticalPathIntent intent) {
        LinkResourceRequest.Builder request = DefaultLinkResourceRequest
                .builder(intent.id(), intent.path().links())
                .addLambdaRequest();
        return resourceService.requestResources(request.build());
    }

    private List<FlowRule> createRules(OpticalPathIntent intent, LinkResourceAllocations allocations) {
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
        selectorBuilder.matchInPort(intent.src().port());

        List<FlowRule> rules = new LinkedList<>();
        ConnectPoint prev = intent.src();

        for (Link link : intent.path().links()) {
            ResourceAllocation allocation = allocations.getResourceAllocation(link).stream()
                    .filter(x -> x.type() == ResourceType.LAMBDA)
                    .findFirst()
                    .orElseThrow(() -> new IntentCompilationException("Lambda was not assigned successfully"));
            LambdaResource la = ((LambdaResourceAllocation) allocation).lambda();

            TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();
            treatmentBuilder.setLambda((short) la.toInt());
            treatmentBuilder.setOutput(link.src().port());

            FlowRule rule = new DefaultFlowRule(prev.deviceId(),
                    selectorBuilder.build(),
                    treatmentBuilder.build(),
                    100,
                    appId,
                    100,
                    true);

            rules.add(rule);

            prev = link.dst();
            selectorBuilder.matchInPort(link.dst().port());
            selectorBuilder.matchOpticalSignalType(SIGNAL_TYPE);
            selectorBuilder.matchLambda((short) la.toInt());

        }

        // build the last T port rule
        TrafficTreatment.Builder treatmentLast = builder();
        treatmentLast.setOutput(intent.dst().port());
        FlowRule rule = new DefaultFlowRule(intent.dst().deviceId(),
                selectorBuilder.build(),
                treatmentLast.build(),
                100,
                appId,
                100,
                true);
        rules.add(rule);

        return rules;
    }
}
