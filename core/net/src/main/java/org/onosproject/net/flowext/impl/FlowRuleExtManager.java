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
package org.onosproject.net.flowext.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.event.AbstractListenerRegistry;
import org.onosproject.event.EventDeliveryService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flowext.FlowExtCompletedOperation;
import org.onosproject.net.flowext.FlowRuleBatchExtEvent;
import org.onosproject.net.flowext.FlowRuleBatchExtRequest;
import org.onosproject.net.flowext.FlowRuleExtEntry;
import org.onosproject.net.flowext.FlowRuleExtEvent;
import org.onosproject.net.flowext.FlowRuleExtListener;
import org.onosproject.net.flowext.FlowRuleExtProvider;
import org.onosproject.net.flowext.FlowRuleExtProviderRegistry;
import org.onosproject.net.flowext.FlowRuleExtProviderService;
import org.onosproject.net.flowext.FlowRuleExtService;
import org.onosproject.net.flowext.FlowRuleExtStore;
import org.onosproject.net.flowext.FlowRuleExtStoreDelegate;
import org.onosproject.net.provider.AbstractProviderRegistry;
import org.onosproject.net.provider.AbstractProviderService;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;

import com.esotericsoftware.kryo.Serializer;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static org.onlab.util.Tools.namedThreads;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides implementation of the flow NB &amp; SB APIs.
 */
