/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.drivers.hp;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.ImmutableList;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.packet.Ethernet;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.NextGroup;
import org.onosproject.net.behaviour.Pipeliner;
import org.onosproject.net.behaviour.PipelinerContext;
import org.onosproject.net.device.DeviceService;
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
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.EthTypeCriterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.FlowObjectiveStore;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.group.DefaultGroupKey;
import org.onosproject.net.group.GroupKey;
import org.onosproject.net.group.GroupService;
import org.onosproject.net.meter.MeterService;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.onosproject.net.flow.FlowRule.Builder;
import static org.onosproject.net.flowobjective.Objective.Operation.ADD;
import static org.slf4j.LoggerFactory.getLogger;


/**
 * Abstraction of the HP pipeline handler.
 * Possibly compliant with all HP OF switches but tested only with HP3800.
 */
public abstract class AbstractHPPipeline extends AbstractHandlerBehaviour implements Pipeliner {


    protected static final String APPLICATION_ID = "org.onosproject.drivers.hp.HPPipeline";
    public static final int CACHE_ENTRY_EXPIRATION_PERIOD = 20;
    private final Logger log = getLogger(getClass());
    protected FlowRuleService flowRuleService;
    protected GroupService groupService;
    protected MeterService meterService;
    protected FlowObjectiveStore flowObjectiveStore;
    protected DeviceId deviceId;
    protected ApplicationId appId;
    protected DeviceService deviceService;
    protected KryoNamespace appKryo = new KryoNamespace.Builder()
            .register(GroupKey.class)
            .register(DefaultGroupKey.class)
            .register(byte[].class)
            .build("AbstractHPPipeline");
    private ServiceDirectory serviceDirectory;
    private CoreService coreService;
    private Cache<Integer, NextObjective> pendingAddNext = CacheBuilder.newBuilder()
            .expireAfterWrite(CACHE_ENTRY_EXPIRATION_PERIOD, TimeUnit.SECONDS)
            .removalListener((RemovalNotification<Integer, NextObjective> notification) -> {
                if (notification.getCause() == RemovalCause.EXPIRED) {
                    notification.getValue().context()
                            .ifPresent(c -> c.onError(notification.getValue(),
                                                      ObjectiveError.FLOWINSTALLATIONFAILED));
                }
            }).build();

    /**
     * Sets default table id.
     * HP3800 switches have 3 tables, so one of them has to be default.
     *
     * @param ruleBuilder flow rule builder to be set table id
     * @return flow rule builder with set table id for flow
     */
    protected abstract FlowRule.Builder setDefaultTableIdForFlowObjective(Builder ruleBuilder);

    @Override
    public void init(DeviceId deviceId, PipelinerContext context) {
        this.serviceDirectory = context.directory();
        this.deviceId = deviceId;

        coreService = serviceDirectory.get(CoreService.class);
        flowRuleService = serviceDirectory.get(FlowRuleService.class);
        groupService = serviceDirectory.get(GroupService.class);
        meterService = serviceDirectory.get(MeterService.class);
        deviceService = serviceDirectory.get(DeviceService.class);
        flowObjectiveStore = context.store();

        appId = coreService.registerApplication(APPLICATION_ID);

        initializePipeline();
    }

    /**
     * Initializes pipeline.
     */
    protected abstract void initializePipeline();

    protected void pass(Objective obj) {
        obj.context().ifPresent(context -> context.onSuccess(obj));
    }

    protected void fail(Objective obj, ObjectiveError error) {
        obj.context().ifPresent(context -> context.onError(obj, error));
    }

