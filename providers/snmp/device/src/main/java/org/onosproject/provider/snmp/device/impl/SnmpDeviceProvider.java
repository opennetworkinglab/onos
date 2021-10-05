/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.provider.snmp.device.impl;

import org.onosproject.snmp.SnmpException;
import org.onosproject.snmp.ctl.DefaultSnmpv3Device;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onlab.packet.ChassisId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
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
import org.onosproject.net.device.DeviceStore;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.snmp.SnmpController;
import org.onosproject.snmp.SnmpDevice;
import org.onosproject.snmp.SnmpDeviceConfig;
import org.onosproject.snmp.ctl.DefaultSnmpDevice;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.snmp4j.mp.SnmpConstants;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provider which will try to fetch the details of SNMP devices from the core and run a capability discovery on each of
 * the device.
 */
@Component(immediate = true)
public class SnmpDeviceProvider extends AbstractProvider
        implements DeviceProvider {

    private final Logger log = getLogger(SnmpDeviceProvider.class);

    private static final String UNKNOWN = "unknown";
    private static final String APP_NAME = "org.onosproject.snmp";
    protected static final String SCHEME = "snmp";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected SnmpController controller;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceStore deviceStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetworkConfigRegistry netCfgService;

    protected DeviceProviderService providerService;

    protected ApplicationId appId;

    private final ExecutorService deviceBuilderExecutor = Executors
            .newFixedThreadPool(5, groupedThreads("onos/snmp", "device-creator", log));

    protected final NetworkConfigListener cfgLister = new InternalNetworkConfigListener();


    protected final ConfigFactory factory =
            new ConfigFactory<DeviceId, SnmpDeviceConfig>(SubjectFactories.DEVICE_SUBJECT_FACTORY,
                                                          SnmpDeviceConfig.class,
                                                          SCHEME) {
                @Override
                public SnmpDeviceConfig createConfig() {
                    return new SnmpDeviceConfig();
                }
            };


    /**
     * Creates a provider with the supplier identifier.
     */
    public SnmpDeviceProvider() {
        super(new ProviderId("snmp", "org.onosproject.provider.device"));
        //FIXME multiple type of SNMP sessions
    }

    @Activate
    public void activate(ComponentContext context) {

        providerService = providerRegistry.register(this);
        appId = coreService.registerApplication(APP_NAME);
        netCfgService.registerConfigFactory(factory);
        netCfgService.addListener(cfgLister);
        connectDevices();
        modified(context);
        log.info("Started");
    }

    @Deactivate
    public void deactivate(ComponentContext context) {

        try {
            controller.getDevices().forEach(device -> {
                deviceBuilderExecutor.execute(new DeviceFactory(device, false));
            });
            deviceBuilderExecutor.awaitTermination(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Device builder did not terminate");
        }
        deviceBuilderExecutor.shutdownNow();
        netCfgService.unregisterConfigFactory(factory);
        netCfgService.removeListener(cfgLister);
        providerRegistry.unregister(this);
        providerService = null;
        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        log.info("Modified");
    }

    //Method to register devices provided via net-cfg under devices/ tree
    private void connectDevices() {
        Set<DeviceId> deviceSubjects =
                netCfgService.getSubjects(DeviceId.class, SnmpDeviceConfig.class);
        deviceSubjects.forEach(deviceId -> {
            SnmpDeviceConfig config =
                    netCfgService.getConfig(deviceId, SnmpDeviceConfig.class);
            if (config.version() == SnmpConstants.version2c) {
                buildDevice(new DefaultSnmpDevice(config));
            } else if (config.version() == SnmpConstants.version3) {
                buildDevice(new DefaultSnmpv3Device(config));
            } else {
                throw new SnmpException(
                        String.format("Invalid snmp version %d", config.version()));
            }
        });
    }

    private void buildDevice(SnmpDevice device) {
        if (device != null) {
            log.debug("Device Detail:host={}, port={}, state={}",
                      device.getSnmpHost(),
                      device.getSnmpPort(),
                      device.isReachable());
            if (device.isReachable()) {
                deviceBuilderExecutor.execute(new DeviceFactory(device, true));
            } else {
                deviceBuilderExecutor.execute(new DeviceFactory(device, false));
            }
        }
    }

    @Override
    public void triggerProbe(DeviceId deviceId) {
        // TODO SNMP devices should be polled at scheduled intervals to retrieve their
        // reachability status and other details e.g.swVersion, serialNumber,chassis,
    }

    @Override
    public void roleChanged(DeviceId deviceId, MastershipRole newRole) {
        // TODO Implement Masterhsip Service
    }

    @Override
    public boolean isReachable(DeviceId deviceId) {
        SnmpDevice snmpDevice = controller.getDevice(deviceId);
        if (snmpDevice == null) {
            log.warn("BAD REQUEST: the requested device id: "
                             + deviceId.toString()
                             + "  is not associated to any SNMP Device");
            return false;
        }
        return snmpDevice.isReachable();
    }

    /**
     * This class is intended to add or remove Configured SNMP Devices. Functionality relies on 'createFlag' and
     * 'SnmpDevice' content. The functionality runs as a thread and depending on the 'createFlag' value it will create
     * or remove Device entry from the core.
     */
    //FIXME consider rework.
    private class DeviceFactory implements Runnable {

        private SnmpDevice device;
        private boolean createFlag;

        public DeviceFactory(SnmpDevice device, boolean createFlag) {
            this.device = device;
            this.createFlag = createFlag;
        }

        @Override
        public void run() {
            if (createFlag) {
                log.debug("Trying to create Device Info on ONOS core");
                advertiseDevices();
            } else {
                log.debug("Trying to remove Device Info on ONOS core");
                removeDevices();
            }
        }

        /**
         * For each SNMP Device, remove the entry from the device store.
         */
        private void removeDevices() {
            if (device == null) {
                log.warn("The Request SNMP Device is null, cannot proceed further");
                return;
            }
            DeviceId did = device.deviceId();
            if (controller.getDevice(did) == null) {
                log.error("BAD Request: 'Currently device is not discovered, "
                                  + "so cannot remove/disconnect the device: "
                                  + device.deviceInfo() + "'");
                return;
            }
            providerService.deviceDisconnected(did);
            device.disconnect();
            controller.removeDevice(did);
        }

        /**
         * Initialize SNMP Device object, and notify core saying device connected.
         */
        private void advertiseDevices() {
            try {
                if (device == null) {
                    log.warn("The Request SNMP Device is null, cannot proceed further");
                    return;
                }
                DeviceId did = device.deviceId();
                ChassisId cid = new ChassisId();

                SparseAnnotations annotations = DefaultAnnotations.builder()
                        .set(AnnotationKeys.PROTOCOL, SCHEME.toUpperCase())
                        .build();

                DeviceDescription desc = new DefaultDeviceDescription(
                        did.uri(), Device.Type.OTHER, UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN, cid, annotations);

                log.debug("Persisting Device " + did.uri().toString());

                controller.addDevice(device);
                providerService.deviceConnected(did, desc);
                log.info("Added device to ONOS core. Device Info: "
                                 + device.deviceInfo() + " " + did.uri().toString());
                //FIXME this description will be populated only if driver is pushed from outside
                // because otherwise default driver is used
                Device d = deviceService.getDevice(did);
                if (d.is(DeviceDescriptionDiscovery.class)) {
                    DeviceDescriptionDiscovery descriptionDiscovery = d.as(DeviceDescriptionDiscovery.class);
                    DeviceDescription description = descriptionDiscovery.discoverDeviceDetails();
                    if (description != null) {
                        deviceStore.createOrUpdateDevice(
                                new ProviderId("snmp", "org.onosproject.provider.device"),
                                did, description);
                    } else {
                        log.info("No other description given for device {}", d.id());
                    }
                    providerService.updatePorts(did, descriptionDiscovery.discoverPortDetails());
                } else {
                    log.warn("No populate description and ports behaviour for device {}", did);
                }
            } catch (Exception e) {
                log.error("Error while initializing session for the device: "
                                  + (device != null ? device.deviceInfo() : null), e);
            }
        }
    }

    private class InternalNetworkConfigListener implements NetworkConfigListener {


        @Override
        public void event(NetworkConfigEvent event) {
            if (event.configClass().equals(SnmpDeviceConfig.class)) {
                connectDevices();
            } else {
                log.warn("Injecting device via this Json is deprecated, " +
                                 "please put configuration under devices/");
            }
        }

        @Override
        public boolean isRelevant(NetworkConfigEvent event) {
            return (event.configClass().equals(SnmpDeviceConfig.class)) &&
                    (event.type() == NetworkConfigEvent.Type.CONFIG_ADDED ||
                            event.type() == NetworkConfigEvent.Type.CONFIG_UPDATED);
        }
    }

    @Override
    public void changePortState(DeviceId deviceId, PortNumber portNumber,
                                boolean enable) {
        // TODO if required
    }
}
