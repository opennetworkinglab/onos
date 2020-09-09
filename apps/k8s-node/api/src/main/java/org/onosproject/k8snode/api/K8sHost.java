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
import org.onosproject.net.DeviceId;

import java.util.Set;

/**
 * Representation of a host used in k8s-networking service.
 */
public interface K8sHost {

    /**
     * Returns the host IP address. Note that the host IP address is unique, and
     * will be used as an identifier for the host.
     *
     * @return host IP address
     */
    IpAddress hostIp();

    /**
     * A set of node names included in this host.
     *
     * @return node names
     */
    Set<String> nodeNames();

    /**
     * Returns kubernetes host state.
     *
     * @return host state
     */
    K8sHostState state();

    /**
     * Returns the OVSDB device ID of the node.
     *
     * @return ovsdb device ID
     */
    DeviceId ovsdb();

    /**
     * Returns the set of tunnel bridges belong to the host.
     *
     * @return a set of tunnel bridges
     */
    Set<K8sTunnelBridge> tunBridges();

    /**
     * Returns the set of router bridges belong to the host.
     *
     * @return a set of router bridges
     */
    Set<K8sRouterBridge> routerBridges();

    /**
     * Returns new kubernetes host instance with given state.
     *
     * @param newState updated state
     * @return updated kubernetes host
     */
    K8sHost updateState(K8sHostState newState);

    /**
     * Returns new kubernetes host instance with given node names.
     *
     * @param nodeNames a set of node names
     * @return updated kubernetes host
     */
    K8sHost updateNodeNames(Set<String> nodeNames);

    /**
     * Builder of new host entity.
     */
    interface Builder {

        /**
         * Builds an immutable kubernetes host instance.
         *
         * @return kubernetes host instance
         */
        K8sHost build();

        /**
         * Returns kubernetes host builder with supplied host IP address.
         *
         * @param hostIp host IP address
         * @return kubernetes host builder
         */
        Builder hostIp(IpAddress hostIp);

        /**
         * Returns kubernetes host builder with supplied node names.
         *
         * @param nodeNames node names
         * @return kubernetes host builder
         */
        Builder nodeNames(Set<String> nodeNames);

        /**
         * Returns kubernetes host builder with supplied host state.
         *
         * @param state host state
         * @return kubernetes host builder
         */
        Builder state(K8sHostState state);

        /**
         * Returns kubernetes host builder with supplied tunnel bridges set.
         *
         * @param tunBridges tunnel bridges
         * @return kubernetes host builder
         */
        Builder tunBridges(Set<K8sTunnelBridge> tunBridges);

        /**
         * Returns kubernetes host builder with supplied router bridges set.
         *
         * @param routerBridges router bridges
         * @return kubernetes host builder
         */
        Builder routerBridges(Set<K8sRouterBridge> routerBridges);
    }
}
