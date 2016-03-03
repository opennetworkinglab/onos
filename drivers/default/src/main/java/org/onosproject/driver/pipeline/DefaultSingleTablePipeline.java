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
package org.onosproject.driver.pipeline;

import org.onlab.osgi.ServiceDirectory;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.Pipeliner;
import org.onosproject.net.behaviour.PipelinerContext;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleOperationsContext;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Simple single table pipeline abstraction.
 */
public class DefaultSingleTablePipeline extends AbstractHandlerBehaviour implements Pipeliner {

    private final Logger log = getLogger(getClass());

    private ServiceDirectory serviceDirectory;
    private FlowRuleService flowRuleService;
    private DeviceId deviceId;

    @Override
    public void init(DeviceId deviceId, PipelinerContext context) {
        this.serviceDirectory = context.directory();
        this.deviceId = deviceId;

        flowRuleService = serviceDirectory.get(FlowRuleService.class);
    }

    @Override
    public void filter(FilteringObjective filter) {

        TrafficTreatment.Builder actions;
        switch (filter.type()) {
            case PERMIT:
                actions = (filter.meta() == null) ?
                        DefaultTrafficTreatment.builder().punt() :
                        DefaultTrafficTreatment.builder(filter.meta());
                break;
            case DENY:
                actions = (filter.meta() == null) ?
                        DefaultTrafficTreatment.builder() :
                        DefaultTrafficTreatment.builder(filter.meta());
                actions.drop();
                break;
            default:
                log.warn("Unknown filter type: {}", filter.type());
                actions = DefaultTrafficTreatment.builder().drop();
        }

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();

        filter.conditions().stream().forEach(selector::add);

        if (filter.key() != null) {
            selector.add(filter.key());
        }

        FlowRule.Builder ruleBuilder = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(actions.build())
                .fromApp(filter.appId())
                .withPriority(filter.priority());

        if (filter.permanent()) {
            ruleBuilder.makePermanent();
        } else {
            ruleBuilder.makeTemporary(filter.timeout());
        }

        installObjective(ruleBuilder, filter);

    }

    @Override
    public void forward(ForwardingObjective fwd) {
        // Deal with SPECIFIC and VERSATILE in the same manner.
        TrafficSelector selector = fwd.selector();
        TrafficTreatment treatment = fwd.treatment();
        if ((fwd.treatment().deferred().size() == 0) &&
                (fwd.treatment().immediate().size() == 0) &&
                (fwd.treatment().tableTransition() == null) &&
                (!fwd.treatment().clearedDeferred())) {
            TrafficTreatment.Builder flowTreatment = DefaultTrafficTreatment.builder();
            flowTreatment.add(Instructions.createNoAction());
            treatment = flowTreatment.build();
        }

        FlowRule.Builder ruleBuilder = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector)
                .withTreatment(treatment)
                .fromApp(fwd.appId())
                .withPriority(fwd.priority());

        if (fwd.permanent()) {
            ruleBuilder.makePermanent();
        } else {
            ruleBuilder.makeTemporary(fwd.timeout());
        }

        installObjective(ruleBuilder, fwd);

    }

    private void installObjective(FlowRule.Builder ruleBuilder, Objective objective) {
        FlowRuleOperations.Builder flowBuilder = FlowRuleOperations.builder();
        switch (objective.op()) {

            case ADD:
                flowBuilder.add(ruleBuilder.build());
                break;
            case REMOVE:
                flowBuilder.remove(ruleBuilder.build());
                break;
            default:
                log.warn("Unknown operation {}", objective.op());
        }

        flowRuleService.apply(flowBuilder.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                objective.context().ifPresent(context -> context.onSuccess(objective));
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                objective.context()
                        .ifPresent(context -> context.onError(objective, ObjectiveError.FLOWINSTALLATIONFAILED));
            }
        }));
    }

    @Override
    public void next(NextObjective nextObjective) {
    }

}
