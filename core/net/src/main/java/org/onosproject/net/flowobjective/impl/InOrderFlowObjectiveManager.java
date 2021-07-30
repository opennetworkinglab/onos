/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.net.flowobjective.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalListeners;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import org.onlab.util.Tools;
import org.onlab.util.Tools.LogLevel;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flowobjective.FilteringObjQueueKey;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.FlowObjectiveStoreDelegate;
import org.onosproject.net.flowobjective.ForwardingObjQueueKey;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjQueueKey;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveContext;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.flowobjective.ObjectiveEvent;
import org.onosproject.net.flowobjective.ObjectiveQueueKey;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.OsgiPropertyConstants.IFOM_OBJ_TIMEOUT_MS;
import static org.onosproject.net.OsgiPropertyConstants.IFOM_OBJ_TIMEOUT_MS_DEFAULT;

/**
 * Provides implementation of the flow objective programming service.
 */
@Component(
        immediate = true,
        service = FlowObjectiveService.class,
        property = {
                IFOM_OBJ_TIMEOUT_MS + ":Integer=" + IFOM_OBJ_TIMEOUT_MS_DEFAULT
        }
)
public class InOrderFlowObjectiveManager extends FlowObjectiveManager {
    private final Logger log = LoggerFactory.getLogger(getClass());

    /** Objective timeout. */
    int objectiveTimeoutMs = IFOM_OBJ_TIMEOUT_MS_DEFAULT;

    private Cache<FilteringObjQueueKey, Objective> filtObjQueueHead;
    private Cache<ForwardingObjQueueKey, Objective> fwdObjQueueHead;
    private Cache<NextObjQueueKey, Objective> nextObjQueueHead;
    private ScheduledExecutorService cacheCleaner;
    private ExecutorService filtCacheEventExecutor;
    private ExecutorService fwdCacheEventExecutor;
    private ExecutorService nextCacheEventExecutor;

    private ListMultimap<FilteringObjQueueKey, Objective> filtObjQueue =
            Multimaps.synchronizedListMultimap(ArrayListMultimap.create());
    private ListMultimap<ForwardingObjQueueKey, Objective> fwdObjQueue =
            Multimaps.synchronizedListMultimap(ArrayListMultimap.create());
    private ListMultimap<NextObjQueueKey, Objective> nextObjQueue =
            Multimaps.synchronizedListMultimap(ArrayListMultimap.create());

    final FlowObjectiveStoreDelegate delegate = new InternalStoreDelegate();

    final RemovalListener<ObjectiveQueueKey, Objective> removalListener = notification -> {
        Objective obj = notification.getValue();
        switch (notification.getCause()) {
            case EXPIRED:
            case COLLECTED:
            case SIZE:
                obj.context().ifPresent(c -> c.onError(obj, ObjectiveError.INSTALLATIONTIMEOUT));
                break;
            case EXPLICIT: // No action when the objective completes correctly
            case REPLACED: // No action when a pending forward or next objective gets executed
            default:
                break;
        }
    };

    @Activate
    protected void activate(ComponentContext context) {
        super.activate(context);

        cfgService.registerProperties(InOrderFlowObjectiveManager.class);

        filtCacheEventExecutor = newSingleThreadExecutor(groupedThreads("onos/flowobj", "cache-event-filt", log));
        fwdCacheEventExecutor = newSingleThreadExecutor(groupedThreads("onos/flowobj", "cache-event-fwd", log));
        nextCacheEventExecutor = newSingleThreadExecutor(groupedThreads("onos/flowobj", "cache-event-next", log));

        filtObjQueueHead = CacheBuilder.newBuilder()
                .expireAfterWrite(objectiveTimeoutMs, TimeUnit.MILLISECONDS)
                .removalListener(RemovalListeners.asynchronous(removalListener, filtCacheEventExecutor))
                .build();
        fwdObjQueueHead = CacheBuilder.newBuilder()
                .expireAfterWrite(objectiveTimeoutMs, TimeUnit.MILLISECONDS)
                .removalListener(RemovalListeners.asynchronous(removalListener, fwdCacheEventExecutor))
                .build();
        nextObjQueueHead = CacheBuilder.newBuilder()
                .expireAfterWrite(objectiveTimeoutMs, TimeUnit.MILLISECONDS)
                .removalListener(RemovalListeners.asynchronous(removalListener, nextCacheEventExecutor))
                .build();

        cacheCleaner = newSingleThreadScheduledExecutor(groupedThreads("onos/flowobj", "cache-cleaner", log));
        cacheCleaner.scheduleAtFixedRate(() -> {
            filtObjQueueHead.cleanUp();
            fwdObjQueueHead.cleanUp();
            nextObjQueueHead.cleanUp();
        }, 0, objectiveTimeoutMs, TimeUnit.MILLISECONDS);

        // Replace store delegate to make sure pendingForward and pendingNext are resubmitted to
        // execute()
        flowObjectiveStore.unsetDelegate(super.delegate);
        flowObjectiveStore.setDelegate(delegate);
    }

