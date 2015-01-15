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
package org.onosproject.net.flowextend.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
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
import org.onosproject.net.flow.FlowRuleBatchOperation;
import org.onosproject.net.flow.FlowRuleEvent;
import org.onosproject.net.flow.FlowRuleListener;
import org.onosproject.net.flow.FlowRuleProvider;
import org.onosproject.net.flow.FlowRuleProviderService;
import org.onosproject.net.flow.FlowRuleBatchEntry.FlowRuleOperation;
import org.onosproject.net.flow.impl.FlowRuleManager;
import org.onosproject.net.flowextend.FlowExtendCompletedOperation;
import org.onosproject.net.flowextend.FlowRuleBatchExtendEvent;
import org.onosproject.net.flowextend.FlowRuleBatchExtendRequest;
import org.onosproject.net.flowextend.FlowRuleExtendEntry;
import org.onosproject.net.flowextend.FlowRuleExtendListener;
import org.onosproject.net.flowextend.FlowRuleExtendProvider;
import org.onosproject.net.flowextend.FlowRuleExtendProviderRegistry;
import org.onosproject.net.flowextend.FlowRuleExtendProviderService;
import org.onosproject.net.flowextend.FlowRuleExtendService;
import org.onosproject.net.flowextend.FlowRuleExtendStore;
import org.onosproject.net.flowextend.FlowRuleExtendStoreDelegate;
import org.onosproject.net.provider.AbstractProviderRegistry;
import org.onosproject.net.provider.AbstractProviderService;
import org.onosproject.net.provider.ProviderId;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.slf4j.Logger;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.Collections;
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
public class FlowRuleExtendManager
        extends AbstractProviderRegistry<FlowRuleExtendProvider, FlowRuleExtendProviderService>
        implements FlowRuleExtendService, FlowRuleExtendProviderRegistry {

    enum BatchState { STARTED, FINISHED, CANCELLED };

    public static final String FLOW_RULE_NULL = "FlowRule cannot be null";
    private final Logger log = getLogger(getClass());

    private final AbstractListenerRegistry<FlowRuleEvent, FlowRuleListener>
            listenerRegistry = new AbstractListenerRegistry<>();

    private final FlowRuleExtendStoreDelegate delegate = new InternalStoreDelegate();

    private ExecutorService futureService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleExtendStore store;

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

 // Store delegate to re-post events emitted from the store.
    private class InternalStoreDelegate implements FlowRuleExtendStoreDelegate {

        // FIXME set appropriate default and make it configurable
        private static final int TIMEOUT_PER_OP = 500; // ms

        @Override
        public void notify(FlowRuleBatchExtendEvent event) {
            // TODO Auto-generated method stub
            switch (event.type()) {
            case BATCH_OPERATION_REQUESTED:
                    //send it
                    FlowRuleBatchExtendRequest flowrules = event.subject();
                    FlowRuleExtendProvider flowRuleProvider =
                                    getProvider(new ProviderId("igp","org.onosproject.provider.igp"));
                    flowRuleProvider.applyFlowRule(flowrules);
                    //do not have transaction, assume it install success
                    FlowExtendCompletedOperation result = new FlowExtendCompletedOperation(true,
                                    Collections.<FlowRuleExtendEntry>emptySet());
                    store.batchOperationComplete(FlowRuleBatchExtendEvent.completed(flowrules, result));
                    break;
            case BATCH_OPERATION_COMPLETED:
                    
                    break;
            default:
                    break;
            }
        }
    }

    @Override
    public Iterable<FlowRuleExtendEntry> getFlowEntries(DeviceId deviceId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Future<FlowExtendCompletedOperation> applyBatch(Collection<FlowRuleExtendEntry> batch) {
        // TODO group the Collection into sub-Collection by deviceId
        Multimap<DeviceId, FlowRuleExtendEntry> perDeviceBatches =
                ArrayListMultimap.create();
        List<Future<FlowExtendCompletedOperation>> futures = Lists.newArrayList();
        for (FlowRuleExtendEntry fbe : batch) {
            perDeviceBatches.put(DeviceId.deviceId(String.valueOf(fbe.getDeviceId())), fbe);
        }

        for (DeviceId deviceId : perDeviceBatches.keySet()) {
            Collection<FlowRuleExtendEntry> flows = perDeviceBatches.get(deviceId);
            Future<FlowExtendCompletedOperation> future = store.storeBatch(flows);
            futures.add(future);
        }
        return new FlowRuleBatchFuture(futures, perDeviceBatches);
    }

    @Override
    public void addListener(FlowRuleExtendListener listener) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void removeListener(FlowRuleExtendListener listener) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Iterable<OFMessage> getOFMessages(DeviceId fpid) {
        // TODO Auto-generated method stub
        return store.getOFMessages(fpid);
    }

    @Override
    protected FlowRuleExtendProviderService createProviderService(FlowRuleExtendProvider provider) {
        // TODO Auto-generated method stub
        return new InternalFlowRuleProviderService(provider);
    }

    private class FlowRuleBatchFuture implements Future<FlowExtendCompletedOperation> {

        private final List<Future<FlowExtendCompletedOperation>> futures;
        private final Multimap<DeviceId, FlowRuleExtendEntry> batches;
        private final AtomicReference<BatchState> state;
        private FlowExtendCompletedOperation overall;

        public FlowRuleBatchFuture(List<Future<FlowExtendCompletedOperation>> futures,
                Multimap<DeviceId, FlowRuleExtendEntry> batches) {
            this.futures = futures;
            this.batches = batches;
            state = new AtomicReference<FlowRuleExtendManager.BatchState>();
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
            for (Future<FlowExtendCompletedOperation> f : futures) {
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
        public FlowExtendCompletedOperation get() throws InterruptedException,
            ExecutionException {

            if (isDone()) {
                return overall;
            }

            boolean success = true;
            Set<FlowRuleExtendEntry> failed = Sets.newHashSet();
            FlowExtendCompletedOperation completed;
            for (Future<FlowExtendCompletedOperation> future : futures) {
                completed = future.get();
                success = validateBatchOperation(failed, completed);
            }
            return finalizeBatchOperation(success, failed);
        }

        @Override
        public FlowExtendCompletedOperation get(long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException,
                TimeoutException {

            if (isDone()) {
                return overall;
            }
            boolean success = true;
            Set<FlowRuleExtendEntry> failed = Sets.newHashSet();
            FlowExtendCompletedOperation completed;
            for (Future<FlowExtendCompletedOperation> future : futures) {
                completed = future.get(timeout, unit);
                success = validateBatchOperation(failed, completed);
            }
            return finalizeBatchOperation(success, failed);
        }

        private boolean validateBatchOperation(Set<FlowRuleExtendEntry> failed,
                                               FlowExtendCompletedOperation completed) {

            if (isCancelled()) {
                throw new CancellationException();
            }
            if (!completed.isSuccess()) {
                log.warn("FlowRuleBatch failed: {}", completed);
                failed.addAll(completed.failedItems());
                cleanUpBatch();
                cancelAllSubBatches();
                return false;
            }
            return true;
        }

        private void cancelAllSubBatches() {
            for (Future<FlowExtendCompletedOperation> f : futures) {
                f.cancel(true);
            }
        }

        private FlowExtendCompletedOperation finalizeBatchOperation(boolean success,
                                                               Set<FlowRuleExtendEntry> failed) {
            synchronized (this) {
                if (!state.compareAndSet(BatchState.STARTED, BatchState.FINISHED)) {
                    if (state.get() == BatchState.FINISHED) {
                        return overall;
                    }
                    throw new CancellationException();
                }
                overall = new FlowExtendCompletedOperation(success, failed);
                return overall;
            }
        }

        private void cleanUpBatch() {
        }
    }
    private class InternalFlowRuleProviderService extends AbstractProviderService<FlowRuleExtendProvider>
    implements FlowRuleExtendProviderService {

        protected InternalFlowRuleProviderService(FlowRuleExtendProvider provider) {
            super(provider);
            // TODO Auto-generated constructor stub
        }
    }
}
