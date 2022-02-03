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

import org.onlab.packet.IpAddress;

/**
 * Representation of floating IP address.
 */
public interface KubevirtFloatingIp {

    /**
     * Returns the floating IP identifier.
     *
     * @return floating IP identifier
     */
    String id();

    /**
     * Returns the name of router where the floating IP is associated.
     *
     * @return name of router
     */
    String routerName();

    /**
     * Returns the name of network where the floating IPs are belong to.
     *
     * @return name of network
     */
    String networkName();

    /**
     * Returns the floating IP address.
     *
     * @return floating IP address
     */
    IpAddress floatingIp();

    /**
     * Returns the fixed IP address.
     *
     * @return fixed IP address
     */
    IpAddress fixedIp();

    /**
     * Returns the name of VM where this floating IP is associated with.
     *
     * @return VM name
     */
    String vmName();

    /**
     * Returns the name of POD where this floating IP is associated with.
     *
     * @return POD name
     */
    String podName();

    /**
     * Updates the kubevirt floating IP with the supplied fixed IP address.
     *
     * @param ip fixed IP address
     * @return kubevirt floating IP
     */
    KubevirtFloatingIp updateFixedIp(IpAddress ip);

    /**
     * Updates the kubevirt floating IP with the supplied pod Name.
     *
     * @param name POD name
     * @return kubevirt floating IP
     */
    KubevirtFloatingIp updatePodName(String name);

    /**
     * Updates the kubevirt floating IP with the supplied VM name.
     *
     * @param name VM name
     * @return kubevirt floating IP
     */
    KubevirtFloatingIp updateVmName(String name);

    interface Builder {
        /**
         * Builds an immutable floaing IP instance.
         *
         * @return kubevirt floating IP
         */
        KubevirtFloatingIp build();

        /**
         * Returns kubevirt floating IP builder with supplied identifier.
         *
         * @param id floating IP identifier
         * @return floating IP builder
         */
        Builder id(String id);

        /**
         * Returns kubevirt floating IP builder with supplied router name.
         *
         * @param name router name
         * @return floating IP builder
         */
        Builder routerName(String name);

        /**
         * Returns kubevirt floating IP builder with supplied network name.
         *
         * @param name network name
         * @return floating IP builder
         */
        Builder networkName(String name);

        /**
         * Returns kubevirt floating IP builder with supplied floating IP address.
         *
         * @param ip floating IP address
         * @return floating IP builder
         */
        Builder floatingIp(IpAddress ip);

        /**
         * Returns kubevirt floating IP builder with supplied fixed IP address.
         *
         * @param ip fixed IP address
         * @return floating IP builder
         */
        Builder fixedIp(IpAddress ip);

        /**
         * Returns kubevirt floating IP builder with supplied VM name.
         *
         * @param name VM name
         * @return floating IP builder
         */
        Builder vmName(String name);

        /**
         * Returns kubevirt floating IP builder with supplied POD name.
         *
         * @param name POD name
         * @return floating IP builder
         */
        Builder podName(String name);
    }
}
