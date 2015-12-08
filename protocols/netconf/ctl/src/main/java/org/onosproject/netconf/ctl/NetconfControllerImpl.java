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

package org.onosproject.netconf.ctl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.IpAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfDevice;
import org.onosproject.netconf.NetconfDeviceInfo;
import org.onosproject.netconf.NetconfDeviceListener;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * The implementation of NetconfController.
 */
@Component(immediate = true)
@Service
public class NetconfControllerImpl implements NetconfController {

    public static final Logger log = LoggerFactory
            .getLogger(NetconfControllerImpl.class);

    public Map<DeviceId, NetconfDevice> netconfDeviceMap = new ConcurrentHashMap<>();

    protected Set<NetconfDeviceListener> netconfDeviceListeners = new CopyOnWriteArraySet<>();

    @Activate
    public void activate(ComponentContext context) {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        netconfDeviceMap.clear();
        log.info("Stopped");
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
            if (IpAddress.valueOf(info.uri().getHost()).equals(ip) &&
                    info.uri().getPort() == port) {
                return netconfDeviceMap.get(info);
            }
        }
        return null;
    }

    @Override
    public NetconfDevice connectDevice(NetconfDeviceInfo deviceInfo) throws IOException {
        if (netconfDeviceMap.containsKey(deviceInfo.getDeviceId())) {
            log.warn("Device {} is already present", deviceInfo);
            return netconfDeviceMap.get(deviceInfo.getDeviceId());
        } else {
            log.info("Creating NETCONF device {}", deviceInfo);
            return createDevice(deviceInfo);
        }
    }

    @Override
    public void removeDevice(NetconfDeviceInfo deviceInfo) {
        if (netconfDeviceMap.containsKey(deviceInfo.getDeviceId())) {
            log.warn("Device {} is not present", deviceInfo);
        } else {
            stopDevice(deviceInfo);
        }
    }

    private NetconfDevice createDevice(NetconfDeviceInfo deviceInfo) throws IOException {
        NetconfDevice netconfDevice = null;
        netconfDevice = new NetconfDeviceImpl(deviceInfo);
        for (NetconfDeviceListener l : netconfDeviceListeners) {
            l.deviceAdded(deviceInfo);
        }
        netconfDeviceMap.put(deviceInfo.getDeviceId(), netconfDevice);
        return netconfDevice;
    }

    private void stopDevice(NetconfDeviceInfo deviceInfo) {
        netconfDeviceMap.get(deviceInfo.getDeviceId()).disconnect();
        netconfDeviceMap.remove(deviceInfo.getDeviceId());
        for (NetconfDeviceListener l : netconfDeviceListeners) {
            l.deviceRemoved(deviceInfo);
        }
    }

    @Override
    public Map<DeviceId, NetconfDevice> getDevicesMap() {
        return netconfDeviceMap;
    }


}
