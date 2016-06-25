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

import com.google.common.base.Preconditions;
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
import org.onosproject.net.behaviour.PortDiscovery;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.protocol.rest.RestSBController;
import org.onosproject.protocol.rest.RestSBDevice;
import org.slf4j.Logger;

import javax.ws.rs.ProcessingException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private static final String REST = "rest";
    private static final String PROVIDER = "org.onosproject.provider.rest.device";
    private static final String IPADDRESS = "ipaddress";
    private static final int TEST_CONNECT_TIMEOUT = 1000;
    private static final String HTTPS = "https";
    private static final String AUTHORIZATION_PROPERTY = "authorization";
    private static final String BASIC_AUTH_PREFIX = "Basic ";
    private static final String URL_SEPARATOR = "://";
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


    private DeviceProviderService providerService;
    protected static final String ISNOTNULL = "Rest device is not null";
    private static final String UNKNOWN = "unknown";

    private final ExecutorService executor =
            Executors.newFixedThreadPool(5, groupedThreads("onos/restsbprovider", "device-installer-%d", log));

    private final ConfigFactory factory =
            new ConfigFactory<ApplicationId, RestProviderConfig>(APP_SUBJECT_FACTORY,
                                                                 RestProviderConfig.class,
                                                                 "restDevices",
                                                                 true) {
                @Override
                public RestProviderConfig createConfig() {
                    return new RestProviderConfig();
                }
            };
    private final NetworkConfigListener cfgLister = new InternalNetworkConfigListener();
    private ApplicationId appId;

    private Set<DeviceId> addedDevices = new HashSet<>();


    @Activate
    public void activate() {
        appId = coreService.registerApplication(APP_NAME);
        providerService = providerRegistry.register(this);
        cfgService.registerConfigFactory(factory);
        cfgService.addListener(cfgLister);
        executor.execute(RestDeviceProvider.this::connectDevices);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        cfgService.removeListener(cfgLister);
        controller.getDevices().keySet().forEach(this::deviceRemoved);
        providerRegistry.unregister(this);
        providerService = null;
        cfgService.unregisterConfigFactory(factory);
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
            log.debug("the requested device id: " +
                              deviceId.toString() +
                              "  is not associated to any REST Device");
            return false;
        }
        return restDevice.isActive();
    }

    private void deviceAdded(RestSBDevice nodeId) {
        Preconditions.checkNotNull(nodeId, ISNOTNULL);
        DeviceId deviceId = nodeId.deviceId();
        ChassisId cid = new ChassisId();
        String ipAddress = nodeId.ip().toString();
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
        nodeId.setActive(true);
        providerService.deviceConnected(deviceId, deviceDescription);
        addedDevices.add(deviceId);
    }

    private void deviceRemoved(DeviceId deviceId) {
        Preconditions.checkNotNull(deviceId, ISNOTNULL);
        providerService.deviceDisconnected(deviceId);
        controller.removeDevice(deviceId);
    }

    private void connectDevices() {
        RestProviderConfig cfg = cfgService.getConfig(appId, RestProviderConfig.class);
        try {
            if (cfg != null && cfg.getDevicesAddresses() != null) {
                //Precomputing the devices to be removed
                Set<RestSBDevice> toBeRemoved = new HashSet<>(controller.getDevices().values());
                toBeRemoved.removeAll(cfg.getDevicesAddresses());
                //Adding new devices
                cfg.getDevicesAddresses().stream()
                        .filter(device -> {
                            device.setActive(false);
                            controller.addDevice(device);
                            return testDeviceConnection(device);
                        })
                        .forEach(device -> {
                            deviceAdded(device);
                        });
                //Removing devices not wanted anymore
                toBeRemoved.stream().forEach(device -> deviceRemoved(device.deviceId()));

            }
        } catch (ConfigException e) {
            log.error("Configuration error {}", e);
        }
        log.debug("REST Devices {}", controller.getDevices());
        addedDevices.forEach(this::discoverPorts);
        addedDevices.clear();

    }

    private void discoverPorts(DeviceId deviceId) {
        Device device = deviceService.getDevice(deviceId);
        //TODO remove when PortDiscovery is removed from master
        if (device.is(PortDiscovery.class)) {
            PortDiscovery portConfig = device.as(PortDiscovery.class);
            providerService.updatePorts(deviceId,
                                        portConfig.getPorts());
        } else if (device.is(DeviceDescriptionDiscovery.class)) {
            DeviceDescriptionDiscovery deviceDescriptionDiscovery =
                    device.as(DeviceDescriptionDiscovery.class);
            providerService.updatePorts(deviceId, deviceDescriptionDiscovery.discoverPortDetails());
        } else {
            log.warn("No portGetter behaviour for device {}", deviceId);
        }
    }

    private boolean testDeviceConnection(RestSBDevice device) {
        try {
            return controller.get(device.deviceId(), "", "json") != null;
        } catch (ProcessingException e) {
            log.warn("Cannot connect to device {}", device, e);
        }
        return false;
    }

    private class InternalNetworkConfigListener implements NetworkConfigListener {


        @Override
        public void event(NetworkConfigEvent event) {
            executor.execute(RestDeviceProvider.this::connectDevices);
        }

        @Override
        public boolean isRelevant(NetworkConfigEvent event) {
            //TODO refactor
            return event.configClass().equals(RestProviderConfig.class) &&
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
