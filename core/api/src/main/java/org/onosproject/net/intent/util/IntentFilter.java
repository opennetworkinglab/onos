/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.net.intent.util;

import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.DefaultNextObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.intent.FlowObjectiveIntent;
import org.onosproject.net.intent.FlowRuleIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Utility to get flow entries corresponding to specified intent.
 */
public class IntentFilter {

    private final IntentService intentService;
    private final FlowRuleService flowRuleService;

    /**
     * Creates an intent filter.
     *
     * @param intentService intent service object
     * @param flowRuleService flow service object
     */
    public IntentFilter(IntentService intentService,
                        FlowRuleService flowRuleService) {
        this.intentService = intentService;
        this.flowRuleService = flowRuleService;
    }

    //checks whether the collection is empty or not.
    private boolean nonEmpty(Collection<?> c) {
        return c != null && !c.isEmpty();
    }

    /**
     * Finds all path (flow entries) corresponding to intent installables.
     *
     * @param installables set of installables
     * @return set of flow entries
     */
    public List<List<FlowEntry>> readIntentFlows(List<Intent> installables) {
        List<List<FlowEntry>> paths = new ArrayList<>();

        for (Intent installable : installables) {

            if (installable instanceof FlowRuleIntent) {
                List<FlowEntry> flowEntries =
                        getFlowEntries((FlowRuleIntent) installable);
                if (nonEmpty(flowEntries)) {
                    paths.add(flowEntries);
                }

            } else if (installable instanceof FlowObjectiveIntent) {
                List<FlowEntry> flowEntries = getFlowEntries(
                        (FlowObjectiveIntent) installable);
                if (nonEmpty(flowEntries)) {
                    paths.add(flowEntries);
                }
            }
        }
        return paths;
    }

    /**
     * Finds all flow entries created by FlowRuleIntent.
     *
     * @param intent FlowRuleIntent Object
     * @return set of flow entries created by FlowRuleIntent
     */
    private List<FlowEntry> getFlowEntries(FlowRuleIntent intent) {
        List<FlowEntry> flowEntries = new ArrayList<>();
        Collection<FlowRule> flowRules = intent.flowRules();
        FlowEntry flowEntry;

        for (FlowRule flowRule : flowRules) {
            flowEntry = getFlowEntry(flowRule);

            if (flowEntry != null) {
                flowEntries.add(flowEntry);
            }
        }
        return flowEntries;
    }

    /**
     * Finds all flow entries created by FlowObjectiveIntent.
     *
     * @param intent FlowObjectiveIntent Object
     * @return set of flow entries created by FlowObjectiveIntent
     */
    private List<FlowEntry> getFlowEntries(FlowObjectiveIntent intent) {
        List<FlowEntry> flowEntries = new ArrayList<>();
        Iterator<Objective> objectives = intent.objectives().iterator();
        Iterator<DeviceId> devices = intent.devices().iterator();
        DefaultNextObjective nextObjective = null;
        DefaultForwardingObjective forwardObjective;
        Objective objective;
        DeviceId deviceId;
        FlowEntry flowEntry;

        while (objectives.hasNext()) {
            objective = objectives.next();
            deviceId = devices.next();

            if (objective instanceof NextObjective) {
                nextObjective = (DefaultNextObjective) objective;
            } else if (objective instanceof ForwardingObjective) {
                forwardObjective = (DefaultForwardingObjective) objective;
                FlowRule.Builder builder = DefaultFlowRule.builder()
                        .forDevice(deviceId)
                        .withSelector(forwardObjective.selector())
                        .withPriority(intent.priority())
                        .fromApp(intent.appId())
                        .makePermanent();
                if (nextObjective != null) {
                    builder.withTreatment(nextObjective.next().iterator().next());
                }
                FlowRule flowRule = builder.build();
                flowEntry = getFlowEntry(flowRule);

                if (flowEntry != null) {
                    flowEntries.add(flowEntry);
                }
            }
        }
        return flowEntries;
    }

    /**
     * Finds FlowEntry matching with the FlowRule.
     *
     * @param flowRule FlowRule object
     * @return flow entry matching to FlowRule
     */
    private FlowEntry getFlowEntry(FlowRule flowRule) {
        Iterable<FlowEntry> flowEntries =
                flowRuleService.getFlowEntries(flowRule.deviceId());

        if (flowEntries != null) {
            for (FlowEntry entry : flowEntries) {
                if (entry.exactMatch(flowRule)) {
                    return entry;
                }
            }
        }
        return null;
    }

}
