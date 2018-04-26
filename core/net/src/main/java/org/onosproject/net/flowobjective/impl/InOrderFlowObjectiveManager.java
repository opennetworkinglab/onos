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
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.Tools;
import org.onlab.util.Tools.LogLevel;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flowobjective.FilteringObjQueueKey;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.FlowObjectiveStoreDelegate;
import org.onosproject.net.flowobjective.ForwardingObjQueueKey;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjQueueKey;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveContext;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.flowobjective.ObjectiveEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;

@Component(immediate = true, enabled = true)
@Service
public class InOrderFlowObjectiveManager extends FlowObjectiveManager {
    private final Logger log = LoggerFactory.getLogger(getClass());

    // TODO Make queue timeout configurable
    static final int OBJ_TIMEOUT_MS = 15000;

    private Cache<FilteringObjQueueKey, Objective> filtObjQueueHead;
    private Cache<ForwardingObjQueueKey, Objective> fwdObjQueueHead;
    private Cache<NextObjQueueKey, Objective> nextObjQueueHead;
    private ScheduledExecutorService cacheCleaner;

    private ListMultimap<FilteringObjQueueKey, Objective> filtObjQueue =
            Multimaps.synchronizedListMultimap(ArrayListMultimap.create());
    private ListMultimap<ForwardingObjQueueKey, Objective> fwdObjQueue =
            Multimaps.synchronizedListMultimap(ArrayListMultimap.create());
    private ListMultimap<NextObjQueueKey, Objective> nextObjQueue =
            Multimaps.synchronizedListMultimap(ArrayListMultimap.create());

    final FlowObjectiveStoreDelegate delegate = new InternalStoreDelegate();

    @Activate
    protected void activate() {
        super.activate();

        // TODO Clean up duplicated code
        filtObjQueueHead = CacheBuilder.newBuilder()
                .expireAfterWrite(OBJ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .removalListener((RemovalNotification<FilteringObjQueueKey, Objective> notification) -> {
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
                }).build();
        fwdObjQueueHead = CacheBuilder.newBuilder()
                .expireAfterWrite(OBJ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .removalListener((RemovalNotification<ForwardingObjQueueKey, Objective> notification) -> {
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
                }).build();
        nextObjQueueHead = CacheBuilder.newBuilder()
                .expireAfterWrite(OBJ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .removalListener((RemovalNotification<NextObjQueueKey, Objective> notification) -> {
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
                }).build();

        cacheCleaner = newSingleThreadScheduledExecutor(groupedThreads("onos/flowobj", "cache-cleaner", log));
        cacheCleaner.scheduleAtFixedRate(() -> {
            filtObjQueueHead.cleanUp();
            fwdObjQueueHead.cleanUp();
            nextObjQueueHead.cleanUp();
        }, 0, OBJ_TIMEOUT_MS, TimeUnit.MILLISECONDS);

        // Replace store delegate to make sure pendingForward and pendingNext are resubmitted to
        // execute()
        flowObjectiveStore.unsetDelegate(super.delegate);
        flowObjectiveStore.setDelegate(delegate);
    }

    @Deactivate
    protected void deactivate() {
        cacheCleaner.shutdown();
        clearQueue();

        super.deactivate();
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
        ObjectiveContext context = new ObjectiveContext() {
            @Override
            public void onSuccess(Objective objective) {
                log.trace("Flow objective onSuccess {}", objective);
                dequeue(deviceId, objective, null);
                originalContext.ifPresent(c -> c.onSuccess(objective));
            }
            @Override
            public void onError(Objective objective, ObjectiveError error) {
                log.warn("Flow objective onError {}. Reason = {}", objective, error);
                dequeue(deviceId, objective, error);
                originalContext.ifPresent(c -> c.onError(objective, error));
            }
        };

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
            filtObjQueueHead.invalidate(k);
            filtObjQueue.remove(k, obj);
            remaining = filtObjQueue.get(k);
        } else if (obj instanceof ForwardingObjective) {
            ForwardingObjQueueKey k =
                    new ForwardingObjQueueKey(deviceId, priority, ((ForwardingObjective) obj).selector());
            fwdObjQueueHead.invalidate(k);
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
            nextObjQueueHead.invalidate(k);
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
}
