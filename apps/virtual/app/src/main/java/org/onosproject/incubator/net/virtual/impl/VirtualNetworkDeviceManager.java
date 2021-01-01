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

package org.onosproject.incubator.net.virtual.impl;

import com.google.common.collect.ImmutableList;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualDevice;
import org.onosproject.incubator.net.virtual.VirtualNetworkEvent;
import org.onosproject.incubator.net.virtual.VirtualNetworkListener;
import org.onosproject.incubator.net.virtual.VirtualNetworkService;
import org.onosproject.incubator.net.virtual.VirtualPort;
import org.onosproject.incubator.net.virtual.event.AbstractVirtualListenerManager;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceEvent.Type;
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
public class VirtualNetworkDeviceManager
        extends AbstractVirtualListenerManager<DeviceEvent, DeviceListener>
        implements DeviceService {

    private static final String TYPE_NULL = "Type cannot be null";
    private static final String DEVICE_NULL = "Device cannot be null";
    private static final String PORT_NUMBER_NULL = "PortNumber cannot be null";
    private VirtualNetworkListener virtualNetworkListener = new InternalVirtualNetworkListener();

    /**
     * Creates a new VirtualNetworkDeviceService object.
     *
     * @param virtualNetworkManager virtual network manager service
     * @param networkId a virtual network identifier
     */
    public VirtualNetworkDeviceManager(VirtualNetworkService virtualNetworkManager,
                                       NetworkId networkId) {
        super(virtualNetworkManager, networkId, DeviceEvent.class);
        manager.addListener(virtualNetworkListener);
    }

    @Override
    public int getDeviceCount() {
        return manager.getVirtualDevices(this.networkId).size();
    }

    @Override
    public Iterable<Device> getDevices() {
        return manager.getVirtualDevices(
                this.networkId).stream().collect(Collectors.toSet());
    }

    @Override
    public Iterable<Device> getDevices(Device.Type type) {
        checkNotNull(type, TYPE_NULL);
        return manager.getVirtualDevices(this.networkId)
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
        Optional<VirtualDevice> foundDevice =
                manager.getVirtualDevices(this.networkId)
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
        return manager.getVirtualPorts(this.networkId, deviceId)
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
    public PortStatistics getStatisticsForPort(DeviceId deviceId,
                                               PortNumber portNumber) {
        checkNotNull(deviceId, DEVICE_NULL);
        checkNotNull(portNumber, PORT_NUMBER_NULL);
        // TODO not supported at the moment.
        return null;
    }

    @Override
    public PortStatistics getDeltaStatisticsForPort(DeviceId deviceId,
                                                    PortNumber portNumber) {
        checkNotNull(deviceId, DEVICE_NULL);
        checkNotNull(portNumber, PORT_NUMBER_NULL);
        // TODO not supported at the moment.
        return null;
    }

    @Override
    public Port getPort(DeviceId deviceId, PortNumber portNumber) {
        checkNotNull(deviceId, DEVICE_NULL);

        Optional<VirtualPort> foundPort =
                manager.getVirtualPorts(this.networkId, deviceId)
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
    public String localStatus(DeviceId deviceId) {
        // TODO not supported at this time
        return null;
    }

    @Override
    public long getLastUpdatedInstant(DeviceId deviceId) {
        // TODO not supported at this time
        return 0;
    }

    /**
     * Translates VirtualNetworkEvent to DeviceEvent.
     */
    private class InternalVirtualNetworkListener implements VirtualNetworkListener {
        @Override
        public boolean isRelevant(VirtualNetworkEvent event) {
            return networkId().equals(event.subject());
        }

        @Override
        public void event(VirtualNetworkEvent event) {
            switch (event.type()) {
                case VIRTUAL_DEVICE_ADDED:
                    post(new DeviceEvent(Type.DEVICE_ADDED, event.virtualDevice()));
                    break;
                case VIRTUAL_DEVICE_UPDATED:
                    post(new DeviceEvent(Type.DEVICE_UPDATED, event.virtualDevice()));
                    break;
                case VIRTUAL_DEVICE_REMOVED:
                    post(new DeviceEvent(Type.DEVICE_REMOVED, event.virtualDevice()));
                    break;
                case VIRTUAL_PORT_ADDED:
                    post(new DeviceEvent(Type.PORT_ADDED, event.virtualDevice(), event.virtualPort()));
                    break;
                case VIRTUAL_PORT_UPDATED:
                    post(new DeviceEvent(Type.PORT_UPDATED, event.virtualDevice(), event.virtualPort()));
                    break;
                case VIRTUAL_PORT_REMOVED:
                    post(new DeviceEvent(Type.PORT_REMOVED, event.virtualDevice(), event.virtualPort()));
                    break;
                case NETWORK_UPDATED:
                case NETWORK_REMOVED:
                case NETWORK_ADDED:
                default:
                    // do nothing
                    break;
            }
        }
    }
}
