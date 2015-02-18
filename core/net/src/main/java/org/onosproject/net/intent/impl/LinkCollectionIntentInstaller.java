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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.DefaultGroupId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperation;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.IntentExtensionService;
import org.onosproject.net.intent.IntentInstaller;
import org.onosproject.net.intent.LinkCollectionIntent;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;

/**
 * Installer for {@link org.onosproject.net.intent.LinkCollectionIntent} path
 * segment intents.
 */
@Component(immediate = true)
public class LinkCollectionIntentInstaller
        implements IntentInstaller<LinkCollectionIntent> {

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentExtensionService intentManager;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    private ApplicationId appId;

    @Activate
    public void activate() {
        appId = coreService.registerApplication("org.onosproject.net.intent");
        intentManager.registerInstaller(LinkCollectionIntent.class, this);
    }

    @Deactivate
    public void deactivate() {
        intentManager.unregisterInstaller(LinkCollectionIntent.class);
    }

    @Override
    public List<Set<FlowRuleOperation>> install(LinkCollectionIntent intent) {
        return generateBatchOperations(intent, FlowRuleOperation.Type.ADD);
    }

    @Override
    public List<Set<FlowRuleOperation>> uninstall(LinkCollectionIntent intent) {
        return generateBatchOperations(intent, FlowRuleOperation.Type.REMOVE);
    }

    private List<Set<FlowRuleOperation>> generateBatchOperations(
            LinkCollectionIntent intent, FlowRuleOperation.Type operation) {

        SetMultimap<DeviceId, PortNumber> outputPorts = HashMultimap.create();

        for (Link link : intent.links()) {
            outputPorts.put(link.src().deviceId(), link.src().port());
        }

        for (ConnectPoint egressPoint : intent.egressPoints()) {
            outputPorts.put(egressPoint.deviceId(), egressPoint.port());
        }

        //FIXME change to new api
        /* Fear of streams */
        /*
        Set<FlowRuleBatchEntry> rules = Sets.newHashSet();
        for (DeviceId deviceId : outputPorts.keys()) {
            rules.add(createBatchEntry(operation,
                      intent, deviceId,
                      outputPorts.get(deviceId)));
        }
        */

        Set<FlowRuleOperation> rules =
                outputPorts
                        .keys()
                        .stream()
                        .map(deviceId -> createBatchEntry(operation,
                                intent, deviceId,
                                outputPorts.get(deviceId)))
                        .collect(Collectors.toSet());

        return Lists.newArrayList(ImmutableSet.of(rules));
    }

    @Override
    public List<Set<FlowRuleOperation>> replace(LinkCollectionIntent oldIntent,
                                                LinkCollectionIntent newIntent) {
        // FIXME: implement this in a more intelligent/less brute force way
        List<Set<FlowRuleOperation>> batches = Lists.newArrayList();
        batches.addAll(uninstall(oldIntent));
        batches.addAll(install(newIntent));
        return batches;
    }

    /**
     * Creates a FlowRuleBatchEntry based on the provided parameters.
     *
     * @param operation the FlowRuleOperation to use
     * @param intent the link collection intent
     * @param deviceId the device ID for the flow rule
     * @param outPorts the set of output ports for the flow rule
     * @return the new flow rule batch entry
     */
    private FlowRuleOperation createBatchEntry(FlowRuleOperation.Type operation,
                                               LinkCollectionIntent intent,
                                               DeviceId deviceId,
                                               Set<PortNumber> outPorts) {

        TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment
                .builder(intent.treatment());

        for (PortNumber outPort : outPorts) {
            treatmentBuilder.setOutput(outPort);
        }
        TrafficTreatment treatment = treatmentBuilder.build();

        TrafficSelector selector = DefaultTrafficSelector
                .builder(intent.selector()).build();

        FlowRule rule = new DefaultFlowRule(deviceId,
                selector, treatment, 123, appId,
                new DefaultGroupId((short) (intent.id().fingerprint() & 0xffff)),
                0, true);

        return new FlowRuleOperation(rule, operation);
    }
}
