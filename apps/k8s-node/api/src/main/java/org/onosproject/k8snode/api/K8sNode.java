/*
 * Copyright 2019-present Open Networking Foundation
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
import org.onosproject.net.PortNumber;

/**
 * Representation of a node used in k8s-networking service.
 */
public interface K8sNode {

    /**
     * Lists of kubernetes node types.
     */
    enum Type {
        /**
         * Signifies that this is a kubernetes master node.
         */
        MASTER,

        /**
         * Signifies that this is a kubernetes minion node.
         */
        MINION
    }

    /**
     * Returns hostname of the node.
     *
     * @return hostname
     */
    String hostname();

    /**
     * Returns the type of the node.
     *
     * @return node type
     */
    Type type();

    /**
     * Returns the OVSDB device ID of the node.
     *
     * @return ovsdb device id
     */
    DeviceId ovsdb();

    /**
     * Returns the device ID of the integration bridge at the node.
     *
     * @return device id
     */
    DeviceId intgBridge();

    /**
     * Returns the management network IP address of the node.
     *
     * @return ip address
     */
    IpAddress managementIp();

    /**
     * Returns the data network IP address used for tunneling.
     *
     * @return ip address; null if vxlan mode is not enabled
     */
    IpAddress dataIp();

    /**
     * Returns the initialization state of the node.
     *
     * @return node state
     */
    K8sNodeState state();

    /**
     * Returns the GRE tunnel port number.
     *
     * @return GRE port number; null if the GRE tunnel port does not exist
     */
    PortNumber grePortNum();

    /**
     * Returns the VXLAN tunnel port number.
     *
     * @return VXLAN port number; null if tunnel port does not exist
     */
    PortNumber vxlanPortNum();

    /**
     * Returns the GENEVE tunnel port number.
     *
     * @return GENEVE port number; null if the GRE tunnel port does not exist
     */
    PortNumber genevePortNum();

    /**
     * Builder of new node entity.
     */
    interface Builder {

        /**
         * Builds an immutable kubernetes node instance.
         *
         * @return kubernetes node instance
         */
        K8sNode build();

        /**
         * Returns kubernetes node builder with supplied hostname.
         *
         * @param hostname hostname of the node
         * @return kubernetes node builder
         */
        Builder hostname(String hostname);

        /**
         * Returns kubernetes node builder with supplied type.
         *
         * @param type kubernetes node type
         * @return kubernetes node builder
         */
        Builder type(Type type);

        /**
         * Returns kubernetes node builder with supplied bridge name.
         *
         * @param deviceId integration bridge device ID
         * @return kubernetes node builder
         */
        Builder intgBridge(DeviceId deviceId);

        /**
         * Returns kubernetes node builder with supplied management IP address.
         *
         * @param managementIp management IP address
         * @return kubernetes node builder
         */
        Builder managementIp(IpAddress managementIp);

        /**
         * Returns kubernetes node builder with supplied data IP address.
         *
         * @param dataIp data IP address
         * @return kubernetes node builder
         */
        Builder dataIp(IpAddress dataIp);

        /**
         * Returns kubernetes node builder with supplied node state.
         *
         * @param state kubernetes node state
         * @return kubernetes node builder
         */
        Builder state(K8sNodeState state);
    }
}
