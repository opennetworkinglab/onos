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

package org.onosproject.incubator.net.virtual;

import org.onlab.osgi.ServiceDirectory;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;

import java.util.Set;

/**
 * Test adapter for virtual network service.
 */
public abstract class VirtualNetworkServiceAdapter implements VirtualNetworkService {
    @Override
    public void addListener(VirtualNetworkListener listener) {

    }

    @Override
    public void removeListener(VirtualNetworkListener listener) {

    }

    @Override
    public Set<VirtualNetwork> getVirtualNetworks(TenantId tenantId) {
        return null;
    }

    @Override
    public Set<VirtualDevice> getVirtualDevices(NetworkId networkId) {
        return null;
    }

    @Override
    public Set<VirtualHost> getVirtualHosts(NetworkId networkId) {
        return null;
    }

    @Override
    public Set<VirtualLink> getVirtualLinks(NetworkId networkId) {
        return null;
    }

    @Override
    public Set<VirtualPort> getVirtualPorts(NetworkId networkId, DeviceId deviceId) {
        return null;
    }

    @Override
    public Set<DeviceId> getPhysicalDevices(NetworkId networkId, DeviceId deviceId) {
        return null;
    }

    @Override
    public <T> T get(NetworkId networkId, Class<T> serviceClass) {
        return null;
    }

    @Override
    public ServiceDirectory getServiceDirectory() {
        return null;
    }

    @Override
    public ApplicationId getVirtualNetworkApplicationId(NetworkId networkId) {
        return null;
    }
}
