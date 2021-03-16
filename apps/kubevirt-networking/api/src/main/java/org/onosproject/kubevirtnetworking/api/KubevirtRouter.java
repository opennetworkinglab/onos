/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.api;

import org.onlab.packet.MacAddress;

import java.util.Map;
import java.util.Set;

/**
 * Representation of virtual router.
 */
public interface KubevirtRouter {

    /**
     * Returns the router name.
     *
     * @return router name
     */
    String name();

    /**
     * Returns the router description.
     *
     * @return router description
     */
    String description();

    /**
     * Returns the SNAT enable flag.
     *
     * @return true if the router support SNAT, false otherwise
     */
    boolean enableSnat();

    /**
     * Returns the MAC address.
     *
     * @return mac address
     */
    MacAddress mac();

    /**
     * Returns a set of internal networks.
     *
     * @return a set of internal networks
     */
    Set<String> internal();

    /**
     * Returns external network along with external router IP address.
     * We use IP address as the key, and external network name as the value.
     *
     * @return external network paired with external router IP address
     */
    Map<String, String> external();

    /**
     * Returns external peer router.
     *
     * @return peer router
     */
    KubevirtPeerRouter peerRouter();

    /**
     * Returns elected gateway node hostname.
     *
     * @return gateway node hostname.
     */
    String electedGateway();

    /**
     * Updates the peer router.
     *
     * @param updated updated peer router
     * @return kubevirt router with updated peer router
     */
    KubevirtRouter updatePeerRouter(KubevirtPeerRouter updated);

    /**
     * Updates the elected gateway node host name.
     *
     * @param updated updated elected gateway node hostname
     * @return kubevirt router with the updated gateway node hostname
     */
    KubevirtRouter updatedElectedGateway(String updated);


    interface Builder {

        /**
         * Builds an immutable router instance.
         *
         * @return kubevirt router
         */
        KubevirtRouter build();

        /**
         * Returns kubevirt router builder with supplied router name.
         *
         * @param name router name
         * @return router builder
         */
        Builder name(String name);

        /**
         * Returns kubevirt router builder with supplied router description.
         *
         * @param description router description
         * @return router builder
         */
        Builder description(String description);

        /**
         * Returns kubevirt router builder with supplied enable SNAT flag.
         *
         * @param flag router flag
         * @return router builder
         */
        Builder enableSnat(boolean flag);

        /**
         * Returns kubevirt router builder with supplied MAC address.
         *
         * @param mac MAC address
         * @return router builder
         */
        Builder mac(MacAddress mac);

        /**
         * Returns kubevirt router builder with supplied internal networks.
         *
         * @param internal internal network set
         * @return router builder
         */
        Builder internal(Set<String> internal);

        /**
         * Returns kubevirt router builder with supplied external network with IP.
         *
         * @param external external network with IP
         * @return router builder
         */
        Builder external(Map<String, String> external);

        /**
         * Returns kubevirt router builder with supplied peer router.
         *
         * @param router peer router
         * @return router builder
         */
        Builder peerRouter(KubevirtPeerRouter router);

        /**
         * Returns kubevirt router builder with supplied elected gateway node hostname.
         *
         * @param gateway gateway node hostname
         * @return router builder
         */
        Builder electedGateway(String gateway);
    }
}
