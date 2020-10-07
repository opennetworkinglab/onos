/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.openstacknode.api;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.net.Annotations;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.ControllerInfo;
import org.onosproject.net.device.DeviceService;
import org.onosproject.openstacknode.api.DpdkConfig.DatapathType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static org.onosproject.net.AnnotationKeys.PORT_MAC;
import static org.onosproject.net.AnnotationKeys.PORT_NAME;
import static org.onosproject.openstacknode.api.Constants.GENEVE_TUNNEL;
import static org.onosproject.openstacknode.api.Constants.GRE_TUNNEL;
import static org.onosproject.openstacknode.api.Constants.PATCH_INTG_BRIDGE;
import static org.onosproject.openstacknode.api.Constants.VXLAN_TUNNEL;

/**
 * Representation of a openstack node.
 */
public class DefaultOpenstackNode implements OpenstackNode {

    private final String hostname;
    private final NodeType type;
    private final DeviceId intgBridge;
    private final IpAddress managementIp;
    private final IpAddress dataIp;
    private final String vlanIntf;
    private final String uplinkPort;
    private final NodeState state;
    private final Collection<OpenstackPhyInterface> phyIntfs;
    private final Collection<ControllerInfo> controllers;
    private final OpenstackSshAuth sshAuth;
    private final DpdkConfig dpdkConfig;
    private final KeystoneConfig keystoneConfig;
    private final NeutronConfig neutronConfig;

    private static final String NOT_NULL_MSG = "Node % cannot be null";

    private static final String OVSDB = "ovsdb:";

    /**
     * A default constructor of Openstack Node.
     *
     * @param hostname          hostname
     * @param type              node type
     * @param intgBridge        integration bridge
     * @param managementIp      management IP address
     * @param dataIp            data IP address
     * @param vlanIntf          VLAN interface
     * @param uplinkPort        uplink port name
     * @param state             node state
     * @param phyIntfs          physical interfaces
     * @param controllers       customized controllers
     * @param sshAuth           ssh authentication info
     * @param dpdkConfig        dpdk config
     * @param keystoneConfig    keystone config
     * @param neutronConfig     neutron config
     */
    protected DefaultOpenstackNode(String hostname, NodeType type,
                                   DeviceId intgBridge,
                                   IpAddress managementIp,
                                   IpAddress dataIp,
                                   String vlanIntf,
                                   String uplinkPort,
                                   NodeState state,
                                   Collection<OpenstackPhyInterface> phyIntfs,
                                   Collection<ControllerInfo> controllers,
                                   OpenstackSshAuth sshAuth,
                                   DpdkConfig dpdkConfig,
                                   KeystoneConfig keystoneConfig,
                                   NeutronConfig neutronConfig) {
        this.hostname = hostname;
        this.type = type;
        this.intgBridge = intgBridge;
        this.managementIp = managementIp;
        this.dataIp = dataIp;
        this.vlanIntf = vlanIntf;
        this.uplinkPort = uplinkPort;
        this.state = state;
        this.phyIntfs = phyIntfs;
        this.controllers = controllers;
        this.sshAuth = sshAuth;
        this.dpdkConfig = dpdkConfig;
        this.keystoneConfig = keystoneConfig;
        this.neutronConfig = neutronConfig;
    }

    @Override
    public String hostname() {
        return hostname;
    }

