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
import org.onosproject.net.intent.PathIntent;
import org.onosproject.net.resource.link.LinkResourceAllocations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Component(immediate = true)
public class PathIntentCompiler implements IntentCompiler<PathIntent> {

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentExtensionService intentManager;

    private ApplicationId appId;

    @Activate
    public void activate() {
        appId = coreService.registerApplication("org.onosproject.net.intent");
        intentManager.registerCompiler(PathIntent.class, this);
    }

    @Deactivate
    public void deactivate() {
        intentManager.unregisterCompiler(PathIntent.class);
    }

    @Override
    public List<Intent> compile(PathIntent intent, List<Intent> installable,
                                Set<LinkResourceAllocations> resources) {
        // Note: right now recompile is not considered
        // TODO: implement recompile behavior

        List<Link> links = intent.path().links();
        List<FlowRule> rules = new ArrayList<>(links.size() - 1);

        for (int i = 0; i < links.size() - 1; i++) {
            ConnectPoint ingress = links.get(i).dst();
            ConnectPoint egress = links.get(i + 1).src();
            FlowRule rule = createFlowRule(intent.selector(), intent.treatment(),
                                           ingress, egress, intent.priority(),
                                           isLast(links, i));
            rules.add(rule);
        }

        return Collections.singletonList(new FlowRuleIntent(appId, null, rules, intent.resources()));
    }

    private FlowRule createFlowRule(TrafficSelector originalSelector, TrafficTreatment originalTreatment,
                                    ConnectPoint ingress, ConnectPoint egress,
                                    int priority, boolean last) {
        TrafficSelector selector = DefaultTrafficSelector.builder(originalSelector)
                .matchInPort(ingress.port())
                .build();

        TrafficTreatment.Builder treatmentBuilder;
        if (last) {
            treatmentBuilder = DefaultTrafficTreatment.builder(originalTreatment);
        } else {
            treatmentBuilder = DefaultTrafficTreatment.builder();
        }
        TrafficTreatment treatment = treatmentBuilder.setOutput(egress.port()).build();

        return DefaultFlowRule.builder()
                .forDevice(ingress.deviceId())
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(priority)
                .fromApp(appId)
                .makePermanent()
                .build();
    }

    private boolean isLast(List<Link> links, int i) {
        return i == links.size() - 2;
    }
}