    @Deactivate
    protected void deactivate() {
        cfgService.unregisterProperties(getClass(), false);

        cacheCleaner.shutdown();
        clearQueue();

        filtCacheEventExecutor.shutdown();
        fwdCacheEventExecutor.shutdown();
        nextCacheEventExecutor.shutdown();

        super.deactivate();
        // Due to the check in the AbstractStore we have to pass the right instance
        // to perform a correct clean up. The unset delegate in the super class
        // will not have any effect
        flowObjectiveStore.unsetDelegate(delegate);
    }

    /**
     * Extracts properties from the component configuration context.
     *
     * @param context the component context
     */
    @Override
    protected void readComponentConfiguration(ComponentContext context) {
        super.readComponentConfiguration(context);

        // objective timeout handling
        String propertyValue = Tools.get(context.getProperties(), IFOM_OBJ_TIMEOUT_MS);
        int newObjectiveTimeoutMs = isNullOrEmpty(propertyValue) ?
                objectiveTimeoutMs : Integer.parseInt(propertyValue);
        if (newObjectiveTimeoutMs != objectiveTimeoutMs && newObjectiveTimeoutMs > 0) {
            objectiveTimeoutMs = newObjectiveTimeoutMs;
            log.info("Reconfigured timeout of the objectives to {}", objectiveTimeoutMs);
            // Recreates the queues
            if (filtObjQueueHead != null) {
                filtObjQueueHead.invalidateAll();
                filtObjQueueHead = null;
            }
            filtObjQueueHead = CacheBuilder.newBuilder()
                    .expireAfterWrite(objectiveTimeoutMs, TimeUnit.MILLISECONDS)
                    .removalListener(RemovalListeners.asynchronous(removalListener, filtCacheEventExecutor))
                    .build();
            if (fwdObjQueueHead != null) {
                fwdObjQueueHead.invalidateAll();
                fwdObjQueueHead = null;
            }
            fwdObjQueueHead = CacheBuilder.newBuilder()
                    .expireAfterWrite(objectiveTimeoutMs, TimeUnit.MILLISECONDS)
                    .removalListener(RemovalListeners.asynchronous(removalListener, fwdCacheEventExecutor))
                    .build();
            if (nextObjQueueHead != null) {
                nextObjQueueHead.invalidateAll();
                nextObjQueueHead = null;
            }
            nextObjQueueHead = CacheBuilder.newBuilder()
                    .expireAfterWrite(objectiveTimeoutMs, TimeUnit.MILLISECONDS)
                    .removalListener(RemovalListeners.asynchronous(removalListener, nextCacheEventExecutor))
                    .build();
            // Restart the cleanup thread
            if (cacheCleaner != null) {
                cacheCleaner.shutdownNow();
                cacheCleaner = null;
            }
            cacheCleaner = newSingleThreadScheduledExecutor(groupedThreads("onos/flowobj", "cache-cleaner", log));
            cacheCleaner.scheduleAtFixedRate(() -> {
                filtObjQueueHead.cleanUp();
                fwdObjQueueHead.cleanUp();
                nextObjQueueHead.cleanUp();
            }, 0, objectiveTimeoutMs, TimeUnit.MILLISECONDS);
        }
    }


