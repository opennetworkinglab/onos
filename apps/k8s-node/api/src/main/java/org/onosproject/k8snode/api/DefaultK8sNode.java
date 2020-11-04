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

import com.google.common.base.MoreObjects;
import org.apache.commons.lang.StringUtils;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.k8snode.api.K8sApiConfig.Mode;
import org.onosproject.net.Annotations;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;

import java.util.Objects;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkArgument;
import static org.onosproject.k8snode.api.Constants.DEFAULT_CLUSTER_NAME;
import static org.onosproject.k8snode.api.Constants.DEFAULT_EXTERNAL_BRIDGE_MAC;
import static org.onosproject.k8snode.api.Constants.DEFAULT_EXTERNAL_GATEWAY_MAC;
import static org.onosproject.k8snode.api.Constants.DEFAULT_INTG_BRIDGE_MAC;
import static org.onosproject.k8snode.api.Constants.EXTERNAL_BRIDGE;
import static org.onosproject.k8snode.api.Constants.EXTERNAL_TO_ROUTER;
import static org.onosproject.k8snode.api.Constants.GENEVE_TUNNEL;
import static org.onosproject.k8snode.api.Constants.GRE_TUNNEL;
import static org.onosproject.k8snode.api.Constants.INTEGRATION_BRIDGE;
import static org.onosproject.k8snode.api.Constants.INTEGRATION_TO_EXTERNAL_BRIDGE;
import static org.onosproject.k8snode.api.Constants.INTEGRATION_TO_LOCAL_BRIDGE;
import static org.onosproject.k8snode.api.Constants.INTEGRATION_TO_TUN_BRIDGE;
import static org.onosproject.k8snode.api.Constants.K8S_EXTERNAL_TO_OS_BRIDGE;
import static org.onosproject.k8snode.api.Constants.K8S_INTEGRATION_TO_OS_BRIDGE;
import static org.onosproject.k8snode.api.Constants.LOCAL_BRIDGE;
import static org.onosproject.k8snode.api.Constants.LOCAL_TO_INTEGRATION_BRIDGE;
import static org.onosproject.k8snode.api.Constants.OS_TO_K8S_EXTERNAL_BRIDGE;
import static org.onosproject.k8snode.api.Constants.OS_TO_K8S_INTEGRATION_BRIDGE;
import static org.onosproject.k8snode.api.Constants.PHYSICAL_EXTERNAL_BRIDGE;
import static org.onosproject.k8snode.api.Constants.ROUTER;
import static org.onosproject.k8snode.api.Constants.ROUTER_TO_EXTERNAL;
import static org.onosproject.k8snode.api.Constants.TUNNEL_BRIDGE;
import static org.onosproject.k8snode.api.Constants.TUN_TO_INTEGRATION_BRIDGE;
import static org.onosproject.k8snode.api.Constants.VXLAN_TUNNEL;
import static org.onosproject.k8snode.api.K8sApiConfig.Mode.NORMAL;
import static org.onosproject.k8snode.api.K8sApiConfig.Mode.PASSTHROUGH;
import static org.onosproject.net.AnnotationKeys.PORT_NAME;

/**
 * Representation of a kubernetes node.
 */
public class DefaultK8sNode implements K8sNode {

    private static final String PORT_MAC = "portMac";
    private static final String FLOW_KEY = "flow";

    private static final int SHORT_NAME_LENGTH = 10;

    private final String clusterName;
    private final String hostname;
    private final Type type;
    private final int segmentId;
    private final Mode mode;
    private final DeviceId intgBridge;
    private final DeviceId extBridge;
    private final DeviceId localBridge;
    private final DeviceId tunBridge;
    private final IpAddress managementIp;
    private final IpAddress dataIp;
    private final K8sNodeInfo nodeInfo;
    private final K8sNodeState state;
    private final K8sExternalNetwork extNetwork;
    private final String podCidr;

    private static final String NOT_NULL_MSG = "Node % cannot be null";

    private static final String OVSDB = "ovsdb:";

