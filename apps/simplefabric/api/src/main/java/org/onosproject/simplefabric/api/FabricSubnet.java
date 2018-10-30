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

package org.onosproject.simplefabric.api;

import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onosproject.net.EncapsulationType;

/**
 * Interface of FabricSubnet.
 */
public interface FabricSubnet {

    /**
     * Gets the IP subnet of the IP subnet entry.
     *
     * @return the IP subnet
     */
    IpPrefix prefix();

    /**
     * Gets the virtual gateway IP address of the IP subnet entry.
     *
     * @return the virtual gateway IP address
     */
    IpAddress gatewayIp();

    /**
     * Gets the virtual gateway Mac address of the IP subnet entry.
     *
     * @return the virtuai gateway Mac address
     */
    MacAddress gatewayMac();

    /**
     * Gets the encapsulation type of IP subnet entry.
     *
     * @return the encapsulation type
     */
    EncapsulationType encapsulation();

    /**
     * Gets the network name.
     *
     * @return the network name
     */
    String networkName();

    /**
     * Tests whether the IP version of this entry is IPv4.
     *
     * @return true if the IP version of this entry is IPv4, otherwise false.
     */
    boolean isIp4();

    /**
     * Tests whether the IP version of this entry is IPv6.
     *
     * @return true if the IP version of this entry is IPv6, otherwise false.
     */
    boolean isIp6();

    /**
     * Builder of Ip Subnet.
     */
    interface Builder {

        /**
         * Returns FabricSubnet builder with supplied IpPrefix.
         *
         * @param ipPrefix IP prefix
         * @return FabricSubnet instance builder
         */
        Builder prefix(IpPrefix ipPrefix);

        /**
         * Returns FabricSubnet builder with supplied gatewayIp.
         *
         * @param gatewayIp gateway IP
         * @return FabricSubnet instance builder
         */
        Builder gatewayIp(IpAddress gatewayIp);

        /**
         * Returns FabricSubnet builder with supplied gatewayMac.
         *
         * @param gatewayMac gateway MAC
         * @return FabricSubnet instance builder
         */
        Builder gatewayMac(MacAddress gatewayMac);

        /**
         * Returns FabricSubnet builder with supplied encapsulation type.
         *
         * @param encapsulation encapsulation type
         * @return FabricSubnet instance builder
         */
        Builder encapsulation(EncapsulationType encapsulation);

        /**
         * Returns FabricSubnet builder with supplied network name.
         *
         * @param networkName network name
         * @return FabricSubnet instance builder
         */
        Builder networkName(String networkName);

        /**
         * Builds an immutable FabricSubnet instance.
         *
         * @return FabricSubnet instance
         */
        FabricSubnet build();
    }
}
