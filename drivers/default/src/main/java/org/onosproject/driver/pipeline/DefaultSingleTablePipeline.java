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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalNotification;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.util.KryoNamespace;
import org.onosproject.net.DeviceId;
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
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.FlowObjectiveStore;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.store.serializers.KryoNamespaces;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.onosproject.net.flowobjective.Objective.Operation.ADD;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Simple single table pipeline abstraction.
 */
public class DefaultSingleTablePipeline extends AbstractHandlerBehaviour implements Pipeliner {

    private final Logger log = getLogger(getClass());

    private ServiceDirectory serviceDirectory;
    private FlowRuleService flowRuleService;
    private FlowObjectiveStore flowObjectiveStore;
    private DeviceId deviceId;

    private KryoNamespace appKryo = new KryoNamespace.Builder()
            .register(KryoNamespaces.API)
            .register(SingleGroup.class)
            .build("DefaultSingleTablePipeline");

    // Fast path for the installation. If we don't find the nextobjective in
    // the cache, as fallback mechanism we will try to retrieve the treatments
    // from the store. It is safe to use this cache for the addition, while it
    // should be avoided for the removal. This cache from Guava does not offer
    // thread-safe read (the read does not take the lock).
    private Cache<Integer, NextObjective> pendingAddNext = CacheBuilder.newBuilder()
            .expireAfterWrite(20, TimeUnit.SECONDS)
            .removalListener((RemovalNotification<Integer, NextObjective> notification) -> {
                if (notification.getCause() == RemovalCause.EXPIRED) {
                    notification.getValue().context()
                            .ifPresent(c -> c.onError(notification.getValue(),
                                                      ObjectiveError.FLOWINSTALLATIONFAILED));
                }
            }).build();


    @Override
    public void init(DeviceId deviceId, PipelinerContext context) {
        this.serviceDirectory = context.directory();
        this.deviceId = deviceId;

        flowRuleService = serviceDirectory.get(FlowRuleService.class);
        flowObjectiveStore = serviceDirectory.get(FlowObjectiveStore.class);

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

        filter.conditions().forEach(selector::add);

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
        TrafficSelector selector = fwd.selector();
        if (fwd.treatment() != null) {
            // Deal with SPECIFIC and VERSATILE in the same manner.
            FlowRule.Builder ruleBuilder = DefaultFlowRule.builder()
                    .forDevice(deviceId)
                    .withSelector(selector)
                    .fromApp(fwd.appId())
                    .withPriority(fwd.priority())
                    .withTreatment(fwd.treatment());

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
                next = flowObjectiveStore.getNextGroup(fwd.nextId());
                if (next == null) {
                    fwd.context().ifPresent(c -> c.onError(fwd, ObjectiveError.GROUPMISSING));
                    return;
                }
                treatment = appKryo.deserialize(next.data());
            }
            // If the treatment is null we cannot re-build the original flow
            if (treatment == null)  {
                fwd.context().ifPresent(c -> c.onError(fwd, ObjectiveError.GROUPMISSING));
                return;
            }
            // Finally we build the flow rule and push to the flowrule subsystem.
            FlowRule.Builder ruleBuilder = DefaultFlowRule.builder()
                    .forDevice(deviceId)
                    .withSelector(selector)
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
                NextGroup next = flowObjectiveStore.removeNextGroup(nextObjective.id());
                if (next == null) {
                    nextObjective.context().ifPresent(context -> context.onError(nextObjective,
                                                                                 ObjectiveError.GROUPMISSING));
                    return;
                }
                break;
            default:
                log.warn("Unsupported operation {}", nextObjective.op());
        }
        nextObjective.context().ifPresent(context -> context.onSuccess(nextObjective));
    }

    @Override
    public List<String> getNextMappings(NextGroup nextGroup) {
        // Default single table pipeline does not use nextObjectives or groups
        return Collections.emptyList();
    }

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