    /**
     * A default constructor of kubernetes Node.
     *
     * @param clusterName       clusterName
     * @param hostname          hostname
     * @param type              node type
     * @param segmentId         segment identifier
     * @param mode              CNI running mode
     * @param intgBridge        integration bridge
     * @param extBridge         external bridge
     * @param localBridge       local bridge
     * @param tunBridge         tunnel bridge
     * @param managementIp      management IP address
     * @param dataIp            data IP address
     * @param nodeInfo          node info
     * @param state             node state
     * @param extNetwork        external network
     * @param podCidr           POD CIDR
     */
    protected DefaultK8sNode(String clusterName, String hostname, Type type,
                             int segmentId, Mode mode, DeviceId intgBridge,
                             DeviceId extBridge, DeviceId localBridge,
                             DeviceId tunBridge, IpAddress managementIp,
                             IpAddress dataIp, K8sNodeInfo nodeInfo, K8sNodeState state,
                             K8sExternalNetwork extNetwork, String podCidr) {
        this.clusterName = clusterName;
        this.hostname = hostname;
        this.type = type;
        this.mode = mode;
        this.segmentId = segmentId;
        this.intgBridge = intgBridge;
        this.extBridge = extBridge;
        this.localBridge = localBridge;
        this.tunBridge = tunBridge;
        this.managementIp = managementIp;
        this.dataIp = dataIp;
        this.nodeInfo = nodeInfo;
        this.state = state;
        this.extNetwork = extNetwork;
        this.podCidr = podCidr;
    }

    @Override
    public String clusterName() {
        return clusterName;
    }

    @Override
    public String hostShortName() {
        return StringUtils.substring(hostname, 0, SHORT_NAME_LENGTH);
    }

    @Override
    public String uniqueString(int length) {
        String uuid = UUID.nameUUIDFromBytes(hostname.getBytes()).toString();
        return StringUtils.substring(uuid, 0, length);
    }

    @Override
    public int segmentId() {
        return segmentId;
    }

    @Override
    public String tunnelKey() {
        if (mode == PASSTHROUGH) {
            return String.valueOf(segmentId);
        } else {
            return FLOW_KEY;
        }
    }

    @Override
    public Mode mode() {
        return mode;
    }

