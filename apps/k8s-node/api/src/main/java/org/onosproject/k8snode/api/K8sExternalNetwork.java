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
import org.onlab.packet.MacAddress;

/**
 * Representation of an external network.
 */
public interface K8sExternalNetwork {

    /**
     * Returns the external bridge's IP address.
     *
     * @return IP address; null if the IP address does not exist
     */
    IpAddress extBridgeIp();

    /**
     * Returns the external gateway IP address.
     *
     * @return IP address; null if the IP address does not exist
     */
    IpAddress extGatewayIp();

    /**
     * Returns the external gateway MAC address.
     *
     * @return MAC address; null if the MAC address does not exist
     */
    MacAddress extGatewayMac();

    /**
     * Returns the external interface name.
     *
     * @return interface name
     */
    String extIntf();

    /**
     * Builder of new network entity.
     */
    interface Builder {

        /**
         * Builds an immutable kubernetes external network instance.
         *
         * @return kubernetes external network
         */
        K8sExternalNetwork build();

        /**
         * Returns kubernetes external network builder with supplied external bridge IP.
         *
         * @param extBridgeIp external bridge IP
         * @return kubernetes external network builder
         */
        Builder extBridgeIp(IpAddress extBridgeIp);

        /**
         * Returns kubernetes external network builder with supplied gateway IP.
         *
         * @param extGatewayIp external gateway IP
         * @return kubernetes external network builder
         */
        Builder extGatewayIp(IpAddress extGatewayIp);

        /**
         * Returns kubernetes external network builder with supplied external gateway MAC.
         *
         * @param extGatewayMac external gateway MAC address
         * @return kubernetes external network builder
         */
        Builder extGatewayMac(MacAddress extGatewayMac);

        /**
         * Returns kubernetes external network builder with supplied external interface.
         *
         * @param extIntf external interface name
         * @return kubernetes external network builder
         */
        Builder extIntf(String extIntf);
    }
}
