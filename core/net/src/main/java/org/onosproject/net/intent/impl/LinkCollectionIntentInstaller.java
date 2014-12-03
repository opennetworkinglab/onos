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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
import org.onosproject.net.flow.FlowRuleBatchEntry;
import org.onosproject.net.flow.FlowRuleBatchEntry.FlowRuleOperation;
import org.onosproject.net.flow.FlowRuleBatchOperation;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.IntentExtensionService;
import org.onosproject.net.intent.IntentInstaller;
import org.onosproject.net.intent.LinkCollectionIntent;
import org.onosproject.net.intent.PathIntent;

import com.google.common.collect.Lists;

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
        intentManager.unregisterInstaller(PathIntent.class);
    }

    @Override
    public List<FlowRuleBatchOperation> install(LinkCollectionIntent intent) {
        Map<DeviceId, Set<PortNumber>> outputMap = new HashMap<DeviceId, Set<PortNumber>>();
        List<FlowRuleBatchEntry> rules = Lists.newLinkedList();

        for (Link link : intent.links()) {
            if (outputMap.get(link.src().deviceId()) == null) {
                outputMap.put(link.src().deviceId(), new HashSet<PortNumber>());
            }
            outputMap.get(link.src().deviceId()).add(link.src().port());

        }

        for (ConnectPoint egressPoint : intent.egressPoints()) {
            if (outputMap.get(egressPoint.deviceId()) == null) {
                outputMap
                        .put(egressPoint.deviceId(), new HashSet<PortNumber>());
            }
            outputMap.get(egressPoint.deviceId()).add(egressPoint.port());

        }

        for (Entry<DeviceId, Set<PortNumber>> entry : outputMap.entrySet()) {
            rules.add(createBatchEntry(FlowRuleOperation.ADD, intent,
                                       entry.getKey(), entry.getValue()));
        }

        return Lists.newArrayList(new FlowRuleBatchOperation(rules));
    }

    @Override
    public List<FlowRuleBatchOperation> uninstall(LinkCollectionIntent intent) {
        Map<DeviceId, Set<PortNumber>> outputMap = new HashMap<DeviceId, Set<PortNumber>>();
        List<FlowRuleBatchEntry> rules = Lists.newLinkedList();

        for (Link link : intent.links()) {
            if (outputMap.get(link.src().deviceId()) == null) {
                outputMap.put(link.src().deviceId(), new HashSet<PortNumber>());
            }
            outputMap.get(link.src().deviceId()).add(link.src().port());
        }

        for (ConnectPoint egressPoint : intent.egressPoints()) {
            if (outputMap.get(egressPoint.deviceId()) == null) {
                outputMap
                        .put(egressPoint.deviceId(), new HashSet<PortNumber>());
            }
            outputMap.get(egressPoint.deviceId()).add(egressPoint.port());
        }

        for (Entry<DeviceId, Set<PortNumber>> entry : outputMap.entrySet()) {
            rules.add(createBatchEntry(FlowRuleOperation.REMOVE, intent,
                                       entry.getKey(), entry.getValue()));
        }

        return Lists.newArrayList(new FlowRuleBatchOperation(rules));
    }

    @Override
    public List<FlowRuleBatchOperation> replace(LinkCollectionIntent intent,
                                                LinkCollectionIntent newIntent) {
        // FIXME: implement
        return null;
    }

    /**
     * Creates a FlowRuleBatchEntry based on the provided parameters.
     *
     * @param operation the FlowRuleOperation to use
     * @param intent the link collection intent
     * @param deviceId the device ID for the flow rule
     * @param outPort the output port of the flow rule
     * @return the new flow rule batch entry
     */
    private FlowRuleBatchEntry createBatchEntry(FlowRuleOperation operation,
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
                selector, treatment, 123,
                appId, (short) (intent.id().fingerprint() &  0xffff), 0, true);

        return new FlowRuleBatchEntry(operation, rule);
    }
}
