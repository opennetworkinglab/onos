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
package org.onlab.onos.net.intent.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.onos.core.ApplicationId;
import org.onlab.onos.core.CoreService;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.flow.DefaultFlowRule;
import org.onlab.onos.net.flow.DefaultTrafficSelector;
import org.onlab.onos.net.flow.DefaultTrafficTreatment;
import org.onlab.onos.net.flow.FlowRule;
import org.onlab.onos.net.flow.FlowRuleBatchEntry;
import org.onlab.onos.net.flow.FlowRuleBatchEntry.FlowRuleOperation;
import org.onlab.onos.net.flow.FlowRuleBatchOperation;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;
import org.onlab.onos.net.intent.IntentExtensionService;
import org.onlab.onos.net.intent.IntentInstaller;
import org.onlab.onos.net.intent.LinkCollectionIntent;
import org.onlab.onos.net.intent.PathIntent;
import org.slf4j.Logger;

import com.google.common.collect.Lists;

/**
 * Installer for {@link org.onlab.onos.net.intent.LinkCollectionIntent}
 * path segment intents.
 */
@Component(immediate = true)
public class LinkCollectionIntentInstaller implements IntentInstaller<LinkCollectionIntent> {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentExtensionService intentManager;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    private ApplicationId appId;

    @Activate
    public void activate() {
        appId = coreService.registerApplication("org.onlab.onos.net.intent");
        intentManager.registerInstaller(LinkCollectionIntent.class, this);
    }

    @Deactivate
    public void deactivate() {
        intentManager.unregisterInstaller(PathIntent.class);
    }

    @Override
    public List<FlowRuleBatchOperation> install(LinkCollectionIntent intent) {
        List<FlowRuleBatchEntry> rules = Lists.newLinkedList();
        for (Link link : intent.links()) {
            rules.add(createBatchEntry(FlowRuleOperation.ADD,
                   intent,
                   link.src().deviceId(),
                   link.src().port()));
        }

        rules.add(createBatchEntry(FlowRuleOperation.ADD,
                intent,
                intent.egressPoint().deviceId(),
                intent.egressPoint().port()));

        return Lists.newArrayList(new FlowRuleBatchOperation(rules));
    }

    @Override
    public List<FlowRuleBatchOperation> uninstall(LinkCollectionIntent intent) {
        List<FlowRuleBatchEntry> rules = Lists.newLinkedList();

        for (Link link : intent.links()) {
            rules.add(createBatchEntry(FlowRuleOperation.REMOVE,
                    intent,
                    link.src().deviceId(),
                    link.src().port()));
        }

        rules.add(createBatchEntry(FlowRuleOperation.REMOVE,
               intent,
               intent.egressPoint().deviceId(),
               intent.egressPoint().port()));

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
                                    PortNumber outPort) {

        TrafficTreatment.Builder treatmentBuilder =
                DefaultTrafficTreatment.builder(intent.treatment());

        TrafficTreatment treatment = treatmentBuilder.setOutput(outPort).build();

        TrafficSelector selector = DefaultTrafficSelector.builder(intent.selector())
                                   .build();

        FlowRule rule = new DefaultFlowRule(deviceId,
                selector, treatment, 123, appId, 0, true);

        return new FlowRuleBatchEntry(operation, rule);
    }
}