    @Override
    public void forward(ForwardingObjective fwd) {

        if (fwd.treatment() != null) {
            // Deal with SPECIFIC and VERSATILE in the same manner.

            TrafficTreatment.Builder noClearTreatment = DefaultTrafficTreatment.builder();
            fwd.treatment().allInstructions().stream()
                    .filter(i -> i.type() != Instruction.Type.QUEUE).forEach(noClearTreatment::add);
            if (fwd.treatment().metered() != null) {
                noClearTreatment.meter(fwd.treatment().metered().meterId());
            }

            TrafficSelector.Builder noVlanSelector = DefaultTrafficSelector.builder();
            fwd.selector().criteria().stream()
                    .filter(c -> c.type() != Criterion.Type.ETH_TYPE || (c.type() == Criterion.Type.ETH_TYPE
                            && ((EthTypeCriterion) c).ethType().toShort() != Ethernet.TYPE_VLAN))
                    .forEach(noVlanSelector::add);

            // Then we create a new forwarding rule without the unsupported actions
            FlowRule.Builder ruleBuilder = DefaultFlowRule.builder()
                    .forDevice(deviceId)
                    .withSelector(noVlanSelector.build())
                    .withTreatment(noClearTreatment.build())
                    .withPriority(fwd.priority())
                    .withPriority(fwd.priority())
                    .fromApp(fwd.appId());

            //TODO: check whether ForwardingObjective can specify table
            setDefaultTableIdForFlowObjective(ruleBuilder);

            if (fwd.permanent()) {
                ruleBuilder.makePermanent();
            } else {
                ruleBuilder.makeTemporary(fwd.timeout());
            }

            installObjective(ruleBuilder, fwd);

        } else {
            NextObjective nextObjective;
            NextGroup next;
            TrafficTreatment treatment;
            if (fwd.op() == ADD) {
                // Give a try to the cache. Doing an operation
                // on the store seems to be very expensive.
                nextObjective = pendingAddNext.getIfPresent(fwd.nextId());
                // If the next objective is not present
                // We will try with the store
                if (nextObjective == null) {
                    next = flowObjectiveStore.getNextGroup(fwd.nextId());
                    // We verify that next was in the store and then de-serialize
                    // the treatment in order to re-build the flow rule.
                    if (next == null) {
                        fwd.context().ifPresent(c -> c.onError(fwd, ObjectiveError.GROUPMISSING));
                        return;
                    }
                    treatment = appKryo.deserialize(next.data());
                } else {
                    pendingAddNext.invalidate(fwd.nextId());
                    treatment = nextObjective.next().iterator().next();
                }
            } else {
                // We get the NextGroup from the remove operation.
                // Doing an operation on the store seems to be very expensive.
                next = flowObjectiveStore.removeNextGroup(fwd.nextId());
                if (next == null) {
                    fwd.context().ifPresent(c -> c.onError(fwd, ObjectiveError.GROUPMISSING));
                    return;
                }
                treatment = appKryo.deserialize(next.data());
            }
            // If the treatment is null we cannot re-build the original flow
            if (treatment == null) {
                fwd.context().ifPresent(c -> c.onError(fwd, ObjectiveError.GROUPMISSING));
                return;
            }
            // Finally we build the flow rule and push to the flowrule subsystem.
            FlowRule.Builder ruleBuilder = DefaultFlowRule.builder()
                    .forDevice(deviceId)
                    .withSelector(fwd.selector())
                    .fromApp(fwd.appId())
                    .withPriority(fwd.priority())
                    .withTreatment(treatment);
            if (fwd.permanent()) {
                ruleBuilder.makePermanent();
            } else {
                ruleBuilder.makeTemporary(fwd.timeout());
            }
            installObjective(ruleBuilder, fwd);
        }
    }