    /**
     * Processes given objective on given device.
     * Objectives submitted through this method are guaranteed to be executed in order.
     *
     * @param deviceId Device ID
     * @param originalObjective Flow objective to be executed
     */
    private void process(DeviceId deviceId, Objective originalObjective) {
        // Inject ObjectiveContext such that we can get notified when it is completed
        Objective.Builder objBuilder = originalObjective.copy();
        Optional<ObjectiveContext> originalContext = originalObjective.context();
        ObjectiveContext context = new InOrderObjectiveContext(deviceId, originalContext.orElse(null));

        // Preserve Objective.Operation
        Objective objective;
        switch (originalObjective.op()) {
            case ADD:
                objective = objBuilder.add(context);
                break;
            case ADD_TO_EXISTING:
                objective = ((NextObjective.Builder) objBuilder).addToExisting(context);
                break;
            case REMOVE:
                objective = objBuilder.remove(context);
                break;
            case REMOVE_FROM_EXISTING:
                objective = ((NextObjective.Builder) objBuilder).removeFromExisting(context);
                break;
            case MODIFY:
                objective = ((NextObjective.Builder) objBuilder).modify(context);
                break;
            case VERIFY:
                objective = ((NextObjective.Builder) objBuilder).verify(context);
                break;
            default:
                log.error("Unknown flow objecitve operation {}", originalObjective.op());
                return;
        }

        enqueue(deviceId, objective);
    }

    @Override
    public void filter(DeviceId deviceId, FilteringObjective filteringObjective) {
        process(deviceId, filteringObjective);
    }

    @Override
    public void forward(DeviceId deviceId, ForwardingObjective forwardingObjective) {
        process(deviceId, forwardingObjective);
    }

    @Override
    public void next(DeviceId deviceId, NextObjective nextObjective) {
        process(deviceId, nextObjective);
    }

    @Override
    public ListMultimap<FilteringObjQueueKey, Objective> getFilteringObjQueue() {
        return filtObjQueue;
    }

    @Override
    public ListMultimap<ForwardingObjQueueKey, Objective> getForwardingObjQueue() {
        return fwdObjQueue;
    }

    @Override
    public ListMultimap<NextObjQueueKey, Objective> getNextObjQueue() {
        return nextObjQueue;
    }

    @Override
    public Map<FilteringObjQueueKey, Objective> getFilteringObjQueueHead() {
        return filtObjQueueHead.asMap();
    }

    @Override
    public Map<ForwardingObjQueueKey, Objective> getForwardingObjQueueHead() {
        return fwdObjQueueHead.asMap();
    }

    @Override
    public Map<NextObjQueueKey, Objective> getNextObjQueueHead() {
        return nextObjQueueHead.asMap();
    }

    @Override
    public void clearQueue() {
        filtObjQueueHead.invalidateAll();
        fwdObjQueueHead.invalidateAll();
        nextObjQueueHead.invalidateAll();

        filtObjQueueHead.cleanUp();
        fwdObjQueueHead.cleanUp();
        nextObjQueueHead.cleanUp();

        filtObjQueue.clear();
        fwdObjQueue.clear();
        nextObjQueue.clear();
    }

