/*
 * Copyright 2015 Open Networking Laboratory
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

import com.btisystems.pronx.ems.core.snmp.DefaultSnmpConfigurationFactory;
import com.btisystems.pronx.ems.core.snmp.ISnmpConfiguration;
import com.btisystems.pronx.ems.core.snmp.ISnmpConfigurationFactory;
import com.btisystems.pronx.ems.core.snmp.ISnmpSession;
import com.btisystems.pronx.ems.core.snmp.ISnmpSessionFactory;
import com.btisystems.pronx.ems.core.snmp.SnmpSessionFactory;
import com.btisystems.pronx.ems.core.snmp.V2cSnmpConfiguration;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.ChassisId;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.behaviour.PortDiscovery;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onlab.util.Tools.delay;
import static org.onlab.util.Tools.get;
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

    protected Map<DeviceId, SnmpDevice> snmpDeviceMap = new ConcurrentHashMap<>();

    private DeviceProviderService providerService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    private final ExecutorService deviceBuilder = Executors
            .newFixedThreadPool(1, groupedThreads("onos/snmp", "device-creator", log));

    // Delay between events in ms.
    private static final int EVENTINTERVAL = 5;

    private static final String SCHEME = "snmp";

    @Property(name = "devConfigs", value = "", label = "Instance-specific configurations")
    private String devConfigs = null;

    @Property(name = "devPasswords", value = "", label = "Instance-specific password")
    private String devPasswords = null;

    //TODO Could be replaced with a service lookup, and bundles per device variant.
    Map<String, SnmpDeviceDescriptionProvider> providers = new HashMap<>();

    private final ISnmpSessionFactory sessionFactory;

    /**
     * Creates a provider with the supplier identifier.
     */
    public SnmpDeviceProvider() {
        super(new ProviderId("snmp", "org.onosproject.provider.device"));
        sessionFactory = new SnmpSessionFactory(
                new DefaultSnmpConfigurationFactory(new V2cSnmpConfiguration()));
        //TODO refactor, no hardcoding in provider, device information should be in drivers
        providers.put("1.3.6.1.4.1.18070.2.2", new Bti7000DeviceDescriptionProvider());
        providers.put("1.3.6.1.4.1.20408", new NetSnmpDeviceDescriptionProvider());
        providers.put("1.3.6.1.4.562.73.6", new LumentumDeviceDescriptionProvider());
    }

    @Activate
    public void activate(ComponentContext context) {
        log.info("activating for snmp devices ...");
        cfgService.registerProperties(getClass());
        providerService = providerRegistry.register(this);
        modified(context);
        log.info("activated ok");
    }

    @Deactivate
    public void deactivate(ComponentContext context) {

        log.info("deactivating for snmp devices ...");

        cfgService.unregisterProperties(getClass(), false);
        try {
            snmpDeviceMap
                    .entrySet().stream().forEach((deviceEntry) -> {
                        deviceBuilder.execute(new DeviceCreator(deviceEntry.getValue(), false));
                    });
            deviceBuilder.awaitTermination(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.error("Device builder did not terminate");
        }
        deviceBuilder.shutdownNow();
        snmpDeviceMap.clear();
        providerRegistry.unregister(this);
        providerService = null;
        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        log.info("modified ...");

        if (context == null) {
            log.info("No configuration file");
            return;
        }
        Dictionary<?, ?> properties = context.getProperties();

        log.info("properties={}", context.getProperties());

        String deviceCfgValue = get(properties, "devConfigs");
        log.info("Settings: devConfigs={}", deviceCfgValue);
        if (!isNullOrEmpty(deviceCfgValue)) {
            addOrRemoveDevicesConfig(deviceCfgValue);
        }
        log.info("... modified");

    }

    private void addOrRemoveDevicesConfig(String deviceConfig) {
        for (String deviceEntry : deviceConfig.split(",")) {
            SnmpDevice device = processDeviceEntry(deviceEntry);
            if (device != null) {
                log.info("Device Detail:host={}, port={}, state={}",
                        new Object[]{device.getSnmpHost(),
                            device.getSnmpPort(),
                            device.getDeviceState().name()}
                );
                if (device.isActive()) {
                    deviceBuilder.execute(new DeviceCreator(device, true));
                } else {
                    deviceBuilder.execute(new DeviceCreator(device, false));
                }
            }
        }
    }

    private SnmpDevice processDeviceEntry(String deviceEntry) {
        if (deviceEntry == null) {
            log.info("No content for Device Entry, so cannot proceed further.");
            return null;
        }
        log.info("Trying to convert {} to a SNMP Device Object", deviceEntry);
        SnmpDevice device = null;
        try {
            String userInfo = deviceEntry.substring(0, deviceEntry
                    .lastIndexOf('@'));
            String hostInfo = deviceEntry.substring(deviceEntry
                    .lastIndexOf('@') + 1);
            String[] infoSplit = userInfo.split(":");
            String username = infoSplit[0];
            String password = infoSplit[1];
            infoSplit = hostInfo.split(":");
            String hostIp = infoSplit[0];
            Integer hostPort;
            try {
                hostPort = Integer.parseInt(infoSplit[1]);
            } catch (NumberFormatException nfe) {
                log.error("Bad Configuration Data: Failed to parse host port number string: "
                        + infoSplit[1]);
                throw nfe;
            }
            String deviceState = infoSplit[2];
            if (isNullOrEmpty(username) || isNullOrEmpty(password)
                    || isNullOrEmpty(hostIp) || hostPort == 0) {
                log.warn("Bad Configuration Data: both user and device information parts of Configuration "
                        + deviceEntry + " should be non-nullable");
            } else {
                device = new SnmpDevice(hostIp, hostPort, password);
                if (!isNullOrEmpty(deviceState)) {
                    if (deviceState.toUpperCase().equals(DeviceState.ACTIVE.name())) {
                        device.setDeviceState(DeviceState.ACTIVE);
                    } else if (deviceState.toUpperCase()
                            .equals(DeviceState.INACTIVE.name())) {
                        device.setDeviceState(DeviceState.INACTIVE);
                    } else {
                        log.warn("Device State Information can not be empty, so marking the state as INVALID");
                        device.setDeviceState(DeviceState.INVALID);
                    }
                } else {
                    log.warn("The device entry do not specify state information, so marking the state as INVALID");
                    device.setDeviceState(DeviceState.INVALID);
                }
            }
        } catch (ArrayIndexOutOfBoundsException aie) {
            log.error("Error while reading config infromation from the config file: "
                    + "The user, host and device state infomation should be "
                    + "in the order 'userInfo@hostInfo:deviceState'"
                    + deviceEntry, aie);
        } catch (Exception e) {
            log.error("Error while parsing config information for the device entry: "
                    + deviceEntry, e);
        }
        return device;
    }

    @Override
    public void triggerProbe(DeviceId deviceId) {
        // TODO SNMP devices should be polled at scheduled intervals to retrieve their
        // reachability status and other details e.g.swVersion, serialNumber,chassis,
    }

    @Override
    public void roleChanged(DeviceId deviceId, MastershipRole newRole) {

    }

    @Override
    public boolean isReachable(DeviceId deviceId) {
        SnmpDevice snmpDevice = snmpDeviceMap.get(deviceId);
        if (snmpDevice == null) {
            log.warn("BAD REQUEST: the requested device id: "
                    + deviceId.toString()
                    + "  is not associated to any SNMP Device");
            return false;
        }
        return snmpDevice.isReachable();
    }

    @Override
    public void enablePort(DeviceId deviceId, PortNumber portNumber) {
        //TODO
    }

    @Override
    public void disablePort(DeviceId deviceId, PortNumber portNumber) {
        //TODO
    }

    /**
     * This class is intended to add or remove Configured SNMP Devices. Functionality relies on 'createFlag' and
     * 'SnmpDevice' content. The functionality runs as a thread and depending on the 'createFlag' value it will create
     * or remove Device entry from the core.
     */
    private class DeviceCreator implements Runnable {

        private SnmpDevice device;
        private boolean createFlag;

        public DeviceCreator(SnmpDevice device, boolean createFlag) {
            this.device = device;
            this.createFlag = createFlag;
        }

        @Override
        public void run() {
            if (createFlag) {
                log.info("Trying to create Device Info on ONOS core");
                advertiseDevices();
            } else {
                log.info("Trying to remove Device Info on ONOS core");
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
            try {
                DeviceId did = getDeviceId();
                if (!snmpDeviceMap.containsKey(did)) {
                    log.error("BAD Request: 'Currently device is not discovered, "
                            + "so cannot remove/disconnect the device: "
                            + device.deviceInfo() + "'");
                    return;
                }
                providerService.deviceDisconnected(did);
                device.disconnect();
                snmpDeviceMap.remove(did);
                delay(EVENTINTERVAL);
            } catch (URISyntaxException uriSyntaxExcpetion) {
                log.error("Syntax Error while creating URI for the device: "
                        + device.deviceInfo()
                        + " couldn't remove the device from the store",
                        uriSyntaxExcpetion);
            }
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
                device.init();
                DeviceId did = getDeviceId();
                ChassisId cid = new ChassisId();

                SparseAnnotations annotations = DefaultAnnotations.builder()
                        .set(AnnotationKeys.PROTOCOL, SCHEME.toUpperCase())
                        .build();

                DeviceDescription desc = new DefaultDeviceDescription(
                        did.uri(), Device.Type.OTHER, UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN, cid, annotations);

                desc = populateDescriptionFromDevice(did, desc);

                log.info("Persisting Device " + did.uri().toString());

                snmpDeviceMap.put(did, device);
                providerService.deviceConnected(did, desc);
                log.info("Done with Device Info Creation on ONOS core. Device Info: "
                        + device.deviceInfo() + " " + did.uri().toString());

                // Do port discovery if driver supports it
                Device d = deviceService.getDevice(did);
                if (d.is(PortDiscovery.class)) {
                    PortDiscovery portConfig = d.as(PortDiscovery.class);
                    if (portConfig != null) {
                        providerService.updatePorts(did, portConfig.getPorts());
                    }
                } else {
                    log.warn("No port discovery behaviour for device {}", did);
                }

                delay(EVENTINTERVAL);
            } catch (URISyntaxException e) {
                log.error("Syntax Error while creating URI for the device: "
                        + device.deviceInfo()
                        + " couldn't persist the device onto the store", e);
            } catch (Exception e) {
                log.error("Error while initializing session for the device: "
                        + (device != null ? device.deviceInfo() : null), e);
            }
        }
        /**
         * @deprecated 1.5.0 Falcon, not compliant with ONOS SB and driver architecture.
         */
        @Deprecated
        private DeviceDescription populateDescriptionFromDevice(DeviceId did, DeviceDescription desc) {
            String[] deviceComponents = did.toString().split(":");
            if (deviceComponents.length > 1) {
                String ipAddress = deviceComponents[1];
                String port = deviceComponents[2];

                ISnmpConfiguration config = new V2cSnmpConfiguration();
                config.setPort(Integer.parseInt(port));

                try (ISnmpSession session = sessionFactory.createSession(config, ipAddress)) {
                    // Each session will be auto-closed.
                    String deviceOid = session.identifyDevice();

                    if (providers.containsKey(deviceOid)) {
                        desc = providers.get(deviceOid).populateDescription(session, desc);
                    }

                } catch (IOException | RuntimeException ex) {
                    log.error("Failed to walk device.", ex.getMessage());
                    log.debug("Detailed problem was ", ex);
                }
            }
            return desc;
        }

        /**
         * This will build a device id for the device.
         */
        private DeviceId getDeviceId() throws URISyntaxException {
            String additionalSsp = new StringBuilder(
                    device.getSnmpHost()).append(":")
                    .append(device.getSnmpPort()).toString();
            return DeviceId.deviceId(new URI(SCHEME, additionalSsp,
                    null));
        }
    }

    protected ISnmpSessionFactory getSessionFactory(ISnmpConfigurationFactory configurationFactory) {
        return new SnmpSessionFactory(configurationFactory);
    }
}
