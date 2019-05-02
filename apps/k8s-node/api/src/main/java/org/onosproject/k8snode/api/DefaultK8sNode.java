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
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.ovsdb.controller.OvsdbClientService;
import org.onosproject.ovsdb.controller.OvsdbController;
import org.onosproject.ovsdb.controller.OvsdbNodeId;
import org.onosproject.ovsdb.rfc.notation.OvsdbMap;
import org.onosproject.ovsdb.rfc.table.Interface;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static org.onosproject.k8snode.api.Constants.EXTERNAL_BRIDGE;
import static org.onosproject.k8snode.api.Constants.GENEVE_TUNNEL;
import static org.onosproject.k8snode.api.Constants.GRE_TUNNEL;
import static org.onosproject.k8snode.api.Constants.INTEGRATION_BRIDGE;
import static org.onosproject.k8snode.api.Constants.INTEGRATION_TO_EXTERNAL_BRIDGE;
import static org.onosproject.k8snode.api.Constants.PHYSICAL_EXTERNAL_BRIDGE;
import static org.onosproject.k8snode.api.Constants.VXLAN_TUNNEL;
import static org.onosproject.net.AnnotationKeys.PORT_NAME;

/**
 * Representation of a kubernetes node.
 */
public class DefaultK8sNode implements K8sNode {

    private static final int DEFAULT_OVSDB_PORT = 6640;
    private static final String IP_ADDRESS = "ip_address";
    private static final String MAC_ADDRESS = "mac_address";
    private static final String EXT_INTF = "ext_interface";
    private static final String EXT_GW_IP = "ext_gw_ip_address";
    private static final String EXT_GW_MAC = "ext_gw_mac_address";

    private final String hostname;
    private final Type type;
    private final DeviceId intgBridge;
    private final DeviceId extBridge;
    private final IpAddress managementIp;
    private final IpAddress dataIp;
    private final K8sNodeState state;
    private final MacAddress extGatewayMac;

    private static final String NOT_NULL_MSG = "Node % cannot be null";

    private static final String OVSDB = "ovsdb:";

