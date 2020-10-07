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
import org.onosproject.event.ListenerService;
import org.onosproject.net.DeviceId;
import org.onosproject.openstacknode.api.OpenstackNode.NodeType;

import java.util.Set;

/**
 * Service for interfacing with the inventory of {@link OpenstackNode}.
 */
public interface OpenstackNodeService extends ListenerService<OpenstackNodeEvent, OpenstackNodeListener> {

    String APP_ID = "org.onosproject.openstacknode";

    /**
     * Returns all registered nodes.
     *
     * @return set of openstack nodes
     */
    Set<OpenstackNode> nodes();

    /**
     * Returns all nodes with the specified type.
     *
     * @param type node type
     * @return set of openstack nodes
     */
    Set<OpenstackNode> nodes(NodeType type);

    /**
     * Returns all nodes with complete state.
     *
     * @return set of openstack nodes
     */
    Set<OpenstackNode> completeNodes();

    /**
     * Returns all nodes with complete state and the specified type.
     *
     * @param type node type
     * @return set of openstack nodes
     */
    Set<OpenstackNode> completeNodes(NodeType type);

    /**
     * Returns the node with the specified hostname.
     *
     * @param hostname hostname
     * @return openstack node
     */
    OpenstackNode node(String hostname);

    /**
     * Returns the node with the specified device ID.
     * The device ID can be any one of integration bridge, router bridge,
     * or ovsdb device.
     *
     * @param deviceId device id
     * @return openstack node
     */
    OpenstackNode node(DeviceId deviceId);

    /**
     * Returns the node with the specified management IP address.
     *
     * @param mgmtIp management IP
     * @return openstack node
     */
    OpenstackNode node(IpAddress mgmtIp);

    /**
     * Adds the vf port to the given openstack node.
     *
     * @param osNode openstack node
     * @param portName port name
     */
    void addVfPort(OpenstackNode osNode, String portName);

    /**
     * Removes vf port to the given openstack node.
     *
     * @param osNode openstack node
     * @param portName port name
     */
    void removeVfPort(OpenstackNode osNode, String portName);
}
