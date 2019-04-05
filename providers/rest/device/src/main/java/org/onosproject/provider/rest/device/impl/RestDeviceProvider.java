/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.provider.rest.device.impl;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import org.onlab.packet.ChassisId;
import org.onlab.util.SharedExecutors;
import org.onlab.util.SharedScheduledExecutorService;
import org.onlab.util.SharedScheduledExecutors;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.behaviour.DevicesDiscovery;
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
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.device.PortStatisticsDiscovery;
import org.onosproject.net.driver.DefaultDriverData;
import org.onosproject.net.driver.DefaultDriverHandler;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverData;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.protocol.rest.DefaultRestSBDevice;
import org.onosproject.protocol.rest.RestSBController;
import org.onosproject.protocol.rest.RestSBDevice;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.config.NetworkConfigEvent.Type.CONFIG_ADDED;
import static org.onosproject.net.config.NetworkConfigEvent.Type.CONFIG_REMOVED;
import static org.onosproject.net.config.NetworkConfigEvent.Type.CONFIG_UPDATED;
import static org.onosproject.provider.rest.device.impl.OsgiPropertyConstants.POLL_FREQUENCY;
import static org.onosproject.provider.rest.device.impl.OsgiPropertyConstants.POLL_FREQUENCY_DEFAULT;
import static org.onosproject.provider.rest.device.impl.OsgiPropertyConstants.TIMEOUT;
import static org.onosproject.provider.rest.device.impl.OsgiPropertyConstants.TIMEOUT_DEFAULT;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provider for devices that use REST as means of configuration communication.
 */
@Component(immediate = true,
        property = {
                POLL_FREQUENCY + ":Integer=" + POLL_FREQUENCY_DEFAULT,
                TIMEOUT + ":Integer=" + TIMEOUT_DEFAULT,
        })
