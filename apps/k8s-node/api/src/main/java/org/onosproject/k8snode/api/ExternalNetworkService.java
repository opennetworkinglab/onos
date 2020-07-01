/*
 * Copyright 2020-present Open Networking Foundation
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
package org.onosproject.k8snode.api;

import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;

import java.util.Set;

/**
 * A service manages external network objects including IP addresses.
 */
public interface ExternalNetworkService {

    /**
     * Registers an external network.
     *
     * @param cidr network CIDR
     */
    void registerNetwork(IpPrefix cidr);

    /**
     * Unregisters an existing external network.
     *
     * @param cidr network CIDR
     */
    void unregisterNetwork(IpPrefix cidr);

    /**
     * Obtains a gateway IP address of the given network CIDR.
     *
     * @param cidr network CIDR
     * @return a gateway IP address
     */
    IpAddress getGatewayIp(IpPrefix cidr);

    /**
     * Obtains an allocated IP address of the given network CIDR.
     *
     * @param cidr network CIDR
     * @return an IP address exists in the given network CIDR IP pool
     */
    IpAddress allocateIp(IpPrefix cidr);

    /**
     * Releases the given IP address to the given network CIDR.
     *
     * @param cidr network CIDR
     * @param ip IP address to be released
     */
    void releaseIp(IpPrefix cidr, IpAddress ip);

    /**
     * Obtains all IP addresses of the given network CIDR.
     *
     * @param cidr network CIDR
     * @return all IP addresses
     */
    Set<String> getAllIps(IpPrefix cidr);
}