    /**
     * Installs objective.
     *
     * @param ruleBuilder flow rule builder used to build rule from objective
     * @param objective   objective to be installed
     */
    protected void installObjective(FlowRule.Builder ruleBuilder, Objective objective) {
        FlowRuleOperations.Builder flowBuilder = FlowRuleOperations.builder();

        switch (objective.op()) {
            case ADD:
                log.trace("Requested installation of objective " + objective.toString());
                FlowRule addRule = ruleBuilder.build();
                log.trace("built rule is " + addRule.toString());
                flowBuilder.add(addRule);
                break;
            case REMOVE:
                log.trace("Requested installation of objective " + objective.toString());
                FlowRule removeRule = ruleBuilder.build();
                log.trace("built rule is " + removeRule.toString());
                flowBuilder.remove(removeRule);
                break;
            default:
                log.warn("Unknown operation {}", objective.op());
        }

        flowRuleService.apply(flowBuilder.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                objective.context().ifPresent(context -> context.onSuccess(objective));
                log.trace("Installed objective " + objective.toString());
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                objective.context()
                        .ifPresent(context -> context.onError(objective, ObjectiveError.FLOWINSTALLATIONFAILED));
                log.trace("Objective installation failed" + objective.toString());
            }
        }));
    }

    @Override
    public void next(NextObjective nextObjective) {
        switch (nextObjective.op()) {
            case ADD:
                // We insert the value in the cache
                pendingAddNext.put(nextObjective.id(), nextObjective);
                // Then in the store, this will unblock the queued fwd obj
                flowObjectiveStore.putNextGroup(
                        nextObjective.id(),
                        new SingleGroup(nextObjective.next().iterator().next())
                );
                break;
            case REMOVE:
                break;
            default:
                log.warn("Unsupported operation {}", nextObjective.op());
        }
        nextObjective.context().ifPresent(context -> context.onSuccess(nextObjective));
    }

    @Override
    public List<String> getNextMappings(NextGroup nextGroup) {
        //TODO: to be implemented
        return ImmutableList.of();
    }

    @Override
    public void filter(FilteringObjective filteringObjective) {
        if (filteringObjective.type() == FilteringObjective.Type.PERMIT) {
            processFilter(filteringObjective,
                          filteringObjective.op() == Objective.Operation.ADD,
                          filteringObjective.appId());
        } else {
            fail(filteringObjective, ObjectiveError.UNSUPPORTED);
        }
    }

    /**
     * Filter processing and installation.
     * Processes and installs filtering rules.
     *
     * @param filt
     * @param install
     * @param applicationId
     */
    private void processFilter(FilteringObjective filt, boolean install,
                               ApplicationId applicationId) {
        // This driver only processes filtering criteria defined with switch
        // ports as the key
        PortCriterion port;
        if (!filt.key().equals(Criteria.dummy()) &&
                filt.key().type() == Criterion.Type.IN_PORT) {
            port = (PortCriterion) filt.key();
        } else {
            log.warn("No key defined in filtering objective from app: {}. Not"
                             + "processing filtering objective", applicationId);
            fail(filt, ObjectiveError.UNKNOWN);
            return;
        }
        // convert filtering conditions for switch-intfs into flowrules
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        for (Criterion c : filt.conditions()) {
            if (c.type() == Criterion.Type.ETH_DST) {
                EthCriterion eth = (EthCriterion) c;
                FlowRule.Builder rule = processEthFiler(filt, eth, port);
                rule.forDevice(deviceId)
                        .fromApp(applicationId);
                ops = install ? ops.add(rule.build()) : ops.remove(rule.build());

            } else if (c.type() == Criterion.Type.VLAN_VID) {
                VlanIdCriterion vlan = (VlanIdCriterion) c;
                FlowRule.Builder rule = processVlanFiler(filt, vlan, port);
                rule.forDevice(deviceId)
                        .fromApp(applicationId);
                ops = install ? ops.add(rule.build()) : ops.remove(rule.build());

            } else if (c.type() == Criterion.Type.IPV4_DST) {
                IPCriterion ip = (IPCriterion) c;
                FlowRule.Builder rule = processIpFilter(filt, ip, port);
                rule.forDevice(deviceId)
                        .fromApp(applicationId);
                ops = install ? ops.add(rule.build()) : ops.remove(rule.build());

            } else {
                log.warn("Driver does not currently process filtering condition"
                                 + " of type: {}", c.type());
                fail(filt, ObjectiveError.UNSUPPORTED);
            }
        }
        // apply filtering flow rules
        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                pass(filt);
                log.trace("Applied filtering rules");
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                fail(filt, ObjectiveError.FLOWINSTALLATIONFAILED);
                log.info("Failed to apply filtering rules");
            }
        }));
    }

    protected abstract Builder processEthFiler(FilteringObjective filt,
                                               EthCriterion eth, PortCriterion port);

    protected abstract Builder processVlanFiler(FilteringObjective filt,
                                                VlanIdCriterion vlan, PortCriterion port);

    protected abstract Builder processIpFilter(FilteringObjective filt,
                                               IPCriterion ip, PortCriterion port);

    private class SingleGroup implements NextGroup {

        private TrafficTreatment nextActions;

        SingleGroup(TrafficTreatment next) {
            this.nextActions = next;
        }

        @Override
        public byte[] data() {
            return appKryo.serialize(nextActions);
        }

        public TrafficTreatment treatment() {
            return nextActions;
        }

    }


}
