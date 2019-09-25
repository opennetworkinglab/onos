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

package org.onosproject.pipelines.fabric.impl.behaviour.pipeliner;

import com.google.common.collect.ImmutableList;
import org.onlab.util.KryoNamespace;
import org.onlab.util.SharedExecutors;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.NextGroup;
import org.onosproject.net.behaviour.Pipeliner;
import org.onosproject.net.behaviour.PipelinerContext;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.FlowObjectiveStore;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.IdNextTreatment;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.NextTreatment;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupService;
import org.onosproject.pipelines.fabric.impl.behaviour.AbstractFabricHandlerBehavior;
import org.onosproject.pipelines.fabric.impl.behaviour.FabricCapabilities;
import org.onosproject.store.serializers.KryoNamespaces;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.onosproject.pipelines.fabric.impl.behaviour.FabricUtils.outputPort;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Pipeliner implementation for fabric pipeline which uses ObjectiveTranslator
 * implementations to translate flow objectives for the different blocks,
 * filtering, forwarding and next.
 */
public class FabricPipeliner extends AbstractFabricHandlerBehavior
        implements Pipeliner {

    private static final Logger log = getLogger(FabricPipeliner.class);

    protected static final KryoNamespace KRYO = new KryoNamespace.Builder()
            .register(KryoNamespaces.API)
            .register(FabricNextGroup.class)
            .build("FabricPipeliner");

    protected DeviceId deviceId;
    protected FlowRuleService flowRuleService;
    protected GroupService groupService;
    protected FlowObjectiveStore flowObjectiveStore;

    private FilteringObjectiveTranslator filteringTranslator;
    private ForwardingObjectiveTranslator forwardingTranslator;
    private NextObjectiveTranslator nextTranslator;

    private final ExecutorService callbackExecutor = SharedExecutors.getPoolThreadExecutor();

    /**
     * Creates a new instance of this behavior with the given capabilities.
     *
     * @param capabilities capabilities
     */
    public FabricPipeliner(FabricCapabilities capabilities) {
        super(capabilities);
    }

    /**
     * Create a new instance of this behaviour. Used by the abstract projectable
     * model (i.e., {@link org.onosproject.net.Device#as(Class)}.
     */
    public FabricPipeliner() {
        super();
    }

    @Override
    public void init(DeviceId deviceId, PipelinerContext context) {
        this.deviceId = deviceId;
        this.flowRuleService = context.directory().get(FlowRuleService.class);
        this.groupService = context.directory().get(GroupService.class);
        this.flowObjectiveStore = context.directory().get(FlowObjectiveStore.class);
        this.filteringTranslator = new FilteringObjectiveTranslator(deviceId, capabilities);
        this.forwardingTranslator = new ForwardingObjectiveTranslator(deviceId, capabilities);
        this.nextTranslator = new NextObjectiveTranslator(deviceId, capabilities);
    }

    @Override
    public void filter(FilteringObjective obj) {
        final ObjectiveTranslation result = filteringTranslator.translate(obj);
        handleResult(obj, result);
    }

    @Override
    public void forward(ForwardingObjective obj) {
        final ObjectiveTranslation result = forwardingTranslator.translate(obj);
        handleResult(obj, result);
    }

    @Override
    public void next(NextObjective obj) {
        if (obj.op() == Objective.Operation.VERIFY) {
            // TODO: support VERIFY operation
            log.debug("VERIFY operation not yet supported for NextObjective, will return success");
            success(obj);
            return;
        }

        if (obj.op() == Objective.Operation.MODIFY) {
            // TODO: support MODIFY operation
            log.warn("MODIFY operation not yet supported for NextObjective, will return failure :(");
            fail(obj, ObjectiveError.UNSUPPORTED);
            return;
        }

        final ObjectiveTranslation result = nextTranslator.translate(obj);
        handleResult(obj, result);
    }

    @Override
    public List<String> getNextMappings(NextGroup nextGroup) {
        final FabricNextGroup fabricNextGroup = KRYO.deserialize(nextGroup.data());
        return fabricNextGroup.nextMappings().stream()
                .map(m -> format("%s -> %s", fabricNextGroup.type(), m))
                .collect(Collectors.toList());
    }

    private void handleResult(Objective obj, ObjectiveTranslation result) {
        if (result.error().isPresent()) {
            fail(obj, result.error().get());
            return;
        }
        processGroups(obj, result.groups());
        processFlows(obj, result.flowRules());
        if (obj instanceof NextObjective) {
            handleNextGroup((NextObjective) obj);
        }
        success(obj);
    }

    private void handleNextGroup(NextObjective obj) {
        switch (obj.op()) {
            case REMOVE:
                removeNextGroup(obj);
                break;
            case ADD:
            case ADD_TO_EXISTING:
            case REMOVE_FROM_EXISTING:
            case MODIFY:
                putNextGroup(obj);
                break;
            case VERIFY:
                break;
            default:
                log.error("Unknown NextObjective operation '{}'", obj.op());
        }
    }

    private void processFlows(Objective objective, Collection<FlowRule> flowRules) {
        if (flowRules.isEmpty()) {
            return;
        }
        final FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        switch (objective.op()) {
            case ADD:
            case ADD_TO_EXISTING:
                flowRules.forEach(ops::add);
                break;
            case REMOVE:
            case REMOVE_FROM_EXISTING:
                flowRules.forEach(ops::remove);
                break;
            default:
                log.warn("Unsupported Objective operation '{}'", objective.op());
                return;
        }
        flowRuleService.apply(ops.build());
    }

    private void processGroups(Objective objective, Collection<GroupDescription> groups) {
        if (groups.isEmpty()) {
            return;
        }
        switch (objective.op()) {
            case ADD:
                groups.forEach(groupService::addGroup);
                break;
            case REMOVE:
                groups.forEach(group -> groupService.removeGroup(
                        deviceId, group.appCookie(), objective.appId()));
                break;
            case ADD_TO_EXISTING:
                groups.forEach(group -> groupService.addBucketsToGroup(
                        deviceId, group.appCookie(), group.buckets(),
                        group.appCookie(), group.appId())
                );
                break;
            case REMOVE_FROM_EXISTING:
                groups.forEach(group -> groupService.removeBucketsFromGroup(
                        deviceId, group.appCookie(), group.buckets(),
                        group.appCookie(), group.appId())
                );
                break;
            default:
                log.warn("Unsupported Objective operation {}", objective.op());
        }
    }

    private void fail(Objective objective, ObjectiveError error) {
        CompletableFuture.runAsync(
                () -> objective.context().ifPresent(
                        ctx -> ctx.onError(objective, error)), callbackExecutor);

    }


    private void success(Objective objective) {
        CompletableFuture.runAsync(
                () -> objective.context().ifPresent(
                        ctx -> ctx.onSuccess(objective)), callbackExecutor);
    }

    private void removeNextGroup(NextObjective obj) {
        final NextGroup removed = flowObjectiveStore.removeNextGroup(obj.id());
        if (removed == null) {
            log.debug("NextGroup {} was not found in FlowObjectiveStore");
        }
    }

    private void putNextGroup(NextObjective obj) {
        final List<String> nextMappings = obj.nextTreatments().stream()
                .map(this::nextTreatmentToMappingString)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        final FabricNextGroup nextGroup = new FabricNextGroup(obj.type(), nextMappings);
        flowObjectiveStore.putNextGroup(obj.id(), nextGroup);
    }

    private String nextTreatmentToMappingString(NextTreatment n) {
        switch (n.type()) {
            case TREATMENT:
                final PortNumber p = outputPort(n);
                return p == null ? "UNKNOWN"
                        : format("OUTPUT:%s", p.toString());
            case ID:
                final IdNextTreatment id = (IdNextTreatment) n;
                return format("NEXT_ID:%d", id.nextId());
            default:
                log.warn("Unknown NextTreatment type '{}'", n.type());
                return "???";
        }
    }

    /**
     * NextGroup implementation.
     */
    private static class FabricNextGroup implements NextGroup {

        private final NextObjective.Type type;
        private final List<String> nextMappings;

        FabricNextGroup(NextObjective.Type type, List<String> nextMappings) {
            this.type = type;
            this.nextMappings = ImmutableList.copyOf(nextMappings);
        }

        NextObjective.Type type() {
            return type;
        }

        Collection<String> nextMappings() {
            return nextMappings;
        }

        @Override
        public byte[] data() {
            return KRYO.serialize(this);
        }
    }
}
