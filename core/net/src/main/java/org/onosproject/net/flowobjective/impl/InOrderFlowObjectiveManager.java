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
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.FlowObjectiveStoreDelegate;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveContext;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.flowobjective.ObjectiveEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Component(immediate = true, enabled = true)
@Service
public class InOrderFlowObjectiveManager extends FlowObjectiveManager {
    private final Logger log = LoggerFactory.getLogger(getClass());

    // TODO Making these cache and timeout the entries
    private ListMultimap<FiltObjQueueKey, Objective> filtObjQueue =
            Multimaps.synchronizedListMultimap(ArrayListMultimap.create());
    private ListMultimap<FwdObjQueueKey, Objective> fwdObjQueue =
            Multimaps.synchronizedListMultimap(ArrayListMultimap.create());
    private ListMultimap<NextObjQueueKey, Objective> nextObjQueue =
            Multimaps.synchronizedListMultimap(ArrayListMultimap.create());

    final FlowObjectiveStoreDelegate delegate = new InternalStoreDelegate();

    @Activate
    protected void activate() {
        super.activate();
        // Replace store delegate to make sure pendingForward and pendingNext are resubmitted to
        // process()
        flowObjectiveStore.unsetDelegate(super.delegate);
        flowObjectiveStore.setDelegate(delegate);
    }

    @Deactivate
    protected void deactivate() {
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
                dequeue(deviceId, objective);
                originalContext.ifPresent(c -> c.onSuccess(objective));
            }
            @Override
            public void onError(Objective objective, ObjectiveError error) {
                log.trace("Flow objective onError {}", objective);
                dequeue(deviceId, objective);
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
            FiltObjQueueKey k = new FiltObjQueueKey(deviceId, priority, ((FilteringObjective) obj).key());
            filtObjQueue.put(k, obj);
            queueSize = filtObjQueue.get(k).size();
        } else if (obj instanceof ForwardingObjective) {
            FwdObjQueueKey k = new FwdObjQueueKey(deviceId, priority, ((ForwardingObjective) obj).selector());
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
     */
    private synchronized void dequeue(DeviceId deviceId, Objective obj) {
        List<Objective> remaining;
        int priority = obj.priority();

        LogLevel logLevel = (obj.op() == Objective.Operation.VERIFY) ? LogLevel.TRACE : LogLevel.DEBUG;
        Tools.log(log, logLevel, "Dequeue {}", obj);

        if (obj instanceof FilteringObjective) {
            FiltObjQueueKey k = new FiltObjQueueKey(deviceId, priority, ((FilteringObjective) obj).key());
            filtObjQueue.remove(k, obj);
            remaining = filtObjQueue.get(k);
        } else if (obj instanceof ForwardingObjective) {
            FwdObjQueueKey k = new FwdObjQueueKey(deviceId, priority, ((ForwardingObjective) obj).selector());
            fwdObjQueue.remove(k, obj);
            remaining = fwdObjQueue.get(k);
        } else if (obj instanceof NextObjective) {
            NextObjQueueKey k = new NextObjQueueKey(deviceId, obj.id());
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

        if (obj instanceof FilteringObjective) {
            super.filter(deviceId, (FilteringObjective) obj);
        } else if (obj instanceof ForwardingObjective) {
            super.forward(deviceId, (ForwardingObjective) obj);
        } else if (obj instanceof NextObjective) {
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
                    // resubmitted back to the execution queue
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
                    // resubmitted back to the execution queue
                    pendNexts.forEach(p -> execute(p.deviceId(), p.flowObjective()));
                }
            }
        }
    }

    private static class FiltObjQueueKey {
        private DeviceId deviceId;
        private int priority;
        private Criterion key;

        FiltObjQueueKey(DeviceId deviceId, int priority, Criterion key) {
            this.deviceId = deviceId;
            this.priority = priority;
            this.key = key;
        }

        @Override
        public int hashCode() {
            return Objects.hash(deviceId, priority, key);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof FiltObjQueueKey)) {
                return false;
            }
            FiltObjQueueKey that = (FiltObjQueueKey) other;
            return Objects.equals(this.deviceId, that.deviceId) &&
                    Objects.equals(this.priority, that.priority) &&
                    Objects.equals(this.key, that.key);
        }
    }

    private static class FwdObjQueueKey {
        private DeviceId deviceId;
        private int priority;
        private TrafficSelector selector;

        FwdObjQueueKey(DeviceId deviceId, int priority, TrafficSelector selector) {
            this.deviceId = deviceId;
            this.priority = priority;
            this.selector = selector;
        }

        @Override
        public int hashCode() {
            return Objects.hash(deviceId, priority, selector);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof FwdObjQueueKey)) {
                return false;
            }
            FwdObjQueueKey that = (FwdObjQueueKey) other;
            return Objects.equals(this.deviceId, that.deviceId) &&
                    Objects.equals(this.priority, that.priority) &&
                    Objects.equals(this.selector, that.selector);
        }
    }

    private static class NextObjQueueKey {
        private DeviceId deviceId;
        private int id;

        NextObjQueueKey(DeviceId deviceId, int id) {
            this.deviceId = deviceId;
            this.id = id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(deviceId, id);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof NextObjQueueKey)) {
                return false;
            }
            NextObjQueueKey that = (NextObjQueueKey) other;
            return Objects.equals(this.deviceId, that.deviceId) &&
                    Objects.equals(this.id, that.id);
        }
    }
}
