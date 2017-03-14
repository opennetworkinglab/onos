/*
 * Copyright 2015-present Open Networking Laboratory
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

package org.onosproject.incubator.net.intf;

import com.google.common.annotations.Beta;
import org.onlab.packet.IpAddress;
import org.onlab.packet.VlanId;
import org.onosproject.event.ListenerService;
import org.onosproject.net.ConnectPoint;

import java.util.Set;

/**
 * Service for interacting with interfaces.
 */
@Beta
public interface InterfaceService
        extends ListenerService<InterfaceEvent, InterfaceListener> {

    /**
     * Returns the set of all interfaces in the system.
     *
     * @return set of interfaces
     */
    Set<Interface> getInterfaces();

    /**
     * Returns the interface with the given name.
     *
     * @param connectPoint connect point of the interface
     * @param name name of the interface
     * @return interface if it exists, otherwise null
     */
    Interface getInterfaceByName(ConnectPoint connectPoint, String name);

    /**
     * Returns the set of interfaces configured on the given port.
     *
     * @param port connect point
     * @return set of interfaces
     */
    Set<Interface> getInterfacesByPort(ConnectPoint port);

    /**
     * Returns the set of interfaces with the given IP address.
     *
     * @param ip IP address
     * @return set of interfaces
     */
    Set<Interface> getInterfacesByIp(IpAddress ip);

    /**
     * Returns the set of interfaces in the given VLAN.
     *
     * @param vlan VLAN ID of the interfaces
     * @return set of interfaces
     */
    Set<Interface> getInterfacesByVlan(VlanId vlan);

    /**
     * Returns an interface that has an address that is in the same subnet as
     * the given IP address.
     *
     * @param ip IP address to find matching subnet interface for
     * @return interface
     */
    Interface getMatchingInterface(IpAddress ip);

    /**
     * Returns all interfaces that have an address that is in the same
     * subnet as the given IP address.
     *
     * @param ip IP address to find matching subnet interface for
     * @return a set of interfaces
     */
    Set<Interface> getMatchingInterfaces(IpAddress ip);
}
