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
package org.onosproject.drivers.netconf;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfDevice;
import org.onosproject.netconf.NetconfDeviceInfo;
import org.onosproject.netconf.NetconfDeviceListener;
import org.onosproject.netconf.NetconfException;

public class MockNetconfController implements NetconfController {

    private Map<DeviceId, NetconfDevice> devicesMap;

    public MockNetconfController() {
        devicesMap = new HashMap<DeviceId, NetconfDevice>();
    }

    @Override
    public void addDeviceListener(NetconfDeviceListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeDeviceListener(NetconfDeviceListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public NetconfDevice connectDevice(DeviceId deviceId) throws NetconfException {
        NetconfDevice mockNetconfDevice = null;

        String[] nameParts = deviceId.uri().toASCIIString().split(":");
        IpAddress ipAddress = Ip4Address.valueOf(nameParts[1]);
        int port = Integer.parseInt(nameParts[2]);
        NetconfDeviceInfo ncdi = new NetconfDeviceInfo("mock", "mock", ipAddress, port);
        try {
            mockNetconfDevice = (new MockNetconfDeviceFactory()).createNetconfDevice(ncdi);
            devicesMap.put(deviceId, mockNetconfDevice);
        } catch (NetconfException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return mockNetconfDevice;
    }

    @Override
    public void disconnectDevice(DeviceId deviceId, boolean remove) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeDevice(DeviceId deviceId) {
        // TODO Auto-generated method stub

    }

    @Override
    public Map<DeviceId, NetconfDevice> getDevicesMap() {
        return devicesMap;
    }

    @Override
    public Set<DeviceId> getNetconfDevices() {
        return devicesMap.keySet();
    }

    @Override
    public NetconfDevice getNetconfDevice(DeviceId deviceInfo) {
        return devicesMap.get(deviceInfo);
    }

    @Override
    public NetconfDevice getNetconfDevice(IpAddress ip, int port) {
        // TODO Auto-generated method stub
        return null;
    }

}
