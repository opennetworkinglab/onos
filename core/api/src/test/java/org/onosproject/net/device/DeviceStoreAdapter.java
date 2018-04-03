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

package org.onosproject.net.device;

import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.provider.ProviderId;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * Test adapter for the DeviceStore API.
 */
public class DeviceStoreAdapter implements DeviceStore {
    @Override
    public int getDeviceCount() {
        return 0;
    }

    @Override
    public int getAvailableDeviceCount() {
        return 0;
    }

    @Override
    public Iterable<Device> getDevices() {
        return null;
    }

    @Override
    public Iterable<Device> getAvailableDevices() {
        return null;
    }

    @Override
    public Device getDevice(DeviceId deviceId) {
        return null;
    }

    @Override
    public DeviceEvent createOrUpdateDevice(ProviderId providerId, DeviceId deviceId,
                                            DeviceDescription deviceDescription) {
        return null;
    }

    @Override
    public DeviceEvent markOnline(DeviceId deviceId) {
        return null;
    }

    @Override
    public DeviceEvent markOffline(DeviceId deviceId) {
        return null;
    }

    @Override
    public List<DeviceEvent> updatePorts(ProviderId providerId, DeviceId deviceId,
                                         List<PortDescription> portDescriptions) {
        return null;
    }

    @Override
    public DeviceEvent updatePortStatus(ProviderId providerId, DeviceId deviceId,
                                        PortDescription portDescription) {
        return null;
    }

    @Override
    public List<Port> getPorts(DeviceId deviceId) {
        return null;
    }

    @Override
    public Stream<PortDescription> getPortDescriptions(ProviderId providerId,
                                                       DeviceId deviceId) {
        return Stream.empty();
    }

    @Override
    public DeviceEvent updatePortStatistics(ProviderId providerId, DeviceId deviceId,
                                            Collection<PortStatistics> portStats) {
        return null;
    }

    @Override
    public List<PortStatistics> getPortStatistics(DeviceId deviceId) {
        return null;
    }

    @Override
    public PortStatistics getStatisticsForPort(DeviceId deviceId, PortNumber portNumber) {
        return null;
    }

    @Override
    public List<PortStatistics> getPortDeltaStatistics(DeviceId deviceId) {
        return null;
    }

    @Override
    public PortStatistics getDeltaStatisticsForPort(DeviceId deviceId, PortNumber portNumber) {
        return null;
    }

    @Override
    public Port getPort(DeviceId deviceId, PortNumber portNumber) {
        return null;
    }

    @Override
    public PortDescription getPortDescription(ProviderId providerId,
                                              DeviceId deviceId,
                                              PortNumber portNumber) {
        return null;
    }

    @Override
    public boolean isAvailable(DeviceId deviceId) {
        return false;
    }

    @Override
    public DeviceEvent removeDevice(DeviceId deviceId) {
        return null;
    }

    @Override
    public void setDelegate(DeviceStoreDelegate delegate) {

    }

    @Override
    public void unsetDelegate(DeviceStoreDelegate delegate) {

    }

    @Override
    public boolean hasDelegate() {
        return false;
    }
}
