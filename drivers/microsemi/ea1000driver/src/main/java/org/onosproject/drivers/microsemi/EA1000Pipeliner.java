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
package org.onosproject.drivers.microsemi;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.onlab.osgi.ServiceDirectory;
import org.onosproject.drivers.microsemi.EA1000FlowRuleProgrammable.Ea1000Port;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.NextGroup;
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
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.Criterion.Type;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flow.instructions.Instructions.OutputInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModVlanIdInstruction;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.slf4j.Logger;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalNotification;

/**
 * Support for FlowObjectives in the EA1000.
 *
 * Used with the CarrierEthernet App
 *
 */
public class EA1000Pipeliner extends AbstractHandlerBehaviour implements Pipeliner {

    protected final Logger log = getLogger(getClass());
    protected ServiceDirectory serviceDirectory;
    protected FlowRuleService flowRuleService;
    protected DeviceId deviceId;
    protected Cache<Integer, NextObjective> pendingNext;
    protected Integer evcIdBase = 1;

    @Override
    public void init(DeviceId deviceId, PipelinerContext context) {
        this.serviceDirectory = context.directory();
        this.deviceId = deviceId;

        flowRuleService = serviceDirectory.get(FlowRuleService.class);

        pendingNext = CacheBuilder.newBuilder()
                .expireAfterWrite(20, TimeUnit.SECONDS)
                .removalListener((RemovalNotification<Integer, NextObjective> notification) -> {
                    if (notification.getCause() == RemovalCause.EXPIRED) {
                        notification.getValue().context()
                                .ifPresent(c -> c.onError(notification.getValue(),
                                        ObjectiveError.FLOWINSTALLATIONFAILED));
                    }
                }).build();

        log.debug("Loaded handler behaviour EA1000Pipeliner for " + handler().data().deviceId().uri());
    }

    @Override
    public void filter(FilteringObjective filterObjective) {
        TrafficTreatment.Builder actions;
        boolean oppositePort = false;
        int evcId = -1;
        switch (filterObjective.type()) {
            case PERMIT:
                if (filterObjective.meta() == null) {
                    actions = DefaultTrafficTreatment.builder().add(Instructions.popVlan());
                } else {
                    oppositePort = true; //Experimental - push happens on the opposite port
                    actions = DefaultTrafficTreatment.builder(filterObjective.meta());
                    if (filterObjective.meta().metered() != null) {
                        actions.meter(filterObjective.meta().metered().meterId());
                    }
                    actions.transition(0);
                    boolean isPush = false;
                    int vid = 0;
                    for (Instruction inst:filterObjective.meta().immediate()) {
                        if (inst.type() == Instruction.Type.L2MODIFICATION) {
                            L2ModificationInstruction l2mod = (L2ModificationInstruction) inst;
                            if (l2mod.subtype() == L2ModificationInstruction.L2SubType.VLAN_PUSH) {
                                isPush = true;
                            } else if (l2mod.subtype() == L2ModificationInstruction.L2SubType.VLAN_ID) {
                                vid = ((ModVlanIdInstruction) l2mod).vlanId().id();
                            }
                        }
                    }
                    if (isPush && vid > 0) {
                        evcId = vid;
                    }
                }
                break;
            case DENY:
                actions = (filterObjective.meta() == null) ?
                        DefaultTrafficTreatment.builder() :
                        DefaultTrafficTreatment.builder(filterObjective.meta());
                actions.drop();
                break;
            default:
                log.warn("Unknown filter type: {}", filterObjective.type());
                actions = DefaultTrafficTreatment.builder().drop();
        }

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();

        for (Criterion c:filterObjective.conditions()) {
            if (c.type() == Type.VLAN_VID && evcId == -1) {
                evcId = ((VlanIdCriterion) c).vlanId().id();
            }
            selector.add(c);
        }

        if (filterObjective.key() != null) {
            if (oppositePort) {
                //Experimental
                Ea1000Port port = Ea1000Port.fromNum(((PortCriterion) filterObjective.key()).port().toLong());
                selector.matchInPort(PortNumber.portNumber(port.opposite().portNum()));
            } else {
                selector.add(filterObjective.key());
            }
        }

        FlowRule.Builder ruleBuilder = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(actions.build())
                .fromApp(filterObjective.appId())
                .forTable(evcId)
                .withPriority(filterObjective.priority());

        if (filterObjective.permanent()) {
            ruleBuilder.makePermanent();
        } else {
            ruleBuilder.makeTemporary(filterObjective.timeout());
        }

        installObjective(ruleBuilder, filterObjective);

        log.debug("filter() of EA1000Pipeliner called for "
                + handler().data().deviceId().uri()
                + ". Objective: " + filterObjective);
    }

