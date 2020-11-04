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
import org.onosproject.k8snode.api.K8sApiConfig.Mode;
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
        MINION,
    }

    /**
     * Returns cluster name of the node.
     *
     * @return cluster name
     */
    String clusterName();

    /**
     * Returns host short name.
     *
     * @return host short name
     */
    String hostShortName();

    /**
     * Returns a unique string with the given length and string.
     *
     * @param length target string length
     * @return a unique string
     */
    String uniqueString(int length);

    /**
     * Returns the segmentation ID.
     *
     * @return segmentation ID
     */
    int segmentId();

    /**
     * Returns the key of VXLAN/GRE/GENEVE tunnel.
     *
     * @return key of various tunnel
     */
    String tunnelKey();

    /**
     * Returns the CNI running mode.
     *
     * @return CNI running mode
     */
    Mode mode();

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
     * Returns the device ID of the tunnel bridge at the node.
     *
     * @return device id
     */
    DeviceId tunBridge();

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
     * Returns new kubernetes node instance with given tun bridge.
     *
     * @param deviceId tunnel bridge device ID
     * @return updated kubernetes node
     */
    K8sNode updateTunBridge(DeviceId deviceId);

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
     * Returns the kubernetes node info.
     *
     * @return node info; null if node info not exists
     */
    K8sNodeInfo nodeInfo();

    /**
     * Returns the kubernetes node IP address.
     *
     * @return node IP address; null if the node IP not exists
     */
    IpAddress nodeIp();

    /**
     * Returns the kubernetes node MAC address.
     *
     * @return node MAC address; null if the node MAC not exists
     */
    MacAddress nodeMac();

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
     * Returns new kubernetes node instance with given node info.
     *
     * @param nodeInfo updated node info
     * @return updated kubernetes node
     */
    K8sNode updateNodeInfo(K8sNodeInfo nodeInfo);

    /**
     * Returns GRE port name.
     *
     * @return GRE port name
     */
    String grePortName();

    /**
     * Returns VXLAN port name.
     *
     * @return VXLAN port name
     */
    String vxlanPortName();

    /**
     * Returns GENEVE port name.
     *
     * @return GENEVE port name
     */
    String genevePortName();

    /**
     * Returns integration bridge name.
     *
     * @return integration bridge name
     */
    String intgBridgeName();

    /**
     * Returns the entry port name of integration bridge.
     *
     * @return entry port name
     */
    String intgEntryPortName();

    /**
     * Returns the entry port MAC address.
     *
     * @return entry port MAC address
     */
    MacAddress intgEntryPortMac();

    /**
     * Returns the port MAC address with the given patch port name.
     *
     * @param deviceId device identifier
     * @param portName patch port name
     * @return port MAC address
     */
    MacAddress portMacByName(DeviceId deviceId, String portName);

    /**
     * Returns the port number with the given patch port name.
     *
     * @param deviceId device identifier
     * @param portName patch port name
     * @return port number
     */
    PortNumber portNumByName(DeviceId deviceId, String portName);

    /**
     * Return the port number of integration bridge's entry port.
     *
     * @return port number
     */
    PortNumber intgEntryPortNum();

    /**
     * Returns external bridge name.
     *
     * @return external bridge name
     */
    String extBridgeName();

    /**
     * Returns local bridge name.
     *
     * @return local bridge name
     */
    String localBridgeName();

    /**
     * Returns tun bridge name.
     *
     * @return tun bridge name
     */
    String tunBridgeName();

    /**
     * Returns integration bridge port name.
     *
     * @return integration bridge port name
     */
    String intgBridgePortName();

    /**
     * Returns external bridge port name.
     *
     * @return external bridge port name
     */
    String extBridgePortName();

    /**
     * Returns local bridge port name.
     *
     * @return local bridge port name
     */
    String localBridgePortName();

    /**
     * Returns tunnel bridge port name.
     *
     * @return tunnel bridge port name
     */
    String tunBridgePortName();

    /**
     * Returns integration to external patch port name.
     *
     * @return integration to external patch port name
     */
    String intgToExtPatchPortName();

    /**
     * Returns integration to tunnel patch port name.
     *
     * @return integration to tunnel patch port name
     */
    String intgToTunPatchPortName();

    /**
     * Returns integration to local patch port name.
     *
     * @return integration to local patch port name
     */
    String intgToLocalPatchPortName();

    /**
     * Returns local to integration patch port name.
     *
     * @return local to integration patch port name
     */
    String localToIntgPatchPortName();

    /**
     * Returns external to integration patch port name.
     *
     * @return external to integration patch port name
     */
    String extToIntgPatchPortName();

    /**
     * Returns tunnel to integration patch port name.
     *
     * @return tunnel to integration patch port name
     */
    String tunToIntgPatchPortName();

    /**
     * Returns kubernetes to openstack integration patch port name.
     *
     * @return kubernetes to openstack integration patch port name
     */
    String k8sIntgToOsPatchPortName();

    /**
     * Returns kubernetes external to openstack patch port name.
     *
     * @return kubernetes external to openstack patch port name
     */
    String k8sExtToOsPatchPortName();

    /**
     * Returns openstack to kubernetes integration patch port name.
     *
     * @return openstack to kubernetes integration patch port name
     */
    String osToK8sIntgPatchPortName();

    /**
     * Returns openstack to kubernetes external patch port name.
     *
     * @return openstack to kubernetes external patch port name
     */
    String osToK8sExtPatchPortName();

    /**
     * Returns router to external bridge patch port name.
     *
     * @return router to external bridge patch port name
     */
    String routerToExtPatchPortName();

    /**
     * Returns external to router bridge patch port name.
     *
     * @return external to router bridge patch port name
     */
    String extToRouterPatchPortName();

    /**
     * Returns router part name.
     *
     * @return router port name
     */
    String routerPortName();

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
    PortNumber localToIntgPatchPortNum();

    /**
     * Returns the external to integration patch port number.
     *
     * @return patch port number
     */
    PortNumber extToIntgPatchPortNum();

    /**
     * Returns the integration to tunnel patch port number.
     *
     * @return patch port number
     */
    PortNumber intgToTunPortNum();

    /**
     * Returns the tunnel to integration patch port number.
     *
     * @return patch port number
     */
    PortNumber tunToIntgPortNum();

    /**
     * Returns the router to external bridge patch port number.
     *
     * @return patch port number
     */
    PortNumber routerToExtPortNum();

    /**
     * Returns the external to router bridge patch port number.
     *
     * @return patch port number
     */
    PortNumber extToRouterPortNum();

    /**
     * Returns the router port number.
     *
     * @return router port number
     */
    PortNumber routerPortNum();

    /**
     * Returns the external bridge to router port number.
     *
     * @return port number, null if the port does not exist
     */
    PortNumber extBridgePortNum();

    /**
     * Returns the external interface (attached to external bridge) port number.
     *
     * @return port number, null if the port does ont exist
     */
    PortNumber extIntfPortNum();

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
         * Returns kubernetes node builder with supplied cluster name.
         *
         * @param clusterName cluster name
         * @return kubernetes node builder
         */
        Builder clusterName(String clusterName);

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
         * Returns kubernetes node builder with supplied segment ID.
         *
         * @param segmentId kubernetes node segment ID
         * @return kubernetes node builder
         */
        Builder segmentId(int segmentId);

        /**
         * Return kubernetes node builder with supplied mode.
         *
         * @param mode kubernetes CNI running mode
         * @return kubernetes node builder
         */
        Builder mode(Mode mode);

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
         * Returns kubernetes node builder with supplied tunnel bridge name.
         *
         * @param deviceId tunnel bridge device ID
         * @return kubernetes node builder
         */
        Builder tunBridge(DeviceId deviceId);

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
         * Returns the kubernetes node builder with supplied node info.
         *
         * @param nodeInfo node info
         * @return kubernetes node builder
         */
        Builder nodeInfo(K8sNodeInfo nodeInfo);

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
