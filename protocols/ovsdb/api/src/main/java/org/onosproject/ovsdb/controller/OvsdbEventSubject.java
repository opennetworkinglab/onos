/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.ovsdb.controller;

import java.util.Set;

import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;

/**
 * Represents for an entity that carry important information for listener.
 */
public interface OvsdbEventSubject extends EventSubject {
    /**
     * Returns the MAC address associated with this host (NIC).
     *
     * @return the MAC address of this host
     */
    MacAddress hwAddress();

    /**
     * Returns the IP address associated with this host's MAC.
     *
     * @return host IP address
     */
    Set<IpAddress> ipAddress();

    /**
     * Returns the Port name associated with the host.
     *
     * @return port name
     */
    OvsdbPortName portName();

    /**
     * Returns the Port number associated with the host.
     *
     * @return port number
     */
    OvsdbPortNumber portNumber();

    /**
     * Returns the Port type associated with the host.
     *
     * @return port type
     */
    OvsdbPortType portType();

    /**
     * Returns the Ovs dpid associated with the host.
     *
     * @return Ovs dpid
     */
    OvsdbDatapathId dpid();

    /**
     * Returns the vm ifaceid associated with the host.
     *
     * @return vm ifaceid
     */
    OvsdbIfaceId ifaceid();
}