    /**
     * A default constructor of kubernetes Node.
     *
     * @param hostname          hostname
     * @param type              node type
     * @param intgBridge        integration bridge
     * @param extBridge         external bridge
     * @param managementIp      management IP address
     * @param dataIp            data IP address
     * @param state             node state
     * @param extGatewayMac     external gateway MAC address
     */
    protected DefaultK8sNode(String hostname, Type type, DeviceId intgBridge,
                             DeviceId extBridge, IpAddress managementIp,
                             IpAddress dataIp, K8sNodeState state,
                             MacAddress extGatewayMac) {
        this.hostname = hostname;
        this.type = type;
        this.intgBridge = intgBridge;
        this.extBridge = extBridge;
        this.managementIp = managementIp;
        this.dataIp = dataIp;
        this.state = state;
        this.extGatewayMac = extGatewayMac;
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
    public K8sNode updateIntgBridge(DeviceId deviceId) {
        return new Builder()
                .hostname(hostname)
                .type(type)
                .intgBridge(deviceId)
                .extBridge(extBridge)
                .managementIp(managementIp)
                .dataIp(dataIp)
                .state(state)
                .extGatewayMac(extGatewayMac)
                .build();
    }

    @Override
    public K8sNode updateExtBridge(DeviceId deviceId) {
        return new Builder()
                .hostname(hostname)
                .type(type)
                .intgBridge(intgBridge)
                .extBridge(deviceId)
                .managementIp(managementIp)
                .dataIp(dataIp)
                .state(state)
                .extGatewayMac(extGatewayMac)
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
    public K8sNodeState state() {
        return state;
    }

    @Override
    public K8sNode updateState(K8sNodeState newState) {
        return new Builder()
                .hostname(hostname)
                .type(type)
                .intgBridge(intgBridge)
                .managementIp(managementIp)
                .dataIp(dataIp)
                .state(newState)
                .extGatewayMac(extGatewayMac)
                .build();
    }

    @Override
    public K8sNode updateExtGatewayMac(MacAddress newMac) {
        return new Builder()
                .hostname(hostname)
                .type(type)
                .intgBridge(intgBridge)
                .managementIp(managementIp)
                .dataIp(dataIp)
                .state(state)
                .extGatewayMac(newMac)
                .build();

    }

    @Override
    public PortNumber grePortNum() {
        return tunnelPortNum(GRE_TUNNEL);
    }

    @Override
    public PortNumber vxlanPortNum() {
        return tunnelPortNum(VXLAN_TUNNEL);
    }

    @Override
    public PortNumber genevePortNum() {
        return tunnelPortNum(GENEVE_TUNNEL);
    }

    @Override
    public PortNumber intgBridgePortNum() {
        DeviceService deviceService = DefaultServiceDirectory.getService(DeviceService.class);
        Port port = deviceService.getPorts(intgBridge).stream()
                .filter(p -> p.isEnabled() &&
                        Objects.equals(p.annotations().value(PORT_NAME), INTEGRATION_BRIDGE))
                .findAny().orElse(null);
        return port != null ? port.number() : null;
    }

    @Override
    public MacAddress intgBridgeMac() {
        OvsdbClientService client = getOvsClient();

        if (client == null) {
            return null;
        }

        Interface iface = getOvsClient().getInterface(INTEGRATION_BRIDGE);
        OvsdbMap data = (OvsdbMap) iface.getExternalIdsColumn().data();
        return MacAddress.valueOf((String) data.map().get(MAC_ADDRESS));
    }

    @Override
    public IpAddress extBridgeIp() {
        OvsdbClientService client = getOvsClient();

        if (client == null) {
            return null;
        }

        Interface iface = getOvsClient().getInterface(EXTERNAL_BRIDGE);
        OvsdbMap data = (OvsdbMap) iface.getExternalIdsColumn().data();
        return IpAddress.valueOf((String) data.map().get(IP_ADDRESS));
    }

    @Override
    public MacAddress extBridgeMac() {
        OvsdbClientService client = getOvsClient();

        if (client == null) {
            return null;
        }

        Interface iface = getOvsClient().getInterface(EXTERNAL_BRIDGE);
        OvsdbMap data = (OvsdbMap) iface.getExternalIdsColumn().data();
        return MacAddress.valueOf((String) data.map().get(MAC_ADDRESS));
    }

    @Override
    public IpAddress extGatewayIp() {
        OvsdbClientService client = getOvsClient();

        if (client == null) {
            return null;
        }

        Interface iface = getOvsClient().getInterface(EXTERNAL_BRIDGE);
        OvsdbMap data = (OvsdbMap) iface.getExternalIdsColumn().data();
        return IpAddress.valueOf((String) data.map().get(EXT_GW_IP));
    }

    @Override
    public MacAddress extGatewayMac() {
        return extGatewayMac;
    }

    @Override
    public PortNumber intgToExtPatchPortNum() {
        return portNumber(intgBridge, INTEGRATION_TO_EXTERNAL_BRIDGE);
    }

    @Override
    public PortNumber extToIntgPatchPortNum() {
        return portNumber(extBridge, PHYSICAL_EXTERNAL_BRIDGE);
    }

    @Override
    public PortNumber extBridgePortNum() {
        OvsdbClientService client = getOvsClient();

        if (client == null) {
            return null;
        }

        Interface iface = getOvsClient().getInterface(EXTERNAL_BRIDGE);
        OvsdbMap data = (OvsdbMap) iface.getExternalIdsColumn().data();
        String extIface = (String) data.map().get(EXT_INTF);
        if (extIface == null) {
            return null;
        }

        return portNumber(extBridge, extIface);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof DefaultK8sNode) {
            DefaultK8sNode that = (DefaultK8sNode) obj;

            return hostname.equals(that.hostname) &&
                    type == that.type &&
                    intgBridge.equals(that.intgBridge) &&
                    extBridge.equals(that.extBridge) &&
                    managementIp.equals(that.managementIp) &&
                    dataIp.equals(that.dataIp) &&
                    state == that.state;
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostname, type, intgBridge, extBridge,
                            managementIp, dataIp, state, extGatewayMac);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("hostname", hostname)
                .add("type", type)
                .add("intgBridge", intgBridge)
                .add("extBridge", extBridge)
                .add("managementIp", managementIp)
                .add("dataIp", dataIp)
                .add("state", state)
                .add("extGatewayMac", extGatewayMac)
                .toString();
    }

    private PortNumber tunnelPortNum(String tunnelType) {
        if (dataIp == null) {
            return null;
        }

        return portNumber(intgBridge, tunnelType);
    }

    private PortNumber portNumber(DeviceId deviceId, String portName) {
        DeviceService deviceService = DefaultServiceDirectory.getService(DeviceService.class);
        Port port = deviceService.getPorts(deviceId).stream()
                .filter(p -> p.isEnabled() &&
                        Objects.equals(p.annotations().value(PORT_NAME), portName))
                .findAny().orElse(null);
        return port != null ? port.number() : null;
    }

    private OvsdbClientService getOvsClient() {
        OvsdbController ovsdbController =
                DefaultServiceDirectory.getService(OvsdbController.class);
        OvsdbNodeId ovsdb = new OvsdbNodeId(this.managementIp, DEFAULT_OVSDB_PORT);
        OvsdbClientService client = ovsdbController.getOvsdbClient(ovsdb);
        if (client == null) {
            return null;
        }

        return client;
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
                .type(node.type())
                .intgBridge(node.intgBridge())
                .extBridge(node.extBridge())
                .managementIp(node.managementIp())
                .dataIp(node.dataIp())
                .state(node.state())
                .extGatewayMac(node.extGatewayMac());
    }

    public static final class Builder implements K8sNode.Builder {

        private String hostname;
        private Type type;
        private DeviceId intgBridge;
        private DeviceId extBridge;
        private IpAddress managementIp;
        private IpAddress dataIp;
        private K8sNodeState state;
        private K8sApiConfig apiConfig;
        private MacAddress extGatewayMac;

        // private constructor not intended to use from external
        private Builder() {
        }

        @Override
        public K8sNode build() {
            checkArgument(hostname != null, NOT_NULL_MSG, "hostname");
            checkArgument(type != null, NOT_NULL_MSG, "type");
            checkArgument(state != null, NOT_NULL_MSG, "state");
            checkArgument(managementIp != null, NOT_NULL_MSG, "management IP");

            return new DefaultK8sNode(hostname,
                    type,
                    intgBridge,
                    extBridge,
                    managementIp,
                    dataIp,
                    state,
                    extGatewayMac);
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
        public Builder state(K8sNodeState state) {
            this.state = state;
            return this;
        }

        @Override
        public Builder extGatewayMac(MacAddress extGatewayMac) {
            this.extGatewayMac = extGatewayMac;
            return this;
        }
    }
}
