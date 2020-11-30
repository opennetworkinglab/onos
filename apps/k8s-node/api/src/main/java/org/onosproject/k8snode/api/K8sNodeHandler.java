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

/**
 * Service handling kubernetes node stats.
 */
public interface K8sNodeHandler {

    /**
     * Processes the given node for init state.
     * It creates required bridges on OVS by referring to node type.
     *
     * @param k8sNode kubernetes node
     */
    void processInitState(K8sNode k8sNode);

    /**
     * Processes the given node for device created state.
     * It creates required ports on the bridges based on the node type.
     *
     * @param k8sNode kubernetes node
     */
    void processDeviceCreatedState(K8sNode k8sNode);

    /**
     * Processes the given node for complete state.
     * It performs post-init jobs for the complete node.
     *
     * @param k8sNode kubernetes node
     */
    void processCompleteState(K8sNode k8sNode);

    /**
     * Processes the given node for incomplete state.
     *
     * @param k8sNode kubernetes node
     */
    void processIncompleteState(K8sNode k8sNode);

    /**
     * Processes the given node for pre-on-board state.
     * It creates required bridges on OVS by referring to node type.
     * It creates required ports on the bridges based on the node type.
     *
     * @param k8sNode kubernetes node
     */
    void processPreOnBoardState(K8sNode k8sNode);

    /**
     * Processes the given node for on boarded state.
     *
     * @param k8sNode kubernetes node
     */
    void processOnBoardedState(K8sNode k8sNode);

    /**
     * Processes the give node for post-on-board state.
     * As long as external interface is configured,
     * it will mark the node state as post-on-board.
     *
     * @param k8sNode kubernetes node
     */
    void processPostOnBoardState(K8sNode k8sNode);
}
