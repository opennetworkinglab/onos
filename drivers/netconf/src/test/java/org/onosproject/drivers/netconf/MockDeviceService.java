/*
 * Copyright 2018-present Open Networking Foundation
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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.onosproject.net.Device;
import org.onosproject.net.Device.Type;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortStatistics;

public class MockDeviceService implements DeviceService {

    private List<Device> devices = new ArrayList<Device>();

    public void addDevice(Device device) {
        if (!devices.contains(device)) {
            devices.add(device);
        }
    }

    @Override
    public void addListener(DeviceListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeListener(DeviceListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public int getDeviceCount() {
        return devices.size();
    }

    @Override
    public Iterable<Device> getDevices() {
        return new ArrayList<Device>(devices);
    }

    @Override
    public Iterable<Device> getDevices(Type type) {
        return devices.stream().filter(d -> d.type() == type).collect(Collectors.toList());
    }

    @Override
    public Iterable<Device> getAvailableDevices() {
        return new ArrayList<Device>(devices);
    }

    @Override
    public Iterable<Device> getAvailableDevices(Type type) {
        return devices.stream().filter(d -> d.type() == type).collect(Collectors.toList());
    }

    @Override
    public Device getDevice(DeviceId deviceId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MastershipRole getRole(DeviceId deviceId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Port> getPorts(DeviceId deviceId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<PortStatistics> getPortStatistics(DeviceId deviceId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<PortStatistics> getPortDeltaStatistics(DeviceId deviceId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Port getPort(DeviceId deviceId, PortNumber portNumber) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isAvailable(DeviceId deviceId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String localStatus(DeviceId deviceId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getLastUpdatedInstant(DeviceId deviceId) {
        // TODO Auto-generated method stub
        return 0;
    }

}
