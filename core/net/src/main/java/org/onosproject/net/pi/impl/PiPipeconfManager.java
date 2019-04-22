/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.net.pi.impl;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Striped;
import org.onlab.util.HexString;
import org.onlab.util.ItemNotFoundException;
import org.onlab.util.SharedExecutors;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.basics.BasicDeviceConfig;
import org.onosproject.net.device.PortStatisticsDiscovery;
import org.onosproject.net.driver.Behaviour;
import org.onosproject.net.driver.DefaultDriver;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverAdminService;
import org.onosproject.net.driver.DriverEvent;
import org.onosproject.net.driver.DriverListener;
import org.onosproject.net.driver.DriverProvider;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiPipeconfId;
import org.onosproject.net.pi.service.PiPipeconfEvent;
import org.onosproject.net.pi.service.PiPipeconfListener;
import org.onosproject.net.pi.service.PiPipeconfMappingStore;
import org.onosproject.net.pi.service.PiPipeconfService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;


/**
 * Implementation of the PiPipeconfService.
 */
@Component(immediate = true, service = PiPipeconfService.class)
@Beta
public class PiPipeconfManager
        extends AbstractListenerManager<PiPipeconfEvent, PiPipeconfListener>
        implements PiPipeconfService {

    private final Logger log = getLogger(getClass());

    private static final String MERGED_DRIVER_SEPARATOR = ":";

    private static final int MISSING_DRIVER_WATCHDOG_INTERVAL = 5; // Seconds.

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetworkConfigRegistry cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DriverAdminService driverAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private PiPipeconfMappingStore pipeconfMappingStore;

    // Registered pipeconf are replicated through the app subsystem and
    // registered on app activated events. Hence, there should be no need of
    // distributing this map.
    protected ConcurrentMap<PiPipeconfId, PiPipeconf> pipeconfs = new ConcurrentHashMap<>();

    private final DriverListener driverListener = new InternalDriverListener();
    private final Set<String> missingMergedDrivers = Sets.newCopyOnWriteArraySet();
    private final Striped<Lock> locks = Striped.lock(20);

    protected ExecutorService executor = Executors.newFixedThreadPool(
            10, groupedThreads("onos/pipeconf-manager", "%d", log));

    @Activate
    public void activate() {
        driverAdminService.addListener(driverListener);
        eventDispatcher.addSink(PiPipeconfEvent.class, listenerRegistry);
        checkMissingMergedDrivers();
        if (!missingMergedDrivers.isEmpty()) {
            // Missing drivers should be created upon detecting registration
            // events of a new pipeconf or a base driver. If, for any reason, we
            // miss such event, here's a watchdog task.
            SharedExecutors.getPoolThreadExecutor()
                    .execute(this::missingDriversWatchdogTask);
        }
        log.info("Started");
    }


    @Deactivate
    public void deactivate() {
        eventDispatcher.removeSink(PiPipeconfEvent.class);
        executor.shutdown();
        driverAdminService.removeListener(driverListener);
        pipeconfs.clear();
        missingMergedDrivers.clear();
        cfgService = null;
        driverAdminService = null;
        log.info("Stopped");
    }

    @Override
    public void register(PiPipeconf pipeconf) throws IllegalStateException {
        checkNotNull(pipeconf);
        if (pipeconfs.containsKey(pipeconf.id())) {
            throw new IllegalStateException(format("Pipeconf %s is already registered", pipeconf.id()));
        }
        pipeconfs.put(pipeconf.id(), pipeconf);
        log.info("New pipeconf registered: {} (fingerprint={})",
                 pipeconf.id(), HexString.toHexString(pipeconf.fingerprint()));
        executor.execute(() -> attemptMergeAll(pipeconf.id()));
        post(new PiPipeconfEvent(PiPipeconfEvent.Type.REGISTERED, pipeconf));
    }

    @Override
    public void unregister(PiPipeconfId pipeconfId) throws IllegalStateException {
        checkNotNull(pipeconfId);
        // TODO add mechanism to remove from device.
        if (!pipeconfs.containsKey(pipeconfId)) {
            throw new IllegalStateException(format("Pipeconf %s is not registered", pipeconfId));
        }
        // TODO remove the binding from the distributed Store when the lifecycle of a pipeconf is defined.
        // pipeconfMappingStore.removeBindings(pipeconfId);
        final PiPipeconf pipeconf = pipeconfs.remove(pipeconfId);
        log.info("Unregistered pipeconf: {} (fingerprint={})",
                 pipeconfId, HexString.toHexString(pipeconf.fingerprint()));
        post(new PiPipeconfEvent(PiPipeconfEvent.Type.UNREGISTERED, pipeconfId));
    }

    @Override
    public Iterable<PiPipeconf> getPipeconfs() {
        return pipeconfs.values();
    }

    @Override
    public Optional<PiPipeconf> getPipeconf(PiPipeconfId id) {
        return Optional.ofNullable(pipeconfs.get(id));
    }

    @Override
    public Optional<PiPipeconf> getPipeconf(DeviceId deviceId) {
        if (pipeconfMappingStore.getPipeconfId(deviceId) == null) {
            return Optional.empty();
        } else {
            return Optional.ofNullable(pipeconfs.get(
                    pipeconfMappingStore.getPipeconfId(deviceId)));
        }
    }

    @Override
    public void bindToDevice(PiPipeconfId pipeconfId, DeviceId deviceId) {
        PiPipeconfId existingPipeconfId = pipeconfMappingStore.getPipeconfId(deviceId);
        if (existingPipeconfId != null && !existingPipeconfId.equals(pipeconfId)) {
            log.error("Cannot set binding for {} to {} as one already exists ({})",
                      deviceId, pipeconfId, existingPipeconfId);
            return;
        }
        pipeconfMappingStore.createOrUpdateBinding(deviceId, pipeconfId);
    }

    @Override
    public String getMergedDriver(DeviceId deviceId, PiPipeconfId pipeconfId) {
        log.debug("Starting device driver merge of {} with {}...", deviceId, pipeconfId);
        final BasicDeviceConfig basicDeviceConfig = cfgService.getConfig(
                deviceId, BasicDeviceConfig.class);
        if (basicDeviceConfig == null) {
            log.warn("Unable to get basic device config for {}, " +
                             "aborting pipeconf driver merge", deviceId);
            return null;
        }
        String baseDriverName = basicDeviceConfig.driver();
        if (baseDriverName == null) {
            log.warn("Missing driver from basic device config for {}, " +
                             "cannot produce merged driver", deviceId);
            return null;
        }
        if (isMergedDriverName(baseDriverName)) {
            // The config already has driver name that is a merged one. We still
            // need to make sure an instance of that merged driver is present in
            // this node.
            log.debug("Base driver of {} ({}) is a merged one",
                      deviceId, baseDriverName);
            baseDriverName = getBaseDriverNameFromMerged(baseDriverName);
        }

        return doMergeDriver(baseDriverName, pipeconfId);
    }

    @Override
    public Optional<PiPipeconfId> ofDevice(DeviceId deviceId) {
        return Optional.ofNullable(pipeconfMappingStore.getPipeconfId(deviceId));
    }

    private String doMergeDriver(String baseDriverName, PiPipeconfId pipeconfId) {
        final String newDriverName = mergedDriverName(baseDriverName, pipeconfId);
        // Serialize per newDriverName, avoid creating duplicates.
        locks.get(newDriverName).lock();
        try {
            // If merged driver exists already we don't create a new one.
            if (getDriver(newDriverName) != null) {
                return newDriverName;
            }
            log.debug("Creating merged driver {}...", newDriverName);
            final Driver mergedDriver = buildMergedDriver(
                    pipeconfId, baseDriverName, newDriverName);
            if (mergedDriver == null) {
                // Error logged by buildMergedDriver
                return null;
            }
            registerMergedDriver(mergedDriver);
            if (missingMergedDrivers.remove(newDriverName)) {
                log.info("There are still {} missing merged drivers",
                         missingMergedDrivers.size());
            }
            return newDriverName;
        } finally {
            locks.get(newDriverName).unlock();
        }
    }

    private String mergedDriverSuffix(PiPipeconfId pipeconfId) {
        return MERGED_DRIVER_SEPARATOR + pipeconfId.id();
    }

    private String mergedDriverName(String baseDriverName, PiPipeconfId pipeconfId) {
        return baseDriverName + mergedDriverSuffix(pipeconfId);
    }

    private String getBaseDriverNameFromMerged(String mergedDriverName) {
        final String[] pieces = mergedDriverName.split(MERGED_DRIVER_SEPARATOR);
        if (pieces.length != 2) {
            return null;
        }
        return pieces[0];
    }

    private PiPipeconfId getPipeconfIdFromMerged(String mergedDriverName) {
        final String[] pieces = mergedDriverName.split(MERGED_DRIVER_SEPARATOR);
        if (pieces.length != 2) {
            return null;
        }
        return new PiPipeconfId(pieces[1]);
    }

    private boolean isMergedDriverName(String driverName) {
        final String[] pieces = driverName.split(MERGED_DRIVER_SEPARATOR);
        return pieces.length == 2;
    }

    private Driver buildMergedDriver(PiPipeconfId pipeconfId, String baseDriverName,
                                     String newDriverName) {
        final Driver baseDriver = getDriver(baseDriverName);
        if (baseDriver == null) {
            log.error("Base driver {} not found, cannot build a merged one",
                      baseDriverName);
            return null;
        }

        final PiPipeconf pipeconf = pipeconfs.get(pipeconfId);
        if (pipeconf == null) {
            log.error("Pipeconf {} is not registered, cannot build a merged driver",
                      pipeconfId);
            return null;
        }

        // extract the behaviours from the pipipeconf.
        final Map<Class<? extends Behaviour>, Class<? extends Behaviour>> behaviours =
                new HashMap<>();
        pipeconf.behaviours().forEach(
                b -> behaviours.put(b, pipeconf.implementation(b).get()));

        // FIXME: remove this check when stratum_bmv2 will be open source and we
        //  will no longer need to read port counters from the p4 program. Here
        //  we ignore the PortStatisticsDiscovery behaviour from the pipeconf if
        //  the base driver (e.g. stratum with gnmi) already has it. But in
        //  general, we should give higher priority to pipeconf behaviours.
        if (baseDriver.hasBehaviour(PortStatisticsDiscovery.class)
                && behaviours.remove(PortStatisticsDiscovery.class) != null) {
            log.warn("Ignoring {} behaviour from pipeconf {}, but using " +
                             "the one provided by {} driver...",
                     PortStatisticsDiscovery.class.getSimpleName(), pipeconfId,
                     baseDriver.name());
        }

        final Driver piPipeconfDriver = new DefaultDriver(
                newDriverName, baseDriver.parents(),
                baseDriver.manufacturer(), baseDriver.hwVersion(),
                baseDriver.swVersion(), behaviours, new HashMap<>());
        // take the base driver created with the behaviours of the PiPeconf and
        // merge it with the base driver that was assigned to the device
        return piPipeconfDriver.merge(baseDriver);
    }

    private void registerMergedDriver(Driver driver) {
        final DriverProvider provider = new InternalDriverProvider(driver);
        if (driverAdminService.getProviders().contains(provider)) {
            // A provider for this driver already exist.
            return;
        }
        driverAdminService.registerProvider(provider);
    }

    private Driver getDriver(String name) {
        try {
            return driverAdminService.getDriver(name);
        } catch (ItemNotFoundException e) {
            return null;
        }
    }

    private boolean driverExists(String name) {
        return getDriver(name) != null;
    }

    private void checkMissingMergedDriver(DeviceId deviceId) {
        final PiPipeconfId pipeconfId = pipeconfMappingStore.getPipeconfId(deviceId);
        final BasicDeviceConfig cfg = cfgService.getConfig(deviceId, BasicDeviceConfig.class);

        if (pipeconfId == null) {
            // No pipeconf associated.
            return;
        }

        if (cfg == null || cfg.driver() == null) {
            log.warn("Missing basic device config or driver key in netcfg for " +
                             "{}, which is odd since it has a " +
                             "pipeconf associated ({})",
                     deviceId, pipeconfId);
            return;
        }

        final String baseDriverName = cfg.driver();
        final String mergedDriverName = mergedDriverName(baseDriverName, pipeconfId);

        if (driverExists(mergedDriverName) ||
                missingMergedDrivers.contains(mergedDriverName)) {
            // Not missing, or already aware of it missing.
            return;
        }

        log.info("Detected missing merged driver: {}", mergedDriverName);
        missingMergedDrivers.add(mergedDriverName);
        // Attempt building the driver now if all pieces are present.
        // If not, either a driver or pipeconf event will re-trigger
        // the process.
        attemptDriverMerge(mergedDriverName);
    }

    private void attemptDriverMerge(String mergedDriverName) {
        final String baseDriverName = getBaseDriverNameFromMerged(mergedDriverName);
        final PiPipeconfId pipeconfId = getPipeconfIdFromMerged(mergedDriverName);
        if (driverExists(baseDriverName) && pipeconfs.containsKey(pipeconfId)) {
            doMergeDriver(baseDriverName, pipeconfId);
        }
    }

    private void missingDriversWatchdogTask() {
        while (true) {
            // Most probably all missing drivers will be created before the
            // watchdog interval, so wait before starting...
            try {
                TimeUnit.SECONDS.sleep(MISSING_DRIVER_WATCHDOG_INTERVAL);
            } catch (InterruptedException e) {
                log.warn("Interrupted! There are still {} missing merged drivers",
                         missingMergedDrivers.size());
            }
            if (missingMergedDrivers.isEmpty()) {
                log.info("There are no more missing merged drivers!");
                return;
            }
            log.info("Detected {} missing merged drivers, attempt merge...",
                     missingMergedDrivers.size());
            missingMergedDrivers.forEach(this::attemptDriverMerge);
        }
    }

    private void checkMissingMergedDrivers() {
        cfgService.getSubjects(DeviceId.class, BasicDeviceConfig.class)
                .forEach(this::checkMissingMergedDriver);
    }

    private void attemptMergeAll(String baseDriverName) {
        missingMergedDrivers.stream()
                .filter(missingDriver -> {
                    // Filter missing merged drivers using this base driver.
                    final String xx = getBaseDriverNameFromMerged(missingDriver);
                    return xx != null && xx.equals(baseDriverName);
                })
                .forEach(this::attemptDriverMerge);
    }

    private void attemptMergeAll(PiPipeconfId pipeconfId) {
        missingMergedDrivers.stream()
                .filter(missingDriver -> {
                    // Filter missing merged drivers using this pipeconf.
                    final PiPipeconfId xx = getPipeconfIdFromMerged(missingDriver);
                    return xx != null && xx.equals(pipeconfId);
                })
                .forEach(this::attemptDriverMerge);
    }

    private class InternalDriverListener implements DriverListener {

        @Override
        public void event(DriverEvent event) {
            executor.execute(() -> attemptMergeAll(event.subject().name()));
        }

        @Override
        public boolean isRelevant(DriverEvent event) {
            return event.type() == DriverEvent.Type.DRIVER_ENHANCED;
        }
    }

    /**
     * Internal driver provider used to register merged pipeconf drivers in the
     * core.
     */
    private class InternalDriverProvider implements DriverProvider {

        Driver driver;

        InternalDriverProvider(Driver driver) {
            this.driver = driver;
        }

        @Override
        public Set<Driver> getDrivers() {
            return ImmutableSet.of(driver);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            InternalDriverProvider that = (InternalDriverProvider) o;
            return Objects.equals(driver.name(), that.driver.name());
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(driver.name());
        }
    }
}
