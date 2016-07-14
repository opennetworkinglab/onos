/*
 * Copyright 2016-present Open Networking Laboratory
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
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.NextGroup;
import org.onosproject.net.behaviour.Pipeliner;
import org.onosproject.net.behaviour.PipelinerContext;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleOperationsContext;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.FlowObjectiveStore;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveError;

import org.slf4j.Logger;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


import static org.slf4j.LoggerFactory.getLogger;

/**
 *  Driver for Hp. Default table starts from 200.
 */
public class HpPipeline extends AbstractHandlerBehaviour implements Pipeliner {


    private final Logger log = getLogger(getClass());

    private ServiceDirectory serviceDirectory;
    private FlowRuleService flowRuleService;
    private CoreService coreService;
    private DeviceId deviceId;
    private ApplicationId appId;
    protected FlowObjectiveStore flowObjectiveStore;

    //FIXME: hp table numbers are configurable . Set this parameter configurable
    private static final int SOFTWARE_TABLE_START = 200;
    private static final int TIME_OUT = 30;

    @Override
    public void init(DeviceId deviceId, PipelinerContext context) {
        log.debug("Initiate HP pipeline");
        this.serviceDirectory = context.directory();
        this.deviceId = deviceId;

        flowRuleService = serviceDirectory.get(FlowRuleService.class);
        coreService = serviceDirectory.get(CoreService.class);
        flowObjectiveStore = context.store();

        appId = coreService.registerApplication(
                "org.onosproject.driver.HpPipeline");
    }

    @Override
    public void filter(FilteringObjective filter)  {
        //Do nothing
    }

    @Override
    public void forward(ForwardingObjective forwardObjective) {

        Collection<FlowRule> rules;
        FlowRuleOperations.Builder flowOpsBuilder = FlowRuleOperations.builder();

        rules = processForward(forwardObjective);

        switch (forwardObjective.op()) {
            case ADD:
                rules.stream()
                        .filter(Objects::nonNull)
                        .forEach(flowOpsBuilder::add);
                break;
            case REMOVE:
                rules.stream()
                        .filter(Objects::nonNull)
                        .forEach(flowOpsBuilder::remove);
                break;
            default:
                fail(forwardObjective, ObjectiveError.UNKNOWN);
                log.warn("Unknown forwarding type {}");
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

    private Collection<FlowRule> processForward(ForwardingObjective fwd) {

        log.debug("Processing forwarding object");

        FlowRule.Builder ruleBuilder = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(fwd.selector())
                .withTreatment(fwd.treatment())
                .withPriority(fwd.priority())
                .fromApp(fwd.appId())
                .forTable(SOFTWARE_TABLE_START);

        if (fwd.permanent()) {
            ruleBuilder.makePermanent();
        } else {
            ruleBuilder.makeTemporary(TIME_OUT);
        }

        return Collections.singletonList(ruleBuilder.build());
    }



    @Override
    public void next(NextObjective nextObjective) {
        // Do nothing
    }

    @Override
    public List<String> getNextMappings(NextGroup nextGroup) {
        // TODO Implementation deferred to vendor
        return null;
    }

    private void pass(Objective obj) {
        obj.context().ifPresent(context -> context.onSuccess(obj));
    }

    private void fail(Objective obj, ObjectiveError error) {
        obj.context().ifPresent(context -> context.onError(obj, error));
    }


}
