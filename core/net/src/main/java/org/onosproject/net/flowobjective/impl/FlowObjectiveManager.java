/*
 * Copyright 2015-present Open Networking Laboratory
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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.util.ItemNotFoundException;
import org.onosproject.cluster.ClusterService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.NextGroup;
import org.onosproject.net.behaviour.Pipeliner;
import org.onosproject.net.behaviour.PipelinerContext;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DefaultDriverProviderService;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.FlowObjectiveStore;
import org.onosproject.net.flowobjective.FlowObjectiveStoreDelegate;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.flowobjective.ObjectiveEvent;
import org.onosproject.net.flowobjective.ObjectiveEvent.Type;
import org.onosproject.net.group.GroupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.security.AppGuard.checkPermission;
import static org.onosproject.security.AppPermission.Type.FLOWRULE_WRITE;

/**
 * Provides implementation of the flow objective programming service.
 */
@Component(immediate = true)
@Service
public class FlowObjectiveManager implements FlowObjectiveService {

    public static final int INSTALL_RETRY_ATTEMPTS = 5;
    public static final long INSTALL_RETRY_INTERVAL = 1000; // ms

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    // Note: The following dependencies are added on behalf of the pipeline
    // driver behaviours to assure these services are available for their
    // initialization.
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected GroupService groupService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowObjectiveStore flowObjectiveStore;

    // Note: This must remain an optional dependency to allow re-install of default drivers.
    // Note: For now disabled until we can move to OPTIONAL_UNARY dependency
    // @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY, policy = ReferencePolicy.DYNAMIC)
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DefaultDriverProviderService defaultDriverService;

    private final FlowObjectiveStoreDelegate delegate = new InternalStoreDelegate();

    private final Map<DeviceId, DriverHandler> driverHandlers = Maps.newConcurrentMap();
    private final Map<DeviceId, Pipeliner> pipeliners = Maps.newConcurrentMap();

    private final PipelinerContext context = new InnerPipelineContext();
    private final DeviceListener deviceListener = new InnerDeviceListener();

    protected ServiceDirectory serviceDirectory = new DefaultServiceDirectory();

    private final Map<Integer, Set<PendingNext>> pendingForwards = Maps.newConcurrentMap();

    // local store to track which nextObjectives were sent to which device
    // for debugging purposes
    private Map<Integer, DeviceId> nextToDevice = Maps.newConcurrentMap();

    private ExecutorService executorService;

    @Activate
    protected void activate() {
        executorService = newFixedThreadPool(4, groupedThreads("onos/objective-installer", "%d", log));
        flowObjectiveStore.setDelegate(delegate);
        deviceService.addListener(deviceListener);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        flowObjectiveStore.unsetDelegate(delegate);
        deviceService.removeListener(deviceListener);
        executorService.shutdown();
        pipeliners.clear();
        driverHandlers.clear();
        nextToDevice.clear();
        log.info("Stopped");
    }

    /**
     * Task that passes the flow objective down to the driver. The task will
     * make a few attempts to find the appropriate driver, then eventually give
     * up and report an error if no suitable driver could be found.
     */
    private class ObjectiveInstaller implements Runnable {
        private final DeviceId deviceId;
        private final Objective objective;

        private final int numAttempts;

        public ObjectiveInstaller(DeviceId deviceId, Objective objective) {
            this(deviceId, objective, 1);
        }

        public ObjectiveInstaller(DeviceId deviceId, Objective objective, int attemps) {
            this.deviceId = checkNotNull(deviceId);
            this.objective = checkNotNull(objective);
            this.numAttempts = checkNotNull(attemps);
        }

        @Override
        public void run() {
            try {
                Pipeliner pipeliner = getDevicePipeliner(deviceId);

                if (pipeliner != null) {
                    if (objective instanceof NextObjective) {
                        pipeliner.next((NextObjective) objective);
                    } else if (objective instanceof ForwardingObjective) {
                        pipeliner.forward((ForwardingObjective) objective);
                    } else {
                        pipeliner.filter((FilteringObjective) objective);
                    }
                    //Attempts to check if pipeliner is null for retry attempts
                } else if (numAttempts < INSTALL_RETRY_ATTEMPTS) {
                    Thread.sleep(INSTALL_RETRY_INTERVAL);
                    executorService.execute(new ObjectiveInstaller(deviceId, objective, numAttempts + 1));
                } else {
                    // Otherwise we've tried a few times and failed, report an
                    // error back to the user.
                    objective.context().ifPresent(
                            c -> c.onError(objective, ObjectiveError.NOPIPELINER));
                }
                //Excpetion thrown
            } catch (Exception e) {
                log.warn("Exception while installing flow objective", e);
            }
        }
    }

    @Override
    public void filter(DeviceId deviceId, FilteringObjective filteringObjective) {
        checkPermission(FLOWRULE_WRITE);
        executorService.execute(new ObjectiveInstaller(deviceId, filteringObjective));
    }

    @Override
    public void forward(DeviceId deviceId, ForwardingObjective forwardingObjective) {
        checkPermission(FLOWRULE_WRITE);
        if (queueObjective(deviceId, forwardingObjective)) {
            return;
        }
        executorService.execute(new ObjectiveInstaller(deviceId, forwardingObjective));
    }

