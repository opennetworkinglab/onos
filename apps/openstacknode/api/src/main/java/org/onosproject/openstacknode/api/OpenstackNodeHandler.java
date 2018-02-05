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

/**
 * Service handling openstack node state.
 */
public interface OpenstackNodeHandler {

    /**
     * Processes the given node for init state.
     * It creates required bridges on OVS based on the node type.
     *
     * @param osNode openstack node
     */
    void processInitState(OpenstackNode osNode);

    /**
     * Processes the given node for device created state.
     * It creates required ports on the bridges based on the node type.
     *
     * @param osNode openstack node
     */
    void processDeviceCreatedState(OpenstackNode osNode);

    /**
     * Processes the given node for complete state.
     * It performs post-init jobs for the complete node.
     *
     * @param osNode openstack node
     */
    void processCompleteState(OpenstackNode osNode);

    /**
     * Processes the given node for incomplete state.
     *
     * @param osNode openstack node
     */
    void processIncompleteState(OpenstackNode osNode);
}
