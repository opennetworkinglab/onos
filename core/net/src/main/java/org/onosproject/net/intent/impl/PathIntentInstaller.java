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

import java.util.Iterator;
import java.util.List;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.DefaultGroupId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Link;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleBatchEntry;
import org.onosproject.net.flow.FlowRuleBatchEntry.FlowRuleOperation;
import org.onosproject.net.flow.FlowRuleBatchOperation;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.IntentExtensionService;
import org.onosproject.net.intent.IntentInstaller;
import org.onosproject.net.intent.PathIntent;
import org.onosproject.net.resource.DefaultLinkResourceRequest;
import org.onosproject.net.resource.LinkResourceAllocations;
import org.onosproject.net.resource.LinkResourceRequest;
import org.onosproject.net.resource.LinkResourceService;
import org.slf4j.Logger;

import com.google.common.collect.Lists;

import static org.onosproject.net.flow.DefaultTrafficTreatment.builder;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Installer for {@link PathIntent packet path connectivity intents}.
 */
@Component(immediate = true)
public class PathIntentInstaller implements IntentInstaller<PathIntent> {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentExtensionService intentManager;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkResourceService resourceService;

    protected ApplicationId appId;

    @Activate
    public void activate() {
        appId = coreService.registerApplication("org.onosproject.net.intent");
        intentManager.registerInstaller(PathIntent.class, this);
    }

    @Deactivate
    public void deactivate() {
        intentManager.unregisterInstaller(PathIntent.class);
    }

    @Override
    public List<FlowRuleBatchOperation> install(PathIntent intent) {
        LinkResourceAllocations allocations = allocateResources(intent);

        TrafficSelector.Builder builder =
                DefaultTrafficSelector.builder(intent.selector());
        Iterator<Link> links = intent.path().links().iterator();
        ConnectPoint prev = links.next().dst();
        List<FlowRuleBatchEntry> rules = Lists.newLinkedList();
        // TODO Generate multiple batches
        while (links.hasNext()) {
            builder.matchInPort(prev.port());
            Link link = links.next();
            // if this is the last flow rule, apply the intent's treatments
            TrafficTreatment treatment =
                    (links.hasNext() ? builder() : builder(intent.treatment()))
                    .setOutput(link.src().port()).build();

            FlowRule rule = new DefaultFlowRule(link.src().deviceId(),
                    builder.build(), treatment, 123, //FIXME 123
                    appId,
                    new DefaultGroupId((short) (intent.id().fingerprint() & 0xffff)),
                    0, true);
            rules.add(new FlowRuleBatchEntry(FlowRuleOperation.ADD, rule,
                                             intent.id().fingerprint()));
            prev = link.dst();
        }
        //FIXME this should change to new api.
        return Lists.newArrayList(new FlowRuleBatchOperation(rules, null, 0));
    }

    @Override
    public List<FlowRuleBatchOperation> uninstall(PathIntent intent) {
        LinkResourceAllocations allocatedResources = resourceService.getAllocations(intent.id());
        if (allocatedResources != null) {
            resourceService.releaseResources(allocatedResources);
        }
        TrafficSelector.Builder builder =
                DefaultTrafficSelector.builder(intent.selector());
        Iterator<Link> links = intent.path().links().iterator();
        ConnectPoint prev = links.next().dst();
        List<FlowRuleBatchEntry> rules = Lists.newLinkedList();
        // TODO Generate multiple batches
        while (links.hasNext()) {
            builder.matchInPort(prev.port());
            Link link = links.next();
            // if this is the last flow rule, apply the intent's treatments
            TrafficTreatment treatment =
                    (links.hasNext() ? builder() : builder(intent.treatment()))
                            .setOutput(link.src().port()).build();
            FlowRule rule = new DefaultFlowRule(link.src().deviceId(),
                    builder.build(), treatment, 123, appId,
                    new DefaultGroupId((short) (intent.id().fingerprint() & 0xffff)),
                    0, true);
            rules.add(new FlowRuleBatchEntry(FlowRuleOperation.REMOVE, rule,
                                             intent.id().fingerprint()));
            prev = link.dst();
        }
        // FIXME this should change to new api
        return Lists.newArrayList(new FlowRuleBatchOperation(rules, null, 0));
    }

    @Override
    public List<FlowRuleBatchOperation> replace(PathIntent oldIntent, PathIntent newIntent) {
        // FIXME: implement this
        List<FlowRuleBatchOperation> batches = Lists.newArrayList();
        batches.addAll(uninstall(oldIntent));
        batches.addAll(install(newIntent));
        return batches;
    }

    /**
     * Allocate resources required for an intent.
     *
     * @param intent intent to allocate resource for
     * @return allocated resources if any are required, null otherwise
     */
    private LinkResourceAllocations allocateResources(PathIntent intent) {
        LinkResourceRequest.Builder builder =
                DefaultLinkResourceRequest.builder(intent.id(), intent.path().links());
        for (Constraint constraint : intent.constraints()) {
            builder.addConstraint(constraint);
        }
        LinkResourceRequest request = builder.build();
        return request.resources().isEmpty() ? null : resourceService.requestResources(request);
    }
}
