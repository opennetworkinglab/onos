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
import org.onosproject.net.Annotations;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static org.onosproject.k8snode.api.Constants.EXTERNAL_BRIDGE;
import static org.onosproject.k8snode.api.Constants.GENEVE_TUNNEL;
import static org.onosproject.k8snode.api.Constants.GRE_TUNNEL;
import static org.onosproject.k8snode.api.Constants.INTEGRATION_BRIDGE;
import static org.onosproject.k8snode.api.Constants.INTEGRATION_TO_EXTERNAL_BRIDGE;
import static org.onosproject.k8snode.api.Constants.INTEGRATION_TO_LOCAL_BRIDGE;
import static org.onosproject.k8snode.api.Constants.LOCAL_TO_INTEGRATION_BRIDGE;
import static org.onosproject.k8snode.api.Constants.PHYSICAL_EXTERNAL_BRIDGE;
import static org.onosproject.k8snode.api.Constants.VXLAN_TUNNEL;
import static org.onosproject.net.AnnotationKeys.PORT_NAME;

/**
 * Representation of a kubernetes node.
 */
public class DefaultK8sNode implements K8sNode {

    private static final String PORT_MAC = "portMac";

    private final String hostname;
    private final Type type;
    private final DeviceId intgBridge;
    private final DeviceId extBridge;
    private final DeviceId localBridge;
    private final IpAddress managementIp;
    private final IpAddress dataIp;
    private final K8sNodeState state;
    private final String extIntf;
    private final IpAddress extBridgeIp;
    private final IpAddress extGatewayIp;
    private final MacAddress extGatewayMac;
    private final String podCidr;

    private static final String NOT_NULL_MSG = "Node % cannot be null";

    private static final String OVSDB = "ovsdb:";

