/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.net.host;

import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.provider.ProviderId;

import java.util.Set;

/**
 * Test adapter for host store.
 */
public class HostStoreAdapter implements HostStore {
    @Override
    public void setDelegate(HostStoreDelegate delegate) {

    }

    @Override
    public void unsetDelegate(HostStoreDelegate delegate) {

    }

    @Override
    public boolean hasDelegate() {
        return false;
    }

    @Override
    public HostEvent createOrUpdateHost(ProviderId providerId,
                                        HostId hostId,
                                        HostDescription hostDescription,
                                        boolean replaceIps) {
        return null;
    }

    @Override
    public HostEvent removeHost(HostId hostId) {
        return null;
    }

    @Override
    public HostEvent removeIp(HostId hostId, IpAddress ipAddress) {
        return null;
    }

    @Override
    public void appendLocation(HostId hostId, HostLocation location) {

    }

    @Override
    public void removeLocation(HostId hostId, HostLocation location) {

    }

    @Override
    public int getHostCount() {
        return 0;
    }

    @Override
    public Iterable<Host> getHosts() {
        return null;
    }

    @Override
    public Host getHost(HostId hostId) {
        return null;
    }

    @Override
    public Set<Host> getHosts(VlanId vlanId) {
        return null;
    }

    @Override
    public Set<Host> getHosts(MacAddress mac) {
        return null;
    }

    @Override
    public Set<Host> getHosts(IpAddress ip) {
        return null;
    }

    @Override
    public Set<Host> getConnectedHosts(ConnectPoint connectPoint) {
        return null;
    }

    @Override
    public Set<Host> getConnectedHosts(DeviceId deviceId) {
        return null;
    }
}
