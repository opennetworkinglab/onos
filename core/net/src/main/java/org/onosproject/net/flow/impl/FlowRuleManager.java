/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.net.flow.impl;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.IdGenerator;
import org.onosproject.event.AbstractListenerRegistry;
import org.onosproject.event.EventDeliveryService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.CompletedBatchOperation;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleBatchEntry;
import org.onosproject.net.flow.FlowRuleBatchEvent;
import org.onosproject.net.flow.FlowRuleBatchOperation;
import org.onosproject.net.flow.FlowRuleBatchRequest;
import org.onosproject.net.flow.FlowRuleEvent;
import org.onosproject.net.flow.FlowRuleListener;
import org.onosproject.net.flow.FlowRuleOperation;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleOperationsContext;
import org.onosproject.net.flow.FlowRuleProvider;
import org.onosproject.net.flow.FlowRuleProviderRegistry;
import org.onosproject.net.flow.FlowRuleProviderService;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.FlowRuleStore;
import org.onosproject.net.flow.FlowRuleStoreDelegate;
import org.onosproject.net.provider.AbstractProviderRegistry;
import org.onosproject.net.provider.AbstractProviderService;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.namedThreads;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides implementation of the flow NB &amp; SB APIs.
 */
@Component(immediate = true)
@Service
public class FlowRuleManager
        extends AbstractProviderRegistry<FlowRuleProvider, FlowRuleProviderService>
        implements FlowRuleService, FlowRuleProviderRegistry {

    enum BatchState { STARTED, FINISHED, CANCELLED }

    public static final String FLOW_RULE_NULL = "FlowRule cannot be null";
    private final Logger log = getLogger(getClass());

    private final AbstractListenerRegistry<FlowRuleEvent, FlowRuleListener>
            listenerRegistry = new AbstractListenerRegistry<>();

    private final FlowRuleStoreDelegate delegate = new InternalStoreDelegate();

    protected ExecutorService deviceInstallers =
            Executors.newCachedThreadPool(namedThreads("onos-device-installer-%d"));

    protected ExecutorService operationsService =
            Executors.newFixedThreadPool(32, namedThreads("onos-flowservice-operations-%d"));

    private IdGenerator idGenerator;

    private Map<Long, FlowOperationsProcessor> pendingFlowOperations = new
            ConcurrentHashMap<>();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EventDeliveryService eventDispatcher;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Activate
    public void activate() {

        idGenerator = coreService.getIdGenerator(FLOW_OP_TOPIC);


        store.setDelegate(delegate);
        eventDispatcher.addSink(FlowRuleEvent.class, listenerRegistry);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        deviceInstallers.shutdownNow();
        operationsService.shutdownNow();
        store.unsetDelegate(delegate);
        eventDispatcher.removeSink(FlowRuleEvent.class);
        log.info("Stopped");
    }

    @Override
    public int getFlowRuleCount() {
        return store.getFlowRuleCount();
    }

    @Override
    public Iterable<FlowEntry> getFlowEntries(DeviceId deviceId) {
        return store.getFlowEntries(deviceId);
    }

    @Override
    public void applyFlowRules(FlowRule... flowRules) {
        FlowRuleOperations.Builder builder = FlowRuleOperations.builder();
        for (int i = 0; i < flowRules.length; i++) {
            builder.add(flowRules[i]);
        }
        apply(builder.build());
    }

    @Override
    public void removeFlowRules(FlowRule... flowRules) {
        FlowRuleOperations.Builder builder = FlowRuleOperations.builder();
        for (int i = 0; i < flowRules.length; i++) {
            builder.remove(flowRules[i]);
        }
        apply(builder.build());
    }

    @Override
    public void removeFlowRulesById(ApplicationId id) {
        removeFlowRules(Iterables.toArray(getFlowRulesById(id), FlowRule.class));
    }

    @Override
    public Iterable<FlowRule> getFlowRulesById(ApplicationId id) {
        Set<FlowRule> flowEntries = Sets.newHashSet();
        for (Device d : deviceService.getDevices()) {
            for (FlowEntry flowEntry : store.getFlowEntries(d.id())) {
                if (flowEntry.appId() == id.id()) {
                    flowEntries.add(flowEntry);
                }
            }
        }
        return flowEntries;
    }

    @Override
    public Iterable<FlowRule> getFlowRulesByGroupId(ApplicationId appId, short groupId) {
        Set<FlowRule> matches = Sets.newHashSet();
        long toLookUp = ((long) appId.id() << 16) | groupId;
        for (Device d : deviceService.getDevices()) {
            for (FlowEntry flowEntry : store.getFlowEntries(d.id())) {
                if ((flowEntry.id().value() >>> 32) == toLookUp) {
                    matches.add(flowEntry);
                }
            }
        }
        return matches;
    }

    @Override
    public Future<CompletedBatchOperation> applyBatch(FlowRuleBatchOperation batch) {


        FlowRuleOperations.Builder fopsBuilder = FlowRuleOperations.builder();
        batch.getOperations().stream().forEach(op -> {
                        switch (op.getOperator()) {
                            case ADD:
                                fopsBuilder.add(op.getTarget());
                                break;
                            case REMOVE:
                                fopsBuilder.remove(op.getTarget());
                                break;
                            case MODIFY:
                                fopsBuilder.modify(op.getTarget());
                                break;
                            default:
                                log.warn("Unknown flow operation operator: {}", op.getOperator());

                        }
                }
        );

        apply(fopsBuilder.build());
        return Futures.immediateFuture(
                new CompletedBatchOperation(true,
                                            Collections.emptySet(), null));

    }

    @Override
    public void apply(FlowRuleOperations ops) {
        operationsService.submit(new FlowOperationsProcessor(ops));
    }

    @Override
    public void addListener(FlowRuleListener listener) {
        listenerRegistry.addListener(listener);
    }

    @Override
    public void removeListener(FlowRuleListener listener) {
        listenerRegistry.removeListener(listener);
    }

    @Override
    protected FlowRuleProviderService createProviderService(
            FlowRuleProvider provider) {
        return new InternalFlowRuleProviderService(provider);
    }

    private class InternalFlowRuleProviderService
            extends AbstractProviderService<FlowRuleProvider>
            implements FlowRuleProviderService {

        final Map<FlowEntry, Long> lastSeen = Maps.newConcurrentMap();

        protected InternalFlowRuleProviderService(FlowRuleProvider provider) {
            super(provider);
        }

        @Override
        public void flowRemoved(FlowEntry flowEntry) {
            checkNotNull(flowEntry, FLOW_RULE_NULL);
            checkValidity();
            lastSeen.remove(flowEntry);
            FlowEntry stored = store.getFlowEntry(flowEntry);
            if (stored == null) {
                log.debug("Rule already evicted from store: {}", flowEntry);
                return;
            }
            Device device = deviceService.getDevice(flowEntry.deviceId());
            FlowRuleProvider frp = getProvider(device.providerId());
            FlowRuleEvent event = null;
            switch (stored.state()) {
                case ADDED:
                case PENDING_ADD:
                    frp.applyFlowRule(stored);
                    break;
                case PENDING_REMOVE:
                case REMOVED:
                    event = store.removeFlowRule(stored);
                    break;
                default:
                    break;

            }
            if (event != null) {
                log.debug("Flow {} removed", flowEntry);
                post(event);
            }
        }


        private void flowMissing(FlowEntry flowRule) {
            checkNotNull(flowRule, FLOW_RULE_NULL);
            checkValidity();
            Device device = deviceService.getDevice(flowRule.deviceId());
            FlowRuleProvider frp = getProvider(device.providerId());
            FlowRuleEvent event = null;
            switch (flowRule.state()) {
                case PENDING_REMOVE:
                case REMOVED:
                    event = store.removeFlowRule(flowRule);
                    frp.removeFlowRule(flowRule);
                    break;
                case ADDED:
                case PENDING_ADD:
                    frp.applyFlowRule(flowRule);
                    break;
                default:
                    log.debug("Flow {} has not been installed.", flowRule);
            }

            if (event != null) {
                log.debug("Flow {} removed", flowRule);
                post(event);
            }

        }


        private void extraneousFlow(FlowRule flowRule) {
            checkNotNull(flowRule, FLOW_RULE_NULL);
            checkValidity();
            FlowRuleProvider frp = getProvider(flowRule.deviceId());
            frp.removeFlowRule(flowRule);
            log.debug("Flow {} is on switch but not in store.", flowRule);
        }


        private void flowAdded(FlowEntry flowEntry) {
            checkNotNull(flowEntry, FLOW_RULE_NULL);
            checkValidity();

            if (checkRuleLiveness(flowEntry, store.getFlowEntry(flowEntry))) {

                FlowRuleEvent event = store.addOrUpdateFlowRule(flowEntry);
                if (event == null) {
                    log.debug("No flow store event generated.");
                } else {
                    log.trace("Flow {} {}", flowEntry, event.type());
                    post(event);
                }
            } else {
                log.debug("Removing flow rules....");
                removeFlowRules(flowEntry);
            }

        }

        private boolean checkRuleLiveness(FlowEntry swRule, FlowEntry storedRule) {
            if (storedRule == null) {
                return false;
            }
            if (storedRule.isPermanent()) {
                return true;
            }

            final long timeout = storedRule.timeout() * 1000;
            final long currentTime = System.currentTimeMillis();
            if (storedRule.packets() != swRule.packets()) {
                lastSeen.put(storedRule, currentTime);
                return true;
            }
            if (!lastSeen.containsKey(storedRule)) {
                // checking for the first time
                lastSeen.put(storedRule, storedRule.lastSeen());
                // Use following if lastSeen attr. was removed.
                //lastSeen.put(storedRule, currentTime);
            }
            Long last = lastSeen.get(storedRule);
            if (last == null) {
                // concurrently removed? let the liveness check fail
                return false;
            }

            if ((currentTime - last) <= timeout) {
                return true;
            }
            return false;
        }

        // Posts the specified event to the local event dispatcher.
        private void post(FlowRuleEvent event) {
            if (event != null) {
                eventDispatcher.post(event);
            }
        }

        @Override
        public void pushFlowMetrics(DeviceId deviceId, Iterable<FlowEntry> flowEntries) {
            Set<FlowEntry> storedRules = Sets.newHashSet(store.getFlowEntries(deviceId));

            for (FlowEntry rule : flowEntries) {
                if (storedRules.remove(rule)) {
                    // we both have the rule, let's update some info then.
                    flowAdded(rule);
                } else {
                    // the device has a rule the store does not have
                    extraneousFlow(rule);
                }
            }
            for (FlowEntry rule : storedRules) {
                // there are rules in the store that aren't on the switch
                flowMissing(rule);

            }
        }

        @Override
        public void batchOperationCompleted(long batchId, CompletedBatchOperation operation) {
            store.batchOperationComplete(FlowRuleBatchEvent.completed(
                    new FlowRuleBatchRequest(batchId, Collections.emptySet()),
                    operation
            ));
        }
    }

    // Store delegate to re-post events emitted from the store.
    private class InternalStoreDelegate implements FlowRuleStoreDelegate {


        // TODO: Right now we only dispatch events at individual flowEntry level.
        // It may be more efficient for also dispatch events as a batch.
        @Override
        public void notify(FlowRuleBatchEvent event) {
            final FlowRuleBatchRequest request = event.subject();
            switch (event.type()) {
            case BATCH_OPERATION_REQUESTED:
                // Request has been forwarded to MASTER Node, and was
                request.ops().stream().forEach(
                        op -> {
                            switch (op.getOperator()) {

                                case ADD:
                                    eventDispatcher.post(
                                            new FlowRuleEvent(
                                                    FlowRuleEvent.Type.RULE_ADD_REQUESTED,
                                                    op.getTarget()));
                                    break;
                                case REMOVE:
                                    eventDispatcher.post(
                                            new FlowRuleEvent(
                                                    FlowRuleEvent.Type.RULE_REMOVE_REQUESTED,
                                                    op.getTarget()));
                                    break;
                                case MODIFY:
                                    //TODO: do something here when the time comes.
                                    break;
                                default:
                                    log.warn("Unknown flow operation operator: {}", op.getOperator());
                            }
                        }
                );

                DeviceId deviceId = event.deviceId();

                FlowRuleBatchOperation batchOperation =
                        request.asBatchOperation(deviceId);

                FlowRuleProvider flowRuleProvider =
                        getProvider(deviceId);

                flowRuleProvider.executeBatch(batchOperation);

                break;

            case BATCH_OPERATION_COMPLETED:

                FlowOperationsProcessor fops = pendingFlowOperations.remove(
                        event.subject().batchId());
                if (event.result().isSuccess()) {
                    if (fops != null) {
                        fops.satisfy(event.deviceId());
                    }
                } else {
                    fops.fail(event.deviceId(), event.result().failedItems());
                }

                break;

            default:
                break;
            }
        }
    }

    private class FlowOperationsProcessor implements Runnable {

        private final List<Set<FlowRuleOperation>> stages;
        private final FlowRuleOperationsContext context;
        private final FlowRuleOperations fops;
        private final AtomicBoolean hasFailed = new AtomicBoolean(false);

        private Set<DeviceId> pendingDevices;

        public FlowOperationsProcessor(FlowRuleOperations ops) {

            this.stages = Lists.newArrayList(ops.stages());
            this.context = ops.callback();
            this.fops = ops;
            pendingDevices = Sets.newConcurrentHashSet();


        }

        @Override
        public void run() {
            if (stages.size() > 0) {
                process(stages.remove(0));
            } else if (!hasFailed.get() && context != null) {
                context.onSuccess(fops);
            }
        }

        private void process(Set<FlowRuleOperation> ops) {
            Multimap<DeviceId, FlowRuleBatchEntry> perDeviceBatches =
                    ArrayListMultimap.create();

            FlowRuleBatchEntry fbe;
            for (FlowRuleOperation flowRuleOperation : ops) {
                switch (flowRuleOperation.type()) {
                    // FIXME: Brian needs imagination when creating class names.
                    case ADD:
                        fbe = new FlowRuleBatchEntry(
                                FlowRuleBatchEntry.FlowRuleOperation.ADD, flowRuleOperation.rule());
                        break;
                    case MODIFY:
                        fbe = new FlowRuleBatchEntry(
                                FlowRuleBatchEntry.FlowRuleOperation.MODIFY, flowRuleOperation.rule());
                        break;
                    case REMOVE:
                        fbe = new FlowRuleBatchEntry(
                                FlowRuleBatchEntry.FlowRuleOperation.REMOVE, flowRuleOperation.rule());
                        break;
                    default:
                        throw new UnsupportedOperationException("Unknown flow rule type " + flowRuleOperation.type());
                }
                pendingDevices.add(flowRuleOperation.rule().deviceId());
                perDeviceBatches.put(flowRuleOperation.rule().deviceId(), fbe);
            }


            for (DeviceId deviceId : perDeviceBatches.keySet()) {
                Long id = idGenerator.getNewId();
                final FlowRuleBatchOperation b = new FlowRuleBatchOperation(perDeviceBatches.get(deviceId),
                                               deviceId, id);
                pendingFlowOperations.put(id, this);
                deviceInstallers.submit(new Runnable() {
                    @Override
                    public void run() {
                        store.storeBatch(b);
                    }
                });
            }
        }

        public void satisfy(DeviceId devId) {
            pendingDevices.remove(devId);
            if (pendingDevices.isEmpty()) {
                operationsService.submit(this);
            }
        }



        public void fail(DeviceId devId, Set<? extends FlowRule> failures) {
            hasFailed.set(true);
            pendingDevices.remove(devId);
            if (pendingDevices.isEmpty()) {
                operationsService.submit(this);
            }

            if (context != null) {
                final FlowRuleOperations.Builder failedOpsBuilder =
                    FlowRuleOperations.builder();
                failures.stream().forEach(failedOpsBuilder::add);

                context.onError(failedOpsBuilder.build());
            }
        }

    }
}
