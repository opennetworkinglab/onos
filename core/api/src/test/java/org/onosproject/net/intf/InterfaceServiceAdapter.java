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

package org.onosproject.net.intf;

import org.onlab.packet.IpAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;

import java.util.Set;

/**
 * Interface service adapter class for tests.
 */
public class InterfaceServiceAdapter implements InterfaceService {
    @Override
    public Set<Interface> getInterfaces() {
        return null;
    }

    @Override
    public Interface getInterfaceByName(ConnectPoint connectPoint, String name) {
        return null;
    }

    @Override
    public Set<Interface> getInterfacesByPort(ConnectPoint port) {
        return null;
    }

    @Override
    public Set<Interface> getInterfacesByIp(IpAddress ip) {
        return null;
    }

    @Override
    public Set<Interface> getInterfacesByVlan(VlanId vlan) {
        return null;
    }

    @Override
    public Interface getMatchingInterface(IpAddress ip) {
        return null;
    }

    @Override
    public Set<Interface> getMatchingInterfaces(IpAddress ip) {
        return null;
    }

    @Override
    public void addListener(InterfaceListener listener) {

    }

    @Override
    public void removeListener(InterfaceListener listener) {

    }

    @Override
    public boolean isConfigured(ConnectPoint connectPoint) {
        return false;
    }
}
