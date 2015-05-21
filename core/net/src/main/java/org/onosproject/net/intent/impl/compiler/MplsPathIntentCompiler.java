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

import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
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
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthTypeCriterion;
import org.onosproject.net.intent.FlowRuleIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentCompiler;
import org.onosproject.net.intent.IntentExtensionService;
import org.onosproject.net.intent.MplsPathIntent;
import org.onosproject.net.link.LinkStore;
import org.onosproject.net.resource.link.DefaultLinkResourceRequest;
import org.onosproject.net.resource.link.LinkResourceAllocations;
import org.onosproject.net.resource.link.LinkResourceRequest;
import org.onosproject.net.resource.link.LinkResourceService;
import org.onosproject.net.resource.link.MplsLabel;
import org.onosproject.net.resource.link.MplsLabelResourceAllocation;
import org.onosproject.net.resource.ResourceAllocation;
import org.onosproject.net.resource.ResourceType;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

@Component(immediate = true)
public class MplsPathIntentCompiler implements IntentCompiler<MplsPathIntent> {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentExtensionService intentExtensionService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkResourceService resourceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkStore linkStore;

    protected ApplicationId appId;

    @Override
    public List<Intent> compile(MplsPathIntent intent, List<Intent> installable,
                                Set<LinkResourceAllocations> resources) {
        LinkResourceAllocations allocations = assignMplsLabel(intent);
        List<FlowRule> rules = generateRules(intent, allocations);

        return Collections.singletonList(new FlowRuleIntent(appId, rules, intent.resources()));
    }

    @Activate
    public void activate() {
        appId = coreService.registerApplication("org.onosproject.net.intent");
        intentExtensionService.registerCompiler(MplsPathIntent.class, this);
    }

    @Deactivate
    public void deactivate() {
        intentExtensionService.unregisterCompiler(MplsPathIntent.class);
    }

    private LinkResourceAllocations assignMplsLabel(MplsPathIntent intent) {
        // TODO: do it better... Suggestions?
        Set<Link> linkRequest = Sets.newHashSetWithExpectedSize(intent.path()
                .links().size() - 2);
        for (int i = 1; i <= intent.path().links().size() - 2; i++) {
            Link link = intent.path().links().get(i);
            linkRequest.add(link);
            // add the inverse link. I want that the label is reserved both for
            // the direct and inverse link
            linkRequest.add(linkStore.getLink(link.dst(), link.src()));
        }

        LinkResourceRequest.Builder request = DefaultLinkResourceRequest
                .builder(intent.id(), linkRequest).addMplsRequest();
        LinkResourceAllocations reqMpls = resourceService
                .requestResources(request.build());
        return reqMpls;
    }

    private MplsLabel getMplsLabel(LinkResourceAllocations allocations, Link link) {
        for (ResourceAllocation allocation : allocations
                .getResourceAllocation(link)) {
            if (allocation.type() == ResourceType.MPLS_LABEL) {
                return ((MplsLabelResourceAllocation) allocation).mplsLabel();

            }
        }
        log.warn("MPLS label was not assigned successfully");
        return null;
    }

    private List<FlowRule> generateRules(MplsPathIntent intent,
                                         LinkResourceAllocations allocations) {

        Iterator<Link> links = intent.path().links().iterator();
        Link srcLink = links.next();
        ConnectPoint prev = srcLink.dst();

        Link link = links.next();
        // List of flow rules to be installed
        List<FlowRule> rules = new LinkedList<>();

        // Ingress traffic
        // Get the new MPLS label
        MplsLabel mpls = getMplsLabel(allocations, link);
        checkNotNull(mpls);
        MplsLabel prevLabel = mpls;
        rules.add(ingressFlow(prev.port(), link, intent, mpls));

        prev = link.dst();

        while (links.hasNext()) {

            link = links.next();

            if (links.hasNext()) {
                // Transit traffic
                // Get the new MPLS label
                mpls = getMplsLabel(allocations, link);
                checkNotNull(mpls);
                rules.add(transitFlow(prev.port(), link, intent,
                        prevLabel, mpls));
                prevLabel = mpls;

            } else {
                // Egress traffic
                rules.add(egressFlow(prev.port(), link, intent,
                        prevLabel));
            }

            prev = link.dst();
        }
        return rules;
    }

    private FlowRule ingressFlow(PortNumber inPort, Link link,
                                 MplsPathIntent intent, MplsLabel label) {

        TrafficSelector.Builder ingressSelector = DefaultTrafficSelector
                .builder(intent.selector());
        TrafficTreatment.Builder treat = DefaultTrafficTreatment.builder();
        ingressSelector.matchInPort(inPort);

        if (intent.ingressLabel().isPresent()) {
            ingressSelector.matchEthType(Ethernet.MPLS_UNICAST)
                    .matchMplsLabel(intent.ingressLabel().get());

            // Swap the MPLS label
            treat.setMpls(label.label());
        } else {
            // Push and set the MPLS label
            treat.pushMpls().setMpls(label.label());
        }
        // Add the output action
        treat.setOutput(link.src().port());

        return createFlowRule(intent, link.src().deviceId(), ingressSelector.build(), treat.build());
    }

    private FlowRule transitFlow(PortNumber inPort, Link link,
                                          MplsPathIntent intent,
                                          MplsLabel prevLabel,
                                          MplsLabel outLabel) {

        // Ignore the ingress Traffic Selector and use only the MPLS label
        // assigned in the previous link
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchInPort(inPort).matchEthType(Ethernet.MPLS_UNICAST)
                .matchMplsLabel(prevLabel.label());
        TrafficTreatment.Builder treat = DefaultTrafficTreatment.builder();

        // Set the new label only if the label on the packet is
        // different
        if (prevLabel.equals(outLabel)) {
            treat.setMpls(outLabel.label());
        }

        treat.setOutput(link.src().port());
        return createFlowRule(intent, link.src().deviceId(), selector.build(), treat.build());
    }

    private FlowRule egressFlow(PortNumber inPort, Link link,
                                         MplsPathIntent intent,
                                         MplsLabel prevLabel) {
        // egress point: either set the egress MPLS label or pop the
        // MPLS label based on the intent annotations

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchInPort(inPort).matchEthType(Ethernet.MPLS_UNICAST)
                .matchMplsLabel(prevLabel.label());

        // apply the intent's treatments
        TrafficTreatment.Builder treat = DefaultTrafficTreatment.builder(intent
                .treatment());

        if (intent.egressLabel().isPresent()) {
            treat.setMpls(intent.egressLabel().get());
        } else {
            // if the ingress ethertype is defined, the egress traffic
            // will be use that value, otherwise the IPv4 ethertype is used.
            Criterion c = intent.selector().getCriterion(Criterion.Type.ETH_TYPE);
            if (c != null && c instanceof EthTypeCriterion) {
                EthTypeCriterion ethertype = (EthTypeCriterion) c;
                treat.popMpls((short) ethertype.ethType());
            } else {
                treat.popMpls(Ethernet.TYPE_IPV4);
            }

        }
        treat.setOutput(link.src().port());
        return createFlowRule(intent, link.src().deviceId(),
                selector.build(), treat.build());
    }

    protected FlowRule createFlowRule(MplsPathIntent intent, DeviceId deviceId,
                                      TrafficSelector selector, TrafficTreatment treat) {
        return new DefaultFlowRule(deviceId, selector, treat, intent.priority(), appId, 0, true);
    }
}