    @Override
    public String hostname() {
        return hostname;
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public DeviceId ovsdb() {
        return DeviceId.deviceId(OVSDB + managementIp().toString());
    }

    @Override
    public DeviceId intgBridge() {
        return intgBridge;
    }

    @Override
    public DeviceId extBridge() {
        return extBridge;
    }

    @Override
    public DeviceId localBridge() {
        return localBridge;
    }

    @Override
    public DeviceId tunBridge() {

        if (mode == PASSTHROUGH) {
            K8sHostService hostService =
                    DefaultServiceDirectory.getService(K8sHostService.class);
            DeviceId deviceId = null;
            for (K8sHost host : hostService.hosts()) {
                if (host.nodeNames().contains(hostname())) {
                    for (K8sTunnelBridge bridge : host.tunBridges()) {
                        if (bridge.tunnelId() == segmentId()) {
                            deviceId = bridge.deviceId();
                        }
                    }
                }
            }
            return deviceId;
        } else {
            return tunBridge;
        }
    }

    @Override
    public String extIntf() {
        return extNetwork.extIntf();
    }

    @Override
    public K8sNode updateIntgBridge(DeviceId deviceId) {
        return new Builder()
                .hostname(hostname)
                .clusterName(clusterName)
                .type(type)
                .segmentId(segmentId)
                .mode(mode)
                .intgBridge(deviceId)
                .extBridge(extBridge)
                .localBridge(localBridge)
                .tunBridge(tunBridge)
                .managementIp(managementIp)
                .dataIp(dataIp)
                .nodeInfo(nodeInfo)
                .state(state)
                .extBridgeIp(extNetwork.extBridgeIp())
                .extGatewayIp(extNetwork.extGatewayIp())
                .extGatewayMac(extNetwork.extGatewayMac())
                .extIntf(extNetwork.extIntf())
                .podCidr(podCidr)
                .build();
    }

    @Override
    public K8sNode updateExtBridge(DeviceId deviceId) {
        return new Builder()
                .hostname(hostname)
                .clusterName(clusterName)
                .type(type)
                .segmentId(segmentId)
                .mode(mode)
                .intgBridge(intgBridge)
                .extBridge(deviceId)
                .localBridge(localBridge)
                .tunBridge(tunBridge)
                .managementIp(managementIp)
                .dataIp(dataIp)
                .nodeInfo(nodeInfo)
                .state(state)
                .extBridgeIp(extNetwork.extBridgeIp())
                .extGatewayIp(extNetwork.extGatewayIp())
                .extGatewayMac(extNetwork.extGatewayMac())
                .extIntf(extNetwork.extIntf())
                .podCidr(podCidr)
                .build();
    }

    @Override
    public K8sNode updateLocalBridge(DeviceId deviceId) {
        return new Builder()
                .hostname(hostname)
                .clusterName(clusterName)
                .type(type)
                .segmentId(segmentId)
                .mode(mode)
                .intgBridge(intgBridge)
                .extBridge(extBridge)
                .localBridge(deviceId)
                .tunBridge(tunBridge)
                .managementIp(managementIp)
                .dataIp(dataIp)
                .nodeInfo(nodeInfo)
                .state(state)
                .extBridgeIp(extNetwork.extBridgeIp())
                .extGatewayIp(extNetwork.extGatewayIp())
                .extGatewayMac(extNetwork.extGatewayMac())
                .extIntf(extNetwork.extIntf())
                .podCidr(podCidr)
                .build();
    }

    @Override
    public K8sNode updateTunBridge(DeviceId deviceId) {
        return new Builder()
                .hostname(hostname)
                .clusterName(clusterName)
                .type(type)
                .segmentId(segmentId)
                .mode(mode)
                .intgBridge(intgBridge)
                .extBridge(extBridge)
                .localBridge(localBridge)
                .tunBridge(deviceId)
                .managementIp(managementIp)
                .dataIp(dataIp)
                .nodeInfo(nodeInfo)
                .state(state)
                .extBridgeIp(extNetwork.extBridgeIp())
                .extGatewayIp(extNetwork.extGatewayIp())
                .extGatewayMac(extNetwork.extGatewayMac())
                .extIntf(extNetwork.extIntf())
                .podCidr(podCidr)
                .build();
    }

    @Override
    public IpAddress managementIp() {
        return managementIp;
    }

    @Override
    public IpAddress dataIp() {
        return dataIp;
    }

    @Override
    public K8sNodeInfo nodeInfo() {
        return nodeInfo;
    }

    @Override
    public IpAddress nodeIp() {
        return nodeInfo.nodeIp();
    }

    @Override
    public MacAddress nodeMac() {
        if (nodeInfo == null) {
            return null;
        } else {
            return nodeInfo.nodeMac();
        }
    }

    @Override
    public K8sNodeState state() {
        return state;
    }

    @Override
    public String podCidr() {
        return podCidr;
    }

    @Override
    public K8sNode updateState(K8sNodeState newState) {
        return new Builder()
                .hostname(hostname)
                .clusterName(clusterName)
                .type(type)
                .segmentId(segmentId)
                .mode(mode)
                .intgBridge(intgBridge)
                .extBridge(extBridge)
                .localBridge(localBridge)
                .tunBridge(tunBridge)
                .managementIp(managementIp)
                .dataIp(dataIp)
                .nodeInfo(nodeInfo)
                .state(newState)
                .extBridgeIp(extNetwork.extBridgeIp())
                .extGatewayIp(extNetwork.extGatewayIp())
                .extGatewayMac(extNetwork.extGatewayMac())
                .extIntf(extNetwork.extIntf())
                .podCidr(podCidr)
                .build();
    }

    @Override
    public K8sNode updateExtGatewayMac(MacAddress newMac) {
        return new Builder()
                .hostname(hostname)
                .clusterName(clusterName)
                .type(type)
                .segmentId(segmentId)
                .mode(mode)
                .intgBridge(intgBridge)
                .extBridge(extBridge)
                .localBridge(localBridge)
                .tunBridge(tunBridge)
                .managementIp(managementIp)
                .dataIp(dataIp)
                .nodeInfo(nodeInfo)
                .state(state)
                .extBridgeIp(extNetwork.extBridgeIp())
                .extGatewayIp(extNetwork.extGatewayIp())
                .extGatewayMac(newMac)
                .extIntf(extNetwork.extIntf())
                .podCidr(podCidr)
                .build();
    }

    @Override
    public K8sNode updateNodeInfo(K8sNodeInfo newNodeInfo) {
        return new Builder()
                .hostname(hostname)
                .clusterName(clusterName)
                .type(type)
                .segmentId(segmentId)
                .mode(mode)
                .intgBridge(intgBridge)
                .extBridge(extBridge)
                .localBridge(localBridge)
                .tunBridge(tunBridge)
                .managementIp(managementIp)
                .dataIp(dataIp)
                .nodeInfo(newNodeInfo)
                .state(state)
                .extBridgeIp(extNetwork.extBridgeIp())
                .extGatewayIp(extNetwork.extGatewayIp())
                .extGatewayMac(extNetwork.extGatewayMac())
                .extIntf(extNetwork.extIntf())
                .podCidr(podCidr)
                .build();
    }

    @Override
    public PortNumber grePortNum() {
        return tunnelPortNum(grePortName());
    }

    @Override
    public PortNumber vxlanPortNum() {
        return tunnelPortNum(vxlanPortName());
    }

    @Override
    public PortNumber genevePortNum() {
        return tunnelPortNum(genevePortName());
    }

    @Override
    public PortNumber intgBridgePortNum() {
        return PortNumber.LOCAL;
    }

    @Override
    public PortNumber intgToExtPatchPortNum() {
        return portNumber(intgBridge, intgToExtPatchPortName());
    }

    @Override
    public PortNumber intgToLocalPatchPortNum() {
        return portNumber(intgBridge, intgToLocalPatchPortName());
    }

    @Override
    public PortNumber localToIntgPatchPortNum() {
        return portNumber(localBridge, localToIntgPatchPortName());
    }

    @Override
    public PortNumber extToIntgPatchPortNum() {
        return portNumber(extBridge, extToIntgPatchPortName());
    }

    @Override
    public PortNumber intgToTunPortNum() {
        return portNumber(intgBridge, intgToTunPatchPortName());
    }

    @Override
    public PortNumber tunToIntgPortNum() {
        if (mode() == PASSTHROUGH) {
            K8sHostService hostService =
                    DefaultServiceDirectory.getService(K8sHostService.class);
            Port port = null;
            for (K8sHost host : hostService.hosts()) {
                if (host.nodeNames().contains(hostname())) {
                    for (K8sTunnelBridge bridge : host.tunBridges()) {
                        if (bridge.tunnelId() == segmentId()) {
                            port = port(bridge.deviceId(), tunToIntgPatchPortName());
                        }
                    }
                }
            }

            if (port == null) {
                return null;
            } else {
                return port.number();
            }
        } else {
            return portNumber(tunBridge, tunToIntgPatchPortName());
        }
    }

    @Override
    public PortNumber routerToExtPortNum() {
        if (mode() == PASSTHROUGH) {
            K8sHostService hostService =
                    DefaultServiceDirectory.getService(K8sHostService.class);
            Port port = null;
            for (K8sHost host : hostService.hosts()) {
                if (host.nodeNames().contains(hostname())) {
                    for (K8sRouterBridge bridge : host.routerBridges()) {
                        if (bridge.segmentId() == segmentId()) {
                            port = port(bridge.deviceId(), routerToExtPatchPortName());
                        }
                    }
                }
            }

            if (port == null) {
                return null;
            } else {
                return port.number();
            }
        }

        return null;
    }

    @Override
    public PortNumber extToRouterPortNum() {
        return portNumber(extBridge, extToRouterPatchPortName());
    }

    @Override
    public PortNumber routerPortNum() {
        if (mode() == PASSTHROUGH) {
            K8sHostService hostService =
                    DefaultServiceDirectory.getService(K8sHostService.class);
            Port port = null;
            for (K8sHost host : hostService.hosts()) {
                if (host.nodeNames().contains(hostname())) {
                    for (K8sRouterBridge bridge : host.routerBridges()) {
                        if (bridge.segmentId() == segmentId()) {
                            port = port(bridge.deviceId(), routerPortName());
                        }
                    }
                }
            }

            if (port == null) {
                return null;
            } else {
                return port.number();
            }
        }

        return null;
    }

    @Override
    public PortNumber extBridgePortNum() {
        return PortNumber.LOCAL;
    }

    @Override
    public PortNumber extIntfPortNum() {
        if (this.extIntf() == null) {
            return null;
        }
        return portNumber(extBridge, extIntf());
    }

    @Override
    public MacAddress intgBridgeMac() {
        if (mode == PASSTHROUGH) {
            return MacAddress.valueOf(DEFAULT_INTG_BRIDGE_MAC);
        } else {
            return macAddress(intgBridge, intgBridgeName());
        }
    }

    @Override
    public IpAddress extBridgeIp() {
        return extNetwork.extBridgeIp();
    }

    @Override
    public MacAddress extBridgeMac() {
        if (MacAddress.valueOf(DEFAULT_EXTERNAL_GATEWAY_MAC).equals(extGatewayMac())) {
            return MacAddress.valueOf(DEFAULT_EXTERNAL_BRIDGE_MAC);
        } else {
            return macAddress(extBridge, extBridgeName());
        }
    }

    @Override
    public IpAddress extGatewayIp() {
        return extNetwork.extGatewayIp();
    }

    @Override
    public MacAddress extGatewayMac() {
        return extNetwork.extGatewayMac();
    }

    @Override
    public String grePortName() {
        if (mode == PASSTHROUGH) {
            return GRE_TUNNEL + "-" + segmentId;
        } else {
            return GRE_TUNNEL;
        }
    }

    @Override
    public String vxlanPortName() {
        if (mode == PASSTHROUGH) {
            return VXLAN_TUNNEL + "-" + segmentId;
        } else {
            return VXLAN_TUNNEL;
        }
    }

    @Override
    public String genevePortName() {
        if (mode == PASSTHROUGH) {
            return GENEVE_TUNNEL + "-" + segmentId;
        } else {
            return GENEVE_TUNNEL;
        }
    }

    @Override
    public String intgBridgeName() {
        if (mode == PASSTHROUGH) {
            return INTEGRATION_BRIDGE + "-" + uniqueString(4);
        } else {
            return INTEGRATION_BRIDGE;
        }
    }

    @Override
    public String intgEntryPortName() {
        if (mode == PASSTHROUGH) {
            return k8sIntgToOsPatchPortName();
        } else {
            return intgBridgeName();
        }
    }

    @Override
    public MacAddress intgEntryPortMac() {
        return macAddress(intgBridge, intgEntryPortName());
    }

    @Override
    public MacAddress portMacByName(DeviceId deviceId, String portName) {
        if (portName == null) {
            return null;
        } else {
            return macAddress(deviceId, portName);
        }
    }

    @Override
    public PortNumber portNumByName(DeviceId deviceId, String portName) {
        if (portName == null) {
            return null;
        } else {
            return portNumber(deviceId, portName);
        }
    }

    @Override
    public PortNumber intgEntryPortNum() {
        if (mode == PASSTHROUGH) {
            return portNumber(intgBridge, k8sIntgToOsPatchPortName());
        } else {
            return intgBridgePortNum();
        }
    }

    @Override
    public String extBridgeName() {
        if (mode == PASSTHROUGH) {
            return EXTERNAL_BRIDGE + "-" + uniqueString(4);
        } else {
            return EXTERNAL_BRIDGE;
        }
    }

    @Override
    public String localBridgeName() {
        if (mode == PASSTHROUGH) {
            return LOCAL_BRIDGE + "-" + uniqueString(4);
        } else {
            return LOCAL_BRIDGE;
        }
    }

    @Override
    public String tunBridgeName() {
        if (mode == PASSTHROUGH) {
            return TUNNEL_BRIDGE + "-" + segmentId;
        } else {
            return TUNNEL_BRIDGE;
        }
    }

    @Override
    public String intgBridgePortName() {
        if (mode == PASSTHROUGH) {
            return INTEGRATION_BRIDGE + "-" + uniqueString(4);
        } else {
            return INTEGRATION_BRIDGE;
        }
    }

    @Override
    public String extBridgePortName() {
        if (mode == PASSTHROUGH) {
            return EXTERNAL_BRIDGE + "-" + uniqueString(4);
        } else {
            return EXTERNAL_BRIDGE;
        }
    }

    @Override
    public String localBridgePortName() {
        if (mode == PASSTHROUGH) {
            return LOCAL_BRIDGE + "-" + uniqueString(4);
        } else {
            return LOCAL_BRIDGE;
        }
    }

    @Override
    public String tunBridgePortName() {
        if (mode == PASSTHROUGH) {
            return TUNNEL_BRIDGE + "-" + uniqueString(4);
        } else {
            return TUNNEL_BRIDGE;
        }
    }

    @Override
    public String intgToExtPatchPortName() {
        if (mode == PASSTHROUGH) {
            return INTEGRATION_TO_EXTERNAL_BRIDGE + "-" + uniqueString(4);
        } else {
            return INTEGRATION_TO_EXTERNAL_BRIDGE;
        }
    }

    @Override
    public String intgToTunPatchPortName() {
        if (mode == PASSTHROUGH) {
            return INTEGRATION_TO_TUN_BRIDGE + "-" + uniqueString(4);
        } else {
            return INTEGRATION_TO_TUN_BRIDGE;
        }
    }

    @Override
    public String intgToLocalPatchPortName() {
        if (mode == PASSTHROUGH) {
            return INTEGRATION_TO_LOCAL_BRIDGE + "-" + uniqueString(4);
        } else {
            return INTEGRATION_TO_LOCAL_BRIDGE;
        }
    }

    @Override
    public String localToIntgPatchPortName() {
        if (mode == PASSTHROUGH) {
            return LOCAL_TO_INTEGRATION_BRIDGE + "-" + uniqueString(4);
        } else {
            return LOCAL_TO_INTEGRATION_BRIDGE;
        }
    }

    @Override
    public String extToIntgPatchPortName() {
        if (mode == PASSTHROUGH) {
            return PHYSICAL_EXTERNAL_BRIDGE + "-" + uniqueString(4);
        } else {
            return PHYSICAL_EXTERNAL_BRIDGE;
        }
    }

    @Override
    public String tunToIntgPatchPortName() {
        if (mode == PASSTHROUGH) {
            return TUN_TO_INTEGRATION_BRIDGE + "-" + uniqueString(4);
        } else {
            return TUN_TO_INTEGRATION_BRIDGE;
        }
    }

    @Override
    public String k8sIntgToOsPatchPortName() {
        if (mode == PASSTHROUGH) {
            return K8S_INTEGRATION_TO_OS_BRIDGE + "-" + uniqueString(4);
        } else {
            return K8S_INTEGRATION_TO_OS_BRIDGE;
        }
    }

    @Override
    public String k8sExtToOsPatchPortName() {
        if (mode == PASSTHROUGH) {
            return K8S_EXTERNAL_TO_OS_BRIDGE + "-" + uniqueString(4);
        } else {
            return K8S_EXTERNAL_TO_OS_BRIDGE;
        }
    }

    @Override
    public String osToK8sIntgPatchPortName() {
        if (mode == PASSTHROUGH) {
            return OS_TO_K8S_INTEGRATION_BRIDGE + "-" + uniqueString(4);
        } else {
            return OS_TO_K8S_INTEGRATION_BRIDGE;
        }
    }

    @Override
    public String osToK8sExtPatchPortName() {
        if (mode == PASSTHROUGH) {
            return OS_TO_K8S_EXTERNAL_BRIDGE + "-" + uniqueString(4);
        } else {
            return OS_TO_K8S_EXTERNAL_BRIDGE;
        }
    }

    @Override
    public String routerToExtPatchPortName() {
        if (mode == PASSTHROUGH) {
            return ROUTER_TO_EXTERNAL + "-" + uniqueString(4);
        } else {
            return ROUTER_TO_EXTERNAL;
        }
    }

    @Override
    public String extToRouterPatchPortName() {
        if (mode == PASSTHROUGH) {
            return EXTERNAL_TO_ROUTER + "-" + uniqueString(4);
        } else {
            return EXTERNAL_TO_ROUTER;
        }
    }

    @Override
    public String routerPortName() {
        if (mode == PASSTHROUGH) {
            return ROUTER + "-" + segmentId();
        } else {
            return ROUTER;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof DefaultK8sNode) {
            DefaultK8sNode that = (DefaultK8sNode) obj;

            return clusterName.equals(that.clusterName) &&
                    hostname.equals(that.hostname) &&
                    type == that.type &&
                    segmentId == that.segmentId &&
                    mode == that.mode &&
                    intgBridge.equals(that.intgBridge) &&
                    extBridge.equals(that.extBridge) &&
                    localBridge.equals(that.localBridge) &&
                    tunBridge.equals(that.tunBridge) &&
                    managementIp.equals(that.managementIp) &&
                    dataIp.equals(that.dataIp) &&
                    nodeInfo.equals(that.nodeInfo) &&
                    extNetwork.equals(that.extNetwork) &&
                    podCidr.equals(that.podCidr) &&
                    state == that.state;
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(clusterName, hostname, type, segmentId, mode, intgBridge, extBridge,
                localBridge, tunBridge, managementIp, dataIp, nodeInfo, state, extNetwork, podCidr);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("clusterName", clusterName)
                .add("hostname", hostname)
                .add("type", type)
                .add("segmentId", segmentId)
                .add("mode", mode)
                .add("intgBridge", intgBridge)
                .add("extBridge", extBridge)
                .add("localBridge", localBridge)
                .add("tunBridge", tunBridge)
                .add("managementIp", managementIp)
                .add("dataIp", dataIp)
                .add("nodeInfo", nodeInfo)
                .add("state", state)
                .add("extBridgeIp", extNetwork.extBridgeIp())
                .add("extGatewayIp", extNetwork.extGatewayIp())
                .add("extGatewayMac", extNetwork.extGatewayMac())
                .add("extIntf", extNetwork.extIntf())
                .add("podCidr", podCidr)
                .toString();
    }

    private PortNumber tunnelPortNum(String tunnelType) {
        if (dataIp == null) {
            return null;
        }

        return portNumber(tunBridge, tunnelType);
    }

    private MacAddress macAddress(DeviceId deviceId, String portName) {
        Port port = port(deviceId, portName);
        Annotations annots = port.annotations();
        return annots != null ? MacAddress.valueOf(annots.value(PORT_MAC)) : null;
    }

    private PortNumber portNumber(DeviceId deviceId, String portName) {
        Port port = port(deviceId, portName);
        return port != null ? port.number() : null;
    }

    private Port port(DeviceId deviceId, String portName) {
        DeviceService deviceService = DefaultServiceDirectory.getService(DeviceService.class);
        return deviceService.getPorts(deviceId).stream()
                .filter(p -> p.isEnabled() &&
                        Objects.equals(p.annotations().value(PORT_NAME), portName))
                .findAny().orElse(null);
    }

    /**
     * Returns new builder instance.
     *
     * @return kubernetes node builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns new builder instance with the given node as a default value.
     *
     * @param node kubernetes node
     * @return kubernetes node builder
     */
    public static Builder from(K8sNode node) {
        return new Builder()
                .hostname(node.hostname())
                .clusterName(node.clusterName())
                .type(node.type())
                .segmentId(node.segmentId())
                .intgBridge(node.intgBridge())
                .extBridge(node.extBridge())
                .localBridge(node.localBridge())
                .tunBridge(node.tunBridge())
                .extIntf(node.extIntf())
                .managementIp(node.managementIp())
                .dataIp(node.dataIp())
                .nodeInfo(node.nodeInfo())
                .state(node.state())
                .extBridgeIp(node.extBridgeIp())
                .extGatewayIp(node.extGatewayIp())
                .extGatewayMac(node.extGatewayMac())
                .extIntf(node.extIntf())
                .podCidr(node.podCidr());
    }

    public static final class Builder implements K8sNode.Builder {

        private String clusterName;
        private String hostname;
        private Type type;
        private int segmentId;
        private Mode mode;
        private DeviceId intgBridge;
        private DeviceId extBridge;
        private DeviceId localBridge;
        private DeviceId tunBridge;
        private IpAddress managementIp;
        private IpAddress dataIp;
        private K8sNodeInfo nodeInfo;
        private K8sNodeState state;
        private K8sApiConfig apiConfig;
        private String extIntf;
        private IpAddress extBridgeIp;
        private IpAddress extGatewayIp;
        private MacAddress extGatewayMac;
        private String podCidr;

        // private constructor not intended to use from external
        private Builder() {
        }

        @Override
        public K8sNode build() {
            checkArgument(hostname != null, NOT_NULL_MSG, "hostname");
            checkArgument(type != null, NOT_NULL_MSG, "type");
            checkArgument(state != null, NOT_NULL_MSG, "state");
            checkArgument(managementIp != null, NOT_NULL_MSG, "management IP");
            checkArgument(nodeInfo != null, NOT_NULL_MSG, "node info");

            if (StringUtils.isEmpty(clusterName)) {
                clusterName = DEFAULT_CLUSTER_NAME;
            }

            if (mode == null) {
                mode = NORMAL;
            }

            K8sExternalNetwork extNetwork = DefaultK8sExternalNetwork.builder()
                    .extBridgeIp(extBridgeIp)
                    .extGatewayIp(extGatewayIp)
                    .extGatewayMac(extGatewayMac)
                    .extIntf(extIntf)
                    .build();

            return new DefaultK8sNode(clusterName,
                    hostname,
                    type,
                    segmentId,
                    mode,
                    intgBridge,
                    extBridge,
                    localBridge,
                    tunBridge,
                    managementIp,
                    dataIp,
                    nodeInfo,
                    state,
                    extNetwork,
                    podCidr);
        }

        @Override
        public Builder clusterName(String clusterName) {
            this.clusterName = clusterName;
            return this;
        }

        @Override
        public Builder hostname(String hostname) {
            this.hostname = hostname;
            return this;
        }

        @Override
        public Builder type(Type type) {
            this.type = type;
            return this;
        }

        @Override
        public Builder segmentId(int segmentId) {
            this.segmentId = segmentId;
            return this;
        }

        @Override
        public Builder mode(Mode mode) {
            this.mode = mode;
            return this;
        }

        @Override
        public Builder intgBridge(DeviceId deviceId) {
            this.intgBridge = deviceId;
            return this;
        }

        @Override
        public Builder extBridge(DeviceId deviceId) {
            this.extBridge = deviceId;
            return this;
        }

        @Override
        public Builder localBridge(DeviceId deviceId) {
            this.localBridge = deviceId;
            return this;
        }

        @Override
        public Builder tunBridge(DeviceId deviceId) {
            this.tunBridge = deviceId;
            return this;
        }

        @Override
        public Builder managementIp(IpAddress managementIp) {
            this.managementIp = managementIp;
            return this;
        }

        @Override
        public Builder dataIp(IpAddress dataIp) {
            this.dataIp = dataIp;
            return this;
        }

        @Override
        public Builder nodeInfo(K8sNodeInfo nodeInfo) {
            this.nodeInfo = nodeInfo;
            return this;
        }

        @Override
        public Builder state(K8sNodeState state) {
            this.state = state;
            return this;
        }

        @Override
        public Builder extBridgeIp(IpAddress extBridgeIp) {
            this.extBridgeIp = extBridgeIp;
            return this;
        }

        @Override
        public Builder extGatewayIp(IpAddress extGatewayIp) {
            this.extGatewayIp = extGatewayIp;
            return this;
        }

        @Override
        public Builder extGatewayMac(MacAddress extGatewayMac) {
            this.extGatewayMac = extGatewayMac;
            return this;
        }

        @Override
        public Builder extIntf(String intf) {
            this.extIntf = intf;
            return this;
        }

        @Override
        public Builder podCidr(String podCidr) {
            this.podCidr = podCidr;
            return this;
        }
    }
}
