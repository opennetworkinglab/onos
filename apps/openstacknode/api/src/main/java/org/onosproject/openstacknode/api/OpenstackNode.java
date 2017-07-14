/*
 * Copyright 2017-present Open Networking Laboratory
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
import org.onosproject.core.GroupId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.group.GroupKey;

/**
 * Representation of a node used in OpenstackNetworking service.
 */
public interface OpenstackNode {

    /**
     * List of valid virtual network modes.
     */
    enum NetworkMode {
        VXLAN,
        VLAN
    }

    /**
     * List of valid node types.
     */
    enum NodeType {
        COMPUTE,
        GATEWAY
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
     * Returns the router bridge device ID.
     *
     * @return device id; null if the node type is compute
     */
    DeviceId routerBridge();

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
     * Returns the gateway group ID of this node.
     *
     * @param mode network mode of the group
     * @return gateway group identifier
     */
    GroupId gatewayGroupId(NetworkMode mode);

    /**
     * Returns the group key of this node.
     *
     * @param mode network mode of the group
     * @return gateway group key
     */
    GroupKey gatewayGroupKey(NetworkMode mode);

    /**
     * Returns the tunnel port number.
     *
     * @return port number; null if tunnel port does not exist
     */
    PortNumber tunnelPortNum();

    /**
     * Returns the vlan port nubmer.
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
     * Returns the vlan port MAC address.
     *
     * @return mac address; null if vlan port does not exist
     */
    MacAddress vlanPortMac();

    /**
     * Returns new openstack node instance with given state.
     *
     * @param newState updated state
     * @return updated openstack node
     */
    OpenstackNode updateState(NodeState newState);

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
         * Returns openstack node builder with supplied router bridge ID.
         *
         * @param routerBridge router bridge id
         * @return openstack node builder
         */
        Builder routerBridge(DeviceId routerBridge);

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
         * Returns openstack node builder with supplied node state.
         *
         * @param state node state
         * @return openstack node builder
         */
        Builder state(NodeState state);
    }
}

