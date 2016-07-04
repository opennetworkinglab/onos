/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.openstacknode;

import org.onlab.packet.IpAddress;
import org.onosproject.event.ListenerService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Handles the bootstrap request for compute/gateway node.
 */
public interface OpenstackNodeService
        extends ListenerService<OpenstackNodeEvent, OpenstackNodeListener> {

    enum NodeType {
        /**
         * Compute or Gateway Node.
         */
        COMPUTE,
        GATEWAY
    }

    /**
     * Adds or updates a new node to the service.
     *
     * @param node openstack node
     */
    void addOrUpdateNode(OpenstackNode node);

    /**
     * Bootstraps node with INIT state.
     *
     * @param node openstack node
     */
    void processInitState(OpenstackNode node);

    /**
     * Bootstraps node with DEVICE_CREATED state.
     *
     * @param node openstack node
     */
    void processDeviceCreatedState(OpenstackNode node);

    /**
     * Bootstraps node with COMPLETE state.
     *
     * @param node openstack node
     */
    void processCompleteState(OpenstackNode node);

    /**
     * Bootstraps node with INCOMPLETE state.
     *
     * @param node openstack node
     */
    void processIncompleteState(OpenstackNode node);

    /**
     * Deletes a node from the service.
     *
     * @param node openstack node
     */
    void deleteNode(OpenstackNode node);

    /**
     * Returns all nodes known to the service.
     *
     * @return list of nodes
     */
    List<OpenstackNode> nodes();

    /**
     * Returns all nodes in complete state.
     *
     * @return set of nodes
     */
    Set<OpenstackNode> completeNodes();

    /**
     * Returns data network IP address of a given integration bridge device.
     *
     * @param intBridgeId integration bridge device id
     * @return ip address; empty value otherwise
     */
    Optional<IpAddress> dataIp(DeviceId intBridgeId);

    /**
     * Returns tunnel port number of a given integration bridge device.
     *
     * @param intBridgeId integration bridge device id
     * @return port number; or empty value
     */
    Optional<PortNumber> tunnelPort(DeviceId intBridgeId);

    /**
     * Returns router bridge device ID connected to a given integration bridge.
     * It returns valid value only if the node type is GATEWAY.
     *
     * @param intBridgeId device id of the integration bridge
     * @return device id of a router bridge; or empty value
     */
    Optional<DeviceId> routerBridge(DeviceId intBridgeId);

    /**
     * Returns port number connected to the router bridge.
     * It returns valid value only if the node type is GATEWAY.
     *
     * @param intBridgeId integration bridge device id
     * @return port number; or empty value
     */
    Optional<PortNumber> externalPort(DeviceId intBridgeId);
}
