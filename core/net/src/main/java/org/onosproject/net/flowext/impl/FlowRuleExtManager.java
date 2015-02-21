/*
 * Copyright 2015 Open Networking Laboratory
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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
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
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleBatchEntry;
import org.onosproject.net.flow.FlowRuleBatchEvent;
import org.onosproject.net.flow.FlowRuleBatchRequest;
import org.onosproject.net.flow.FlowRuleEvent;
import org.onosproject.net.flow.FlowRuleListener;
import org.onosproject.net.flow.FlowRuleProvider;
import org.onosproject.net.flow.impl.FlowRuleManager;
import org.onosproject.net.flowext.FlowExtCompletedOperation;
import org.onosproject.net.flowext.FlowRuleExtRouter;
import org.onosproject.net.flowext.FlowRuleExtRouterListener;
import org.onosproject.net.flowext.FlowRuleExtService;
import org.slf4j.Logger;

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

import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Experimental extension to the flow rule subsystem; still under development.
 */
@Component(immediate = true, enabled = false)
@Service
public class FlowRuleExtManager extends FlowRuleManager
        implements FlowRuleExtService {

    enum BatchState {
        STARTED, FINISHED, CANCELLED
    }

    public static final String FLOW_RULE_NULL = "FlowRule cannot be null";
    private final Logger log = getLogger(getClass());

    private final AbstractListenerRegistry<FlowRuleEvent, FlowRuleListener>
            listenerRegistry = new AbstractListenerRegistry<>();

    private ExecutorService futureService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleExtRouter router;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EventDeliveryService eventDispatcher;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    InternalFlowRuleExtRouterListener routerListener = new InternalFlowRuleExtRouterListener();

    @Activate
    public void activate() {
        futureService = Executors.newFixedThreadPool(
                32, groupedThreads("onos/flow", "provider-future-listeners-%d"));
        eventDispatcher.addSink(FlowRuleEvent.class, listenerRegistry);
        router.addListener(routerListener);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        futureService.shutdownNow();
        eventDispatcher.removeSink(FlowRuleEvent.class);
        router.removeListener(routerListener);
        log.info("Stopped");
    }

    /**
     * Applies a batch operation of FlowRules.
     * this batch can be divided into many sub-batch by deviceId
     *
     * @param batch batch operation to apply
     * @return future indicating the state of the batch operation
     */
    @Override
    public Future<FlowExtCompletedOperation> applyBatch(FlowRuleBatchRequest batch) {
        // TODO group the Collection into sub-Collection by deviceId
        Multimap<DeviceId, FlowRuleBatchEntry> perDeviceBatches = ArrayListMultimap
                .create();
        for (FlowRuleBatchEntry fbe : batch.ops()) {
            FlowRule flowRule = fbe.target();
            perDeviceBatches.put(flowRule.deviceId(), fbe);
        }

        List<Future<FlowExtCompletedOperation>> futures = Lists.newArrayList();
        for (DeviceId deviceId : perDeviceBatches.keySet()) {
            Collection<FlowRuleBatchEntry> flows = perDeviceBatches.get(deviceId);
            //FIXME if there can be collisions, than converting the collection to a set will drop flow rules
            FlowRuleBatchRequest subBatch = new FlowRuleBatchRequest(batch.batchId(), Sets.newHashSet(flows));
            Future<FlowExtCompletedOperation> future = router.applySubBatch(subBatch);
            futures.add(future);
        }
        return new FlowRuleBatchFuture(batch.batchId(), futures);
    }

    /**
     * Batch futures include all flow extension entries in one batch.
     * Using for transaction and will use in next-step.
     */
    private class FlowRuleBatchFuture
            implements Future<FlowExtCompletedOperation> {

        private final List<Future<FlowExtCompletedOperation>> futures;
        private final long batchId;
        private final AtomicReference<BatchState> state;
        private FlowExtCompletedOperation overall;

        public FlowRuleBatchFuture(long batchId, List<Future<FlowExtCompletedOperation>> futures) {
            this.futures = futures;
            this.batchId = batchId;
            state = new AtomicReference<FlowRuleExtManager.BatchState>();
            state.set(BatchState.STARTED);
        }

        /**
         * Attempts to cancel execution of this task.
         *
         * @param mayInterruptIfRunning {@code true} if the thread executing this
         *                              task should be interrupted; otherwise, in-progress tasks are allowed
         *                              to complete
         * @return {@code false} if the task could not be cancelled,
         * typically because it has already completed normally;
         * {@code true} otherwise
         */
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

        /**
         * Judge whether the task cancelled completely.
         *
         * @return {@code true} if this task was cancelled before it completed
         */
        @Override
        public boolean isCancelled() {
            return state.get() == BatchState.CANCELLED;
        }

        /**
         * Judge whether the task finished completely.
         *
         * @return {@code true} if this task completed
         */
        @Override
        public boolean isDone() {
            return state.get() == BatchState.FINISHED;
        }

        /**
         * Get the result of apply flow extension rules.
         * If the task isn't finished, the thread block here.
         */
        @Override
        public FlowExtCompletedOperation get()
                throws InterruptedException, ExecutionException {

            if (isDone()) {
                return overall;
            }
            boolean success = true;
            Set<FlowRule> failed = Sets.newHashSet();
            FlowExtCompletedOperation completed;
            for (Future<FlowExtCompletedOperation> future : futures) {
                completed = future.get();
                success = validateBatchOperation(failed, completed);
            }
            return finalizeBatchOperation(success, failed);
        }

        /**
         * Waits if necessary for at most the given time for the computation
         * to complete, and then retrieves its result, if available. In here,
         * the maximum of time out is sum of given time for every computation.
         *
         * @param timeout the maximum time to wait
         * @param unit    the time unit of the timeout argument
         * @return the computed result
         * @throws CancellationException if the computation was cancelled
         * @throws ExecutionException    if the computation threw an
         *                               exception
         * @throws InterruptedException  if the current thread was interrupted
         *                               while waiting
         * @throws TimeoutException      if the wait timed out
         */
        @Override
        public FlowExtCompletedOperation get(long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException,
                TimeoutException {

            if (isDone()) {
                return overall;
            }
            boolean success = true;
            Set<FlowRule> failed = Sets.newHashSet();
            FlowExtCompletedOperation completed;
            for (Future<FlowExtCompletedOperation> future : futures) {
                completed = future.get(timeout, unit);
                success = validateBatchOperation(failed, completed);
            }
            return finalizeBatchOperation(success, failed);
        }

        /**
         * Confirm whether the batch operation success.
         *
         * @param failed    using to populate failed entries
         * @param completed the result of apply flow extension entries
         * @return {@code true} if all entries applies successful
         */
        private boolean validateBatchOperation(Set<FlowRule> failed,
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

        /**
         * Once one subBatch failed, cancel the rest of them.
         */
        private void cancelAllSubBatches() {
            for (Future<FlowExtCompletedOperation> f : futures) {
                f.cancel(true);
            }
        }

        /**
         * Construct the result of batch operation.
         *
         * @param success the result of batch operation
         * @param failed  the failed entries of batch operation
         * @return FlowExtCompletedOperation of batch operation
         */
        private FlowExtCompletedOperation finalizeBatchOperation(boolean success,
                                                                 Set<FlowRule> failed) {
            synchronized (this) {
                if (!state.compareAndSet(BatchState.STARTED,
                                         BatchState.FINISHED)) {
                    if (state.get() == BatchState.FINISHED) {
                        return overall;
                    }
                    throw new CancellationException();
                }
                overall = new FlowExtCompletedOperation(batchId, success, failed);
                return overall;
            }
        }

        private void cleanUpBatch() {
        }
    }

    /**
     * South Bound API to south plug-in.
     */
    private class InternalFlowRuleExtRouterListener
            implements FlowRuleExtRouterListener {
        @Override
        public void notify(FlowRuleBatchEvent event) {
            // Request has been forwarded to MASTER Node
            for (FlowRuleBatchEntry entry : event.subject().ops()) {
                switch (entry.operator()) {
                    case ADD:
                        eventDispatcher
                                .post(new FlowRuleEvent(FlowRuleEvent.Type.RULE_ADD_REQUESTED,
                                                        entry.target()));
                        break;
                    // FALLTHROUGH
                    case REMOVE:
                    case MODIFY:
                    default:
                        // TODO not implemented
                        break;
                }
            }
            // send it
            FlowRuleProvider flowRuleProvider = getProvider(event.subject().ops()
                                                                    .iterator().next().target().deviceId());
            // TODO we may want to specify a deviceId
            flowRuleProvider.executeBatch(event.subject().asBatchOperation(null));
            // do not have transaction, assume it install success
            // temporarily
            FlowExtCompletedOperation result = new FlowExtCompletedOperation(
                    event.subject().batchId(), true, Collections.emptySet());
            futureService.submit(() -> {
                router.batchOperationComplete(FlowRuleBatchEvent
                                                      .completed(event.subject(), result));
            });
        }
    }
}
