/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.provider.bmv2.device.impl;

import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.util.SharedScheduledExecutors;
import org.onosproject.bmv2.api.runtime.Bmv2Device;
import org.onosproject.bmv2.api.runtime.Bmv2RuntimeException;
import org.onosproject.bmv2.api.service.Bmv2Controller;
import org.onosproject.bmv2.api.service.Bmv2DeviceContextService;
import org.onosproject.bmv2.api.service.Bmv2DeviceListener;
import org.onosproject.bmv2.api.service.Bmv2TableEntryService;
import org.onosproject.common.net.AbstractDeviceProvider;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.net.config.basics.ConfigException;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.driver.DefaultDriverData;
import org.onosproject.net.driver.DefaultDriverHandler;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.bmv2.api.runtime.Bmv2Device.*;
import static org.onosproject.net.Device.Type.SWITCH;
import static org.onosproject.net.config.basics.SubjectFactories.APP_SUBJECT_FACTORY;
import static org.onosproject.provider.bmv2.device.impl.Bmv2PortStatisticsGetter.getPortStatistics;
import static org.onosproject.provider.bmv2.device.impl.Bmv2PortStatisticsGetter.initCounters;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * BMv2 device provider.
 */
@Component(immediate = true)
public class Bmv2DeviceProvider extends AbstractDeviceProvider {

    private static final String APP_NAME = "org.onosproject.bmv2";

    private static final int POLL_PERIOD = 5_000; // milliseconds

    private final Logger log = getLogger(this.getClass());

    private final ExecutorService executorService = Executors
            .newFixedThreadPool(16, groupedThreads("onos/bmv2", "device-discovery", log));

    private final ScheduledExecutorService scheduledExecutorService = SharedScheduledExecutors.getPoolThreadExecutor();

    private final NetworkConfigListener cfgListener = new InternalNetworkConfigListener();

    private final ConfigFactory cfgFactory = new InternalConfigFactory();

    private final Map<DeviceId, DeviceDescription> lastDescriptions = Maps.newHashMap();

    private final ConcurrentMap<DeviceId, Lock> deviceLocks = Maps.newConcurrentMap();

    private final InternalDeviceListener deviceListener = new InternalDeviceListener();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry netCfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected Bmv2Controller controller;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected Bmv2DeviceContextService contextService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected Bmv2TableEntryService tableEntryService;

    private ApplicationId appId;
    private ScheduledFuture<?> poller;

    /**
     * Creates a Bmv2 device provider with the supplied identifier.
     */
    public Bmv2DeviceProvider() {
        super(new ProviderId("bmv2", "org.onosproject.provider.device"));
    }

    @Override
    protected void activate() {
        appId = coreService.registerApplication(APP_NAME);
        netCfgService.registerConfigFactory(cfgFactory);
        netCfgService.addListener(cfgListener);
        controller.addDeviceListener(deviceListener);
        if (poller != null) {
            poller.cancel(false);
        }
        poller = scheduledExecutorService.scheduleAtFixedRate(this::pollDevices, 1_000, POLL_PERIOD, MILLISECONDS);
        super.activate();
    }

    @Override
    protected void deactivate() {
        if (poller != null) {
            poller.cancel(false);
        }
        controller.removeDeviceListener(deviceListener);
        try {
            lastDescriptions.forEach((did, value) -> {
                executorService.execute(() -> disconnectDevice(did));
            });
            executorService.awaitTermination(1000, MILLISECONDS);
        } catch (InterruptedException e) {
            log.error("Device discovery threads did not terminate");
        }
        executorService.shutdownNow();
        netCfgService.unregisterConfigFactory(cfgFactory);
        netCfgService.removeListener(cfgListener);
        super.deactivate();
    }

    @Override
    public void triggerProbe(DeviceId deviceId) {
        // Asynchronously trigger probe task.
        executorService.execute(() -> executeProbe(deviceId));
    }

    private void executeProbe(DeviceId did) {
        boolean reachable = isReachable(did);
        log.debug("Probed device: id={}, reachable={}", did.toString(), reachable);
        if (reachable) {
            discoverDevice(did);
        } else {
            disconnectDevice(did);
        }
    }

    @Override
    public void roleChanged(DeviceId deviceId, MastershipRole newRole) {
        log.debug("roleChanged() is not yet implemented");
        // TODO: implement mastership handling
    }

    @Override
    public boolean isReachable(DeviceId deviceId) {
        return controller.isReacheable(deviceId);
    }

    @Override
    public void changePortState(DeviceId deviceId, PortNumber portNumber, boolean enable) {
        log.warn("changePortState() not supported");
    }

    private void discoverDevice(DeviceId did) {
        // Serialize discovery for the same device.
        Lock lock = deviceLocks.computeIfAbsent(did, k -> new ReentrantLock());
        lock.lock();
        try {
            log.debug("Starting device discovery... deviceId={}", did);

            if (contextService.getContext(did) == null) {
                // Device is a first timer.
                log.info("Setting DEFAULT context for {}", did);
                // It is important to do this before creating the device in the core
                // so other services won't find a null context.
                contextService.setDefaultContext(did);
                // Abort discovery, we'll receive a new hello once the swap has been performed.
                return;
            }

            DeviceDescription lastDescription = lastDescriptions.get(did);
            DeviceDescription thisDescription = getDeviceDescription(did);

            if (thisDescription != null) {
                boolean descriptionChanged = lastDescription == null ||
                        (!Objects.equals(thisDescription, lastDescription) ||
                                !Objects.equals(thisDescription.annotations(), lastDescription.annotations()));
                if (descriptionChanged || !deviceService.isAvailable(did)) {
                    resetDeviceState(did);
                    initPortCounters(did);
                    providerService.deviceConnected(did, thisDescription);
                    updatePortsAndStats(did);
                }
                lastDescriptions.put(did, thisDescription);
            } else {
                log.warn("Unable to get device description for {}", did);
                lastDescriptions.put(did, lastDescription);
            }
        } finally {
            lock.unlock();
        }
    }

