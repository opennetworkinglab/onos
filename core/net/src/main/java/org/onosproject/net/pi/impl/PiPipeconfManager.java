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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableSet;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.ItemNotFoundException;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.basics.BasicDeviceConfig;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.net.driver.Behaviour;
import org.onosproject.net.driver.DefaultDriver;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverAdminService;
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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    private static final String DRIVER = "driver";
    private static final String CFG_SCHEME = "piPipeconf";

    private static final String DRIVER_MERGE_TOPIC =
            PiPipeconfManager.class.getSimpleName() + "-driver-merge-";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private LeadershipService leadershipService;

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
        log.info("Started");
    }


    @Deactivate
    public void deactivate() {
        executor.shutdown();
        cfgService.unregisterConfigFactory(configFactory);
        pipeconfs.clear();
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
    public CompletableFuture<Boolean> bindToDevice(
            PiPipeconfId pipeconfId, DeviceId deviceId) {
        return CompletableFuture.supplyAsync(() -> doMergeDriver(
                deviceId, pipeconfId), executor);
    }

    @Override
    public Optional<PiPipeconfId> ofDevice(DeviceId deviceId) {
        return Optional.ofNullable(pipeconfMappingStore.getPipeconfId(deviceId));
    }

    private boolean doMergeDriver(DeviceId deviceId, PiPipeconfId pipeconfId) {
        // Perform the following operations:
        // 1) ALL nodes: create and register new merged driver (pipeconf + base driver)
        // 2) ONE node (leader): updated netcfg with new driver
        log.warn("Starting device driver merge of {} with {}...", deviceId, pipeconfId);
        final BasicDeviceConfig basicDeviceConfig = cfgService.getConfig(
                deviceId, BasicDeviceConfig.class);
        final Driver baseDriver = driverService.getDriver(
                basicDeviceConfig.driver());
        final String newDriverName = baseDriver.name() + ":" + pipeconfId;
        if (baseDriver.name().equals(newDriverName)) {
            log.warn("Requested to merge {} driver with {} for {}, "
                             + "but current device driver is already merged",
                     baseDriver.name(), pipeconfId, deviceId);
            return true;
        }
        final PiPipeconf pipeconf = pipeconfs.get(pipeconfId);
        if (pipeconf == null) {
            log.error("Pipeconf {} is not registered", pipeconfId);
            return false;
        }
        // 1) if merged driver exists already we don't create a new one.
        try {
            driverService.getDriver(newDriverName);
            log.info("Found existing merged driver {}, re-using that", newDriverName);
        } catch (ItemNotFoundException e) {
            log.info("Creating merged driver {}...", newDriverName);
            createMergedDriver(pipeconf, baseDriver, newDriverName);
        }
        // 2) Updating device cfg to enforce the merged driver (one node only)
        final boolean isLeader = leadershipService
                .runForLeadership(DRIVER_MERGE_TOPIC + deviceId.toString())
                .leaderNodeId()
                .equals(clusterService.getLocalNode().id());
        if (isLeader) {
            // FIXME: this binding should be updated by the same entity
            // deploying the pipeconf.
            pipeconfMappingStore.createOrUpdateBinding(deviceId, pipeconfId);
            if (!basicDeviceConfig.driver().equals(newDriverName)) {
                log.info("Applying new driver {} for device {} via cfg...",
                         newDriverName, deviceId);
                setDriverViaCfg(deviceId, newDriverName, basicDeviceConfig);
            }
        }
        return true;
    }

    private void createMergedDriver(PiPipeconf pipeconf, Driver baseDriver,
                                    String newDriverName) {
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
        final Driver completeDriver = piPipeconfDriver.merge(baseDriver);
        // This might lead to explosion of number of providers in the core,
        // due to 1:1:1 pipeconf:driver:provider maybe find better way
        final DriverProvider provider = new PiPipeconfDriverProviderInternal(
                completeDriver);
        // register the merged driver
        driverAdminService.registerProvider(provider);
    }

    private void setDriverViaCfg(DeviceId deviceId, String driverName,
                                 BasicDeviceConfig basicDeviceConfig) {
        ObjectNode newCfg = (ObjectNode) basicDeviceConfig.node();
        newCfg = newCfg.put(DRIVER, driverName);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode newCfgNode = mapper.convertValue(newCfg, JsonNode.class);
        cfgService.applyConfig(deviceId, BasicDeviceConfig.class, newCfgNode);
    }

    private class PiPipeconfDriverProviderInternal implements DriverProvider {

        Driver driver;

        PiPipeconfDriverProviderInternal(Driver driver) {
            this.driver = driver;
        }

        @Override
        public Set<Driver> getDrivers() {
            return ImmutableSet.of(driver);
        }
    }
}
