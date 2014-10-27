/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.onlab.onos.sdnip;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Set;

import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.host.HostService;
import org.onlab.onos.net.host.PortAddresses;
import org.onlab.onos.sdnip.config.Interface;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;

import com.google.common.collect.Sets;

/**
 * Provides InterfaceService using PortAddresses data from the HostService.
 */
public class HostToInterfaceAdaptor implements InterfaceService {

    private final HostService hostService;

    public HostToInterfaceAdaptor(HostService hostService) {
        this.hostService = checkNotNull(hostService);
    }

    @Override
    public Set<Interface> getInterfaces() {
        Set<PortAddresses> addresses = hostService.getAddressBindings();
        Set<Interface> interfaces = Sets.newHashSetWithExpectedSize(addresses.size());
        for (PortAddresses a : addresses) {
            interfaces.add(new Interface(a));
        }
        return interfaces;
    }

    @Override
    public Interface getInterface(ConnectPoint connectPoint) {
        checkNotNull(connectPoint);

        PortAddresses portAddresses =
                hostService.getAddressBindingsForPort(connectPoint);

        if (!portAddresses.ips().isEmpty()) {
            return new Interface(portAddresses);
        }

        return null;
    }

    @Override
    public Interface getMatchingInterface(IpAddress ipAddress) {
        checkNotNull(ipAddress);

        for (PortAddresses portAddresses : hostService.getAddressBindings()) {
            for (IpPrefix p : portAddresses.ips()) {
                if (p.contains(ipAddress)) {
                    return new Interface(portAddresses);
                }
            }
        }

        return null;
    }

}
