package org.onlab.onos.net.flow.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.ApplicationId;
import org.onlab.onos.event.AbstractListenerRegistry;
import org.onlab.onos.event.EventDeliveryService;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.device.DeviceService;
import org.onlab.onos.net.flow.CompletedBatchOperation;
import org.onlab.onos.net.flow.DefaultFlowEntry;
import org.onlab.onos.net.flow.FlowEntry;
import org.onlab.onos.net.flow.FlowRule;
import org.onlab.onos.net.flow.FlowRuleBatchEntry;
import org.onlab.onos.net.flow.FlowRuleBatchEntry.FlowRuleOperation;
import org.onlab.onos.net.flow.FlowRuleBatchOperation;
import org.onlab.onos.net.flow.FlowRuleEvent;
import org.onlab.onos.net.flow.FlowRuleListener;
import org.onlab.onos.net.flow.FlowRuleProvider;
import org.onlab.onos.net.flow.FlowRuleProviderRegistry;
import org.onlab.onos.net.flow.FlowRuleProviderService;
import org.onlab.onos.net.flow.FlowRuleService;
import org.onlab.onos.net.flow.FlowRuleStore;
import org.onlab.onos.net.flow.FlowRuleStoreDelegate;
import org.onlab.onos.net.provider.AbstractProviderRegistry;
import org.onlab.onos.net.provider.AbstractProviderService;
import org.slf4j.Logger;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

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

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EventDeliveryService eventDispatcher;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Activate
    public void activate() {
        store.setDelegate(delegate);
        eventDispatcher.addSink(FlowRuleEvent.class, listenerRegistry);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
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
        for (int i = 0; i < flowRules.length; i++) {
            FlowRule f = flowRules[i];
            boolean local = store.storeFlowRule(f);
            if (local) {
                // TODO: aggregate all local rules and push down once?
                applyFlowRulesToProviders(f);
            }
        }
    }

    private void applyFlowRulesToProviders(FlowRule... flowRules) {
        DeviceId did = null;
        FlowRuleProvider frp = null;
        for (FlowRule f : flowRules) {
            if (!f.deviceId().equals(did)) {
                did = f.deviceId();
                final Device device = deviceService.getDevice(did);
                frp = getProvider(device.providerId());
            }
            if (frp != null) {
                frp.applyFlowRule(f);
            }
        }
    }

    @Override
    public void removeFlowRules(FlowRule... flowRules) {
        FlowRule f;
        for (int i = 0; i < flowRules.length; i++) {
            f = flowRules[i];
            boolean local = store.deleteFlowRule(f);
            if (local) {
                // TODO: aggregate all local rules and push down once?
                removeFlowRulesFromProviders(f);
            }
        }
    }

    private void removeFlowRulesFromProviders(FlowRule... flowRules) {
        DeviceId did = null;
        FlowRuleProvider frp = null;
        for (FlowRule f : flowRules) {
            if (!f.deviceId().equals(did)) {
                did = f.deviceId();
                final Device device = deviceService.getDevice(did);
                frp = getProvider(device.providerId());
            }
            if (frp != null) {
                frp.removeFlowRule(f);
            }
        }
    }

    @Override
    public void removeFlowRulesById(ApplicationId id) {
        Iterable<FlowRule> rules = getFlowRulesById(id);
        FlowRuleProvider frp;
        Device device;

        for (FlowRule f : rules) {
            store.deleteFlowRule(f);
            // FIXME: only accept request and push to provider on internal event
            device = deviceService.getDevice(f.deviceId());
            frp = getProvider(device.providerId());
            // FIXME: flows removed from store and flows removed from might diverge
            //        get rid of #removeRulesById?
            frp.removeRulesById(id, f);
        }
    }

    @Override
    public Iterable<FlowRule> getFlowRulesById(ApplicationId id) {
        return store.getFlowRulesByAppId(id);
    }

    @Override
    public Future<CompletedBatchOperation> applyBatch(
            FlowRuleBatchOperation batch) {
        Multimap<FlowRuleProvider, FlowRuleBatchEntry> batches =
                ArrayListMultimap.create();
        List<Future<CompletedBatchOperation>> futures = Lists.newArrayList();
        for (FlowRuleBatchEntry fbe : batch.getOperations()) {
            final FlowRule f = fbe.getTarget();
            final Device device = deviceService.getDevice(f.deviceId());
            final FlowRuleProvider frp = getProvider(device.providerId());
            batches.put(frp, fbe);
            switch (fbe.getOperator()) {
                case ADD:
                    store.storeFlowRule(f);
                    break;
                case REMOVE:
                    store.deleteFlowRule(f);
                    break;
                case MODIFY:
                default:
                    log.error("Batch operation type {} unsupported.", fbe.getOperator());
            }
        }
        for (FlowRuleProvider provider : batches.keySet()) {
            FlowRuleBatchOperation b =
                    new FlowRuleBatchOperation(batches.get(provider));
            Future<CompletedBatchOperation> future = provider.executeBatch(b);
            futures.add(future);
        }
        return new FlowRuleBatchFuture(futures, batches);
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
                log.info("Rule already evicted from store: {}", flowEntry);
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
            removeFlowRules(flowRule);
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
                    log.debug("Flow {} {}", flowEntry, event.type());
                    post(event);
                }
            } else {
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
            List<FlowEntry> storedRules = Lists.newLinkedList(store.getFlowEntries(deviceId));

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
        @Override
        public void notify(FlowRuleEvent event) {
            switch (event.type()) {
            case RULE_ADD_REQUESTED:
                applyFlowRulesToProviders(event.subject());
                break;
            case RULE_REMOVE_REQUESTED:
                removeFlowRulesFromProviders(event.subject());
                break;

            case RULE_ADDED:
            case RULE_REMOVED:
            case RULE_UPDATED:
                // only dispatch events related to switch
                eventDispatcher.post(event);
                break;
            default:
                break;
            }
        }
    }

    private class FlowRuleBatchFuture
        implements Future<CompletedBatchOperation> {

        private final List<Future<CompletedBatchOperation>> futures;
        private final Multimap<FlowRuleProvider, FlowRuleBatchEntry> batches;
        private final AtomicReference<BatchState> state;
        private CompletedBatchOperation overall;



        public FlowRuleBatchFuture(List<Future<CompletedBatchOperation>> futures,
                Multimap<FlowRuleProvider, FlowRuleBatchEntry> batches) {
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
            List<FlowEntry> failed = Lists.newLinkedList();
            CompletedBatchOperation completed;
            for (Future<CompletedBatchOperation> future : futures) {
                completed = future.get();
                success = validateBatchOperation(failed, completed);
            }

            return finalizeBatchOperation(success, failed);

        }

        @Override
        public CompletedBatchOperation get(long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException,
                TimeoutException {

            if (isDone()) {
                return overall;
            }
            boolean success = true;
            List<FlowEntry> failed = Lists.newLinkedList();
            CompletedBatchOperation completed;
            long start = System.nanoTime();
            long end = start + unit.toNanos(timeout);

            for (Future<CompletedBatchOperation> future : futures) {
                long now = System.nanoTime();
                long thisTimeout = end - now;
                completed = future.get(thisTimeout, TimeUnit.NANOSECONDS);
                success = validateBatchOperation(failed, completed);
            }
            return finalizeBatchOperation(success, failed);
        }

        private boolean validateBatchOperation(List<FlowEntry> failed,
                CompletedBatchOperation completed) {

            if (isCancelled()) {
                throw new CancellationException();
            }
            if (!completed.isSuccess()) {
                failed.addAll(completed.failedItems());
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
                List<FlowEntry> failed) {
            synchronized (this) {
                if (!state.compareAndSet(BatchState.STARTED, BatchState.FINISHED)) {
                    if (state.get() == BatchState.FINISHED) {
                        return overall;
                    }
                    throw new CancellationException();
                }
                overall = new CompletedBatchOperation(success, failed);
                return overall;
            }
        }

        private void cleanUpBatch() {
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
