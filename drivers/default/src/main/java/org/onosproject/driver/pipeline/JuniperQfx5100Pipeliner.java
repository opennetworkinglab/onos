/*
 * Copyright 2017-present Open Networking Foundation
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

import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import java.util.Optional;


import com.google.common.collect.ImmutableList;
import org.onlab.osgi.ServiceDirectory;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Device;
import org.onosproject.net.behaviour.NextGroup;
import org.onosproject.net.behaviour.Pipeliner;
import org.onosproject.net.behaviour.PipelinerContext;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleOperationsContext;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.slf4j.Logger;


/**
 * Juniper QFX5100 Series Switch single table pipeline abstraction.
 */
public class JuniperQfx5100Pipeliner extends DefaultSingleTablePipeline implements Pipeliner {

    private final Logger log = getLogger(getClass());

    //Juniper Switch Default flow table starts from 1
    private static final int DEFAULT_TABLE = 1;

    private ServiceDirectory serviceDirectory;
    private DeviceId deviceId;
    private FlowRuleService flowRuleService;
    protected DeviceService deviceService;

    @Override
    public void init(DeviceId deviceId, PipelinerContext context) {
        super.init(deviceId, context);
        this.deviceId = deviceId;
        this.serviceDirectory = context.directory();
        this.flowRuleService = serviceDirectory.get(FlowRuleService.class);
        deviceService = serviceDirectory.get(DeviceService.class);
    }

    @Override
    public void filter(FilteringObjective filterObjective) {
        //Do nothing
        log.debug("No action is needed here");
    }

    @Override
    public void forward(ForwardingObjective forwardObjective) {
        FlowRule rule;
        FlowRuleOperations.Builder flowOpsBuilder = FlowRuleOperations.builder();

        ForwardingObjective newFwd = forwardObjective;
        Device device = deviceService.getDevice(deviceId);

        if (forwardObjective.treatment() != null && forwardObjective.treatment().clearedDeferred()) {
            log.warn("Using 'clear actions' instruction which is not supported by {}  {} {} Switch",
                    device.id(), device.manufacturer(), device.hwVersion());
            newFwd = forwardingObjectiveWithoutCleardDef(forwardObjective).orElse(forwardObjective);
        }

        rule = processForward(newFwd);
        switch (forwardObjective.op()) {
            case ADD:
                flowOpsBuilder.add(rule);
                break;
            case REMOVE:
                flowOpsBuilder.remove(rule);
                break;
            default:
                fail(forwardObjective, ObjectiveError.UNKNOWN);
                log.warn("Unknown forwarding type {}", forwardObjective.op());
        }

        flowRuleService.apply(flowOpsBuilder.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                pass(forwardObjective);
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                fail(forwardObjective, ObjectiveError.FLOWINSTALLATIONFAILED);
            }
        }));
    }

    private Optional<ForwardingObjective> forwardingObjectiveWithoutCleardDef(ForwardingObjective forwardingObjective) {

        TrafficTreatment treatment = trafficTreatmentWithoutCleardDeffered(forwardingObjective.treatment());

        DefaultForwardingObjective.Builder foBuilder = (DefaultForwardingObjective.Builder) forwardingObjective.copy();
        foBuilder.withTreatment(treatment);


        switch (forwardingObjective.op()) {
            case ADD:
                return Optional.of(foBuilder.add(forwardingObjective.context().orElse(null)));
            case REMOVE:
                return Optional.of(foBuilder.remove(forwardingObjective.context().orElse(null)));
            default:
                log.warn("Driver Not support other operations for forwarding objective");
                return Optional.empty();
        }
    }

    private TrafficTreatment trafficTreatmentWithoutCleardDeffered(TrafficTreatment treatment) {
        return DefaultTrafficTreatment.builder(treatment)
                .notWipeDeferred()
                .build();
    }

    private FlowRule processForward(ForwardingObjective fwd) {

        FlowRule.Builder ruleBuilder = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(fwd.selector())
                .withTreatment(fwd.treatment())
                .withPriority(fwd.priority())
                .fromApp(fwd.appId())
                .forTable(DEFAULT_TABLE);
        if (fwd.permanent()) {
            ruleBuilder.makePermanent();
        } else {
            ruleBuilder.makeTemporary(fwd.timeout());
        }

        return ruleBuilder.build();

    }

    @Override
    public void next(NextObjective nextObjective) {
        //Do nothing
        log.debug("no action is needed here");
    }

    @Override
    public List<String> getNextMappings(NextGroup nextGroup) {

        return ImmutableList.of();
    }

    private void pass(Objective obj) {
        obj.context().ifPresent(context -> context.onSuccess(obj));
    }

    private void fail(Objective obj, ObjectiveError error) {
        obj.context().ifPresent(context -> context.onError(obj, error));
    }
}
