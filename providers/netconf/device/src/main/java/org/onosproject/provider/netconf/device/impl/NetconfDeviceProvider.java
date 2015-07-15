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
package org.onosproject.provider.netconf.device.impl;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onlab.util.Tools.delay;
import static org.onlab.util.Tools.get;
import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Dictionary;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.provider.netconf.device.impl.NetconfDevice.DeviceState;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

/**
 * Provider which will try to fetch the details of NETCONF devices from the core
 * and run a capability discovery on each of the device.
 */
@Component(immediate = true)
public class NetconfDeviceProvider extends AbstractProvider
        implements DeviceProvider {

    private final Logger log = getLogger(NetconfDeviceProvider.class);

    protected Map<DeviceId, NetconfDevice> netconfDeviceMap = new ConcurrentHashMap<DeviceId, NetconfDevice>();

    private DeviceProviderService providerService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    private ExecutorService deviceBuilder = Executors
            .newFixedThreadPool(1, groupedThreads("onos/netconf", "device-creator"));

    // Delay between events in ms.
    private static final int EVENTINTERVAL = 5;

    private static final String SCHEME = "netconf";

    @Property(name = "devConfigs", value = "", label = "Instance-specific configurations")
    private String devConfigs = null;

    @Property(name = "devPasswords", value = "", label = "Instance-specific password")
    private String devPasswords = null;

    /**
     * Creates a provider with the supplier identifier.
     */
    public NetconfDeviceProvider() {
        super(new ProviderId("netconf", "org.onosproject.provider.netconf"));
    }

    @Activate
    public void activate(ComponentContext context) {
        cfgService.registerProperties(getClass());
        providerService = providerRegistry.register(this);
        modified(context);
        log.info("Started");
    }

    @Deactivate
    public void deactivate(ComponentContext context) {
        cfgService.unregisterProperties(getClass(), false);
        try {
            for (Entry<DeviceId, NetconfDevice> deviceEntry : netconfDeviceMap
                    .entrySet()) {
                deviceBuilder.submit(new DeviceCreator(deviceEntry.getValue(),
                                                       false));
            }
            deviceBuilder.awaitTermination(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.error("Device builder did not terminate");
        }
        deviceBuilder.shutdownNow();
        netconfDeviceMap.clear();
        providerRegistry.unregister(this);
        providerService = null;
        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        if (context == null) {
            log.info("No configuration file");
            return;
        }
        Dictionary<?, ?> properties = context.getProperties();
        String deviceCfgValue = get(properties, "devConfigs");
        log.info("Settings: devConfigs={}", deviceCfgValue);
        if (!isNullOrEmpty(deviceCfgValue)) {
            addOrRemoveDevicesConfig(deviceCfgValue);
        }
    }

    private void addOrRemoveDevicesConfig(String deviceConfig) {
        for (String deviceEntry : deviceConfig.split(",")) {
            NetconfDevice device = processDeviceEntry(deviceEntry);
            if (device != null) {
                log.info("Device Detail: username: {}, host={}, port={}, state={}",
                        device.getUsername(), device.getSshHost(),
                         device.getSshPort(), device.getDeviceState().name());
                if (device.isActive()) {
                    deviceBuilder.submit(new DeviceCreator(device, true));
                } else {
                    deviceBuilder.submit(new DeviceCreator(device, false));
                }
            }
        }
    }

    private NetconfDevice processDeviceEntry(String deviceEntry) {
        if (deviceEntry == null) {
            log.info("No content for Device Entry, so cannot proceed further.");
            return null;
        }
        log.info("Trying to convert Device Entry String: " + deviceEntry
                + " to a Netconf Device Object");
        NetconfDevice device = null;
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
                device = new NetconfDevice(hostIp, hostPort, username, password);
                if (!isNullOrEmpty(deviceState)) {
                    if (deviceState.toUpperCase().equals(DeviceState.ACTIVE
                                                                 .name())) {
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
        // TODO Auto-generated method stub
    }

    @Override
    public void roleChanged(DeviceId deviceId, MastershipRole newRole) {

    }

    @Override
    public boolean isReachable(DeviceId deviceId) {
        NetconfDevice netconfDevice = netconfDeviceMap.get(deviceId);
        if (netconfDevice == null) {
            log.warn("BAD REQUEST: the requested device id: "
                    + deviceId.toString()
                    + "  is not associated to any NETCONF Device");
            return false;
        }
        return netconfDevice.isReachable();
    }

    /**
     * This class is intended to add or remove Configured Netconf Devices.
     * Functionality relies on 'createFlag' and 'NetconfDevice' content. The
     * functionality runs as a thread and dependening on the 'createFlag' value
     * it will create or remove Device entry from the core.
     */
    private class DeviceCreator implements Runnable {

        private NetconfDevice device;
        private boolean createFlag;

        public DeviceCreator(NetconfDevice device, boolean createFlag) {
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
         * For each Netconf Device, remove the entry from the device store.
         */
        private void removeDevices() {
            if (device == null) {
                log.warn("The Request Netconf Device is null, cannot proceed further");
                return;
            }
            try {
                DeviceId did = getDeviceId();
                if (!netconfDeviceMap.containsKey(did)) {
                    log.error("BAD Request: 'Currently device is not discovered, "
                            + "so cannot remove/disconnect the device: "
                            + device.deviceInfo() + "'");
                    return;
                }
                providerService.deviceDisconnected(did);
                device.disconnect();
                netconfDeviceMap.remove(did);
                delay(EVENTINTERVAL);
            } catch (URISyntaxException uriSyntaxExcpetion) {
                log.error("Syntax Error while creating URI for the device: "
                                  + device.deviceInfo()
                                  + " couldn't remove the device from the store",
                          uriSyntaxExcpetion);
            }
        }

        /**
         * Initialize Netconf Device object, and notify core saying device
         * connected.
         */
        private void advertiseDevices() {
            try {
                if (device == null) {
                    log.warn("The Request Netconf Device is null, cannot proceed further");
                    return;
                }
                device.init();
                DeviceId did = getDeviceId();
                ChassisId cid = new ChassisId();
                DeviceDescription desc = new DefaultDeviceDescription(
                                                                      did.uri(),
                                                                      Device.Type.OTHER,
                                                                      "", "",
                                                                      "", "",
                                                                      cid);
                log.info("Persisting Device" + did.uri().toString());

                netconfDeviceMap.put(did, device);
                providerService.deviceConnected(did, desc);
                log.info("Done with Device Info Creation on ONOS core. Device Info: "
                        + device.deviceInfo() + " " + did.uri().toString());
                delay(EVENTINTERVAL);
            } catch (URISyntaxException e) {
                log.error("Syntax Error while creating URI for the device: "
                        + device.deviceInfo()
                        + " couldn't persist the device onto the store", e);
            } catch (SocketTimeoutException e) {
                log.error("Error while setting connection for the device: "
                        + device.deviceInfo(), e);
            } catch (IOException e) {
                log.error("Error while setting connection for the device: "
                        + device.deviceInfo(), e);
            } catch (Exception e) {
                log.error("Error while initializing session for the device: "
                        + (device != null ? device.deviceInfo() : null), e);
            }
        }

        /**
         * This will build a device id for the device.
         */
        private DeviceId getDeviceId() throws URISyntaxException {
            String additionalSSP = new StringBuilder(device.getUsername())
                    .append("@").append(device.getSshHost()).append(":")
                    .append(device.getSshPort()).toString();
            DeviceId did = DeviceId.deviceId(new URI(SCHEME, additionalSSP,
                                                     null));
            return did;
        }
    }
}
