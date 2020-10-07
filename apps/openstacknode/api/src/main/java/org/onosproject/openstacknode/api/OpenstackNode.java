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

import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.ControllerInfo;
import org.onosproject.openstacknode.api.DpdkConfig.DatapathType;

import java.util.Collection;

/**
 * Representation of a node used in OpenstackNetworking service.
 */
public interface OpenstackNode {

    /**
     * List of valid node types.
     */
    enum NodeType {
        COMPUTE,
        GATEWAY,
        CONTROLLER
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
    NodeType type();

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
     * Returns the name of the vlan interface.
     *
     * @return vlan interface name; null if vlan mode is not enabled
     */
    String vlanIntf();

    /**
     * Returns the initialization state of the node.
     *
     * @return node state
     */
    NodeState state();

    /**
     * Returns the GRE tunnel port number.
     *
     * @return GRE port number; null if the GRE tunnel port does not exist
     */
    PortNumber greTunnelPortNum();

    /**
     * Returns the VXLAN tunnel port number.
     *
     * @return VXLAN port number; null if tunnel port does not exist
     */
    PortNumber vxlanTunnelPortNum();

    /**
     * Returns the GENEVE tunnel port number.
     *
     * @return GENEVE port number; null if the GRE tunnel port does not exist
     */
    PortNumber geneveTunnelPortNum();

    /**
     * Returns the vlan port number.
     *
     * @return port number; null if vlan port does not exist
     */
    PortNumber vlanPortNum();

    /**
     * Returns the patch port number of the integration bridge.
     *
     * @return port number; null if the node type is compute
     */
    PortNumber patchPortNum();

    /**
     * Returns the port MAC address with the given patch port name.
     *
     * @param portName patch port name
     * @return port MAC address
     */
    MacAddress portMacByName(String portName);

    /**
     * Returns the port number with the given patch port name.
     *
     * @param portName patch port name
     * @return port number
     */
    PortNumber portNumByName(String portName);

    /**
     * Returns the vlan port MAC address.
     *
     * @return mac address; null if vlan port does not exist
     */
    MacAddress vlanPortMac();

    /**
     * Returns the uplink port name.
     *
     * @return uplink port name; null if the node type is compute
     */
    String uplinkPort();

    /**
     * Returns the data path type.
     *
     * @return data path type; normal or netdev
     */
    DatapathType datapathType();

    /**
     * Returns socket directory which dpdk port bound to.
     *
     * @return socket directory
     */
    String socketDir();

    /**
     * Returns the uplink port number.
     *
     * @return uplink port number
     */
    PortNumber uplinkPortNum();

    /**
     * Returns new openstack node instance with given state.
     *
     * @param newState updated state
     * @return updated openstack node
     */
    OpenstackNode updateState(NodeState newState);

    /**
     * Returns new openstack node instance with given integration bridge.
     *
     * @param newIntgBridge updated integration bridge
     * @return updated openstack node
     */
    OpenstackNode updateIntbridge(DeviceId newIntgBridge);

    /**
     * Returns a collection of physical interfaces.
     *
     * @return physical interfaces
     */
    Collection<OpenstackPhyInterface> phyIntfs();

    /**
     * Returns a collection of customized controllers.
     *
     * @return customized controllers
     */
    Collection<ControllerInfo> controllers();

    /**
     * Returns the ssh authentication info.
     *
     * @return ssh authentication info
     */
    OpenstackSshAuth sshAuthInfo();

    /**
     * Returns the dpdk config info.
     *
     * @return dpdk config
     */
    DpdkConfig dpdkConfig();

    /**
     * Returns the keystone config info.
     *
     * @return keystone config
     */
    KeystoneConfig keystoneConfig();

    /**
     * Returns the neutron config info.
     *
     * @return neutron config
     */
    NeutronConfig neutronConfig();

    /**
     * Builder of new node entities.
     */
    interface Builder {

        /**
         * Builds an immutable openstack node instance.
         *
         * @return openstack node instance
         */
        OpenstackNode build();

        /**
         * Returns openstack node builder with supplied hostname.
         *
         * @param hostname hostname of the node
         * @return opesntack node builder
         */
        Builder hostname(String hostname);

        /**
         * Returns openstack node builder with supplied type.
         *
         * @param type openstack node type
         * @return openstack node builder
         */
        Builder type(NodeType type);

        /**
         * Returns openstack node builder with supplied integration bridge ID.
         *
         * @param intgBridge integration bridge device id
         * @return openstack node builder
         */
        Builder intgBridge(DeviceId intgBridge);

        /**
         * Returns openstack node builder with supplied management IP address.
         *
         * @param managementIp management ip address
         * @return openstack node builder
         */
        Builder managementIp(IpAddress managementIp);

        /**
         * Returns openstack node builder with supplied data network IP address.
         *
         * @param dataIp data network ip address
         * @return openstack node builder
         */
        Builder dataIp(IpAddress dataIp);

        /**
         * Returns openstack node builder with supplied vlan interface.
         *
         * @param vlanIntf vlan interface name
         * @return openstack node builder
         */
        Builder vlanIntf(String vlanIntf);

        /**
         * Returns openstack node builder with supplied uplink port.
         *
         * @param uplinkPort uplink port name
         * @return openstack node builder
         */
        Builder uplinkPort(String uplinkPort);

        /**
         * Returns openstack node builder with supplied node state.
         *
         * @param state node state
         * @return openstack node builder
         */
        Builder state(NodeState state);

        /**
         * Returns openstack node builder with supplied physical interfaces.
         *
         * @param phyIntfs a collection of physical interfaces
         * @return openstack node builder
         */
        Builder phyIntfs(Collection<OpenstackPhyInterface> phyIntfs);

        /**
         * Returns openstack node builder with supplied customized controllers.
         *
         * @param controllers a collection of customized controllers
         * @return openstack node builder
         */
        Builder controllers(Collection<ControllerInfo> controllers);

        /**
         * Returns openstack node builder with supplied ssh authentication info.
         *
         * @param sshAuth ssh authentication info
         * @return openstack node builder
         */
        Builder sshAuthInfo(OpenstackSshAuth sshAuth);

        /**
         * Returns openstack node builder with supplied dpdk config info.
         *
         * @param dpdkConfig dpdk config
         * @return openstack node builder
         */
        Builder dpdkConfig(DpdkConfig dpdkConfig);

        /**
         * Returns openstack node builder with supplied keystone config info.
         *
         * @param keystoneConfig keystone config
         * @return openstack node builder
         */
        Builder keystoneConfig(KeystoneConfig keystoneConfig);

        /**
         * Returns openstack node builder with supplied neutron config info.
         *
         * @param neutronConfig neutron config
         * @return openstack node builder
         */
        Builder neutronConfig(NeutronConfig neutronConfig);
    }
}

