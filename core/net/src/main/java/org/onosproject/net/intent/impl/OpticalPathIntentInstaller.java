/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.net.intent.impl;

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
import org.onosproject.net.flow.FlowRuleBatchEntry;
import org.onosproject.net.flow.FlowRuleBatchEntry.FlowRuleOperation;
import org.onosproject.net.flow.FlowRuleBatchOperation;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.IntentExtensionService;
import org.onosproject.net.intent.IntentInstaller;
import org.onosproject.net.intent.OpticalPathIntent;
import org.onosproject.net.resource.DefaultLinkResourceRequest;
import org.onosproject.net.resource.Lambda;
import org.onosproject.net.resource.LambdaResourceAllocation;
import org.onosproject.net.resource.LinkResourceAllocations;
import org.onosproject.net.resource.LinkResourceRequest;
import org.onosproject.net.resource.LinkResourceService;
import org.onosproject.net.resource.ResourceAllocation;
import org.onosproject.net.resource.ResourceType;
import org.onosproject.net.topology.TopologyService;
import org.slf4j.Logger;

import java.util.List;

import static org.onosproject.net.flow.DefaultTrafficTreatment.builder;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Installer for {@link org.onosproject.net.intent.OpticalPathIntent optical path connectivity intents}.
 */
@Component(immediate = true)
public class OpticalPathIntentInstaller implements IntentInstaller<OpticalPathIntent> {
    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentExtensionService intentManager;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

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
        intentManager.registerInstaller(OpticalPathIntent.class, this);
    }

    @Deactivate
    public void deactivate() {
        intentManager.unregisterInstaller(OpticalPathIntent.class);
    }

    @Override
    public List<FlowRuleBatchOperation> install(OpticalPathIntent intent) {
        LinkResourceAllocations allocations = assignWavelength(intent);
        return generateRules(intent, allocations, FlowRuleOperation.ADD);
    }

    @Override
    public List<FlowRuleBatchOperation> uninstall(OpticalPathIntent intent) {
        LinkResourceAllocations allocations = resourceService.getAllocations(intent.id());
        List<FlowRuleBatchOperation> rules = generateRules(intent, allocations, FlowRuleOperation.REMOVE);
        log.info("uninstall rules: {}", rules);
        return rules;
    }

    @Override
    public List<FlowRuleBatchOperation> replace(OpticalPathIntent oldIntent,
                                                OpticalPathIntent newIntent) {
        // FIXME: implement this
        List<FlowRuleBatchOperation> batches = Lists.newArrayList();
        batches.addAll(uninstall(oldIntent));
        batches.addAll(install(newIntent));
        return batches;
    }

    private LinkResourceAllocations assignWavelength(OpticalPathIntent intent) {
        LinkResourceRequest.Builder request = DefaultLinkResourceRequest.builder(intent.id(),
                                                                                 intent.path().links())
                .addLambdaRequest();
        LinkResourceAllocations retLambda = resourceService.requestResources(request.build());
        return retLambda;
    }

    private List<FlowRuleBatchOperation> generateRules(OpticalPathIntent intent,
                                                       LinkResourceAllocations allocations,
                                                       FlowRuleOperation operation) {
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
        selectorBuilder.matchInPort(intent.src().port());

        List<FlowRuleBatchEntry> rules = Lists.newLinkedList();
        ConnectPoint prev = intent.src();

        //FIXME check for null allocations
        //TODO throw exception if the lambda was not assigned successfully
        for (Link link : intent.path().links()) {
            Lambda la = null;
            for (ResourceAllocation allocation : allocations.getResourceAllocation(link)) {
                if (allocation.type() == ResourceType.LAMBDA) {
                    la = ((LambdaResourceAllocation) allocation).lambda();
                    break;
                }
            }

            if (la == null) {
                log.info("Lambda was not assigned successfully");
                return null;
            }

            TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();
            treatmentBuilder.setOutput(link.src().port());
            treatmentBuilder.setLambda((short) la.toInt());

            FlowRule rule = new DefaultFlowRule(prev.deviceId(),
                                                selectorBuilder.build(),
                                                treatmentBuilder.build(),
                                                100,
                                                appId,
                                                100,
                                                true);

            rules.add(new FlowRuleBatchEntry(operation, rule));

            prev = link.dst();
            selectorBuilder.matchInPort(link.dst().port());
            selectorBuilder.matchOpticalSignalType(SIGNAL_TYPE); //todo
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
        rules.add(new FlowRuleBatchEntry(operation, rule));

        //FIXME change to new api
        return Lists.newArrayList(new FlowRuleBatchOperation(rules, null, 0));
    }
}
