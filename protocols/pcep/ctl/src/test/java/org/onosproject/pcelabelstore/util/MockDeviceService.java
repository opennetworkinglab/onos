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
package org.onosproject.pcelabelstore.util;

import java.util.LinkedList;
import java.util.List;

import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.Device;
import org.onosproject.net.Device.Type;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;

/**
 * Test fixture for the device service.
 */
public class MockDeviceService implements DeviceService {
    private List<Device> devices = new LinkedList<>();
    private DeviceListener listener;

    /**
     * Adds a new device.
     *
     * @param dev device to be added
     */
    public void addDevice(Device dev) {
        devices.add(dev);
    }

    /**
     * Removes the specified device.
     *
     * @param dev device to be removed
     */
    public void removeDevice(Device dev) {
        devices.remove(dev);
    }

    @Override
    public Device getDevice(DeviceId deviceId) {
        for (Device dev : devices) {
            if (dev.id().equals(deviceId)) {
                return dev;
            }
        }
        return null;
    }

    @Override
    public Iterable<Device> getAvailableDevices() {
        return devices;
    }

    @Override
    public void addListener(DeviceListener listener) {
        this.listener = listener;
    }

    /**
     * Get the listener.
     */
    public DeviceListener getListener() {
        return listener;
    }

    @Override
    public void removeListener(DeviceListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public int getDeviceCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Iterable<Device> getDevices() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Iterable<Device> getDevices(Type type) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Iterable<Device> getAvailableDevices(Type type) {
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
}
