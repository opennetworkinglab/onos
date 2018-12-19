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

package org.onosproject.provider.netconf.device.impl;

import org.onlab.packet.IpAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfDevice;
import org.onosproject.netconf.NetconfDeviceListener;
import org.onosproject.netconf.NetconfException;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Test Adapter for NetconfControllerAdapter API.
 */
public class NetconfControllerAdapter implements NetconfController {

    private Map<DeviceId, NetconfDevice> netconfDeviceMap = new ConcurrentHashMap<>();

    @Override
    public void addDeviceListener(NetconfDeviceListener listener) {
    }

    @Override
    public void removeDeviceListener(NetconfDeviceListener listener) {
    }

    @Override
    public NetconfDevice connectDevice(DeviceId deviceId) throws NetconfException {
        return null;
    }

    @Override
    public void disconnectDevice(DeviceId deviceId, boolean remove) {
    }

    @Override
    public void removeDevice(DeviceId deviceId) {

    }

    @Override
    public Map<DeviceId, NetconfDevice> getDevicesMap() {
        return netconfDeviceMap;
    }

    @Override
    public Set<DeviceId> getNetconfDevices() {
        return netconfDeviceMap.keySet();
    }

    @Override
    public NetconfDevice getNetconfDevice(DeviceId deviceInfo) {
        return netconfDeviceMap.get(deviceInfo);
    }

    @Override
    public NetconfDevice getNetconfDevice(IpAddress ip, int port) {
        return null;
    }

    @Override
    public NetconfDevice getNetconfDevice(IpAddress ip, int port, String path) {
        return null;
    }
}