public class RestDeviceProvider extends AbstractProvider
        implements DeviceProvider {
    private static final String APP_NAME = "org.onosproject.restsb";
    protected static final String REST = "rest";
    private static final String PROVIDER = "org.onosproject.provider.rest.device";
    private static final String IPADDRESS = "ipaddress";
    private static final String DEVICENULL = "Rest device is null";
    private static final String DRIVERNULL = "Driver is null";
    private static final String UNKNOWN = "unknown";
    private static final int EXECUTOR_THREAD_POOL_SIZE = 8;
    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected RestSBController controller;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetworkConfigRegistry netCfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService compCfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DriverService driverService;

    /** Configure poll frequency for port status and statistics; default is 30 seconds. */
    private int pollFrequency = POLL_FREQUENCY_DEFAULT;

    /** Configure timeout for a device reply; default is 5 seconds. */
    private int replyTimeout = TIMEOUT_DEFAULT;

    private DeviceProviderService providerService;
    private ApplicationId appId;

    private ExecutorService executor;
    private final SharedScheduledExecutorService portStatisticsExecutor =
            SharedScheduledExecutors.getPoolThreadExecutor();

    private final SharedScheduledExecutorService deviceConnectionExecutor =
            SharedScheduledExecutors.getPoolThreadExecutor();
    private ScheduledFuture<?> devicePollTask;
    private final List<ConfigFactory> factories = ImmutableList.of(
            new ConfigFactory<DeviceId, RestDeviceConfig>(SubjectFactories.DEVICE_SUBJECT_FACTORY,
                                                          RestDeviceConfig.class,
                                                          REST) {
                @Override
                public RestDeviceConfig createConfig() {
                    return new RestDeviceConfig();
                }
            });

    private final NetworkConfigListener configListener = new InternalNetworkConfigListener();

    private ScheduledFuture<?> scheduledTask;


    @Activate
    public void activate() {
        appId = coreService.registerApplication(APP_NAME);
        compCfgService.registerProperties(getClass());
        providerService = providerRegistry.register(this);
        factories.forEach(netCfgService::registerConfigFactory);
        executor = Executors.newFixedThreadPool(
            EXECUTOR_THREAD_POOL_SIZE, groupedThreads("onos/restsbprovider", "device-installer-%d", log)
        );
        netCfgService.addListener(configListener);
        executor.execute(RestDeviceProvider.this::createAndConnectDevices);
        scheduledTask = schedulePolling();
        devicePollTask = scheduleDevicePolling();
        log.info("Started");
    }

    @Modified
    public void modified(ComponentContext context) {
        int previousPollFrequency = pollFrequency;

        if (context != null) {
            Dictionary<?, ?> properties = context.getProperties();
            pollFrequency = Tools.getIntegerProperty(properties, POLL_FREQUENCY,
                                                     POLL_FREQUENCY_DEFAULT);
            replyTimeout = Tools.getIntegerProperty(properties, TIMEOUT,
                    TIMEOUT_DEFAULT);
            log.info("Configured. Poll frequency = {} seconds, reply timeout = {} seconds ",
                    pollFrequency, replyTimeout);
        }

        // Re-schedule only if frequency has changed
        if (!scheduledTask.isCancelled() && (previousPollFrequency != pollFrequency)) {
            log.info("Re-scheduling port statistics task with frequency {} seconds", pollFrequency);
            scheduledTask.cancel(true);
            scheduledTask = schedulePolling();
        }
    }

    @Deactivate
    public void deactivate() {
        compCfgService.unregisterProperties(getClass(), false);
        netCfgService.removeListener(configListener);
        providerRegistry.unregister(this);
        providerService = null;
        factories.forEach(netCfgService::unregisterConfigFactory);
        scheduledTask.cancel(true);
        executor.shutdown();
        devicePollTask.cancel(true);
        log.info("Stopped");
    }

    public RestDeviceProvider() {
        super(new ProviderId(REST, PROVIDER));
    }

    @Override
    public void triggerProbe(DeviceId deviceId) {
        // TODO: This will be implemented later.
        log.info("Triggering probe on device {}", deviceId);
    }

    @Override
    public void roleChanged(DeviceId deviceId, MastershipRole newRole) {
        log.debug("Received role {} request for device {}", newRole, deviceId);
        RestSBDevice device = controller.getDevice(deviceId);
        if (device != null && testDeviceConnection(device)) {
            providerService.receivedRoleReply(deviceId, newRole, newRole);
        } else {
            log.warn("Device not present or available {}", deviceId);
            providerService.receivedRoleReply(deviceId, newRole, MastershipRole.NONE);
        }
    }

    @Override
    public boolean isReachable(DeviceId deviceId) {
        RestSBDevice restDevice = controller.getDevice(deviceId);
        return restDevice != null && restDevice.isActive();
    }

    private ScheduledFuture scheduleDevicePolling() {
        return deviceConnectionExecutor.scheduleWithFixedDelay(() -> {
            try {
                controller.getDevices().values().stream().forEach(restSBDevice -> {
                            DeviceId deviceId = restSBDevice.deviceId();
                            if (deviceService.getDevice(deviceId) != null) {
                                boolean connected = testDeviceConnection(restSBDevice);
                                restSBDevice.setActive(connected);
                                if (deviceService.isAvailable(deviceId) && (!connected)) {
                                    providerService.deviceDisconnected(deviceId);
                                } else if (!deviceService.isAvailable(deviceId) && connected) {
                                    DeviceDescription devDesc = getDesc(restSBDevice);
                                    checkNotNull(devDesc, "deviceDescription cannot be null");
                                    providerService.deviceConnected(
                                            deviceId, mergeAnn(deviceId, devDesc));
                                }
                            }
                        }
                );
            } catch (Exception e) {
                log.error("Exception at schedule Device polling", e);
            }
        }, 1, pollFrequency, TimeUnit.SECONDS);
    }

    private DeviceDescription getDesc(RestSBDevice restSBDev) {
        DeviceId deviceId = restSBDev.deviceId();

        Driver driver = getDriver(restSBDev);

        if (restSBDev.isProxy()) {
            if (driver != null && driver.hasBehaviour(DevicesDiscovery.class)) {

                //Creates the driver to communicate with the server
                DevicesDiscovery devicesDiscovery =
                        devicesDiscovery(restSBDev, driver);
                return devicesDiscovery.deviceDetails(deviceId);
            } else {
                log.warn("Driver not found for {}", restSBDev);
                return null;
            }
        } else if (driver != null && driver.hasBehaviour(DeviceDescriptionDiscovery.class)) {
            DriverHandler h = driverService.createHandler(deviceId);
            DeviceDescriptionDiscovery deviceDiscovery = h.behaviour(DeviceDescriptionDiscovery.class);
            return deviceDiscovery.discoverDeviceDetails();
        }
        ChassisId cid = new ChassisId();
        String ipAddress = restSBDev.ip().toString();
        SparseAnnotations annotations = DefaultAnnotations.builder()
                .set(IPADDRESS, ipAddress)
                .set(AnnotationKeys.PROTOCOL, REST.toUpperCase())
                .build();
        String manufacturer = UNKNOWN;
        String hwVersion = UNKNOWN;
        String swVersion = UNKNOWN;
        String serialNumber = UNKNOWN;

        Device device = deviceService.getDevice(deviceId);
        if (device != null) {
            manufacturer = device.manufacturer();
            hwVersion = device.hwVersion();
            swVersion = device.swVersion();
            serialNumber = device.serialNumber();
        }

        return new DefaultDeviceDescription(
                deviceId.uri(),
                Device.Type.SWITCH,
                manufacturer, hwVersion,
                swVersion, serialNumber,
                cid,
                annotations);
    }

    private void deviceAdded(RestSBDevice restSBDev) {
        checkNotNull(restSBDev, DEVICENULL);

        Driver driver = getDriver(restSBDev);

        // Check if the server is controlling a single or multiple devices
        if (restSBDev.isProxy()) {
            if (driver.hasBehaviour(DevicesDiscovery.class)) {
                DevicesDiscovery devicesDiscovery = devicesDiscovery(restSBDev, driver);
                Set<DeviceId> deviceIds = devicesDiscovery.deviceIds();
                restSBDev.setActive(true);
                deviceIds.forEach(deviceId -> {
                    controller.addProxiedDevice(deviceId, restSBDev);
                    DeviceDescription devDesc = devicesDiscovery.deviceDetails(deviceId);
                    checkNotNull(devDesc, "DeviceDescription cannot be null");
                    providerService.deviceConnected(deviceId, mergeAnn(restSBDev.deviceId(), devDesc));

                    if (driver.hasBehaviour(DeviceDescriptionDiscovery.class)) {
                        DriverHandler h = driverService.createHandler(deviceId);
                        DeviceDescriptionDiscovery devDisc = h.behaviour(DeviceDescriptionDiscovery.class);
                        providerService.updatePorts(deviceId, devDisc.discoverPortDetails());
                    }

                    checkAndUpdateDevice(deviceId);
                });
            } else {
                log.warn("Device is proxy but driver does not have proxy discovery behaviour {}", restSBDev);
            }
        } else {
            DeviceId deviceId = restSBDev.deviceId();

            if (driver != null && driver.hasBehaviour(DevicesDiscovery.class)) {
                restSBDev.setActive(true);
                DevicesDiscovery devicesDiscovery = devicesDiscovery(restSBDev, driver);
                DeviceDescription deviceDescription = devicesDiscovery.deviceDetails(deviceId);
                checkNotNull(deviceDescription, "DeviceDescription cannot be null");
                providerService.deviceConnected(deviceId, deviceDescription);

                if (driver.hasBehaviour(DeviceDescriptionDiscovery.class)) {
                    DriverHandler h = driverService.createHandler(deviceId);
                    DeviceDescriptionDiscovery deviceDiscovery = h.behaviour(DeviceDescriptionDiscovery.class);
                    providerService.updatePorts(deviceId, deviceDiscovery.discoverPortDetails());
                }
            } else {
                DeviceDescription deviceDescription = getDesc(restSBDev);
                restSBDev.setActive(true);
                providerService.deviceConnected(deviceId, deviceDescription);
            }
            checkAndUpdateDevice(deviceId);
        }
    }

    private Driver getDriver(RestSBDevice restSBDev) {
        String driverName = netCfgService.getConfig(restSBDev.deviceId(), BasicDeviceConfig.class).driver();

        Driver driver = driverService.getDriver(driverName);

        if (driver == null) {
            driver = driverService.getDriver(restSBDev.manufacturer().get(),
                    restSBDev.hwVersion().get(),
                    restSBDev.swVersion().get());
        }

        checkNotNull(driver, DRIVERNULL);
        return driver;
    }

    private DefaultDeviceDescription mergeAnn(DeviceId devId, DeviceDescription desc) {
        return new DefaultDeviceDescription(
                desc,
                DefaultAnnotations.merge(
                        DefaultAnnotations.builder()
                                .set(AnnotationKeys.PROTOCOL, REST.toUpperCase())
                                // The rest server added as annotation to the device
                                .set(AnnotationKeys.REST_SERVER, devId.toString())
                                .build(),
                        desc.annotations()));
    }

    private DevicesDiscovery devicesDiscovery(RestSBDevice restSBDevice, Driver driver) {
        DriverData driverData = new DefaultDriverData(driver, restSBDevice.deviceId());
        DevicesDiscovery devicesDiscovery = driver.createBehaviour(driverData, DevicesDiscovery.class);
        devicesDiscovery.setHandler(new DefaultDriverHandler(driverData));
        return devicesDiscovery;
    }

    private void checkAndUpdateDevice(DeviceId deviceId) {
        if (deviceService.getDevice(deviceId) == null) {
            log.warn("Device {} has not been added to store, maybe due to a problem in connectivity", deviceId);
        } else {
            boolean isReachable = isReachable(deviceId);
            if (isReachable && deviceService.isAvailable(deviceId)) {
                Device device = deviceService.getDevice(deviceId);
                if (device.is(DeviceDescriptionDiscovery.class)) {
                    DeviceDescriptionDiscovery deviceDescriptionDiscovery =
                            device.as(DeviceDescriptionDiscovery.class);
                    DeviceDescription updatedDeviceDescription =
                            deviceDescriptionDiscovery.discoverDeviceDetails();
                    if (updatedDeviceDescription != null &&
                            !descriptionEquals(device, updatedDeviceDescription)) {
                        providerService.deviceConnected(
                                deviceId,
                                new DefaultDeviceDescription(
                                        updatedDeviceDescription, true,
                                        updatedDeviceDescription.annotations()));
                    }
                    //if ports are not discovered, retry the discovery
                    if (deviceService.getPorts(deviceId).isEmpty()) {
                        discoverPorts(deviceId);
                    }
                } else {
                    log.warn("No DeviceDescriptionDiscovery behaviour for device {}", deviceId);
                }
            } else if (!isReachable && deviceService.isAvailable(deviceId)) {
                providerService.deviceDisconnected(deviceId);
            }
        }
    }

    private boolean descriptionEquals(Device device, DeviceDescription updatedDeviceDescription) {
        return Objects.equal(device.id().uri(), updatedDeviceDescription.deviceUri())
                && Objects.equal(device.type(), updatedDeviceDescription.type())
                && Objects.equal(device.manufacturer(), updatedDeviceDescription.manufacturer())
                && Objects.equal(device.hwVersion(), updatedDeviceDescription.hwVersion())
                && Objects.equal(device.swVersion(), updatedDeviceDescription.swVersion())
                && Objects.equal(device.serialNumber(), updatedDeviceDescription.serialNumber())
                && Objects.equal(device.chassisId(), updatedDeviceDescription.chassisId())
                && Objects.equal(device.annotations(), updatedDeviceDescription.annotations());
    }

    private void deviceRemoved(DeviceId deviceId) {
        checkNotNull(deviceId, DEVICENULL);
        log.debug("Device removed called for {}", deviceId);
        providerService.deviceDisconnected(deviceId);
        controller.getProxiedDevices(deviceId).forEach(device -> {
            controller.removeProxiedDevice(device);
            providerService.deviceDisconnected(device);
        });
        controller.removeDevice(deviceId);
    }

    //Method to connect devices provided via net-cfg under devices/ tree
    private void createAndConnectDevices() {
        Set<DeviceId> deviceSubjects =
                netCfgService.getSubjects(DeviceId.class, RestDeviceConfig.class);
        log.debug("Connecting and configuring devices with received configuration:{}",
                deviceSubjects);
        connectDevices(deviceSubjects.stream()
                .filter(deviceId -> deviceService.getDevice(deviceId) == null)
                .map(deviceId -> {
                    RestDeviceConfig config =
                            netCfgService.getConfig(deviceId, RestDeviceConfig.class);
                    return toInactiveRestSBDevice(config);
                }).collect(Collectors.toSet()));
    }

    private RestSBDevice toInactiveRestSBDevice(RestDeviceConfig config) {
        return new DefaultRestSBDevice(config.ip(),
                config.port(),
                config.username(),
                config.password(),
                config.protocol(),
                config.url(),
                false,
                config.testUrl(),
                config.manufacturer(),
                config.hwVersion(),
                config.swVersion(),
                config.isProxy(),
                config.authenticationScheme(),
                config.token()
        );
    }

    private void connectDevices(Set<RestSBDevice> devices) {
        //Precomputing the devices to be removed
        Set<RestSBDevice> toBeRemoved = new HashSet<>(controller.getDevices().values());
        toBeRemoved.removeAll(devices);
        //Adding new devices
        devices.stream()
                .filter(device -> {
                    device.setActive(false);
                    controller.addDevice(device);
                    return testDeviceConnection(device);
                })
                .forEach(this::deviceAdded);
        //Removing devices not wanted anymore
        toBeRemoved.forEach(device -> deviceRemoved(device.deviceId()));
    }

    private void connectDevice(RestSBDevice device) {
        // TODO borrowed from above,
        // not sure why setting it to inactive
        device.setActive(false);
        controller.addDevice(device);
        if (testDeviceConnection(device)) {
            deviceAdded(device);
        }
    }

    private ScheduledFuture schedulePolling() {
        return portStatisticsExecutor.scheduleAtFixedRate(this::executePortStatisticsUpdate,
                                                          pollFrequency / 2, pollFrequency,
                                                          TimeUnit.SECONDS);
    }

    private void executePortStatisticsUpdate() {
        controller.getDevices().keySet().forEach(this::updatePortStatistics);
    }

    private void updatePortStatistics(DeviceId deviceId) {
        Device device = deviceService.getDevice(deviceId);
        checkNotNull(device, "device cannot be null");

        if (device.is(PortStatisticsDiscovery.class)) {
            PortStatisticsDiscovery portStatisticsDiscovery = device.as(PortStatisticsDiscovery.class);
            Collection<PortStatistics> portStatistics = portStatisticsDiscovery.discoverPortStatistics();
            if (portStatistics != null && !portStatistics.isEmpty()) {
                providerService.updatePortStatistics(deviceId, portStatistics);
            }
        } else {
            log.debug("No port statistics getter behaviour for device {}", deviceId);
        }
    }

    private void discoverPorts(DeviceId deviceId) {
        Device device = deviceService.getDevice(deviceId);
        DeviceDescriptionDiscovery deviceDescriptionDiscovery = device.as(DeviceDescriptionDiscovery.class);
        providerService.updatePorts(deviceId, deviceDescriptionDiscovery.discoverPortDetails());
    }

    private boolean testDeviceConnection(RestSBDevice dev) {
        try {
            Callable<Boolean> connectionSuccess;

            if (dev.testUrl().isPresent()) {
                connectionSuccess = () ->
                        controller.get(dev.deviceId(), dev.testUrl().get(), MediaType.APPLICATION_JSON_TYPE) != null;
            } else {
                connectionSuccess = () ->
                        controller.get(dev.deviceId(), "", MediaType.APPLICATION_JSON_TYPE) != null;
            }

            Future<Boolean> future = executor.submit(connectionSuccess);
            try {
                return future.get(replyTimeout, TimeUnit.SECONDS);
            } catch (TimeoutException ex) {
                log.warn("Connection to device {} timed out: {}", dev.deviceId(), ex.getMessage());
                return false;
            } catch (InterruptedException ex) {
                log.warn("Connection to device {} interrupted: {}", dev.deviceId(), ex.getMessage());
                Thread.currentThread().interrupt();
                return false;
            } catch (ExecutionException ex) {
                log.warn("Connection to device {} had an execution exception.", dev.deviceId(), ex);
                return false;
            }

        } catch (ProcessingException e) {
            log.warn("Cannot connect to device {}", dev, e);
        }
        return false;
    }

    private Runnable exceptionSafe(Runnable runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (Exception e) {
                log.error("Unhandled Exception", e);
            }
        };
    }

    private class InternalNetworkConfigListener implements NetworkConfigListener {
        @Override
        public void event(NetworkConfigEvent event) {
            if (!isRelevant(event)) {
                log.warn("Irrelevant network configuration event: {}", event);
                return;
            }

            ExecutorService bg = SharedExecutors.getSingleThreadExecutor();
            if (event.type() == CONFIG_REMOVED) {
                log.debug("Config {} event for rest device provider for {}",
                        event.type(), event.prevConfig().get().subject());
                DeviceId did = (DeviceId) event.subject();
                bg.execute(() -> deviceRemoved(did));
            } else {
                //CONFIG_ADDED or CONFIG_UPDATED
                log.debug("Config {} event for rest device provider for {}",
                        event.type(), event.config().get().subject());
                RestDeviceConfig cfg = (RestDeviceConfig) event.config().get();
                RestSBDevice restSBDevice = toInactiveRestSBDevice(cfg);
                bg.execute(exceptionSafe(() -> connectDevice(restSBDevice)));
            }
        }

        @Override
        public boolean isRelevant(NetworkConfigEvent event) {
            return event.configClass().equals(RestDeviceConfig.class) &&
                    (event.type() == CONFIG_ADDED ||
                     event.type() == CONFIG_UPDATED ||
                     event.type() == CONFIG_REMOVED);
        }
    }

    @Override
    public void changePortState(DeviceId deviceId, PortNumber portNumber,
                                boolean enable) {
        Device device = deviceService.getDevice(deviceId);
        if (device != null) {
            if (device.is(PortAdmin.class)) {
                PortAdmin portAdmin = device.as(PortAdmin.class);
                CompletableFuture<Boolean> modified;
                if (enable) {
                    modified = portAdmin.enable(portNumber);
                } else {
                    modified = portAdmin.disable(portNumber);
                }
                modified.thenAcceptAsync(result -> {
                    if (!result) {
                        log.warn("Device {} port {} state can't be changed to {}",
                                 deviceId, portNumber, enable);
                    }
                });

            } else {
                log.warn("Device {} does not support PortAdmin behavior", deviceId);
            }
        } else {
            log.warn("unable to get the device {}, port {} state can't be changed to {}",
                     deviceId, portNumber, enable);
        }
    }
}
