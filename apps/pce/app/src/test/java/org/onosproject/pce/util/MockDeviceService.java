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
package org.onosproject.pce.util;

import java.util.LinkedList;
import java.util.List;

import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;

/**
 * Test fixture for the device service.
 */
public class MockDeviceService extends DeviceServiceAdapter {
    private List<Device> devices = new LinkedList<>();
    private DeviceListener listener;

    public void addDevice(Device dev) {
        devices.add(dev);
    }

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
}
