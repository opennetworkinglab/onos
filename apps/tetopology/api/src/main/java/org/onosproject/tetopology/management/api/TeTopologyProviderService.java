/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.tetopology.management.api;

import org.onosproject.net.provider.ProviderService;
import org.onosproject.tetopology.management.api.link.NetworkLink;
import org.onosproject.tetopology.management.api.link.NetworkLinkKey;
import org.onosproject.tetopology.management.api.node.NetworkNode;
import org.onosproject.tetopology.management.api.node.NetworkNodeKey;
import org.onosproject.tetopology.management.api.node.TerminationPoint;
import org.onosproject.tetopology.management.api.node.TerminationPointKey;

/**
 * APIs for Provider to notify Manager about the network topology updates.
 */
public interface TeTopologyProviderService
           extends ProviderService<TeTopologyProvider> {

    /**
     * Signals that a Network has been created/updated.
     *
     * @param network value of the network to be updated
     */
    void networkUpdated(Network network);

    /**
     * Signals that a Network has been removed.
     *
     * @param networkId network id in URI format
     */
    void networkRemoved(KeyId networkId);

    /**
     * Signals that a Link has been created/updated.
     *
     * @param linkKey link id
     * @param link link object to be updated
    */
    void linkUpdated(NetworkLinkKey linkKey, NetworkLink link);

    /**
     * Signals that a Network has been removed.
     *
     * @param linkKey link id
     */
    void linkRemoved(NetworkLinkKey linkKey);

    /**
     * Signals that a Node has been created/updated.
     *
     * @param nodeKey node id
     * @param node node object to be updated
     */
    void nodeUpdated(NetworkNodeKey nodeKey, NetworkNode node);

    /**
     * Signals that a Node has been removed.
     *
     * @param nodeKey node id
     */
    void nodeRemoved(NetworkNodeKey nodeKey);

    /**
     * Signals that a TerminationPoint has been created/updated.
     *
     * @param terminationPointKey termination point id
     * @param terminationPoint termination point object to be updated
     */
    void terminationPointUpdated(TerminationPointKey terminationPointKey,
                                TerminationPoint terminationPoint);

    /**
     * Signals that a TerminationPoint has been removed.
     *
     * @param terminationPointKey termination point id
     */
    void terminationPointRemoved(TerminationPointKey terminationPointKey);

}