    @Override
    public NodeType type() {
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
    public IpAddress managementIp() {
        return managementIp;
    }

    @Override
    public IpAddress dataIp() {
        return dataIp;
    }

    @Override
    public String vlanIntf() {
        return vlanIntf;
    }

    @Override
    public String uplinkPort() {
        return uplinkPort;
    }

    @Override
    public DatapathType datapathType() {
        if (dpdkConfig == null) {
            return DatapathType.NORMAL;
        }
        return dpdkConfig.datapathType();
    }

    @Override
    public String socketDir() {
        if (dpdkConfig == null) {
            return null;
        }
        return dpdkConfig.socketDir();
    }

    @Override
    public NodeState state() {
        return state;
    }

    @Override
    public PortNumber uplinkPortNum() {
        if (uplinkPort == null) {
            return null;
        }

        DeviceService deviceService = DefaultServiceDirectory.getService(DeviceService.class);
        Port port = deviceService.getPorts(intgBridge).stream()
                .filter(p -> p.isEnabled() &&
                        Objects.equals(p.annotations().value(PORT_NAME), uplinkPort))
                .findAny().orElse(null);

        return port != null ? port.number() : null;

    }

    @Override
    public PortNumber vxlanTunnelPortNum() {
        return tunnelPortNum(VXLAN_TUNNEL);
    }

    @Override
    public PortNumber geneveTunnelPortNum() {
        return tunnelPortNum(GENEVE_TUNNEL);
    }

    @Override
    public PortNumber greTunnelPortNum() {
        return tunnelPortNum(GRE_TUNNEL);

    }

    private PortNumber tunnelPortNum(String tunnelType) {
        if (dataIp == null) {
            return null;
        }
        DeviceService deviceService = DefaultServiceDirectory.getService(DeviceService.class);
        Port port = deviceService.getPorts(intgBridge).stream()
                .filter(p -> p.isEnabled() &&
                        Objects.equals(p.annotations().value(PORT_NAME), tunnelType))
                .findAny().orElse(null);
        return port != null ? port.number() : null;
    }

    @Override
    public PortNumber vlanPortNum() {
        if (vlanIntf == null) {
            return null;
        }
        DeviceService deviceService = DefaultServiceDirectory.getService(DeviceService.class);
        Port port = deviceService.getPorts(intgBridge).stream()
                .filter(p -> p.isEnabled() &&
                        Objects.equals(p.annotations().value(PORT_NAME), vlanIntf))
                .findAny().orElse(null);
        return port != null ? port.number() : null;
    }

    @Override
    public PortNumber patchPortNum() {
        if (type == NodeType.COMPUTE) {
            return null;
        }
        DeviceService deviceService = DefaultServiceDirectory.getService(DeviceService.class);
        Port port = deviceService.getPorts(intgBridge).stream()
                .filter(p -> p.isEnabled() &&
                        Objects.equals(p.annotations().value(PORT_NAME), PATCH_INTG_BRIDGE))
                .findAny().orElse(null);
        return port != null ? port.number() : null;
    }

    @Override
    public MacAddress portMacByName(String portName) {
        if (portName == null) {
            return null;
        } else {
            return macAddress(this.intgBridge, portName);
        }
    }

    @Override
    public PortNumber portNumByName(String portName) {
        if (portName == null) {
            return null;
        } else {
            return portNumber(this.intgBridge, portName);
        }
    }

    @Override
    public MacAddress vlanPortMac() {
        if (vlanIntf == null) {
            return null;
        }
        DeviceService deviceService = DefaultServiceDirectory.getService(DeviceService.class);
        Port port = deviceService.getPorts(intgBridge).stream()
                .filter(p -> p.annotations().value(PORT_NAME).equals(vlanIntf))
                .findAny().orElse(null);
        return port != null ? MacAddress.valueOf(port.annotations().value(PORT_MAC)) : null;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof DefaultOpenstackNode) {
            DefaultOpenstackNode that = (DefaultOpenstackNode) obj;
            return Objects.equals(hostname, that.hostname) &&
                    Objects.equals(type, that.type) &&
                    Objects.equals(intgBridge, that.intgBridge) &&
                    Objects.equals(managementIp, that.managementIp) &&
                    Objects.equals(dataIp, that.dataIp) &&
                    Objects.equals(uplinkPort, that.uplinkPort) &&
                    Objects.equals(vlanIntf, that.vlanIntf) &&
                    Objects.equals(phyIntfs, that.phyIntfs) &&
                    Objects.equals(controllers, that.controllers) &&
                    Objects.equals(sshAuth, that.sshAuth) &&
                    Objects.equals(dpdkConfig, that.dpdkConfig) &&
                    Objects.equals(keystoneConfig, that.keystoneConfig) &&
                    Objects.equals(neutronConfig, that.neutronConfig);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostname,
                type,
                intgBridge,
                managementIp,
                dataIp,
                vlanIntf,
                uplinkPort,
                phyIntfs,
                controllers,
                sshAuth,
                dpdkConfig,
                keystoneConfig,
                neutronConfig);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("hostname", hostname)
                .add("type", type)
                .add("integrationBridge", intgBridge)
                .add("managementIp", managementIp)
                .add("dataIp", dataIp)
                .add("vlanIntf", vlanIntf)
                .add("uplinkPort", uplinkPort)
                .add("state", state)
                .add("phyIntfs", phyIntfs)
                .add("controllers", controllers)
                .add("sshAuth", sshAuth)
                .add("dpdkConfig", dpdkConfig)
                .add("keystoneConfig", keystoneConfig)
                .add("neutronConfig", neutronConfig)
                .toString();
    }

    @Override
    public OpenstackNode updateState(NodeState newState) {
        return new Builder()
                .type(type)
                .hostname(hostname)
                .intgBridge(intgBridge)
                .managementIp(managementIp)
                .dataIp(dataIp)
                .vlanIntf(vlanIntf)
                .uplinkPort(uplinkPort)
                .state(newState)
                .phyIntfs(phyIntfs)
                .controllers(controllers)
                .sshAuthInfo(sshAuth)
                .dpdkConfig(dpdkConfig)
                .keystoneConfig(keystoneConfig)
                .neutronConfig(neutronConfig)
                .build();
    }

    @Override
    public OpenstackNode updateIntbridge(DeviceId newIntgBridge) {
        return new Builder()
                .type(type)
                .hostname(hostname)
                .intgBridge(newIntgBridge)
                .managementIp(managementIp)
                .dataIp(dataIp)
                .vlanIntf(vlanIntf)
                .uplinkPort(uplinkPort)
                .state(state)
                .phyIntfs(phyIntfs)
                .sshAuthInfo(sshAuth)
                .dpdkConfig(dpdkConfig)
                .keystoneConfig(keystoneConfig)
                .neutronConfig(neutronConfig)
                .controllers(controllers)
                .build();
    }

