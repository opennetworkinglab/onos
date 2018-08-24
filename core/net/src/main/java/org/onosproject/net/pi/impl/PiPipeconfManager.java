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
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.ItemNotFoundException;
import org.onosproject.cluster.ClusterService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.basics.BasicDeviceConfig;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.net.driver.Behaviour;
import org.onosproject.net.driver.DefaultDriver;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverAdminService;
import org.onosproject.net.driver.DriverEvent;
import org.onosproject.net.driver.DriverListener;
import org.onosproject.net.driver.DriverProvider;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiPipeconfId;
import org.onosproject.net.pi.service.PiPipeconfConfig;
import org.onosproject.net.pi.service.PiPipeconfMappingStore;
import org.onosproject.net.pi.service.PiPipeconfService;
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
import java.util.concurrent.locks.Lock;

import static java.lang.String.format;
import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;


/**
 * Implementation of the PiPipeconfService.
 */
@Component(immediate = true)
@Service
@Beta
public class PiPipeconfManager implements PiPipeconfService {

    private final Logger log = getLogger(getClass());

    private static final String MERGED_DRIVER_SEPARATOR = ":";
    private static final String CFG_SCHEME = "piPipeconf";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverAdminService driverAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private PiPipeconfMappingStore pipeconfMappingStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    // Registered pipeconf are replicated through the app subsystem and
    // registered on app activated events. Hence, there should be no need of
    // distributing this map.
    protected ConcurrentMap<PiPipeconfId, PiPipeconf> pipeconfs = new ConcurrentHashMap<>();

    private final DriverListener driverListener = new InternalDriverListener();
    private final Set<String> missingMergedDrivers = Sets.newCopyOnWriteArraySet();
    private final Striped<Lock> locks = Striped.lock(20);

    protected ExecutorService executor = Executors.newFixedThreadPool(
            10, groupedThreads("onos/pipeconf-manager", "%d", log));

    protected final ConfigFactory configFactory =
            new ConfigFactory<DeviceId, PiPipeconfConfig>(
                    SubjectFactories.DEVICE_SUBJECT_FACTORY,
                    PiPipeconfConfig.class, CFG_SCHEME) {
                @Override
                public PiPipeconfConfig createConfig() {
                    return new PiPipeconfConfig();
                }
            };

    @Activate
    public void activate() {
        cfgService.registerConfigFactory(configFactory);
        driverService.addListener(driverListener);
        checkMissingMergedDrivers();
        log.info("Started");
    }


    @Deactivate
    public void deactivate() {
        executor.shutdown();
        cfgService.unregisterConfigFactory(configFactory);
        driverService.removeListener(driverListener);
        pipeconfs.clear();
        missingMergedDrivers.clear();
        cfgService = null;
        driverAdminService = null;
        driverService = null;
        log.info("Stopped");
    }

    @Override
    public void register(PiPipeconf pipeconf) throws IllegalStateException {
        if (pipeconfs.containsKey(pipeconf.id())) {
            throw new IllegalStateException(format("Pipeconf %s is already registered", pipeconf.id()));
        }
        pipeconfs.put(pipeconf.id(), pipeconf);
        log.info("New pipeconf registered: {}", pipeconf.id());
        executor.execute(() -> mergeAll(pipeconf.id()));
    }

    @Override
    public void remove(PiPipeconfId pipeconfId) throws IllegalStateException {
        // TODO add mechanism to remove from device.
        if (!pipeconfs.containsKey(pipeconfId)) {
            throw new IllegalStateException(format("Pipeconf %s is not registered", pipeconfId));
        }
        // TODO remove the binding from the distributed Store when the lifecycle of a pipeconf is defined.
        // pipeconfMappingStore.removeBindings(pipeconfId);
        log.info("Removing pipeconf {}", pipeconfId);
        pipeconfs.remove(pipeconfId);
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
    public void bindToDevice(PiPipeconfId pipeconfId, DeviceId deviceId) {
        pipeconfMappingStore.createOrUpdateBinding(deviceId, pipeconfId);
    }

    @Override
    public String mergeDriver(DeviceId deviceId, PiPipeconfId pipeconfId) {
        log.debug("Starting device driver merge of {} with {}...", deviceId, pipeconfId);
        final BasicDeviceConfig basicDeviceConfig = cfgService.getConfig(
                deviceId, BasicDeviceConfig.class);
        if (basicDeviceConfig == null) {
            log.warn("Unable to get basic device config for {}, " +
                             "aborting pipeconf driver merge");
            return null;
        }
        String baseDriverName = basicDeviceConfig.driver();
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
            log.info("Creating merged driver {}...", newDriverName);
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
            return driverService.getDriver(name);
        } catch (ItemNotFoundException e) {
            return null;
        }
    }

    private void checkMissingMergedDrivers() {
        cfgService.getSubjects(DeviceId.class, BasicDeviceConfig.class).stream()
                .map(d -> cfgService.getConfig(d, BasicDeviceConfig.class))
                .map(BasicDeviceConfig::driver)
                .filter(Objects::nonNull)
                .filter(d -> getDriver(d) == null)
                .forEach(driverName -> {
                    final String baseDriverName = getBaseDriverNameFromMerged(driverName);
                    final PiPipeconfId pipeconfId = getPipeconfIdFromMerged(driverName);
                    if (baseDriverName == null || pipeconfId == null) {
                        // Not a merged driver.
                        return;
                    }
                    log.info("Detected missing merged driver: {}", driverName);
                    missingMergedDrivers.add(driverName);
                    // Attempt building the driver now if all pieces are present.
                    // If not, either a driver or pipeconf event will re-trigger
                    // the merge process.
                    if (getDriver(baseDriverName) != null
                            && pipeconfs.containsKey(pipeconfId)) {
                        mergedDriverName(baseDriverName, pipeconfId);
                    }
                });
    }

    private void mergeAll(String baseDriverName) {
        missingMergedDrivers.stream()
                .filter(driverName -> {
                    final String xx = getBaseDriverNameFromMerged(driverName);
                    return xx != null && xx.equals(baseDriverName);
                })
                .forEach(driverName -> {
                    final PiPipeconfId pipeconfId = getPipeconfIdFromMerged(driverName);
                    if (pipeconfs.containsKey(pipeconfId)) {
                        doMergeDriver(baseDriverName, pipeconfId);
                    }
                });
    }

    private void mergeAll(PiPipeconfId pipeconfId) {
        missingMergedDrivers.stream()
                .filter(driverName -> {
                    final PiPipeconfId xx = getPipeconfIdFromMerged(driverName);
                    return xx != null && xx.equals(pipeconfId);
                })
                .forEach(driverName -> {
                    final String baseDriverName = getBaseDriverNameFromMerged(driverName);
                    if (getDriver(baseDriverName) != null) {
                        doMergeDriver(baseDriverName, pipeconfId);
                    }
                });
    }

    private class InternalDriverListener implements DriverListener {

        @Override
        public void event(DriverEvent event) {
            executor.execute(() -> mergeAll(event.subject().name()));
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
