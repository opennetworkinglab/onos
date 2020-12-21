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

/**
 * Service for administering inventory of {@link KubevirtNode}.
 */
public interface KubevirtNodeAdminService extends KubevirtNodeService {

    /**
     * Creates a new node.
     *
     * @param node kubevirt node
     */
    void createNode(KubevirtNode node);

    /**
     * Updates the node.
     *
     * @param node kubevirt node
     */
    void updateNode(KubevirtNode node);

    /**
     * Removes the node.
     *
     * @param hostname kubevirt node hostname
     * @return removed node; null if the node does not exist
     */
    KubevirtNode removeNode(String hostname);
}