    @Override
    public void next(DeviceId deviceId, NextObjective nextObjective) {
        checkPermission(FLOWRULE_WRITE);
        nextToDevice.put(nextObjective.id(), deviceId);
        executorService.execute(new ObjectiveInstaller(deviceId, nextObjective));
    }

    @Override
    public int allocateNextId() {
        checkPermission(FLOWRULE_WRITE);
        return flowObjectiveStore.allocateNextId();
    }

    @Override
    public void initPolicy(String policy) {}

    private boolean queueObjective(DeviceId deviceId, ForwardingObjective fwd) {
        if (fwd.nextId() == null ||
                flowObjectiveStore.getNextGroup(fwd.nextId()) != null) {
            // fast path
            return false;
        }
        boolean queued = false;
        synchronized (pendingForwards) {
            // double check the flow objective store, because this block could run
            // after a notification arrives
            if (flowObjectiveStore.getNextGroup(fwd.nextId()) == null) {
                pendingForwards.compute(fwd.nextId(), (id, pending) -> {
                    PendingNext next = new PendingNext(deviceId, fwd);
                    if (pending == null) {
                        return Sets.newHashSet(next);
                    } else {
                        pending.add(next);
                        return pending;
                    }
                });
                queued = true;
            }
        }
        if (queued) {
            log.debug("Queued forwarding objective {} for nextId {} meant for device {}",
                      fwd.id(), fwd.nextId(), deviceId);
        }
        return queued;
    }

    /**
     * Retrieves (if it exists) the device pipeline behaviour from the cache.
     * Otherwise it warms the caches and triggers the init method of the Pipeline.
     *
     * @param deviceId the id of the device associated to the pipeline
     * @return the implementation of the Pipeliner behaviour
     */
    private Pipeliner getDevicePipeliner(DeviceId deviceId) {
        return pipeliners.computeIfAbsent(deviceId, this::initPipelineHandler);
    }

    /**
     * Retrieves (if it exists) the device pipeline behaviour from the cache and
     * and triggers the init method of the pipeline. Otherwise (DEVICE_ADDED) it warms
     * the caches and triggers the init method of the Pipeline. The rationale of this
     * method is for managing the scenario of a switch that goes down for a failure
     * and goes up after a while.
     *
     * @param deviceId the id of the device associated to the pipeline
     * @return the implementation of the Pipeliner behaviour
     */
    private Pipeliner getAndInitDevicePipeliner(DeviceId deviceId) {
        return pipeliners.compute(deviceId, (deviceIdValue, pipelinerValue) -> {
            if (pipelinerValue != null) {
                pipelinerValue.init(deviceId, context);
                return pipelinerValue;
            }
            return this.initPipelineHandler(deviceId);
        });
    }

    /**
     * Creates and initialize {@link Pipeliner}.
     * <p>
     * Note: Expected to be called under per-Device lock.
     *      e.g., {@code pipeliners}' Map#compute family methods
     *
     * @param deviceId Device to initialize pipeliner
     * @return {@link Pipeliner} instance or null
     */
    private Pipeliner initPipelineHandler(DeviceId deviceId) {
        start = now();

        // Attempt to lookup the handler in the cache
        DriverHandler handler = driverHandlers.get(deviceId);
        cTime = now();

        if (handler == null) {
            try {
                // Otherwise create it and if it has pipeline behaviour, cache it
                handler = driverService.createHandler(deviceId);
                dTime = now();
                if (!handler.driver().hasBehaviour(Pipeliner.class)) {
                    log.warn("Pipeline behaviour not supported for device {}",
                             deviceId);
                    return null;
                }
            } catch (ItemNotFoundException e) {
                log.warn("No applicable driver for device {}", deviceId);
                return null;
            }

            driverHandlers.put(deviceId, handler);
            eTime = now();
        }

        // Always (re)initialize the pipeline behaviour
        log.info("Driver {} bound to device {} ... initializing driver",
                 handler.driver().name(), deviceId);
        hTime = now();
        Pipeliner pipeliner = handler.behaviour(Pipeliner.class);
        hbTime = now();
        pipeliner.init(deviceId, context);
        stopWatch();
        return pipeliner;
    }

