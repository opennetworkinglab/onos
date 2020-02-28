/*
 * Copyright 2020-present Open Networking Foundation
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

package org.onosproject.t3.api;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents Network Information Base (NIB) for devices
 * and supports alternative functions to
 * {@link org.onosproject.net.device.DeviceService} for offline data.
 */
public class DeviceNib extends AbstractNib {

    private Map<Device, Set<Port>> devicePortMap;

    // use the singleton helper to create the instance
    protected DeviceNib() {
    }

    /**
     * Sets a map of device : ports of the device.
     *
     * @param devicePortMap device-ports map
     */
    public void setDevicePortMap(Map<Device, Set<Port>> devicePortMap) {
        this.devicePortMap = devicePortMap;
    }

    /**
     * Returns the device-ports map.
     *
     * @return device-ports map
     */
    public Map<Device, Set<Port>> getDevicePortMap() {
        return ImmutableMap.copyOf(devicePortMap);
    }

    /**
     * Returns the device with the specified identifier.
     *
     * @param deviceId device identifier
     * @return device or null if one with the given identifier is not known
     */
    public Device getDevice(DeviceId deviceId) {
        return devicePortMap.keySet().stream()
                .filter(device -> device.id().equals(deviceId))
                .findFirst().orElse(null);
    }

    /**
     * Returns the port with the specified connect point.
     *
     * @param cp connect point
     * @return device port
     */
    public Port getPort(ConnectPoint cp) {
        return devicePortMap.get(getDevice(cp.deviceId())).stream()
                .filter(port -> port.number().equals(cp.port()))
                .findFirst().orElse(null);
    }

    /**
     * Returns the list of ports associated with the device.
     *
     * @param deviceId device identifier
     * @return list of ports
     */
    public List<Port> getPorts(DeviceId deviceId) {
        return ImmutableList.copyOf(devicePortMap.get(getDevice(deviceId)));
    }

    /**
     * Indicates whether or not the device is presently online and available.
     * Availability, unlike reachability, denotes whether ANY node in the
     * cluster can discover that this device is in an operational state,
     * this does not necessarily mean that there exists a node that can
     * control this device.
     *
     * @param deviceId device identifier
     * @return true if the device is available
     */
    public boolean isAvailable(DeviceId deviceId) {
        Device device = getDevice(deviceId);
        // TODO: may need an extra REST API to get availableDevices from DeviceService, not from device annotations
        return device.annotations().value("available").equals("true") ? true : false;
    }

    /**
     * Returns the singleton instance of devices NIB.
     *
     * @return instance of devices NIB
     */
    public static DeviceNib getInstance() {
        return DeviceNib.SingletonHelper.INSTANCE;
    }

    private static class SingletonHelper {
        private static final DeviceNib INSTANCE = new DeviceNib();
    }

}
