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

package org.onosproject.provider.rest.device.impl;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.ChassisId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.net.config.basics.ConfigException;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.behaviour.DevicesDiscovery;
import org.onosproject.net.behaviour.PortDiscovery;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.DeviceService;
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
import org.slf4j.Logger;

import javax.ws.rs.ProcessingException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.config.NetworkConfigEvent.Type.CONFIG_ADDED;
import static org.onosproject.net.config.NetworkConfigEvent.Type.CONFIG_UPDATED;
import static org.onosproject.net.config.basics.SubjectFactories.APP_SUBJECT_FACTORY;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provider for devices that use REST as means of configuration communication.
 */
@Component(immediate = true)
public class RestDeviceProvider extends AbstractProvider
        implements DeviceProvider {
    private static final String APP_NAME = "org.onosproject.restsb";
    protected static final String REST = "rest";
    private static final String JSON = "json";
    private static final String PROVIDER = "org.onosproject.provider.rest.device";
    private static final String IPADDRESS = "ipaddress";
    private static final String HTTPS = "https";
    private static final String AUTHORIZATION_PROPERTY = "authorization";
    private static final String BASIC_AUTH_PREFIX = "Basic ";
    private static final String URL_SEPARATOR = "://";
    protected static final String ISNOTNULL = "Rest device is not null";
    private static final String UNKNOWN = "unknown";
    private static final int REST_TIMEOUT_SEC = 5;
    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected RestSBController controller;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverService driverService;


    private DeviceProviderService providerService;
    private ApplicationId appId;

    private final ExecutorService executor =
            Executors.newFixedThreadPool(5, groupedThreads("onos/restsbprovider", "device-installer-%d", log));

    protected final List<ConfigFactory> factories = ImmutableList.of(
            new ConfigFactory<ApplicationId, RestProviderConfig>(APP_SUBJECT_FACTORY,
                                                                 RestProviderConfig.class,
                                                                 "rest_devices",
                                                                 true) {
                @Override
                public RestProviderConfig createConfig() {
                    return new RestProviderConfig();
                }
            },
            new ConfigFactory<DeviceId, RestDeviceConfig>(SubjectFactories.DEVICE_SUBJECT_FACTORY,
                                                          RestDeviceConfig.class,
                                                          REST) {
                @Override
                public RestDeviceConfig createConfig() {
                    return new RestDeviceConfig();
                }
            });

    private final NetworkConfigListener cfgLister = new InternalNetworkConfigListener();

    private Set<DeviceId> addedDevices = new HashSet<>();


    @Activate
    public void activate() {
        appId = coreService.registerApplication(APP_NAME);
        providerService = providerRegistry.register(this);
        factories.forEach(cfgService::registerConfigFactory);
        cfgService.addListener(cfgLister);
        executor.execute(RestDeviceProvider.this::createAndConnectDevices);
        executor.execute(RestDeviceProvider.this::createDevices);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        cfgService.removeListener(cfgLister);
        controller.getDevices().keySet().forEach(this::deviceRemoved);
        providerRegistry.unregister(this);
        providerService = null;
        factories.forEach(cfgService::unregisterConfigFactory);
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
        // TODO: This will be implemented later.
    }


    @Override
    public boolean isReachable(DeviceId deviceId) {
        RestSBDevice restDevice = controller.getDevice(deviceId);
        if (restDevice == null) {
            restDevice = controller.getProxySBDevice(deviceId);
            if (restDevice == null) {
                log.debug("the requested device id: " +
                                  deviceId.toString() +
                                  "  is not associated to any REST or REST " +
                                  "proxy Device");
                return false;
            }
        }
        return restDevice.isActive();
    }

    private void deviceAdded(RestSBDevice restSBDev) {
        checkNotNull(restSBDev, ISNOTNULL);

        //check if the server is controlling a single or multiple devices
        if (restSBDev.isProxy()) {

            Driver driver = driverService.getDriver(restSBDev.manufacturer().get(),
                                                    restSBDev.hwVersion().get(),
                                                    restSBDev.swVersion().get());

            if (driver != null && driver.hasBehaviour(DevicesDiscovery.class)) {

                //Creates the driver to communicate with the server
                DevicesDiscovery devicesDiscovery =
                        devicesDiscovery(restSBDev, driver);
                Set<DeviceId> deviceIds = devicesDiscovery.deviceIds();
                restSBDev.setActive(true);
                deviceIds.stream().forEach(deviceId -> {
                    controller.addProxiedDevice(deviceId, restSBDev);
                    DeviceDescription devDesc =
                            devicesDiscovery.deviceDetails(deviceId);
                    checkNotNull(devDesc,
                                 "deviceDescription cannot be null");
                    providerService.deviceConnected(
                            deviceId, mergeAnn(restSBDev.deviceId(), devDesc));

                    if (driver.hasBehaviour(DeviceDescriptionDiscovery.class)) {
                        DriverHandler h = driverService.createHandler(deviceId);
                        DeviceDescriptionDiscovery devDisc =
                                h.behaviour(DeviceDescriptionDiscovery.class);
                        providerService.updatePorts(deviceId,
                                                    devDisc.discoverPortDetails());
                    }

                    checkAndUpdateDevice(deviceId);
                    addedDevices.add(deviceId);
                });
            } else {
                log.warn("Driver not found for {}", restSBDev);
            }
        } else {
            DeviceId deviceId = restSBDev.deviceId();
            ChassisId cid = new ChassisId();
            String ipAddress = restSBDev.ip().toString();
            SparseAnnotations annotations = DefaultAnnotations.builder()
                    .set(IPADDRESS, ipAddress)
                    .set(AnnotationKeys.PROTOCOL, REST.toUpperCase())
                    .build();
            DeviceDescription deviceDescription = new DefaultDeviceDescription(
                    deviceId.uri(),
                    Device.Type.SWITCH,
                    UNKNOWN, UNKNOWN,
                    UNKNOWN, UNKNOWN,
                    cid,
                    annotations);
            restSBDev.setActive(true);
            providerService.deviceConnected(deviceId, deviceDescription);
            checkAndUpdateDevice(deviceId);
            addedDevices.add(deviceId);
        }
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
        DevicesDiscovery devicesDiscovery = driver.createBehaviour(driverData,
                                                                   DevicesDiscovery.class);
        devicesDiscovery.setHandler(new DefaultDriverHandler(driverData));
        return devicesDiscovery;
    }

    private void checkAndUpdateDevice(DeviceId deviceId) {
        if (deviceService.getDevice(deviceId) == null) {
            log.warn("Device {} has not been added to store, " +
                             "maybe due to a problem in connectivity", deviceId);
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
                        //if ports are not discovered, retry the discovery
                        if (deviceService.getPorts(deviceId).isEmpty()) {
                            discoverPorts(deviceId);
                        }
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
        checkNotNull(deviceId, ISNOTNULL);
        providerService.deviceDisconnected(deviceId);
        controller.getProxiedDevices(deviceId).stream().forEach(device -> {
            controller.removeProxiedDevice(device);
            providerService.deviceDisconnected(device);
        });
        controller.removeDevice(deviceId);
    }

    //Method to connect devices provided via net-cfg under devices/ tree
    private void createAndConnectDevices() {
        Set<DeviceId> deviceSubjects =
                cfgService.getSubjects(DeviceId.class, RestDeviceConfig.class);
        connectDevices(deviceSubjects.stream()
                               .filter(deviceId -> deviceService.getDevice(deviceId) == null)
                               .map(deviceId -> {
                                   RestDeviceConfig config =
                                           cfgService.getConfig(deviceId, RestDeviceConfig.class);
                                   RestSBDevice device = new DefaultRestSBDevice(config.ip(),
                                                                                 config.port(),
                                                                                 config.username(),
                                                                                 config.password(),
                                                                                 config.protocol(),
                                                                                 config.url(),
                                                                                 false, config.testUrl(),
                                                                                 config.manufacturer(),
                                                                                 config.hwVersion(),
                                                                                 config.swVersion());
                                   return device;

                               }).collect(Collectors.toSet()));
    }

    //Old method to register devices provided via net-cfg under apps/rest/ tree
    private void createDevices() {
        RestProviderConfig cfg = cfgService.getConfig(appId, RestProviderConfig.class);
        try {
            if (cfg != null && cfg.getDevicesAddresses() != null) {
                connectDevices(cfg.getDevicesAddresses());

            }
        } catch (ConfigException e) {
            log.error("Configuration error {}", e);
        }
        log.debug("REST Devices {}", controller.getDevices());
        addedDevices.clear();
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
                .forEach(device -> {
                    deviceAdded(device);
                });
        //Removing devices not wanted anymore
        toBeRemoved.forEach(device -> deviceRemoved(device.deviceId()));
    }

    private void discoverPorts(DeviceId deviceId) {
        Device device = deviceService.getDevice(deviceId);
        //TODO remove when PortDiscovery is removed from master
        if (device.is(PortDiscovery.class)) {
            PortDiscovery portConfig = device.as(PortDiscovery.class);
            providerService.updatePorts(deviceId, portConfig.getPorts());
        } else {
            DeviceDescriptionDiscovery deviceDescriptionDiscovery =
                    device.as(DeviceDescriptionDiscovery.class);
            providerService.updatePorts(deviceId, deviceDescriptionDiscovery.discoverPortDetails());
        }
    }

    private boolean testDeviceConnection(RestSBDevice dev) {
        try {
            Callable<Boolean> connectionSuccess;

            if (dev.testUrl().isPresent()) {
                connectionSuccess = new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        return controller
                                .get(dev.deviceId(), dev.testUrl().get(), JSON) != null;
                    }
                };
            } else {
                connectionSuccess = new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        return controller.get(dev.deviceId(), "", JSON) != null;
                    }
                };
            }

            Future<Boolean> future = executor.submit(connectionSuccess);
            try {
                Boolean result = future.get(REST_TIMEOUT_SEC, TimeUnit.SECONDS);
                return result;
            } catch (TimeoutException ex) {
                log.warn("Connection to device {} timed out", dev.deviceId());
                return false;
            } catch (InterruptedException ex) {
                log.warn("Connection to device {} interrupted", dev.deviceId());
                return false;
            } catch (ExecutionException ex) {
                log.warn("Connection to device {} had a execution exception", dev.deviceId());
                return false;
            }

        } catch (ProcessingException e) {
            log.warn("Cannot connect to device {}", dev, e);
        }
        return false;
    }

    private class InternalNetworkConfigListener implements NetworkConfigListener {
        @Override
        public void event(NetworkConfigEvent event) {
            if (event.configClass().equals(RestDeviceConfig.class)) {
                executor.execute(RestDeviceProvider.this::createAndConnectDevices);
            } else {
                log.warn("Injecting device via this Json is deprecated, " +
                                 "please put configuration under devices/");
                executor.execute(RestDeviceProvider.this::createDevices);
            }
        }

        @Override
        public boolean isRelevant(NetworkConfigEvent event) {
            return (event.configClass().equals(RestDeviceConfig.class) ||
                    event.configClass().equals(RestProviderConfig.class)) &&
                    (event.type() == CONFIG_ADDED ||
                            event.type() == CONFIG_UPDATED);
        }
    }

    @Override
    public void changePortState(DeviceId deviceId, PortNumber portNumber,
                                boolean enable) {
        // TODO if required
    }
}
