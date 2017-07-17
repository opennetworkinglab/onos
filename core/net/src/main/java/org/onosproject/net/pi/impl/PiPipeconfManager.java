/*
 * Copyright 2017-present Open Networking Laboratory
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
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
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
import org.onosproject.net.pi.runtime.PiPipeconfConfig;
import org.onosproject.net.pi.runtime.PiPipeconfService;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
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

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverAdminService driverAdminService;

    //TODO move to replicated map
    protected ConcurrentHashMap<PiPipeconfId, PiPipeconf> piPipeconfs = new ConcurrentHashMap<>();
    //TODO move to replicated map
    protected ConcurrentHashMap<DeviceId, PiPipeconfId> devicesToPipeconf = new ConcurrentHashMap<>();

    protected ExecutorService executor =
            Executors.newFixedThreadPool(5, groupedThreads("onos/pipipeconfservice",
                                                           "pipeline-to-device-%d", log));

    protected final ConfigFactory factory =
            new ConfigFactory<DeviceId, PiPipeconfConfig>(
                    SubjectFactories.DEVICE_SUBJECT_FACTORY,
                    PiPipeconfConfig.class, CFG_SCHEME) {
                @Override
                public PiPipeconfConfig createConfig() {
                    return new PiPipeconfConfig();
                }
            };

    protected final NetworkConfigListener cfgListener = new InternalNetworkConfigListener();

    @Activate
    public void activate() {
        cfgService.registerConfigFactory(factory);
        cfgService.addListener(cfgListener);
        cfgService.getSubjects(DeviceId.class, PiPipeconfConfig.class)
                .forEach(this::addPipeconfFromCfg);
        log.info("Started");
    }


    @Deactivate
    public void deactivate() {
        executor.shutdown();
        cfgService.removeListener(cfgListener);
        cfgService.unregisterConfigFactory(factory);
        piPipeconfs.clear();
        devicesToPipeconf.clear();
        cfgService = null;
        driverAdminService = null;
        driverService = null;
        log.info("Stopped");
    }

    @Override
    public void register(PiPipeconf pipeconf) throws IllegalStateException {
        log.warn("Currently using local maps, needs to be moved to a distributed store");
        if (piPipeconfs.containsKey(pipeconf.id())) {
            throw new IllegalStateException(format("Pipeconf %s is already registered", pipeconf.id()));
        }
        piPipeconfs.put(pipeconf.id(), pipeconf);
        log.info("New pipeconf registered: {}", pipeconf.id());
    }

    @Override
    public Iterable<PiPipeconf> getPipeconfs() {
        return piPipeconfs.values();
    }

    @Override
    public Optional<PiPipeconf> getPipeconf(PiPipeconfId id) {
        return Optional.ofNullable(piPipeconfs.get(id));
    }

    @Override
    public CompletableFuture<Boolean> bindToDevice(PiPipeconfId pipeconfId, DeviceId deviceId) {
        CompletableFuture<Boolean> operationResult = new CompletableFuture<>();

        executor.execute(() -> {
            BasicDeviceConfig basicDeviceConfig =
                    cfgService.getConfig(deviceId, BasicDeviceConfig.class);
            Driver baseDriver = driverService.getDriver(basicDeviceConfig.driver());

            String completeDriverName = baseDriver.name() + ":" + pipeconfId;
            PiPipeconf piPipeconf = piPipeconfs.get(pipeconfId);
            if (piPipeconf == null) {
                log.warn("Pipeconf {} is not present", pipeconfId);
                operationResult.complete(false);
            } else {
                //if driver exists already we don't create a new one.
                //needs to be done via exception catching due to DriverRegistry throwing it on a null return from
                //the driver map.
                try {
                    driverService.getDriver(completeDriverName);
                } catch (ItemNotFoundException e) {

                    log.debug("First time pipeconf {} is used with base driver {}, merging the two",
                              pipeconfId, baseDriver);
                    //extract the behaviours from the pipipeconf.
                    Map<Class<? extends Behaviour>, Class<? extends Behaviour>> behaviours = new HashMap<>();
                    piPipeconf.behaviours().forEach(b -> {
                        behaviours.put(b, piPipeconf.implementation(b).get());
                    });

                    Driver piPipeconfDriver = new DefaultDriver(completeDriverName, baseDriver.parents(),
                                                                baseDriver.manufacturer(), baseDriver.hwVersion(),
                                                                baseDriver.swVersion(), behaviours, new HashMap<>());
                    //we take the base driver created with the behaviours of the PiPeconf and
                    // merge it with the base driver that was assigned to the device
                    Driver completeDriver = piPipeconfDriver.merge(baseDriver);

                    //This might lead to explosion of number of providers in the core,
                    // due to 1:1:1 pipeconf:driver:provider maybe find better way
                    DriverProvider provider = new PiPipeconfDriverProviderInternal(completeDriver);

                    //we register to the dirver susbystem the driver provider containing the merged driver
                    driverAdminService.registerProvider(provider);
                }

                //Changing the configuration for the device to enforce the full driver with pipipeconf
                // and base behaviours
                ObjectNode newCfg = (ObjectNode) basicDeviceConfig.node();
                newCfg = newCfg.put(DRIVER, completeDriverName);
                ObjectMapper mapper = new ObjectMapper();
                JsonNode newCfgNode = mapper.convertValue(newCfg, JsonNode.class);
                cfgService.applyConfig(deviceId, BasicDeviceConfig.class, newCfgNode);
                //Completable future is needed for when this method will also apply the pipeline to the device.
                operationResult.complete(true);
            }
        });
        return operationResult;
    }

    @Override
    public Optional<PiPipeconfId> ofDevice(DeviceId deviceId) {
        return Optional.ofNullable(devicesToPipeconf.get(deviceId));
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

    private void addPipeconfFromCfg(DeviceId deviceId) {
        PiPipeconfConfig pipeconfConfig =
                cfgService.getConfig(deviceId, PiPipeconfConfig.class);
        PiPipeconfId id = pipeconfConfig.piPipeconfId();
        if (id.id().equals("")) {
            log.warn("Not adding empty pipeconfId for device {}", deviceId);
        } else {
            devicesToPipeconf.put(deviceId, pipeconfConfig.piPipeconfId());
        }
    }

    /**
     * Listener for configuration events.
     */
    private class InternalNetworkConfigListener implements NetworkConfigListener {


        @Override
        public void event(NetworkConfigEvent event) {
            DeviceId deviceId = (DeviceId) event.subject();
            addPipeconfFromCfg(deviceId);
        }

        @Override
        public boolean isRelevant(NetworkConfigEvent event) {
            return event.configClass().equals(PiPipeconfConfig.class) &&
                    (event.type() == NetworkConfigEvent.Type.CONFIG_ADDED ||
                            event.type() == NetworkConfigEvent.Type.CONFIG_UPDATED);
        }
    }
}