    private DeviceDescription getDeviceDescription(DeviceId did) {
        Device device = deviceService.getDevice(did);
        DeviceDescriptionDiscovery discovery = null;
        if (device == null) {
            // Device not yet in the core. Manually get a driver.
            Driver driver = driverService.getDriver(MANUFACTURER, HW_VERSION, SW_VERSION);
            if (driver.hasBehaviour(DeviceDescriptionDiscovery.class)) {
                discovery = driver.createBehaviour(new DefaultDriverHandler(new DefaultDriverData(driver, did)),
                                                   DeviceDescriptionDiscovery.class);
            }
        } else if (device.is(DeviceDescriptionDiscovery.class)) {
            discovery = device.as(DeviceDescriptionDiscovery.class);
        }
        if (discovery == null) {
            log.warn("No DeviceDescriptionDiscovery behavior for device {}", did);
            return null;
        } else {
            return discovery.discoverDeviceDetails();
        }
    }

    private void resetDeviceState(DeviceId did) {
        try {
            controller.getAgent(did).resetState();
            // Tables emptied. Reset all bindings.
            tableEntryService.unbindAll(did);
        } catch (Bmv2RuntimeException e) {
            log.warn("Unable to reset {}: {}", did, e.toString());
        }
    }

    private void initPortCounters(DeviceId did) {
        try {
            initCounters(controller.getAgent(did));
        } catch (Bmv2RuntimeException e) {
            log.warn("Unable to init counter on {}: {}", did, e.explain());
        }
    }

    private void updatePortsAndStats(DeviceId did) {
        Device device = deviceService.getDevice(did);
        if (device.is(DeviceDescriptionDiscovery.class)) {
            DeviceDescriptionDiscovery discovery = device.as(DeviceDescriptionDiscovery.class);
            List<PortDescription> portDescriptions = discovery.discoverPortDetails();
            if (portDescriptions != null) {
                providerService.updatePorts(did, portDescriptions);
            }
        } else {
            log.warn("No DeviceDescriptionDiscovery behavior for device {}", did);
        }
        try {
            Collection<PortStatistics> portStats = getPortStatistics(controller.getAgent(did),
                                                                     deviceService.getPorts(did));
            providerService.updatePortStatistics(did, portStats);
        } catch (Bmv2RuntimeException e) {
            log.warn("Unable to get port statistics for {}: {}", did, e.explain());
        }
    }

    private void disconnectDevice(DeviceId did) {
        log.debug("Disconnecting device from core... deviceId={}", did);
        providerService.deviceDisconnected(did);
        lastDescriptions.remove(did);
    }

    private void pollDevices() {
        for (Device device: deviceService.getAvailableDevices(SWITCH)) {
            if (device.id().uri().getScheme().equals(SCHEME) &&
                    mastershipService.isLocalMaster(device.id())) {
                executorService.execute(() -> pollingTask(device.id()));
            }
        }
    }

    private void pollingTask(DeviceId deviceId) {
        log.debug("Polling device {}...", deviceId);
        if (isReachable(deviceId)) {
            updatePortsAndStats(deviceId);
        } else {
            disconnectDevice(deviceId);
        }
    }

    /**
     * Internal net-cfg config factory.
     */
    private class InternalConfigFactory extends ConfigFactory<ApplicationId, Bmv2ProviderConfig> {

        InternalConfigFactory() {
            super(APP_SUBJECT_FACTORY, Bmv2ProviderConfig.class, "devices", true);
        }

        @Override
        public Bmv2ProviderConfig createConfig() {
            return new Bmv2ProviderConfig();
        }
    }

    /**
     * Internal net-cfg event listener.
     */
    private class InternalNetworkConfigListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {
            Bmv2ProviderConfig cfg = netCfgService.getConfig(appId, Bmv2ProviderConfig.class);
            if (cfg != null) {
                try {
                    cfg.getDevicesInfo().stream().forEach(info -> {
                        // FIXME: require also bmv2 internal device id from net-cfg (now is default 0)
                        Bmv2Device bmv2Device = new Bmv2Device(info.ip().toString(), info.port(), 0);
                        triggerProbe(bmv2Device.asDeviceId());
                    });
                } catch (ConfigException e) {
                    log.error("Unable to read config: " + e);
                }
            } else {
                log.error("Unable to read config (was null)");
            }
        }

        @Override
        public boolean isRelevant(NetworkConfigEvent event) {
            return event.configClass().equals(Bmv2ProviderConfig.class) &&
                    (event.type() == NetworkConfigEvent.Type.CONFIG_ADDED ||
                            event.type() == NetworkConfigEvent.Type.CONFIG_UPDATED);
        }
    }

    /**
     * Listener triggered by the BMv2 controller each time a hello message is received.
     */
    private class InternalDeviceListener implements Bmv2DeviceListener {
        @Override
        public void handleHello(Bmv2Device device, int instanceId, String jsonConfigMd5) {
            log.debug("Received hello from {}", device);
            triggerProbe(device.asDeviceId());
        }
    }
}
