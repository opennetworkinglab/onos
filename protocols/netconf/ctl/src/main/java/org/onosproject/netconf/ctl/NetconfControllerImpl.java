/*
 * Copyright 2015-present Open Networking Laboratory
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

package org.onosproject.netconf.ctl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.IpAddress;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.key.DeviceKeyId;
import org.onosproject.net.key.DeviceKeyService;
import org.onosproject.net.key.UsernamePassword;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfDevice;
import org.onosproject.netconf.NetconfDeviceFactory;
import org.onosproject.netconf.NetconfDeviceInfo;
import org.onosproject.netconf.NetconfDeviceListener;
import org.onosproject.netconf.NetconfDeviceOutputEvent;
import org.onosproject.netconf.NetconfDeviceOutputEventListener;
import org.onosproject.netconf.NetconfException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onlab.util.Tools.get;

/**
 * The implementation of NetconfController.
 */
@Component(immediate = true)
@Service
public class NetconfControllerImpl implements NetconfController {
    private static final String PROP_NETCONF_REPLY_TIMEOUT = "netconfReplyTimeout";
    private static final int DEFAULT_REPLY_TIMEOUT_SECONDS = 5;
    @Property(name = PROP_NETCONF_REPLY_TIMEOUT, intValue = DEFAULT_REPLY_TIMEOUT_SECONDS,
            label = "Time (in seconds) waiting for a NetConf reply")
    protected static int netconfReplyTimeout = DEFAULT_REPLY_TIMEOUT_SECONDS;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceKeyService deviceKeyService;

    public static final Logger log = LoggerFactory
            .getLogger(NetconfControllerImpl.class);

    private Map<DeviceId, NetconfDevice> netconfDeviceMap = new ConcurrentHashMap<>();

    private final NetconfDeviceOutputEventListener downListener = new DeviceDownEventListener();

    protected Set<NetconfDeviceListener> netconfDeviceListeners = new CopyOnWriteArraySet<>();
    protected NetconfDeviceFactory deviceFactory = new DefaultNetconfDeviceFactory();

    @Activate
    public void activate(ComponentContext context) {
        cfgService.registerProperties(getClass());
        modified(context);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        cfgService.unregisterProperties(getClass(), false);
        netconfDeviceMap.clear();
        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        if (context == null) {
            netconfReplyTimeout = DEFAULT_REPLY_TIMEOUT_SECONDS;
            log.info("No component configuration");
            return;
        }

        Dictionary<?, ?> properties = context.getProperties();

        int newNetconfReplyTimeout;
        try {
            String s = get(properties, PROP_NETCONF_REPLY_TIMEOUT);
            newNetconfReplyTimeout = isNullOrEmpty(s) ?
                    netconfReplyTimeout : Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            log.warn("Component configuration had invalid value", e);
            return;
        }

        netconfReplyTimeout = newNetconfReplyTimeout;
        log.info("Settings: {} = {}", PROP_NETCONF_REPLY_TIMEOUT, netconfReplyTimeout);
    }

    @Override
    public void addDeviceListener(NetconfDeviceListener listener) {
        if (!netconfDeviceListeners.contains(listener)) {
            netconfDeviceListeners.add(listener);
        }
    }

    @Override
    public void removeDeviceListener(NetconfDeviceListener listener) {
        netconfDeviceListeners.remove(listener);
    }

    @Override
    public NetconfDevice getNetconfDevice(DeviceId deviceInfo) {
        return netconfDeviceMap.get(deviceInfo);
    }

    @Override
    public NetconfDevice getNetconfDevice(IpAddress ip, int port) {
        for (DeviceId info : netconfDeviceMap.keySet()) {
            if (info.uri().getSchemeSpecificPart().equals(ip.toString() + ":" + port)) {
                return netconfDeviceMap.get(info);
            }
        }
        return null;
    }

