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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.core.ApplicationId;
import org.onosproject.event.AbstractListenerRegistry;
import org.onosproject.event.EventDeliveryService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.CompletedBatchOperation;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleBatchEntry;
import org.onosproject.net.flow.FlowRuleBatchEntry.FlowRuleOperation;
import org.onosproject.net.flow.FlowRuleBatchEvent;
import org.onosproject.net.flow.FlowRuleBatchOperation;
import org.onosproject.net.flow.FlowRuleBatchRequest;
import org.onosproject.net.flow.FlowRuleEvent;
import org.onosproject.net.flow.FlowRuleListener;
import org.onosproject.net.flow.FlowRuleProvider;
import org.onosproject.net.flow.FlowRuleProviderRegistry;
import org.onosproject.net.flow.FlowRuleProviderService;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.FlowRuleStore;
import org.onosproject.net.flow.FlowRuleStoreDelegate;
import org.onosproject.net.provider.AbstractProviderRegistry;
import org.onosproject.net.provider.AbstractProviderService;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
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

    enum BatchState { STARTED, FINISHED, CANCELLED };

    public static final String FLOW_RULE_NULL = "FlowRule cannot be null";
    private final Logger log = getLogger(getClass());

    private final AbstractListenerRegistry<FlowRuleEvent, FlowRuleListener>
            listenerRegistry = new AbstractListenerRegistry<>();

    private final FlowRuleStoreDelegate delegate = new InternalStoreDelegate();

    private ExecutorService futureService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EventDeliveryService eventDispatcher;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Activate
    public void activate() {
        futureService =
                Executors.newFixedThreadPool(32, namedThreads("provider-future-listeners-%d"));
        store.setDelegate(delegate);
        eventDispatcher.addSink(FlowRuleEvent.class, listenerRegistry);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        futureService.shutdownNow();

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
        Set<FlowRuleBatchEntry> toAddBatchEntries = Sets.newHashSet();
        for (int i = 0; i < flowRules.length; i++) {
            toAddBatchEntries.add(new FlowRuleBatchEntry(FlowRuleOperation.ADD, flowRules[i]));
        }
        applyBatch(new FlowRuleBatchOperation(toAddBatchEntries));
    }

    @Override
    public void removeFlowRules(FlowRule... flowRules) {
        Set<FlowRuleBatchEntry> toRemoveBatchEntries = Sets.newHashSet();
        for (int i = 0; i < flowRules.length; i++) {
            toRemoveBatchEntries.add(new FlowRuleBatchEntry(FlowRuleOperation.REMOVE, flowRules[i]));
        }
        applyBatch(new FlowRuleBatchOperation(toRemoveBatchEntries));
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
    public Future<CompletedBatchOperation> applyBatch(
            FlowRuleBatchOperation batch) {
        Multimap<DeviceId, FlowRuleBatchEntry> perDeviceBatches =
                ArrayListMultimap.create();
        List<Future<CompletedBatchOperation>> futures = Lists.newArrayList();
        for (FlowRuleBatchEntry fbe : batch.getOperations()) {
            final FlowRule f = fbe.getTarget();
            perDeviceBatches.put(f.deviceId(), fbe);
        }

        for (DeviceId deviceId : perDeviceBatches.keySet()) {
            FlowRuleBatchOperation b =
                    new FlowRuleBatchOperation(perDeviceBatches.get(deviceId));
            Future<CompletedBatchOperation> future = store.storeBatch(b);
            futures.add(future);
        }
        return new FlowRuleBatchFuture(futures, perDeviceBatches);
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
    }

    // Store delegate to re-post events emitted from the store.
    private class InternalStoreDelegate implements FlowRuleStoreDelegate {

        // FIXME set appropriate default and make it configurable
        private static final int TIMEOUT_PER_OP = 500; // ms

        // TODO: Right now we only dispatch events at individual flowEntry level.
        // It may be more efficient for also dispatch events as a batch.
        @Override
        public void notify(FlowRuleBatchEvent event) {
            final FlowRuleBatchRequest request = event.subject();
            switch (event.type()) {
            case BATCH_OPERATION_REQUESTED:
                // Request has been forwarded to MASTER Node, and was
                for (FlowRule entry : request.toAdd()) {
                    eventDispatcher.post(new FlowRuleEvent(FlowRuleEvent.Type.RULE_ADD_REQUESTED, entry));
                }
                for (FlowRule entry : request.toRemove()) {
                    eventDispatcher.post(new FlowRuleEvent(FlowRuleEvent.Type.RULE_REMOVE_REQUESTED, entry));
                }
                // FIXME: what about op.equals(FlowRuleOperation.MODIFY) ?

                FlowRuleBatchOperation batchOperation = request.asBatchOperation();

                FlowRuleProvider flowRuleProvider =
                        getProvider(batchOperation.getOperations().get(0).getTarget().deviceId());
                final Future<CompletedBatchOperation> result =
                        flowRuleProvider.executeBatch(batchOperation);
                futureService.submit(new Runnable() {
                    @Override
                    public void run() {
                        CompletedBatchOperation res;
                        try {
                            res = result.get(TIMEOUT_PER_OP * batchOperation.size(), TimeUnit.MILLISECONDS);
                            store.batchOperationComplete(FlowRuleBatchEvent.completed(request, res));
                        } catch (TimeoutException | InterruptedException | ExecutionException e) {
                            log.warn("Something went wrong with the batch operation {}",
                                     request.batchId(), e);

                            Set<FlowRule> failures = new HashSet<>(batchOperation.size());
                            for (FlowRuleBatchEntry op : batchOperation.getOperations()) {
                                failures.add(op.getTarget());
                            }
                            res = new CompletedBatchOperation(false, failures);
                            store.batchOperationComplete(FlowRuleBatchEvent.completed(request, res));
                        }
                    }
                });
                break;

            case BATCH_OPERATION_COMPLETED:
                // MASTER Node has pushed the batch down to the Device

                // Note: RULE_ADDED will be posted
                // when Flow was actually confirmed by stats reply.
                break;

            default:
                break;
            }
        }
    }

    private class FlowRuleBatchFuture implements Future<CompletedBatchOperation> {

        private final List<Future<CompletedBatchOperation>> futures;
        private final Multimap<DeviceId, FlowRuleBatchEntry> batches;
        private final AtomicReference<BatchState> state;
        private CompletedBatchOperation overall;

        public FlowRuleBatchFuture(List<Future<CompletedBatchOperation>> futures,
                Multimap<DeviceId, FlowRuleBatchEntry> batches) {
            this.futures = futures;
            this.batches = batches;
            state = new AtomicReference<FlowRuleManager.BatchState>();
            state.set(BatchState.STARTED);
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            if (state.get() == BatchState.FINISHED) {
                return false;
            }
            if (log.isDebugEnabled()) {
                log.debug("Cancelling FlowRuleBatchFuture",
                          new RuntimeException("Just printing backtrace"));
            }
            if (!state.compareAndSet(BatchState.STARTED, BatchState.CANCELLED)) {
                return false;
            }
            cleanUpBatch();
            for (Future<CompletedBatchOperation> f : futures) {
                f.cancel(mayInterruptIfRunning);
            }
            return true;
        }

        @Override
        public boolean isCancelled() {
            return state.get() == BatchState.CANCELLED;
        }

        @Override
        public boolean isDone() {
            return state.get() == BatchState.FINISHED;
        }


        @Override
        public CompletedBatchOperation get() throws InterruptedException,
            ExecutionException {

            if (isDone()) {
                return overall;
            }

            boolean success = true;
            Set<FlowRule> failed = Sets.newHashSet();
            Set<Long> failedIds = Sets.newHashSet();
            CompletedBatchOperation completed;
            for (Future<CompletedBatchOperation> future : futures) {
                completed = future.get();
                success = validateBatchOperation(failed, failedIds, completed);
            }

            return finalizeBatchOperation(success, failed, failedIds);

        }

        @Override
        public CompletedBatchOperation get(long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException,
                TimeoutException {

            if (isDone()) {
                return overall;
            }
            boolean success = true;
            Set<FlowRule> failed = Sets.newHashSet();
            Set<Long> failedIds = Sets.newHashSet();
            CompletedBatchOperation completed;
            for (Future<CompletedBatchOperation> future : futures) {
                completed = future.get(timeout, unit);
                success = validateBatchOperation(failed, failedIds, completed);
            }
            return finalizeBatchOperation(success, failed, failedIds);
        }

        private boolean validateBatchOperation(Set<FlowRule> failed,
                                               Set<Long> failedIds,
                                               CompletedBatchOperation completed) {

            if (isCancelled()) {
                throw new CancellationException();
            }
            if (!completed.isSuccess()) {
                log.warn("FlowRuleBatch failed: {}", completed);
                failed.addAll(completed.failedItems());
                failedIds.addAll(completed.failedIds());
                cleanUpBatch();
                cancelAllSubBatches();
                return false;
            }
            return true;
        }

        private void cancelAllSubBatches() {
            for (Future<CompletedBatchOperation> f : futures) {
                f.cancel(true);
            }
        }

        private CompletedBatchOperation finalizeBatchOperation(boolean success,
                                                               Set<FlowRule> failed,
                                                               Set<Long> failedIds) {
            synchronized (this) {
                if (!state.compareAndSet(BatchState.STARTED, BatchState.FINISHED)) {
                    if (state.get() == BatchState.FINISHED) {
                        return overall;
                    }
                    throw new CancellationException();
                }
                overall = new CompletedBatchOperation(success, failed, failedIds);
                return overall;
            }
        }

        private void cleanUpBatch() {
            log.debug("cleaning up batch");
            // TODO convert these into a batch?
            for (FlowRuleBatchEntry fbe : batches.values()) {
                if (fbe.getOperator() == FlowRuleOperation.ADD ||
                    fbe.getOperator() == FlowRuleOperation.MODIFY) {
                    store.deleteFlowRule(fbe.getTarget());
                } else if (fbe.getOperator() == FlowRuleOperation.REMOVE) {
                    store.removeFlowRule(new DefaultFlowEntry(fbe.getTarget()));
                    store.storeFlowRule(fbe.getTarget());
                }
            }
        }
    }
}