@Component(immediate = true)
@Service
public class FlowRuleExtManager
        extends AbstractProviderRegistry<FlowRuleExtProvider, FlowRuleExtProviderService>
        implements FlowRuleExtService, FlowRuleExtProviderRegistry {

    enum BatchState { STARTED, FINISHED, CANCELLED };

    public static final String FLOW_RULE_NULL = "FlowRule cannot be null";
    private final Logger log = getLogger(getClass());

    private final AbstractListenerRegistry<FlowRuleExtEvent, FlowRuleExtListener>
            listenerRegistry = new AbstractListenerRegistry<>();

    private final FlowRuleExtStoreDelegate delegate = new InternalStoreDelegate();

    private ExecutorService futureService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleExtStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EventDeliveryService eventDispatcher;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Activate
    public void activate() {
        futureService =
                Executors.newFixedThreadPool(32, namedThreads("provider-future-listeners-%d"));
        store.setDelegate(delegate);
        eventDispatcher.addSink(FlowRuleExtEvent.class, listenerRegistry);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        futureService.shutdownNow();
        store.unsetDelegate(delegate);
        eventDispatcher.removeSink(FlowRuleExtEvent.class);
        log.info("Stopped");
    }

 // Store delegate to re-post events emitted from the store.
    private class InternalStoreDelegate implements FlowRuleExtStoreDelegate {

        // FIXME set appropriate default and make it configurable
        private static final int TIMEOUT_PER_OP = 500; // ms

        @Override
        public void notify(FlowRuleBatchExtEvent event) {
            // TODO Auto-generated method stub
            switch (event.type()) {
            case BATCH_OPERATION_REQUESTED:
                    //send it
                    FlowRuleBatchExtRequest flowrules = event.subject();
                    FlowRuleExtProvider flowRuleProvider =
                                    getProvider(new ProviderId("igp", "org.onosproject.provider.igp"));
                    flowRuleProvider.applyFlowRule(flowrules);
                    //do not have transaction, assume it install success
                    FlowExtCompletedOperation result = new FlowExtCompletedOperation(true,
                                    Collections.<FlowRuleExtEntry>emptySet());
                    store.batchOperationComplete(FlowRuleBatchExtEvent.completed(flowrules, result));
                    break;
            case BATCH_OPERATION_COMPLETED:

                    break;
            default:
                    break;
            }
        }
    }

    @Override
    public Iterable<FlowRuleExtEntry> getFlowEntries(DeviceId deviceId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Future<FlowExtCompletedOperation> applyBatch(Collection<FlowRuleExtEntry> batch) {
        // TODO group the Collection into sub-Collection by deviceId
        Multimap<DeviceId, FlowRuleExtEntry> perDeviceBatches =
                ArrayListMultimap.create();
        List<Future<FlowExtCompletedOperation>> futures = Lists.newArrayList();
        for (FlowRuleExtEntry fbe : batch) {
            perDeviceBatches.put(fbe.getDeviceId(), fbe);
        }

        for (DeviceId deviceId : perDeviceBatches.keySet()) {
            Collection<FlowRuleExtEntry> flows = perDeviceBatches.get(deviceId);
            Future<FlowExtCompletedOperation> future = store.storeBatch(flows);
            futures.add(future);
        }
        return new FlowRuleBatchFuture(futures, perDeviceBatches);
    }

    @Override
    public void addListener(FlowRuleExtListener listener) {
        // TODO Auto-generated method stub
    }

    @Override
    public void removeListener(FlowRuleExtListener listener) {
        // TODO Auto-generated method stub
    }

    @Override
    public Iterable<?> getExtMessages(DeviceId deviceId) {
        // TODO Auto-generated method stub
        return store.getExtMessages(deviceId);
    }

    @Override
    protected FlowRuleExtProviderService createProviderService(FlowRuleExtProvider provider) {
        // TODO Auto-generated method stub
        return new InternalFlowRuleProviderService(provider);
    }

    private class FlowRuleBatchFuture implements Future<FlowExtCompletedOperation> {

        private final List<Future<FlowExtCompletedOperation>> futures;
        private final Multimap<DeviceId, FlowRuleExtEntry> batches;
        private final AtomicReference<BatchState> state;
        private FlowExtCompletedOperation overall;

        public FlowRuleBatchFuture(List<Future<FlowExtCompletedOperation>> futures,
                Multimap<DeviceId, FlowRuleExtEntry> batches) {
            this.futures = futures;
            this.batches = batches;
            state = new AtomicReference<FlowRuleExtManager.BatchState>();
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
            for (Future<FlowExtCompletedOperation> f : futures) {
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
        public FlowExtCompletedOperation get() throws InterruptedException,
            ExecutionException {

            if (isDone()) {
                return overall;
            }
            boolean success = true;
            Set<FlowRuleExtEntry> failed = Sets.newHashSet();
            FlowExtCompletedOperation completed;
            for (Future<FlowExtCompletedOperation> future : futures) {
                completed = future.get();
                success = validateBatchOperation(failed, completed);
            }
            return finalizeBatchOperation(success, failed);
        }

        @Override
        public FlowExtCompletedOperation get(long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException,
                TimeoutException {

            if (isDone()) {
                return overall;
            }
            boolean success = true;
            Set<FlowRuleExtEntry> failed = Sets.newHashSet();
            FlowExtCompletedOperation completed;
            for (Future<FlowExtCompletedOperation> future : futures) {
                completed = future.get(timeout, unit);
                success = validateBatchOperation(failed, completed);
            }
            return finalizeBatchOperation(success, failed);
        }

        private boolean validateBatchOperation(Set<FlowRuleExtEntry> failed,
                                               FlowExtCompletedOperation completed) {

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
            for (Future<FlowExtCompletedOperation> f : futures) {
                f.cancel(true);
            }
        }

        private FlowExtCompletedOperation finalizeBatchOperation(boolean success,
                                                               Set<FlowRuleExtEntry> failed) {
            synchronized (this) {
                if (!state.compareAndSet(BatchState.STARTED, BatchState.FINISHED)) {
                    if (state.get() == BatchState.FINISHED) {
                        return overall;
                    }
                    throw new CancellationException();
                }
                overall = new FlowExtCompletedOperation(success, failed);
                return overall;
            }
        }

        private void cleanUpBatch() {
        }
    }

    private class InternalFlowRuleProviderService extends AbstractProviderService<FlowRuleExtProvider>
    implements FlowRuleExtProviderService {

        protected InternalFlowRuleProviderService(FlowRuleExtProvider provider) {
            super(provider);
            // TODO Auto-generated constructor stub
        }
    }

    @Override
    public void registerSerializer(Class<?> classT, Serializer<?> serializer) {
        // TODO Auto-generated method stub
        store.registerSerializer(classT, serializer);
    }
}