    /**
     * A default constructor of kubernetes Node.
     *
     * @param hostname          hostname
     * @param type              node type
     * @param intgBridge        integration bridge
     * @param extBridge         external bridge
     * @param localBridge       local bridge
     * @param extIntf           external interface
     * @param managementIp      management IP address
     * @param dataIp            data IP address
     * @param state             node state
     * @param extBridgeIp       external bridge IP address
     * @param extGatewayIp      external gateway IP address
     * @param extGatewayMac     external gateway MAC address
     * @param podCidr           POD CIDR
     */
    protected DefaultK8sNode(String hostname, Type type, DeviceId intgBridge,
                             DeviceId extBridge, DeviceId localBridge,
                             String extIntf, IpAddress managementIp,
                             IpAddress dataIp, K8sNodeState state,
                             IpAddress extBridgeIp, IpAddress extGatewayIp,
                             MacAddress extGatewayMac, String podCidr) {
        this.hostname = hostname;
        this.type = type;
        this.intgBridge = intgBridge;
        this.extBridge = extBridge;
        this.localBridge = localBridge;
        this.extIntf = extIntf;
        this.managementIp = managementIp;
        this.dataIp = dataIp;
        this.state = state;
        this.extBridgeIp = extBridgeIp;
        this.extGatewayIp = extGatewayIp;
        this.extGatewayMac = extGatewayMac;
        this.podCidr = podCidr;
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
    public String extIntf() {
        return extIntf;
    }

    @Override
    public K8sNode updateIntgBridge(DeviceId deviceId) {
        return new Builder()
                .hostname(hostname)
                .type(type)
                .intgBridge(deviceId)
                .extBridge(extBridge)
                .localBridge(localBridge)
                .extIntf(extIntf)
                .managementIp(managementIp)
                .dataIp(dataIp)
                .state(state)
                .extBridgeIp(extBridgeIp)
                .extGatewayIp(extGatewayIp)
                .extGatewayMac(extGatewayMac)
                .podCidr(podCidr)
                .build();
    }

    @Override
    public K8sNode updateExtBridge(DeviceId deviceId) {
        return new Builder()
                .hostname(hostname)
                .type(type)
                .intgBridge(intgBridge)
                .extBridge(deviceId)
                .localBridge(localBridge)
                .extIntf(extIntf)
                .managementIp(managementIp)
                .dataIp(dataIp)
                .state(state)
                .extBridgeIp(extBridgeIp)
                .extGatewayIp(extGatewayIp)
                .extGatewayMac(extGatewayMac)
                .podCidr(podCidr)
                .build();
    }

    @Override
    public K8sNode updateLocalBridge(DeviceId deviceId) {
        return new Builder()
                .hostname(hostname)
                .type(type)
                .intgBridge(intgBridge)
                .extBridge(extBridge)
                .localBridge(deviceId)
                .extIntf(extIntf)
                .managementIp(managementIp)
                .dataIp(dataIp)
                .state(state)
                .extBridgeIp(extBridgeIp)
                .extGatewayIp(extGatewayIp)
                .extGatewayMac(extGatewayMac)
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
                .type(type)
                .intgBridge(intgBridge)
                .extBridge(extBridge)
                .localBridge(localBridge)
                .extIntf(extIntf)
                .managementIp(managementIp)
                .dataIp(dataIp)
                .state(newState)
                .extBridgeIp(extBridgeIp)
                .extGatewayIp(extGatewayIp)
                .extGatewayMac(extGatewayMac)
                .podCidr(podCidr)
                .build();
    }

    @Override
    public K8sNode updateExtGatewayMac(MacAddress newMac) {
        return new Builder()
                .hostname(hostname)
                .type(type)
                .intgBridge(intgBridge)
                .extBridge(extBridge)
                .localBridge(localBridge)
                .extIntf(extIntf)
                .managementIp(managementIp)
                .dataIp(dataIp)
                .state(state)
                .extBridgeIp(extBridgeIp)
                .extGatewayIp(extGatewayIp)
                .extGatewayMac(newMac)
                .podCidr(podCidr)
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
        return portNumber(intgBridge, INTEGRATION_BRIDGE);
    }

    @Override
    public PortNumber intgToExtPatchPortNum() {
        return portNumber(intgBridge, INTEGRATION_TO_EXTERNAL_BRIDGE);
    }

    @Override
    public PortNumber intgToLocalPatchPortNum() {
        return portNumber(intgBridge, INTEGRATION_TO_LOCAL_BRIDGE);
    }

    @Override
    public PortNumber localToIntgPatchPortNumber() {
        return portNumber(localBridge, LOCAL_TO_INTEGRATION_BRIDGE);
    }

    @Override
    public PortNumber extToIntgPatchPortNum() {
        return portNumber(extBridge, PHYSICAL_EXTERNAL_BRIDGE);
    }

    @Override
    public PortNumber extBridgePortNum() {
        if (this.extIntf == null) {
            return null;
        }

        return portNumber(extBridge, this.extIntf);
    }

    @Override
    public MacAddress intgBridgeMac() {
        return macAddress(intgBridge, INTEGRATION_BRIDGE);
    }

    @Override
    public IpAddress extBridgeIp() {
        return extBridgeIp;
    }

    @Override
    public MacAddress extBridgeMac() {
        return macAddress(extBridge, EXTERNAL_BRIDGE);
    }

    @Override
    public IpAddress extGatewayIp() {
        return extGatewayIp;
    }

    @Override
    public MacAddress extGatewayMac() {
        return extGatewayMac;
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
                    localBridge.equals(that.localBridge) &&
                    extIntf.equals(that.extIntf) &&
                    managementIp.equals(that.managementIp) &&
                    dataIp.equals(that.dataIp) &&
                    extBridgeIp.equals(that.extBridgeIp) &&
                    extGatewayIp.equals(that.extGatewayIp) &&
                    podCidr.equals(that.podCidr) &&
                    state == that.state;
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostname, type, intgBridge, extBridge, localBridge,
                            extIntf, managementIp, dataIp, state, extBridgeIp,
                            extGatewayIp, extGatewayMac, podCidr);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("hostname", hostname)
                .add("type", type)
                .add("intgBridge", intgBridge)
                .add("extBridge", extBridge)
                .add("localBridge", localBridge)
                .add("extIntf", extIntf)
                .add("managementIp", managementIp)
                .add("dataIp", dataIp)
                .add("state", state)
                .add("extBridgeIp", extBridgeIp)
                .add("extGatewayIp", extGatewayIp)
                .add("extGatewayMac", extGatewayMac)
                .add("podCidr", podCidr)
                .toString();
    }

    private PortNumber tunnelPortNum(String tunnelType) {
        if (dataIp == null) {
            return null;
        }

        return portNumber(intgBridge, tunnelType);
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
                .type(node.type())
                .intgBridge(node.intgBridge())
                .extBridge(node.extBridge())
                .localBridge(node.localBridge())
                .extIntf(node.extIntf())
                .managementIp(node.managementIp())
                .dataIp(node.dataIp())
                .state(node.state())
                .extBridgeIp(node.extBridgeIp())
                .extGatewayIp(node.extGatewayIp())
                .extGatewayMac(node.extGatewayMac())
                .podCidr(node.podCidr());
    }

    public static final class Builder implements K8sNode.Builder {

        private String hostname;
        private Type type;
        private DeviceId intgBridge;
        private DeviceId extBridge;
        private DeviceId localBridge;
        private IpAddress managementIp;
        private IpAddress dataIp;
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

            return new DefaultK8sNode(hostname,
                    type,
                    intgBridge,
                    extBridge,
                    localBridge,
                    extIntf,
                    managementIp,
                    dataIp,
                    state,
                    extBridgeIp,
                    extGatewayIp,
                    extGatewayMac,
                    podCidr);
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
        public Builder localBridge(DeviceId deviceId) {
            this.localBridge = deviceId;
            return this;
        }

        @Override
        public Builder extIntf(String intf) {
            this.extIntf = intf;
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
        public Builder podCidr(String podCidr) {
            this.podCidr = podCidr;
            return this;
        }
    }
}