    /**
     * Enqueue flow objective. Execute the flow objective if there is no pending objective ahead.
     *
     * @param deviceId Device ID
     * @param obj Flow objective
     */
    private synchronized void enqueue(DeviceId deviceId, Objective obj) {
        int queueSize;
        int priority = obj.priority();

        LogLevel logLevel = (obj.op() == Objective.Operation.VERIFY) ? LogLevel.TRACE : LogLevel.DEBUG;
        Tools.log(log, logLevel, "Enqueue {}", obj);

        if (obj instanceof FilteringObjective) {
            FilteringObjQueueKey k = new FilteringObjQueueKey(deviceId, priority, ((FilteringObjective) obj).key());
            filtObjQueue.put(k, obj);
            queueSize = filtObjQueue.get(k).size();
        } else if (obj instanceof ForwardingObjective) {
            ForwardingObjQueueKey k =
                    new ForwardingObjQueueKey(deviceId, priority, ((ForwardingObjective) obj).selector());
            fwdObjQueue.put(k, obj);
            queueSize = fwdObjQueue.get(k).size();
        } else if (obj instanceof NextObjective) {
            NextObjQueueKey k = new NextObjQueueKey(deviceId, obj.id());
            nextObjQueue.put(k, obj);
            queueSize = nextObjQueue.get(k).size();
        } else {
            log.error("Unknown flow objective instance: {}", obj.getClass().getName());
            return;
        }
        log.trace("{} queue size {}", obj.getClass().getSimpleName(), queueSize);

        // Execute immediately if there is no pending obj ahead
        if (queueSize == 1) {
            execute(deviceId, obj);
        }
    }

    /**
     * Dequeue flow objective. Execute the next flow objective in the queue, if any.
     *
     * @param deviceId Device ID
     * @param obj Flow objective
     * @param error ObjectiveError that triggers this dequeue. Null if this is not triggered by an error.
     */
    private synchronized void dequeue(DeviceId deviceId, Objective obj, ObjectiveError error) {
        List<Objective> remaining;
        int priority = obj.priority();

        LogLevel logLevel = (obj.op() == Objective.Operation.VERIFY) ? LogLevel.TRACE : LogLevel.DEBUG;
        Tools.log(log, logLevel, "Dequeue {}", obj);

        if (obj instanceof FilteringObjective) {
            FilteringObjQueueKey k = new FilteringObjQueueKey(deviceId, priority, ((FilteringObjective) obj).key());
            if (!Objects.equals(ObjectiveError.INSTALLATIONTIMEOUT, error)) {
                filtObjQueueHead.invalidate(k);
            }
            filtObjQueue.remove(k, obj);
            remaining = filtObjQueue.get(k);
        } else if (obj instanceof ForwardingObjective) {
            ForwardingObjQueueKey k =
                    new ForwardingObjQueueKey(deviceId, priority, ((ForwardingObjective) obj).selector());
            if (!Objects.equals(ObjectiveError.INSTALLATIONTIMEOUT, error)) {
                fwdObjQueueHead.invalidate(k);
            }
            fwdObjQueue.remove(k, obj);
            remaining = fwdObjQueue.get(k);
        } else if (obj instanceof NextObjective) {
            if (error != null) {
                // Remove pendingForwards and pendingNexts if next objective failed
                Set<PendingFlowObjective> removedForwards = pendingForwards.remove(obj.id());
                List<PendingFlowObjective> removedNexts = pendingNexts.remove(obj.id());

                if (removedForwards != null) {
                    removedForwards.stream().map(PendingFlowObjective::flowObjective)
                            .forEach(pendingObj -> pendingObj.context().ifPresent(c ->
                                    c.onError(pendingObj, error)));
                }
                if (removedNexts != null) {
                    removedNexts.stream().map(PendingFlowObjective::flowObjective)
                            .forEach(pendingObj -> pendingObj.context().ifPresent(c ->
                                    c.onError(pendingObj, error)));
                }
            }
            NextObjQueueKey k = new NextObjQueueKey(deviceId, obj.id());
            if (!Objects.equals(ObjectiveError.INSTALLATIONTIMEOUT, error)) {
                nextObjQueueHead.invalidate(k);
            }
            nextObjQueue.remove(k, obj);
            remaining = nextObjQueue.get(k);
        } else {
            log.error("Unknown flow objective instance: {}", obj.getClass().getName());
            return;
        }
        log.trace("{} queue size {}", obj.getClass().getSimpleName(), remaining.size());

        // Submit the next one in the queue, if any
        if (remaining.size() > 0) {
            execute(deviceId, remaining.get(0));
        }
    }

