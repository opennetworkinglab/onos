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
package org.onosproject.openstacknetworking.impl;

import com.google.common.collect.ImmutableSet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.openstacknetworking.api.InstancePort;
import org.onosproject.openstacknetworking.api.InstancePortListener;
import org.onosproject.openstacknetworking.api.InstancePortService;

import java.util.Set;

/**
 * Test adapter for instance port service.
 */
public class InstancePortServiceAdapter implements InstancePortService {
    @Override
    public InstancePort instancePort(MacAddress macAddress) {
        return null;
    }

    @Override
    public InstancePort instancePort(IpAddress ipAddress, String osNetId) {
        return null;
    }

    @Override
    public InstancePort instancePort(String osPortId) {
        return null;
    }

    @Override
    public InstancePort instancePort(DeviceId deviceId, PortNumber portNumber) {
        return null;
    }

    @Override
    public Set<InstancePort> instancePort(DeviceId deviceId) {
        return ImmutableSet.of();
    }

    @Override
    public Set<InstancePort> instancePorts() {
        return ImmutableSet.of();
    }

    @Override
    public Set<InstancePort> instancePorts(String osNetId) {
        return ImmutableSet.of();
    }

    @Override
    public IpAddress floatingIp(String osPortId) {
        return null;
    }

    @Override
    public void addListener(InstancePortListener listener) {

    }

    @Override
    public void removeListener(InstancePortListener listener) {

    }
}
