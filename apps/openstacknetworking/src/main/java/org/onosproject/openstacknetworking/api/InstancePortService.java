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
package org.onosproject.openstacknetworking.api;

import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.event.ListenerService;

import java.util.Set;

/**
 * Service for interacting with the inventory of instance ports.
 */
public interface InstancePortService
        extends ListenerService<InstancePortEvent, InstancePortListener> {

    /**
     * Returns instance port with the given MAC address.
     *
     * @param macAddress mac address
     * @return instance port; null if not found
     */
    InstancePort instancePort(MacAddress macAddress);

    /**
     * Returns instance port with the given IP address in the given OpenStack network.
     *
     * @param ipAddress ip address
     * @param osNetId   openstack network id
     * @return instance port; null if not found
     */
    InstancePort instancePort(IpAddress ipAddress, String osNetId);

    /**
     * Returns instance port with the given openstack port ID.
     *
     * @param osPortId openstack port id
     * @return instance port; null if not found
     */
    InstancePort instancePort(String osPortId);

    /**
     * Returns all instance ports.
     *
     * @return set of instance ports; empty list if no port exists
     */
    Set<InstancePort> instancePorts();

    /**
     * Returns instance ports in the given OpenStack network.
     *
     * @param osNetId openstack network
     * @return set of instance ports; empty list if no port exists
     */
    Set<InstancePort> instancePorts(String osNetId);
}
