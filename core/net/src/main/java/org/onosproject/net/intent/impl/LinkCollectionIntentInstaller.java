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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;
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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    public List<Collection<FlowRuleOperation>> install(LinkCollectionIntent intent) {
        return generateBatchOperations(intent, FlowRuleOperation.Type.ADD);
    }

    @Override
    public List<Collection<FlowRuleOperation>> uninstall(LinkCollectionIntent intent) {
        return generateBatchOperations(intent, FlowRuleOperation.Type.REMOVE);
    }

    private List<Collection<FlowRuleOperation>> generateBatchOperations(
            LinkCollectionIntent intent, FlowRuleOperation.Type operation) {

        //TODO do we need a set here?
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

        List<FlowRuleOperation> rules = Lists.newArrayList();
        outputPorts.keys().stream()
            .map(deviceId -> createBatchEntries(operation,
                                                intent, deviceId,
                                                inputPorts.get(deviceId),
                                                outputPorts.get(deviceId)))
            .forEach(rules::addAll);

        return Lists.newArrayList(ImmutableSet.of(rules));
    }

    @Override
    public List<Collection<FlowRuleOperation>> replace(LinkCollectionIntent oldIntent,
                                                LinkCollectionIntent newIntent) {
        // FIXME: implement this in a more intelligent/less brute force way
        List<Collection<FlowRuleOperation>> batches = Lists.newArrayList();
        batches.addAll(uninstall(oldIntent));
        batches.addAll(install(newIntent));
        return batches;
    }

    /**
     * Creates a collection of FlowRuleOperation based on the provided
     * parameters.
     *
     * @param operation the FlowRuleOperation type to use
     * @param intent the link collection intent
     * @param deviceId the device ID for the flow rule
     * @param inPorts the logical input ports of the flow rule
     * @param outPorts the set of output ports for the flow rule
     * @return a collection with the new flow rule batch entries
     */
    private Collection<FlowRuleOperation> createBatchEntries(
                                FlowRuleOperation.Type operation,
                                LinkCollectionIntent intent,
                                DeviceId deviceId,
                                Set<PortNumber> inPorts,
                                Set<PortNumber> outPorts) {
        Collection<FlowRuleOperation> result = Lists.newLinkedList();
        Set<PortNumber> ingressPorts = new HashSet<PortNumber>();

        //
        // Collect all ingress ports for this device.
        // The intent treatment is applied only on those ports.
        //
        for (ConnectPoint cp : intent.ingressPoints()) {
            if (cp.deviceId().equals(deviceId)) {
                ingressPorts.add(cp.port());
            }
        }

        //
        // Create two treatments: one for setting the output ports,
        // and a second one that applies the intent treatment and sets the
        // output ports.
        // NOTE: The second one is created only if there are ingress ports.
        //
        TrafficTreatment.Builder defaultTreatmentBuilder =
            DefaultTrafficTreatment.builder();
        for (PortNumber outPort : outPorts) {
            defaultTreatmentBuilder.setOutput(outPort);
        }
        TrafficTreatment defaultTreatment = defaultTreatmentBuilder.build();
        TrafficTreatment intentTreatment = null;
        if (!ingressPorts.isEmpty()) {
            TrafficTreatment.Builder intentTreatmentBuilder =
                DefaultTrafficTreatment.builder(intent.treatment());
            for (PortNumber outPort : outPorts) {
                intentTreatmentBuilder.setOutput(outPort);
            }
            intentTreatment = intentTreatmentBuilder.build();
        }

        for (PortNumber inPort : inPorts) {
            TrafficSelector selector = DefaultTrafficSelector
                .builder(intent.selector()).matchInPort(inPort).build();
            TrafficTreatment treatment = defaultTreatment;
            if (ingressPorts.contains(inPort)) {
                // Use the intent treatment if this is ingress port
                treatment = intentTreatment;
            }
            FlowRule rule = new DefaultFlowRule(deviceId,
                selector, treatment, 123, appId,
                new DefaultGroupId((short) (intent.id().fingerprint() & 0xffff)),
                0, true);
            result.add(new FlowRuleOperation(rule, operation));
        }

        return result;
    }
}
