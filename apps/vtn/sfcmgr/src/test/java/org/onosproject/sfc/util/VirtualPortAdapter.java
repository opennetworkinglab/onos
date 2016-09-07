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
package org.onosproject.sfc.util;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.vtnrsc.FixedIp;
import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.TenantNetworkId;
import org.onosproject.vtnrsc.VirtualPort;
import org.onosproject.vtnrsc.VirtualPortId;
import org.onosproject.vtnrsc.virtualport.VirtualPortListener;
import org.onosproject.vtnrsc.virtualport.VirtualPortService;

/**
 * Provides implementation of the VirtualPort APIs.
 */
public class VirtualPortAdapter implements VirtualPortService {

    protected ConcurrentMap<VirtualPortId, VirtualPort> vPortStore = new ConcurrentHashMap<>();

    @Override
    public boolean exists(VirtualPortId vPortId) {
        return vPortStore.containsKey(vPortId);
    }

    @Override
    public VirtualPort getPort(VirtualPortId vPortId) {
        return vPortStore.get(vPortId);
    }

    @Override
    public VirtualPort getPort(FixedIp fixedIP) {
        return null;
    }

    @Override
    public VirtualPort getPort(MacAddress mac) {
        return null;
    }

    @Override
    public Collection<VirtualPort> getPorts() {
        return null;
    }

    @Override
    public Collection<VirtualPort> getPorts(TenantNetworkId networkId) {
        return null;
    }

    @Override
    public Collection<VirtualPort> getPorts(TenantId tenantId) {
        return null;
    }

    @Override
    public Collection<VirtualPort> getPorts(DeviceId deviceId) {
        return null;
    }

    @Override
    public VirtualPort getPort(TenantNetworkId networkId, IpAddress ipAddress) {
        return null;
    }

    @Override
    public boolean createPorts(Iterable<VirtualPort> vPorts) {
        for (VirtualPort vPort : vPorts) {
            vPortStore.put(vPort.portId(), vPort);
            if (!vPortStore.containsKey(vPort.portId())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean updatePorts(Iterable<VirtualPort> vPorts) {
        return true;
    }

    @Override
    public boolean removePorts(Iterable<VirtualPortId> vPortIds) {
        return true;
    }

    @Override
    public void addListener(VirtualPortListener listener) {
    }

    @Override
    public void removeListener(VirtualPortListener listener) {
    }
}
