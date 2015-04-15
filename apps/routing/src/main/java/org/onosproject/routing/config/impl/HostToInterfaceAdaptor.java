/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.routing.config.impl;

import com.google.common.collect.Sets;
import org.onlab.packet.IpAddress;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.host.HostService;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.host.PortAddresses;
import org.onosproject.routing.config.Interface;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Adapts PortAddresses data from the HostService into Interface data used by
 * the routing module.
 */
public class HostToInterfaceAdaptor {

    private final HostService hostService;

    public HostToInterfaceAdaptor(HostService hostService) {
        this.hostService = checkNotNull(hostService);
    }

    public Set<Interface> getInterfaces() {
        Set<PortAddresses> addresses = hostService.getAddressBindings();
        Set<Interface> interfaces = Sets.newHashSetWithExpectedSize(addresses.size());
        for (PortAddresses a : addresses) {
            interfaces.add(new Interface(a));
        }
        return interfaces;
    }

    public Interface getInterface(ConnectPoint connectPoint) {
        checkNotNull(connectPoint);

        Set<PortAddresses> portAddresses =
                hostService.getAddressBindingsForPort(connectPoint);

        for (PortAddresses addresses : portAddresses) {
            if (addresses.connectPoint().equals(connectPoint)) {
                return new Interface(addresses);
            }
        }

        return null;
    }

    public Interface getMatchingInterface(IpAddress ipAddress) {
        checkNotNull(ipAddress);

        for (PortAddresses portAddresses : hostService.getAddressBindings()) {
            for (InterfaceIpAddress ia : portAddresses.ipAddresses()) {
                if (ia.subnetAddress().contains(ipAddress)) {
                    return new Interface(portAddresses);
                }
            }
        }

        return null;
    }

}
