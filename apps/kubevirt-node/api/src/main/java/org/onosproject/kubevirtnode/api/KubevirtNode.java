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
package org.onosproject.kubevirtnode.api;

import org.onlab.packet.IpAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import java.util.Collection;
import java.util.Set;

/**
 * Representation of a KubeVirt node used in kubevirt networking service.
 */
public interface KubevirtNode {

    /**
     * Lists of kubernetes node types.
     */
    enum Type {
        /**
         * Signifies that this is a kubevirt master node.
         */
        MASTER,

        /**
         * Signifies that this is a kubevirt worker node.
         */
        WORKER,

        /**
         * Signifies that this is a gateway which is running on master node.
         */
        GATEWAY,

        /**
         * Signifies that this is a unmanaged node.
         */
        OTHER,
    }

    /**
     * Returns cluster name of the node.
     *
     * @return cluster name
     */
    String clusterName();

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
    KubevirtNode.Type type();

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
     * Returns the device ID of the tunnel bridge at the node.
     *
     * @return device id
     */
    DeviceId tunBridge();

    /**
     * Returns the management network IP address of the node.
     *
     * @return ip address
     */
    IpAddress managementIp();

    /**
     * Returns the data network IP address used for tunneling.
     *
     * @return ip address; null if tunnel mode is not enabled
     */
    IpAddress dataIp();

    /**
     * Returns the initialization state of the node.
     *
     * @return node state
     */
    KubevirtNodeState state();

    /**
     * Returns new kubevirt node instance with given state.
     *
     * @param newState updated state
     * @return updated kubevirt node
     */
    KubevirtNode updateState(KubevirtNodeState newState);

    /**
     * Returns new kubevirt node instance with given integration bridge.
     *
     * @param deviceId  integration bridge device ID
     * @return updated kubevirt node
     */
    KubevirtNode updateIntgBridge(DeviceId deviceId);

    /**
     * Returns new kubevirt node instance with given tunnel bridge.
     *
     * @param deviceId  tunnel bridge device ID
     * @return updated kubevirt node
     */
    KubevirtNode updateTunBridge(DeviceId deviceId);

    /**
     * Returns a collection of physical interfaces.
     *
     * @return physical interfaces
     */
    Collection<KubevirtPhyInterface> phyIntfs();

    /**
     * Returns a set of integration to physnet patch port number.
     *
     * @return a set of patch port numbers
     */
    Set<PortNumber> physPatchPorts();

    /**
     * Returns the VXLAN tunnel port.
     *
     * @return VXLAN port number; null if tunnel port does not exist
     */
    PortNumber vxlanPort();

    /**
     * Returns the GRE tunnel port.
     *
     * @return GRE port number; null if the GRE tunnel port does not exist
     */
    PortNumber grePort();

    /**
     * Returns the GENEVE tunnel port number.
     *
     * @return GENEVE port number; null if the GRE tunnel port does not exist
     */
    PortNumber genevePort();

    /**
     * Returns the STT tunnel port number.
     *
     * @return STT port number; null if the STT tunnel port does not exist
     */
    PortNumber sttPort();

    /**
     * Returns the name of the gateway bridge.
     *
     * @return gateway bridge name
     */
    String gatewayBridgeName();

    /**
     * Builder of new node entity.
     */
    interface Builder {
        /**
         * Builds an immutable kubevirt node instance.
         *
         * @return kubevirt node instance
         */
        KubevirtNode build();

        /**
         * Returns kubevirt node builder with supplied cluster name.
         *
         * @param clusterName cluster name
         * @return kubevirt node builder
         */
        KubevirtNode.Builder clusterName(String clusterName);

        /**
         * Returns kubevirt node builder with supplied hostname.
         *
         * @param hostname hostname of the node
         * @return kubevirt node builder
         */
        KubevirtNode.Builder hostname(String hostname);

        /**
         * Returns kubevirt node builder with supplied type.
         *
         * @param type kubevirt node type
         * @return kubevirt node builder
         */
        KubevirtNode.Builder type(KubevirtNode.Type type);

        /**
         * Returns kubevirt node builder with supplied integration bridge name.
         *
         * @param deviceId integration bridge device ID
         * @return kubevirt node builder
         */
        KubevirtNode.Builder intgBridge(DeviceId deviceId);

        /**
         * Returns kubevirt node builder with supplied tunnel bridge name.
         *
         * @param deviceId tunnel bridge device ID
         * @return kubevirt node builder
         */
        KubevirtNode.Builder tunBridge(DeviceId deviceId);

        /**
         * Returns kubevirt node builder with supplied management IP address.
         *
         * @param managementIp management IP address
         * @return kubevirt node builder
         */
        KubevirtNode.Builder managementIp(IpAddress managementIp);

        /**
         * Returns kubevirt node builder with supplied data IP address.
         *
         * @param dataIp data IP address
         * @return kubevirt node builder
         */
        KubevirtNode.Builder dataIp(IpAddress dataIp);

        /**
         * Returns kubevirt node builder with supplied physical interfaces.
         *
         * @param phyIntfs a collection of physical interfaces
         * @return kubevirt node builder
         */
        KubevirtNode.Builder phyIntfs(Collection<KubevirtPhyInterface> phyIntfs);

        /**
         * Returns kubevirt node builder with supplied node state.
         *
         * @param state kubevirt node state
         * @return kubevirt node builder
         */
        KubevirtNode.Builder state(KubevirtNodeState state);

        /**
         * Returns kubevirt node builder with supplied gateway bridge name.
         *
         * @param gatewayBridgeName gateway bridge name
         * @return kubevirt node builder
         */
        KubevirtNode.Builder gatewayBridgeName(String gatewayBridgeName);
    }
}
