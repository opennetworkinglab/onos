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

import com.google.common.util.concurrent.SettableFuture;
import org.onlab.osgi.ServiceDirectory;
import org.onosproject.core.DefaultGroupId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.Pipeliner;
import org.onosproject.net.behaviour.PipelinerContext;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleOperationsContext;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
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
        throw new UnsupportedOperationException("Single table does not filter.");
    }

    @Override
    public void forward(ForwardingObjective fwd) {
        FlowRuleOperations.Builder flowBuilder = FlowRuleOperations.builder();

        if (fwd.flag() != ForwardingObjective.Flag.VERSATILE) {
            throw new UnsupportedOperationException(
                    "Only VERSATILE is supported.");
        }

        TrafficSelector selector = fwd.selector();

        FlowRule rule = new DefaultFlowRule(deviceId, selector,
                                            fwd.treatment(),
                                            fwd.priority(), fwd.appId(),
                                            new DefaultGroupId(fwd.id()),
                                            fwd.timeout(), fwd.permanent());

        switch (fwd.op()) {

            case ADD:
                flowBuilder.add(rule);
                break;
            case REMOVE:
                flowBuilder.remove(rule);
                break;
            default:
                log.warn("Unknown operation {}", fwd.op());
        }


        SettableFuture<Boolean> future = SettableFuture.create();

        flowRuleService.apply(flowBuilder.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                if (fwd.context().isPresent()) {
                    fwd.context().get().onSuccess(fwd);
                }
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                if (fwd.context().isPresent()) {
                    fwd.context().get().onError(fwd, ObjectiveError.FLOWINSTALLATIONFAILED);
                }
            }
        }));

    }

    @Override
    public void next(NextObjective nextObjective) {
        throw new UnsupportedOperationException("Single table does not next hop.");
    }

}
