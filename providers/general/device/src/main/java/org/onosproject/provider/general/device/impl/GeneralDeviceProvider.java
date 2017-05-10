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

package org.onosproject.provider.general.device.impl;

import com.google.common.annotations.Beta;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.ChassisId;
import org.onlab.util.ItemNotFoundException;
import org.onosproject.core.CoreService;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.behaviour.PortAdmin;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.basics.BasicDeviceConfig;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceHandshaker;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.device.PortStatisticsDiscovery;
import org.onosproject.net.driver.Behaviour;
import org.onosproject.net.driver.DefaultDriverData;
import org.onosproject.net.driver.DefaultDriverHandler;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverData;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.key.DeviceKeyAdminService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.provider.general.device.api.GeneralProviderDeviceConfig;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.Executors.newScheduledThreadPool;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.device.DeviceEvent.Type;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provider which uses drivers to detect device and do initial handshake
 * and channel establishment with devices. Any other provider specific operation
 * is also delegated to the DeviceHandshaker driver.
 */
@Beta
@Component(immediate = true)
public class GeneralDeviceProvider extends AbstractProvider
        implements DeviceProvider {
    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceKeyAdminService deviceKeyAdminService;

    protected static final String APP_NAME = "org.onosproject.generaldeviceprovider";
    protected static final String URI_SCHEME = "device";
    protected static final String CFG_SCHEME = "generalprovider";
    private static final String DEVICE_PROVIDER_PACKAGE = "org.onosproject.general.provider.device";
    private static final int CORE_POOL_SIZE = 10;
    private static final String UNKNOWN = "unknown";
    private static final int PORT_STATS_PERIOD_SECONDS = 10;


    protected ScheduledExecutorService connectionExecutor
            = newScheduledThreadPool(CORE_POOL_SIZE,
            groupedThreads("onos/generaldeviceprovider-device",
                    "connection-executor-%d", log));
    protected ScheduledExecutorService portStatsExecutor
            = newScheduledThreadPool(CORE_POOL_SIZE,
            groupedThreads("onos/generaldeviceprovider-port-stats",
                    "port-stats-executor-%d", log));

    protected DeviceProviderService providerService;
    private InternalDeviceListener deviceListener = new InternalDeviceListener();

    protected final ConfigFactory factory =
            new ConfigFactory<DeviceId, GeneralProviderDeviceConfig>(
                    SubjectFactories.DEVICE_SUBJECT_FACTORY,
                    GeneralProviderDeviceConfig.class, CFG_SCHEME) {
                @Override
                public GeneralProviderDeviceConfig createConfig() {
                    return new GeneralProviderDeviceConfig();
                }
            };

    protected final NetworkConfigListener cfgListener = new InternalNetworkConfigListener();


    @Activate
    public void activate() {
        providerService = providerRegistry.register(this);
        coreService.registerApplication(APP_NAME);
        cfgService.registerConfigFactory(factory);
        cfgService.addListener(cfgListener);
        deviceService.addListener(deviceListener);
        //This will fail if ONOS has CFG and drivers which depend on this provider
        // are activated, failing due to not finding the driver.
        cfgService.getSubjects(DeviceId.class, GeneralProviderDeviceConfig.class)
                .forEach(did -> connectionExecutor.execute(() -> connectDevice(did)));
        log.info("Started");
    }


    @Deactivate
    public void deactivate() {
        portStatsExecutor.shutdown();
        cfgService.removeListener(cfgListener);
        //Not Removing the device so they can still be used from other driver providers
        //cfgService.getSubjects(DeviceId.class, GeneralProviderDeviceConfig.class)
        //          .forEach(did -> connectionExecutor.execute(() -> disconnectDevice(did)));
        connectionExecutor.shutdown();
        deviceService.removeListener(deviceListener);
        providerRegistry.unregister(this);
        providerService = null;
        cfgService.unregisterConfigFactory(factory);
        log.info("Stopped");
    }

    public GeneralDeviceProvider() {
        super(new ProviderId(URI_SCHEME, DEVICE_PROVIDER_PACKAGE));
    }


    @Override
    public void triggerProbe(DeviceId deviceId) {
        //TODO Really don't see the point of this in non OF Context,
        // for now testing reachability, can be moved to no-op
        log.debug("Triggering probe equals testing reachability on device {}", deviceId);
        isReachable(deviceId);
    }

    @Override
    public void roleChanged(DeviceId deviceId, MastershipRole newRole) {
        log.debug("Received role {} for device {}", newRole, deviceId);
        CompletableFuture<MastershipRole> roleReply = getHandshaker(deviceId).roleChanged(newRole);
        roleReply.thenAcceptAsync(mastership -> providerService.receivedRoleReply(deviceId, newRole, mastership));
    }

    @Override
    public boolean isReachable(DeviceId deviceId) {
        log.debug("Testing rechability for device {}", deviceId);
        CompletableFuture<Boolean> reachable = getHandshaker(deviceId).isReachable();
        try {
            return reachable.get(10, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("Device {} is not reachable", deviceId, e);
            return false;
        }
    }

    @Override
    public void changePortState(DeviceId deviceId, PortNumber portNumber,
                                boolean enable) {
        if (deviceService.getDevice(deviceId).is(PortAdmin.class)) {

            PortAdmin portAdmin = getPortAdmin(deviceId);
            CompletableFuture<Boolean> modified;
            if (enable) {
                modified = portAdmin.enable(portNumber);
            } else {
                modified = portAdmin.disable(portNumber);
            }
            modified.thenAcceptAsync(result -> {
                if (!result) {
                    log.warn("Your device {} port {} status can't be changed to {}",
                            deviceId, portNumber, enable);
                }
            });

        } else {
            log.warn("Device {} does not support PortAdmin behaviour", deviceId);
        }
    }

    private DeviceHandshaker getHandshaker(DeviceId deviceId) {
        Driver driver = getDriver(deviceId);
        return getBehaviour(driver, DeviceHandshaker.class,
                new DefaultDriverData(driver, deviceId));
    }

    private PortAdmin getPortAdmin(DeviceId deviceId) {
        Driver driver = getDriver(deviceId);
        return getBehaviour(driver, PortAdmin.class,
                new DefaultDriverData(driver, deviceId));

    }

    private Driver getDriver(DeviceId deviceId) {
        Driver driver;
        try {
            driver = driverService.getDriver(deviceId);
        } catch (ItemNotFoundException e) {
            log.debug("Falling back to configuration to fetch driver " +
                    "for device {}", deviceId);
            driver = driverService.getDriver(
                    cfgService.getConfig(deviceId, BasicDeviceConfig.class).driver());
        }
        return driver;
    }

    //needed since the device manager will not return the driver through implementation()
    // method since the device is not pushed to the core so for the connectDevice
    // we need to work around that in order to test before calling
    // store.createOrUpdateDevice
    private <T extends Behaviour> T getBehaviour(Driver driver, Class<T> type,
                                                 DriverData data) {
        if (driver.hasBehaviour(type)) {
            DefaultDriverHandler handler = new DefaultDriverHandler(data);
            return driver.createBehaviour(handler, type);
        } else {
            return null;
        }
    }

    //Connects a general device
    private void connectDevice(DeviceId deviceId) {
        //retrieve the configuration
        GeneralProviderDeviceConfig providerConfig =
                cfgService.getConfig(deviceId, GeneralProviderDeviceConfig.class);
        BasicDeviceConfig basicDeviceConfig =
                cfgService.getConfig(deviceId, BasicDeviceConfig.class);

        if (providerConfig == null || basicDeviceConfig == null) {
            log.error("Configuration is NULL: basic config {}, general provider " +
                    "config {}", basicDeviceConfig, providerConfig);
        } else {
            log.info("Connecting to device {}", deviceId);

            Driver driver = driverService.getDriver(basicDeviceConfig.driver());
            DriverData driverData = new DefaultDriverData(driver, deviceId);

            DeviceHandshaker handshaker =
                    getBehaviour(driver, DeviceHandshaker.class, driverData);

            if (handshaker != null) {

                //Storing deviceKeyId and all other config values
                // as data in the driver with protocol_<info>
                // name as the key. e.g protocol_ip
                providerConfig.protocolsInfo()
                        .forEach((protocol, deviceInfoConfig) -> {
                            deviceInfoConfig.configValues()
                                    .forEach((k, v) -> driverData.set(protocol + "_" + k, v));
                            driverData.set(protocol + "_key", deviceInfoConfig.deviceKeyId());
                        });

                //Connecting to the device
                CompletableFuture<Boolean> connected = handshaker.connect();

                connected.thenAcceptAsync(result -> {
                    if (result) {

                        //Populated with the default values obtained by the driver
                        ChassisId cid = new ChassisId();
                        SparseAnnotations annotations = DefaultAnnotations.builder()
                                .set(AnnotationKeys.PROTOCOL,
                                        providerConfig.protocolsInfo().keySet().toString())
                                .build();
                        DeviceDescription description =
                                new DefaultDeviceDescription(deviceId.uri(), Device.Type.SWITCH,
                                        driver.manufacturer(), driver.hwVersion(),
                                        driver.swVersion(), UNKNOWN,
                                        cid, false, annotations);
                        //Empty list of ports
                        List<PortDescription> ports = new ArrayList<>();

                        if (driver.hasBehaviour(DeviceDescriptionDiscovery.class)) {
                            DeviceDescriptionDiscovery deviceDiscovery = driver
                                    .createBehaviour(driverData, DeviceDescriptionDiscovery.class);

                            DeviceDescription newdescription = deviceDiscovery.discoverDeviceDetails();
                            if (newdescription != null) {
                                description = newdescription;
                            }
                            ports = deviceDiscovery.discoverPortDetails();
                        }
                        providerService.deviceConnected(deviceId, description);
                        providerService.updatePorts(deviceId, ports);

                    } else {
                        log.warn("Can't connect to device {}", deviceId);
                    }
                });
            } else {
                log.error("Device {}, with driver {} does not support DeviceHandshaker " +
                        "behaviour, {}", deviceId, driver.name(), driver.behaviours());
            }
        }
    }

    private void disconnectDevice(DeviceId deviceId) {
        log.info("Disconnecting for device {}", deviceId);
        DeviceHandshaker handshaker = getHandshaker(deviceId);
        if (handshaker != null) {
            CompletableFuture<Boolean> disconnect = handshaker.disconnect();

            disconnect.thenAcceptAsync(result -> {
                if (result) {
                    log.info("Disconnected device {}", deviceId);
                    providerService.deviceDisconnected(deviceId);
                } else {
                    log.warn("Device {} was unable to disconnect", deviceId);
                }
            });
        } else {
            //gracefully ignoring.
            log.info("No DeviceHandshaker for device {}", deviceId);
        }
    }

    //Needed to catch the exception in the executors since are not rethrown otherwise.
    private Runnable exceptionSafe(Runnable runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (Exception e) {
                log.error("Unhandled Exception", e);
            }
        };
    }

    private void updatePortStatistics(DeviceId deviceId) {
        Collection<PortStatistics> statistics = deviceService.getDevice(deviceId)
                .as(PortStatisticsDiscovery.class)
                .discoverPortStatistics();
        providerService.updatePortStatistics(deviceId, statistics);
    }

    /**
     * Listener for configuration events.
     */
    private class InternalNetworkConfigListener implements NetworkConfigListener {


        @Override
        public void event(NetworkConfigEvent event) {
            DeviceId deviceId = (DeviceId) event.subject();
            //Assuming that the deviceId comes with uri 'device:'
            if (!deviceId.uri().getScheme().equals(URI_SCHEME)) {
                // not under my scheme, skipping
                log.debug("{} is not my scheme, skipping", deviceId);
                return;
            }
            if (deviceService.getDevice(deviceId) == null || !deviceService.isAvailable(deviceId)) {
                connectionExecutor.submit(exceptionSafe(() -> connectDevice(deviceId)));
            } else {
                log.info("Device {} is already connected to ONOS and is available", deviceId);
            }
        }

        @Override
        public boolean isRelevant(NetworkConfigEvent event) {
            return event.configClass().equals(GeneralProviderDeviceConfig.class) &&
                    (event.type() == NetworkConfigEvent.Type.CONFIG_ADDED ||
                            event.type() == NetworkConfigEvent.Type.CONFIG_UPDATED);
        }
    }

    /**
     * Listener for core device events.
     */
    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            Type type = event.type();

            if (type.equals((Type.DEVICE_ADDED))) {

                //For now this is scheduled periodically, when streaming API will
                // be available we check and base it on the streaming API (e.g. gNMI)
                if (deviceService.getDevice(event.subject().id()).
                        is(PortStatisticsDiscovery.class)) {
                    portStatsExecutor.scheduleAtFixedRate(exceptionSafe(() ->
                                    updatePortStatistics(event.subject().id())),
                            0, PORT_STATS_PERIOD_SECONDS, TimeUnit.SECONDS);
                    updatePortStatistics(event.subject().id());
                }

            } else if (type.equals(Type.DEVICE_REMOVED)) {
                connectionExecutor.submit(exceptionSafe(() ->
                        disconnectDevice(event.subject().id())));
            }
        }

        @Override
        public boolean isRelevant(DeviceEvent event) {
            return URI_SCHEME.toUpperCase()
                    .equals(event.subject().id().uri().toString());
        }
    }
}
