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
package org.onosproject.net.utils;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import org.onosproject.net.Device;
import org.onosproject.net.Device.Type;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortStatistics;

import com.google.common.annotations.Beta;

/**
 * A DeviceService which forwards all its method calls to another DeviceService.
 */
@Beta
public abstract class ForwardingDeviceService implements DeviceService {

    private final DeviceService delegate;

    protected ForwardingDeviceService(DeviceService delegate) {
        this.delegate = checkNotNull(delegate);
    }

    protected final DeviceService delegate() {
        return delegate;
    }

    @Override
    public void addListener(DeviceListener listener) {
        delegate.addListener(listener);
    }

    @Override
    public void removeListener(DeviceListener listener) {
        delegate.removeListener(listener);
    }

    @Override
    public int getDeviceCount() {
        return delegate.getDeviceCount();
    }

    @Override
    public Iterable<Device> getDevices() {
        return delegate.getDevices();
    }

    @Override
    public Iterable<Device> getDevices(Type type) {
        return delegate.getDevices(type);
    }

    @Override
    public Iterable<Device> getAvailableDevices() {
        return delegate.getAvailableDevices();
    }

    @Override
    public Iterable<Device> getAvailableDevices(Type type) {
        return delegate.getAvailableDevices(type);
    }

    @Override
    public Device getDevice(DeviceId deviceId) {
        return delegate.getDevice(deviceId);
    }

    @Override
    public MastershipRole getRole(DeviceId deviceId) {
        return delegate.getRole(deviceId);
    }

    @Override
    public List<Port> getPorts(DeviceId deviceId) {
        return delegate.getPorts(deviceId);
    }

    @Override
    public List<PortStatistics> getPortStatistics(DeviceId deviceId) {
        return delegate.getPortStatistics(deviceId);
    }

    @Override
    public List<PortStatistics> getPortDeltaStatistics(DeviceId deviceId) {
        return delegate.getPortDeltaStatistics(deviceId);
    }

    @Override
    public Port getPort(DeviceId deviceId, PortNumber portNumber) {
        return delegate.getPort(deviceId, portNumber);
    }

    @Override
    public boolean isAvailable(DeviceId deviceId) {
        return delegate.isAvailable(deviceId);
    }

    @Override
    public String localStatus(DeviceId deviceId) {
        return delegate.localStatus(deviceId);
    }

}
