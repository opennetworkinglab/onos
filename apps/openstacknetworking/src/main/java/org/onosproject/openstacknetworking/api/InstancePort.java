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
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

/**
 * Representation of virtual instance port.
 */
public interface InstancePort {

    /**
     * Returns the OpenStack network ID of the instance port.
     *
     * @return openstack network id
     */
    String networkId();

    /**
     * Returns the OpenStack port ID of a given host.
     *
     * @return openstack port id
     */
    String portId();

    /**
     * Returns the MAC address of the instance port.
     *
     * @return mac address
     */
    MacAddress macAddress();

    /**
     * Returns the IP address of the instance port.
     *
     * @return ip address
     */
    IpAddress ipAddress();

    /**
     * Returns the device ID of the instance port.
     *
     * @return device id
     */
    DeviceId deviceId();

    /**
     * Returns the port number of the instance port.
     *
     * @return port number
     */
    PortNumber portNumber();
}