    /**
     * Submit the flow objective. Starting from this point on, the execution order is not guaranteed.
     * Therefore we must be certain that this method is called in-order.
     *
     * @param deviceId Device ID
     * @param obj Flow objective
     */
    private void execute(DeviceId deviceId, Objective obj) {
        LogLevel logLevel = (obj.op() == Objective.Operation.VERIFY) ? LogLevel.TRACE : LogLevel.DEBUG;
        Tools.log(log, logLevel, "Submit objective installer, deviceId {}, obj {}", deviceId, obj);

        int priority = obj.priority();
        if (obj instanceof FilteringObjective) {
            FilteringObjQueueKey k = new FilteringObjQueueKey(deviceId, priority, ((FilteringObjective) obj).key());
            filtObjQueueHead.put(k, obj);
            super.filter(deviceId, (FilteringObjective) obj);
        } else if (obj instanceof ForwardingObjective) {
            ForwardingObjQueueKey k =
                    new ForwardingObjQueueKey(deviceId, priority, ((ForwardingObjective) obj).selector());
            fwdObjQueueHead.put(k, obj);
            super.forward(deviceId, (ForwardingObjective) obj);
        } else if (obj instanceof NextObjective) {
            NextObjQueueKey k = new NextObjQueueKey(deviceId, obj.id());
            nextObjQueueHead.put(k, obj);
            super.next(deviceId, (NextObjective) obj);
        } else {
            log.error("Unknown flow objective instance: {}", obj.getClass().getName());
        }
    }

    private class InternalStoreDelegate implements FlowObjectiveStoreDelegate {
        @Override
        public void notify(ObjectiveEvent event) {
            if (event.type() == ObjectiveEvent.Type.ADD) {
                log.debug("Received notification of obj event {}", event);
                Set<PendingFlowObjective> pending;

                // first send all pending flows
                synchronized (pendingForwards) {
                    // needs to be synchronized for queueObjective lookup
                    pending = pendingForwards.remove(event.subject());
                }
                if (pending == null) {
                    log.debug("No forwarding objectives pending for this obj event {}", event);
                } else {
                    log.debug("Processing {} pending forwarding objectives for nextId {}",
                            pending.size(), event.subject());
                    // execute pending forwards one by one
                    pending.forEach(p -> execute(p.deviceId(), p.flowObjective()));
                }

                // now check for pending next-objectives
                // Note: This is still necessary despite the existence of in-order execution.
                //       Since the in-order execution does not handle the case of
                //       ADD_TO_EXISTING coming before ADD
                List<PendingFlowObjective> pendNexts;
                synchronized (pendingNexts) {
                    // needs to be synchronized for queueObjective lookup
                    pendNexts = pendingNexts.remove(event.subject());
                }
                if (pendNexts == null) {
                    log.debug("No next objectives pending for this obj event {}", event);
                } else {
                    log.debug("Processing {} pending next objectives for nextId {}",
                            pendNexts.size(), event.subject());
                    // execute pending nexts one by one
                    pendNexts.forEach(p -> execute(p.deviceId(), p.flowObjective()));
                }
            }
        }
    }

    final class InOrderObjectiveContext implements ObjectiveContext {
        private final DeviceId deviceId;
        private final ObjectiveContext originalContext;
        // Prevent onSuccess from being executed after onError is called
        // i.e. when the context actually succeed after the cache timeout
        private final AtomicBoolean failed;

        InOrderObjectiveContext(DeviceId deviceId, ObjectiveContext originalContext) {
            this.deviceId = deviceId;
            this.originalContext = originalContext;
            this.failed = new AtomicBoolean(false);
        }

        @Override
        public void onSuccess(Objective objective) {
            log.trace("Flow objective onSuccess {}", objective);

            if (!failed.get()) {
                dequeue(deviceId, objective, null);
                if (originalContext != null) {
                    originalContext.onSuccess(objective);
                }
            }

        }
        @Override
        public void onError(Objective objective, ObjectiveError error) {
            log.warn("Flow objective onError {}. Reason = {}", objective, error);

            if (!failed.getAndSet(true)) {
                dequeue(deviceId, objective, error);
                if (originalContext != null) {
                    originalContext.onError(objective, error);
                }
            }
        }
    }
}
