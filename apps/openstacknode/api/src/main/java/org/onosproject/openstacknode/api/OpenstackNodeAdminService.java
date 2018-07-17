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
 * Service for administering inventory of opestackNode.
 */
public interface OpenstackNodeAdminService extends OpenstackNodeService {

    /**
     * Creates a new node.
     *
     * @param osNode openstack node
     */
    void createNode(OpenstackNode osNode);

    /**
     * Updates the node.
     *
     * @param osNode openstack node
     */
    void updateNode(OpenstackNode osNode);

    /**
     * Removes the node.
     *
     * @param hostname openstack node hostname
     * @return removed node; null if the node does not exist
     */
    OpenstackNode removeNode(String hostname);
}