    @Override
    public Collection<OpenstackPhyInterface> phyIntfs() {
        if (phyIntfs == null) {
            return new ArrayList<>();
        }

        return phyIntfs;
    }


    @Override
    public Collection<ControllerInfo> controllers() {
        if (controllers == null) {
            return new ArrayList<>();
        }

        return controllers;
    }

    @Override
    public OpenstackSshAuth sshAuthInfo() {
        return sshAuth;
    }

    @Override
    public DpdkConfig dpdkConfig() {
        return dpdkConfig;
    }

    @Override
    public KeystoneConfig keystoneConfig() {
        return keystoneConfig;
    }

    @Override
    public NeutronConfig neutronConfig() {
        return neutronConfig;
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
     * @return openstack node builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns new builder instance with the given node as a default value.
     *
     * @param osNode openstack node
     * @return openstack node builder
     */
    public static Builder from(OpenstackNode osNode) {
        return new Builder()
                .hostname(osNode.hostname())
                .type(osNode.type())
                .intgBridge(osNode.intgBridge())
                .managementIp(osNode.managementIp())
                .dataIp(osNode.dataIp())
                .vlanIntf(osNode.vlanIntf())
                .uplinkPort(osNode.uplinkPort())
                .state(osNode.state())
                .phyIntfs(osNode.phyIntfs())
                .controllers(osNode.controllers())
                .sshAuthInfo(osNode.sshAuthInfo())
                .dpdkConfig(osNode.dpdkConfig())
                .keystoneConfig(osNode.keystoneConfig())
                .neutronConfig(osNode.neutronConfig());
    }

    /**
     * A builder class for openstack Node.
     */
    public static final class Builder implements OpenstackNode.Builder {

        private String hostname;
        private NodeType type;
        private DeviceId intgBridge;
        private IpAddress managementIp;
        private IpAddress dataIp;
        private String vlanIntf;
        private String uplinkPort;
        private NodeState state;
        private Collection<OpenstackPhyInterface> phyIntfs;
        private Collection<ControllerInfo> controllers;
        private OpenstackSshAuth sshAuth;
        private DpdkConfig dpdkConfig;
        private KeystoneConfig keystoneConfig;
        private NeutronConfig neutronConfig;

        // private constructor not intended to use from external
        private Builder() {
        }

        @Override
        public DefaultOpenstackNode build() {
            checkArgument(hostname != null, NOT_NULL_MSG, "hostname");
            checkArgument(type != null, NOT_NULL_MSG, "type");
            checkArgument(state != null, NOT_NULL_MSG, "state");
            checkArgument(managementIp != null, NOT_NULL_MSG, "management IP");

            if (type != NodeType.CONTROLLER) {
                if (dataIp == null && Strings.isNullOrEmpty(vlanIntf)) {
                    throw new IllegalArgumentException("Either data IP or VLAN interface is required");
                }
            } else {
                checkArgument(keystoneConfig != null, NOT_NULL_MSG, "keystone config");
            }

            if (type == NodeType.GATEWAY && uplinkPort == null) {
                throw new IllegalArgumentException("Uplink port is required for gateway node");
            }

            return new DefaultOpenstackNode(hostname,
                    type,
                    intgBridge,
                    managementIp,
                    dataIp,
                    vlanIntf,
                    uplinkPort,
                    state,
                    phyIntfs,
                    controllers,
                    sshAuth,
                    dpdkConfig,
                    keystoneConfig,
                    neutronConfig);
        }

        @Override
        public Builder hostname(String hostname) {
            if (!Strings.isNullOrEmpty(hostname)) {
                this.hostname = hostname;
            }
            return this;
        }

        @Override
        public Builder type(NodeType type) {
            this.type = type;
            return this;
        }

        @Override
        public Builder intgBridge(DeviceId intgBridge) {
            this.intgBridge = intgBridge;
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
        public Builder vlanIntf(String vlanIntf) {
            this.vlanIntf = vlanIntf;
            return this;
        }

        @Override
        public Builder uplinkPort(String uplinkPort) {
            this.uplinkPort = uplinkPort;
            return this;
        }

        @Override
        public Builder state(NodeState state) {
            this.state = state;
            return this;
        }

        @Override
        public Builder phyIntfs(Collection<OpenstackPhyInterface> phyIntfs) {
            this.phyIntfs = phyIntfs;
            return this;
        }

        @Override
        public Builder controllers(Collection<ControllerInfo> controllers) {
            this.controllers = controllers;
            return this;
        }

        @Override
        public Builder sshAuthInfo(OpenstackSshAuth sshAuth) {
            this.sshAuth = sshAuth;
            return this;
        }

        @Override
        public Builder dpdkConfig(DpdkConfig dpdkConfig) {
            this.dpdkConfig = dpdkConfig;
            return this;
        }

        @Override
        public Builder keystoneConfig(KeystoneConfig keystoneConfig) {
            this.keystoneConfig = keystoneConfig;
            return this;
        }

        @Override
        public Builder neutronConfig(NeutronConfig neutronConfig) {
            this.neutronConfig = neutronConfig;
            return this;
        }
    }
}

