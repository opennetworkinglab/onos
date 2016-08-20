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

package org.onosproject.incubator.net.virtual.impl;

import com.google.common.collect.ImmutableList;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.incubator.net.virtual.VirtualDevice;
import org.onosproject.incubator.net.virtual.VirtualNetwork;
import org.onosproject.incubator.net.virtual.VirtualNetworkService;
import org.onosproject.incubator.net.virtual.VirtualPort;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortStatistics;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Device service implementation built on the virtual network service.
 */
public class VirtualNetworkDeviceService extends AbstractListenerManager<DeviceEvent, DeviceListener>
        implements DeviceService, VnetService {

    private static final String NETWORK_NULL = "Network ID cannot be null";
    private static final String TYPE_NULL = "Type cannot be null";
    private static final String DEVICE_NULL = "Device cannot be null";
    private static final String PORT_NUMBER_NULL = "PortNumber cannot be null";

    private final VirtualNetwork network;
    private final VirtualNetworkService manager;

    /**
     * Creates a new VirtualNetworkDeviceService object.
     *
     * @param virtualNetworkManager virtual network manager service
     * @param network               virtual network
     */
    public VirtualNetworkDeviceService(VirtualNetworkService virtualNetworkManager, VirtualNetwork network) {
        checkNotNull(network, NETWORK_NULL);
        this.network = network;
        this.manager = virtualNetworkManager;
    }

    @Override
    public int getDeviceCount() {
        return manager.getVirtualDevices(this.network.id()).size();
    }

    @Override
    public Iterable<Device> getDevices() {
        return manager.getVirtualDevices(this.network.id()).stream().collect(Collectors.toSet());
    }

    @Override
    public Iterable<Device> getDevices(Device.Type type) {
        checkNotNull(type, TYPE_NULL);
        return manager.getVirtualDevices(this.network.id())
                .stream()
                .filter(device -> type.equals(device.type()))
                .collect(Collectors.toSet());
    }

    @Override
    public Iterable<Device> getAvailableDevices() {
        return getDevices();
    }

    @Override
    public Iterable<Device> getAvailableDevices(Device.Type type) {
        return getDevices(type);
    }

    @Override
    public Device getDevice(DeviceId deviceId) {
        checkNotNull(deviceId, DEVICE_NULL);
        Optional<VirtualDevice> foundDevice =  manager.getVirtualDevices(this.network.id())
                .stream()
                .filter(device -> deviceId.equals(device.id()))
                .findFirst();
        if (foundDevice.isPresent()) {
            return foundDevice.get();
        }
        return null;
    }

    @Override
    public MastershipRole getRole(DeviceId deviceId) {
        checkNotNull(deviceId, DEVICE_NULL);
        // TODO hard coded to master for now.
        return MastershipRole.MASTER;
    }

    @Override
    public List<Port> getPorts(DeviceId deviceId) {
        checkNotNull(deviceId, DEVICE_NULL);
        return manager.getVirtualPorts(this.network.id(), deviceId)
                .stream()
                .collect(Collectors.toList());
    }

    @Override
    public List<PortStatistics> getPortStatistics(DeviceId deviceId) {
        checkNotNull(deviceId, DEVICE_NULL);
        // TODO not supported at the moment.
        return ImmutableList.of();
    }

    @Override
    public List<PortStatistics> getPortDeltaStatistics(DeviceId deviceId) {
        checkNotNull(deviceId, DEVICE_NULL);
        // TODO not supported at the moment.
        return ImmutableList.of();
    }

    @Override
    public PortStatistics getStatisticsForPort(DeviceId deviceId, PortNumber portNumber) {
        checkNotNull(deviceId, DEVICE_NULL);
        checkNotNull(deviceId, PORT_NUMBER_NULL);
        // TODO not supported at the moment.
        return null;
    }

    @Override
    public PortStatistics getDeltaStatisticsForPort(DeviceId deviceId, PortNumber portNumber) {
        checkNotNull(deviceId, DEVICE_NULL);
        checkNotNull(deviceId, PORT_NUMBER_NULL);
        // TODO not supported at the moment.
        return null;
    }

    @Override
    public Port getPort(DeviceId deviceId, PortNumber portNumber) {
        checkNotNull(deviceId, DEVICE_NULL);

        Optional<VirtualPort> foundPort =  manager.getVirtualPorts(this.network.id(), deviceId)
                .stream()
                .filter(port -> port.number().equals(portNumber))
                .findFirst();
        if (foundPort.isPresent()) {
            return foundPort.get();
        }
        return null;
    }

    @Override
    public boolean isAvailable(DeviceId deviceId) {
        return getDevice(deviceId) != null;
    }

    @Override
    public VirtualNetwork network() {
        return network;
    }
}