    @Override
    public void forward(ForwardingObjective forwardObjective) {
        TrafficSelector selector = forwardObjective.selector();

        if (forwardObjective.treatment() != null) {
            List<Instruction> instructions = forwardObjective.treatment().immediate();
            if (instructions != null && instructions.size() == 1
                    && instructions.get(0).type() == Instruction.Type.OUTPUT
                    && ((OutputInstruction) instructions.get(0)).port() == PortNumber.CONTROLLER) {
                Set<Criterion> criteria = forwardObjective.selector().criteria();
                log.info("EA1000 does not yet implement forwarding to CONTROLLER for flow objective for: "
                        + handler().data().deviceId().uri()
                        + ". "
                        + forwardObjective);
                return;
            } else {
                FlowRule.Builder ruleBuilder = DefaultFlowRule.builder()
                        .forDevice(deviceId)
                        .withSelector(selector)
                        .fromApp(forwardObjective.appId())
                        .withPriority(forwardObjective.priority())
                        .withTreatment(forwardObjective.treatment());

                if (forwardObjective.permanent()) {
                    ruleBuilder.makePermanent();
                } else {
                    ruleBuilder.makeTemporary(forwardObjective.timeout());
                }
                installObjective(ruleBuilder, forwardObjective);
            }
        } else {
            NextObjective nextObjective = pendingNext.getIfPresent(forwardObjective.nextId());
            if (nextObjective != null) {
                pendingNext.invalidate(forwardObjective.nextId());
                nextObjective.next().forEach(treat -> {
                    FlowRule.Builder ruleBuilder = DefaultFlowRule.builder()
                            .forDevice(deviceId)
                            .withSelector(selector)
                            .fromApp(forwardObjective.appId())
                            .withPriority(forwardObjective.priority())
                            .withTreatment(treat);

                    if (forwardObjective.permanent()) {
                        ruleBuilder.makePermanent();
                    } else {
                        ruleBuilder.makeTemporary(forwardObjective.timeout());
                    }
                    installObjective(ruleBuilder, forwardObjective);
                });
            } else {
                forwardObjective.context().ifPresent(c -> c.onError(forwardObjective,
                        ObjectiveError.GROUPMISSING));
            }
        }
        log.debug("EA1000: Unhandled Forwarding Objective for: "
                + handler().data().deviceId().uri()
                + ". "
                + forwardObjective);
    }

    @Override
    public void next(NextObjective nextObjective) {
        pendingNext.put(nextObjective.id(), nextObjective);
        nextObjective.context().ifPresent(context -> context.onSuccess(nextObjective));

        log.debug("next() of EA1000Pipeliner called for "
                + handler().data().deviceId().uri()
                + ". Objective: " + nextObjective);
    }

    @Override
    public List<String> getNextMappings(NextGroup nextGroup) {
        log.debug("getNextMappings() of EA1000Pipeliner called for "
                + handler().data().deviceId().uri()
                + ". Objective: " + nextGroup);
        return new ArrayList<String>();
    }

    protected void installObjective(FlowRule.Builder ruleBuilder, Objective objective) {
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
}
