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

import org.onosproject.event.ListenerService;
import org.onosproject.k8snode.api.K8sNode.Type;
import org.onosproject.net.DeviceId;

import java.util.Set;

/**
 * Service for interfacing with the inventory of Kubernetes Node.
 */
public interface K8sNodeService extends ListenerService<K8sNodeEvent, K8sNodeListener> {

    String APP_ID = "org.onosproject.k8snode";

    /**
     * Returns all registered nodes.
     *
     * @return set of kubernetes nodes
     */
    Set<K8sNode> nodes();

    /**
     * Returns kubernetes nodes associated with cluster name.
     *
     * @param clusterName cluster name
     * @return set of kubernetes nodes
     */
    Set<K8sNode> nodes(String clusterName);

    /**
     * Returns all nodes with the specified type.
     *
     * @param type node type
     * @return set of kubernetes nodes
     */
    Set<K8sNode> nodes(Type type);

    /**
     * Returns all nodes with complete states.
     *
     * @return set of kubernetes nodes
     */
    Set<K8sNode> completeNodes();

    /**
     * Returns all nodes with complete state and the specified type.
     *
     * @param type node type
     * @return set of kubernetes nodes
     */
    Set<K8sNode> completeNodes(Type type);

    /**
     * Returns the node with the specified hostname.
     *
     * @param hostname hostname
     * @return kubernetes node
     */
    K8sNode node(String hostname);

    /**
     * Returns the node with the specified device ID.
     * The device ID can be any one of integration bridge, router bridge,
     * or ovsdb device.
     *
     * @param deviceId device ID
     * @return kubernetes node
     */
    K8sNode node(DeviceId deviceId);
}