    // Triggers driver setup when a device is (re)detected.
    private class InnerDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            switch (event.type()) {
                case DEVICE_ADDED:
                case DEVICE_AVAILABILITY_CHANGED:
                    log.debug("Device either added or availability changed {}",
                              event.subject().id());
                    if (deviceService.isAvailable(event.subject().id())) {
                        log.debug("Device is now available {}", event.subject().id());
                        getAndInitDevicePipeliner(event.subject().id());
                    } else {
                        log.debug("Device is no longer available {}", event.subject().id());
                    }
                    break;
                case DEVICE_UPDATED:
                    break;
                case DEVICE_REMOVED:
                    // evict Pipeliner and Handler cache, when
                    // the Device was administratively removed.
                    //
                    // System expect the user to clear all existing flows,
                    // before removing device, especially if they intend to
                    // replace driver/pipeliner assigned to the device.
                    driverHandlers.remove(event.subject().id());
                    pipeliners.remove(event.subject().id());
                    break;
                case DEVICE_SUSPENDED:
                    break;
                case PORT_ADDED:
                    break;
                case PORT_UPDATED:
                    break;
                case PORT_REMOVED:
                    break;
                default:
                    break;
            }
        }
    }

    // Temporary mechanism to monitor pipeliner setup time-cost; there are
    // intermittent time where this takes in excess of 2 seconds. Why?
    private long start = 0, totals = 0, count = 0;
    private long cTime, dTime, eTime, hTime, hbTime;
    private static final long LIMIT = 500;

    private long now() {
        return System.currentTimeMillis();
    }

    private void stopWatch() {
        long duration = System.currentTimeMillis() - start;
        totals += duration;
        count += 1;
        if (duration > LIMIT) {
            log.info("Pipeline setup took {} ms; avg {} ms; cTime={}, dTime={}, eTime={}, hTime={}, hbTime={}",
                     duration, totals / count, diff(cTime), diff(dTime), diff(eTime), diff(hTime), diff(hbTime));
        }
    }

    private long diff(long bTime) {
        long diff = bTime - start;
        return diff < 0 ? 0 : diff;
    }

    // Processing context for initializing pipeline driver behaviours.
    private class InnerPipelineContext implements PipelinerContext {
        @Override
        public ServiceDirectory directory() {
            return serviceDirectory;
        }

        @Override
        public FlowObjectiveStore store() {
            return flowObjectiveStore;
        }
    }

    private class InternalStoreDelegate implements FlowObjectiveStoreDelegate {
        @Override
        public void notify(ObjectiveEvent event) {
            if (event.type() == Type.ADD) {
                log.debug("Received notification of obj event {}", event);
                Set<PendingNext> pending;
                synchronized (pendingForwards) {
                    // needs to be synchronized for queueObjective lookup
                    pending = pendingForwards.remove(event.subject());
                }

                if (pending == null) {
                    log.debug("Nothing pending for this obj event {}", event);
                    return;
                }

                log.debug("Processing {} pending forwarding objectives for nextId {}",
                         pending.size(), event.subject());
                pending.forEach(p -> getDevicePipeliner(p.deviceId())
                                .forward(p.forwardingObjective()));
            }
        }
    }

    /**
     * Data class used to hold a pending forwarding objective that could not
     * be processed because the associated next object was not present.
     */
    private class PendingNext {
        private final DeviceId deviceId;
        private final ForwardingObjective fwd;

        public PendingNext(DeviceId deviceId, ForwardingObjective fwd) {
            this.deviceId = deviceId;
            this.fwd = fwd;
        }

        public DeviceId deviceId() {
            return deviceId;
        }

        public ForwardingObjective forwardingObjective() {
            return fwd;
        }

        @Override
        public int hashCode() {
            return Objects.hash(deviceId, fwd);
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof PendingNext)) {
                return false;
            }
            final PendingNext other = (PendingNext) obj;
            if (this.deviceId.equals(other.deviceId) &&
                    this.fwd.equals(other.fwd)) {
                return true;
            }
            return false;
        }
    }

    @Override
    public List<String> getNextMappings() {
        List<String> mappings = new ArrayList<>();
        Map<Integer, NextGroup> allnexts = flowObjectiveStore.getAllGroups();
        // XXX if the NextGroup after de-serialization actually stored info of the deviceId
        // then info on any nextObj could be retrieved from one controller instance.
        // Right now the drivers on one instance can only fetch for next-ids that came
        // to them.
        // Also, we still need to send the right next-id to the right driver as potentially
        // there can be different drivers for different devices. But on that account,
        // no instance should be decoding for another instance's nextIds.

        for (Map.Entry<Integer, NextGroup> e : allnexts.entrySet()) {
            // get the device this next Objective was sent to
            DeviceId deviceId = nextToDevice.get(e.getKey());
            mappings.add("NextId " + e.getKey() + ": " +
                    ((deviceId != null) ? deviceId : "nextId not in this onos instance"));
            if (deviceId != null) {
                // this instance of the controller sent the nextObj to a driver
                Pipeliner pipeliner = getDevicePipeliner(deviceId);
                List<String> nextMappings = pipeliner.getNextMappings(e.getValue());
                if (nextMappings != null) {
                    mappings.addAll(nextMappings);
                }
            }
        }
        return mappings;
    }

    @Override
    public List<String> getPendingNexts() {
        List<String> pendingNexts = new ArrayList<>();
        for (Integer nextId : pendingForwards.keySet()) {
            Set<PendingNext> pnext = pendingForwards.get(nextId);
            StringBuilder pend = new StringBuilder();
            pend.append("Next Id: ").append(Integer.toString(nextId))
                .append(" :: ");
            for (PendingNext pn : pnext) {
                pend.append(Integer.toString(pn.forwardingObjective().id()))
                    .append(" ");
            }
            pendingNexts.add(pend.toString());
        }
        return pendingNexts;
    }
}
