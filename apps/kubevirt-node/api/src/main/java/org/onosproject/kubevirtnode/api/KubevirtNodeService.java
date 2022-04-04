/*
 * Copyright 2020-present Open Networking Foundation
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
package org.onosproject.kubevirtnode.api;

import org.onlab.packet.IpAddress;
import org.onosproject.event.ListenerService;
import org.onosproject.net.DeviceId;

import java.util.Set;

/**
 * Service for interfacing with the inventory of {@link KubevirtNode}.
 */
public interface KubevirtNodeService extends ListenerService<KubevirtNodeEvent, KubevirtNodeListener> {

    String APP_ID = "org.onosproject.kubevirtnode";

    /**
     * Returns all registered nodes.
     *
     * @return set of kubevirt nodes
     */
    Set<KubevirtNode> nodes();

    /**
     * Returns all nodes with the specified type.
     *
     * @param type node type
     * @return set of kubevirt nodes
     */
    Set<KubevirtNode> nodes(KubevirtNode.Type type);

    /**
     * Returns all nodes with complete state.
     *
     * @return set of kubevirt nodes
     */
    Set<KubevirtNode> completeNodes();

    /**
     * Returns all nodes with complete state and the specified type.
     *
     * @param type node type
     * @return set of kubevirt nodes
     */
    Set<KubevirtNode> completeNodes(KubevirtNode.Type type);

    /**
     * Returns the node with the specified hostname.
     *
     * @param hostname hostname
     * @return kubevirt node
     */
    KubevirtNode node(String hostname);

    /**
     * Returns the node with the specified device ID.
     * The device ID can be any one of integration bridge or ovsdb device.
     *
     * @param deviceId device id
     * @return kubevirt node
     */
    KubevirtNode node(DeviceId deviceId);

    /**
     * Returns the node with the specified management IP address.
     *
     * @param mgmtIp management IP
     * @return kubevirt node
     */
    KubevirtNode node(IpAddress mgmtIp);

    /**
     * Checks whether has the node with the given hostname.
     *
     * @param hostname hostname
     * @return true if it has the node, false otherwise
     */
    boolean hasNode(String hostname);

    /**
     * Returns the node with the specified tunnel device ID.
     * The device ID tunnel bridge.
     *
     * @param deviceId device id
     * @return kubevirt node
     */
    KubevirtNode nodeByTunBridge(DeviceId deviceId);

    /**
     * Returns the node with the specified physical device ID.
     *
     * @param deviceId device id
     * @return node
     */
    KubevirtNode nodeByPhyBridge(DeviceId deviceId);
}
