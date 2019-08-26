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
import org.onlab.packet.MacAddress;
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
     * Returns the device ID of the external bridge at the node.
     *
     * @return device id
     */
    DeviceId extBridge();

    /**
     * Returns the device ID of the local bridge at the node.
     *
     * @return device id
     */
    DeviceId localBridge();

    /**
     * Returns the external interface name.
     *
     * @return external interface name
     */
    String extIntf();

    /**
     * Returns new kubernetes node instance with given integration bridge.
     *
     * @param deviceId  integration bridge device ID
     * @return updated kubernetes node
     */
    K8sNode updateIntgBridge(DeviceId deviceId);

    /**
     * Returns new kubernetes node instance with given external bridge.
     *
     * @param deviceId external bridge device ID
     * @return updated kubernetes node
     */
    K8sNode updateExtBridge(DeviceId deviceId);

    /**
     * Returns new kubernetes node instance with given local bridge.
     *
     * @param deviceId local bridge device ID
     * @return updated kubernetes node
     */
    K8sNode updateLocalBridge(DeviceId deviceId);

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
     * Returns the POD CIDR of the node.
     *
     * @return POD CIDR (e.g., 10.10.0.0/24)
     */
    String podCidr();

    /**
     * Returns new kubernetes node instance with given state.
     *
     * @param newState updated state
     * @return updated kubernetes node
     */
    K8sNode updateState(K8sNodeState newState);

    /**
     * Returns new kubernetes node instance with given external gateway MAC address.
     *
     * @param macAddress updated MAC address
     * @return updated kubernetes node
     */
    K8sNode updateExtGatewayMac(MacAddress macAddress);

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
     * Returns the host port number.
     *
     * @return host port number; null if the host port does not exist
     */
    PortNumber intgBridgePortNum();

    /**
     * Returns the integration to external patch port number.
     *
     * @return patch port number
     */
    PortNumber intgToExtPatchPortNum();

    /**
     * Returns the integration to local patch port number.
     *
     * @return patch port number
     */
    PortNumber intgToLocalPatchPortNum();

    /**
     * Returns the local to integration patch port number.
     *
     * @return patch port number
     */
    PortNumber localToIntgPatchPortNumber();

    /**
     * Returns the external to integration patch port number.
     *
     * @return patch port number
     */
    PortNumber extToIntgPatchPortNum();

    /**
     * Returns the external bridge to router port number.
     *
     * @return port number, null if the port does not exist
     */
    PortNumber extBridgePortNum();

    /**
     * Returns the integration bridge's MAC address.
     *
     * @return MAC address; null if the MAC address does not exist
     */
    MacAddress intgBridgeMac();

    /**
     * Returns the external bridge's IP address.
     *
     * @return IP address; null if the IP address does not exist
     */
    IpAddress extBridgeIp();

    /**
     * Returns the external bridge's MAC address.
     *
     * @return MAC address; null if the MAC address does not exist
     */
    MacAddress extBridgeMac();

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
         * Returns kubernetes node builder with supplied integration bridge name.
         *
         * @param deviceId integration bridge device ID
         * @return kubernetes node builder
         */
        Builder intgBridge(DeviceId deviceId);

        /**
         * Returns kubernetes node builder with supplied external bridge name.
         *
         * @param deviceId external bridge device ID
         * @return kubernetes node builder
         */
        Builder extBridge(DeviceId deviceId);

        /**
         * Returns kubernetes node builder with supplied local bridge name.
         *
         * @param deviceId local bridge device ID
         * @return kubernetes node builder
         */
        Builder localBridge(DeviceId deviceId);

        /**
         * Returns kubernetes node builder with supplied external interface.
         *
         * @param intf external interface
         * @return kubernetes node builder
         */
        Builder extIntf(String intf);

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

        /**
         * Returns kubernetes node builder with supplied external bridge IP.
         *
         * @param extBridgeIp external bridge IP
         * @return kubernetes node builder
         */
        Builder extBridgeIp(IpAddress extBridgeIp);

        /**
         * Returns kubernetes node builder with supplied gateway IP.
         *
         * @param extGatewayIp external gateway IP
         * @return kubernetes node builder
         */
        Builder extGatewayIp(IpAddress extGatewayIp);

        /**
         * Returns kubernetes node builder with supplied external gateway MAC.
         *
         * @param extGatewayMac external gateway MAC address
         * @return kubernetes node builder
         */
        Builder extGatewayMac(MacAddress extGatewayMac);

        /**
         * Returns kubernetes node builder with supplied POD CIDR.
         *
         * @param podCidr POD CIDR
         * @return kubernetes node builder
         */
        Builder podCidr(String podCidr);
    }
}
