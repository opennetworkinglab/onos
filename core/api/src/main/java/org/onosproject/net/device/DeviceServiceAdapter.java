/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.net.device;

import java.util.Collections;
import java.util.List;

import org.onosproject.net.Device;
import org.onosproject.net.Device.Type;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;

import com.google.common.collect.FluentIterable;

/**
 * Test adapter for device service.
 */
public class DeviceServiceAdapter implements DeviceService {

    private List<Port> portList;

    /**
     * Constructor with port list.
     *
     * @param portList port list
     */
    public DeviceServiceAdapter(List<Port> portList) {
        this.portList = portList;
    }

    /**
     * Default constructor.
     */
    public DeviceServiceAdapter() {

    }

    @Override
    public int getDeviceCount() {
        return 0;
    }

    @Override
    public Iterable<Device> getDevices() {
        return Collections.emptyList();
    }

    @Override
    public Iterable<Device> getAvailableDevices() {
        return FluentIterable.from(getDevices())
                .filter(input -> isAvailable(input.id()));
    }

    @Override
    public Device getDevice(DeviceId deviceId) {
        return null;
    }

    @Override
    public MastershipRole getRole(DeviceId deviceId) {
        return MastershipRole.NONE;
    }

    @Override
    public List<Port> getPorts(DeviceId deviceId) {
        return portList;
    }

    @Override
    public List<PortStatistics> getPortStatistics(DeviceId deviceId) {
        return null;
    }

    @Override
    public List<PortStatistics> getPortDeltaStatistics(DeviceId deviceId) {
        return null;
    }

    @Override
    public PortStatistics getStatisticsForPort(DeviceId deviceId, PortNumber portNumber) {
        return null;
    }

    @Override
    public PortStatistics getDeltaStatisticsForPort(DeviceId deviceId, PortNumber portNumber) {
        return null;
    }

    @Override
    public Port getPort(DeviceId deviceId, PortNumber portNumber) {
        return getPorts(deviceId).stream()
                   .filter(port -> deviceId.equals(port.element().id()))
                   .filter(port -> portNumber.equals(port.number()))
                   .findFirst().orElse(null);
    }

    @Override
    public boolean isAvailable(DeviceId deviceId) {
        return false;
    }

    @Override
    public void addListener(DeviceListener listener) {
    }

    @Override
    public void removeListener(DeviceListener listener) {
    }

    @Override
    public Iterable<Device> getDevices(Type type) {
        return Collections.emptyList();
    }

    @Override
    public Iterable<Device> getAvailableDevices(Type type) {
        return Collections.emptyList();
    }

    @Override
    public String localStatus(DeviceId deviceId) {
        return null;
    }

}
