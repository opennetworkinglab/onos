/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.pipelines.basic;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.NextGroup;
import org.onosproject.net.behaviour.Pipeliner;
import org.onosproject.net.behaviour.PipelinerContext;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.List;

import static org.onosproject.pipelines.basic.BasicConstants.INGRESS_TABLE0_CONTROL_TABLE0;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Pipeliner implementation for basic.p4 that maps all forwarding objectives to
 * table0. All other types of objectives are not supported.
 */
public class BasicPipelinerImpl extends AbstractHandlerBehaviour implements Pipeliner {

    private final Logger log = getLogger(getClass());

    private FlowRuleService flowRuleService;
    private DeviceId deviceId;


    @Override
    public void init(DeviceId deviceId, PipelinerContext context) {
        this.deviceId = deviceId;
        this.flowRuleService = context.directory().get(FlowRuleService.class);

    }

    @Override
    public void filter(FilteringObjective obj) {
        obj.context().ifPresent(c -> c.onError(obj, ObjectiveError.UNSUPPORTED));
    }

    @Override
    public void forward(ForwardingObjective obj) {
        if (obj.treatment() == null) {
            obj.context().ifPresent(c -> c.onError(obj, ObjectiveError.UNSUPPORTED));
        }

        // Simply create an equivalent FlowRule for table 0.
        final FlowRule.Builder ruleBuilder = DefaultFlowRule.builder()
                .forTable(INGRESS_TABLE0_CONTROL_TABLE0)
                .forDevice(deviceId)
                .withSelector(obj.selector())
                .fromApp(obj.appId())
                .withPriority(obj.priority())
                .withTreatment(obj.treatment());

        if (obj.permanent()) {
            ruleBuilder.makePermanent();
        } else {
            ruleBuilder.makeTemporary(obj.timeout());
        }

        switch (obj.op()) {
            case ADD:
                flowRuleService.applyFlowRules(ruleBuilder.build());
                break;
            case REMOVE:
                flowRuleService.removeFlowRules(ruleBuilder.build());
                break;
            default:
                log.warn("Unknown operation {}", obj.op());
        }

        obj.context().ifPresent(c -> c.onSuccess(obj));
    }

    @Override
    public void next(NextObjective obj) {
        obj.context().ifPresent(c -> c.onError(obj, ObjectiveError.UNSUPPORTED));
    }

    @Override
    public void purgeAll(ApplicationId appId) {
        flowRuleService.purgeFlowRules(deviceId, appId);
    }

    @Override
    public List<String> getNextMappings(NextGroup nextGroup) {
        // We do not use nextObjectives or groups.
        return Collections.emptyList();
    }
}