    @Override
    public NetconfDevice connectDevice(DeviceId deviceId) throws NetconfException {
        if (netconfDeviceMap.containsKey(deviceId)) {
            log.debug("Device {} is already present", deviceId);
            return netconfDeviceMap.get(deviceId);
        } else {
            log.debug("Creating NETCONF device {}", deviceId);
            Device device = deviceService.getDevice(deviceId);
            String ip;
            int port;
            if (device != null) {
                ip = device.annotations().value("ipaddress");
                port = Integer.parseInt(device.annotations().value("port"));
            } else {
                String[] info = deviceId.toString().split(":");
                if (info.length == 3) {
                    ip = info[1];
                    port = Integer.parseInt(info[2]);
                } else {
                    ip = Arrays.asList(info).stream().filter(el -> !el.equals(info[0])
                    && !el.equals(info[info.length - 1]))
                            .reduce((t, u) -> t + ":" + u)
                            .get();
                    log.debug("ip v6 {}", ip);
                    port = Integer.parseInt(info[info.length - 1]);
                }
            }
            try {
                UsernamePassword deviceKey = deviceKeyService.getDeviceKey(
                        DeviceKeyId.deviceKeyId(deviceId.toString())).asUsernamePassword();

                NetconfDeviceInfo deviceInfo = new NetconfDeviceInfo(deviceKey.username(),
                                                                     deviceKey.password(),
                                                                     IpAddress.valueOf(ip),
                                                                     port);
                NetconfDevice netconfDevicedevice = createDevice(deviceInfo);

                netconfDevicedevice.getSession().addDeviceOutputListener(downListener);
                return netconfDevicedevice;
            } catch (NullPointerException e) {
                throw new NetconfException("No Device Key for device " + deviceId, e);
            }
        }
    }

    @Override
    public void disconnectDevice(DeviceId deviceId, boolean remove) {
        if (!netconfDeviceMap.containsKey(deviceId)) {
            log.warn("Device {} is not present", deviceId);
        } else {
            stopDevice(deviceId, remove);
        }
    }

    private void stopDevice(DeviceId deviceId, boolean remove) {
        netconfDeviceMap.get(deviceId).disconnect();
        netconfDeviceMap.remove(deviceId);
        if (remove) {
            for (NetconfDeviceListener l : netconfDeviceListeners) {
                l.deviceRemoved(deviceId);
            }
        }
    }

    @Override
    public void removeDevice(DeviceId deviceId) {
        if (!netconfDeviceMap.containsKey(deviceId)) {
            log.warn("Device {} is not present", deviceId);
            for (NetconfDeviceListener l : netconfDeviceListeners) {
                l.deviceRemoved(deviceId);
            }
        } else {
            netconfDeviceMap.remove(deviceId);
            for (NetconfDeviceListener l : netconfDeviceListeners) {
                l.deviceRemoved(deviceId);
            }
        }
    }

    private NetconfDevice createDevice(NetconfDeviceInfo deviceInfo) throws NetconfException {
        NetconfDevice netconfDevice = deviceFactory.createNetconfDevice(deviceInfo);
        netconfDeviceMap.put(deviceInfo.getDeviceId(), netconfDevice);
        for (NetconfDeviceListener l : netconfDeviceListeners) {
            l.deviceAdded(deviceInfo.getDeviceId());
        }
        return netconfDevice;
    }


    @Override
    public Map<DeviceId, NetconfDevice> getDevicesMap() {
        return netconfDeviceMap;
    }

    @Override
    public Set<DeviceId> getNetconfDevices() {
        return netconfDeviceMap.keySet();
    }



    //Device factory for the specific NetconfDeviceImpl
    private class DefaultNetconfDeviceFactory implements NetconfDeviceFactory {

        @Override
        public NetconfDevice createNetconfDevice(NetconfDeviceInfo netconfDeviceInfo) throws NetconfException {
            return new DefaultNetconfDevice(netconfDeviceInfo);
        }
    }

    //Listener for closed session with devices, gets triggered whe devices goes down
    // or sends the endpattern ]]>]]>
    private class DeviceDownEventListener implements NetconfDeviceOutputEventListener {

        @Override
        public void event(NetconfDeviceOutputEvent event) {
            if (event.type().equals(NetconfDeviceOutputEvent.Type.DEVICE_UNREGISTERED)) {
                removeDevice(event.getDeviceInfo().getDeviceId());
            }
        }

        @Override
        public boolean isRelevant(NetconfDeviceOutputEvent event) {
            return getDevicesMap().containsKey(event.getDeviceInfo().getDeviceId());
        }
    }
}
