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
package org.onosproject.ofagent.api;

import org.onosproject.incubator.net.virtual.NetworkId;

import java.util.Set;

/**
 * Service for administering OF agents for a virtual network.
 */
public interface OFAgentService {

    /**
     * Returns the OpenFlow agent list.
     *
     * @return set of openflow agents
     */
    Set<OFAgent> agents();

    /**
     * Creates an OpenFlow agent for a given virtual network with given controllers.
     *
     * @param networkId   id of the virtual network
     * @param controllers list of controllers
     */
    void createAgent(NetworkId networkId, OFController... controllers);

    /**
     * Removes the OpenFlow agent for the given virtual network.
     *
     * @param networkId virtual network identifier
     */
    void removeAgent(NetworkId networkId);

    /**
     * Starts the agent for the given network.
     *
     * @param networkId virtual network identifier
     */
    void startAgent(NetworkId networkId);

    /**
     * Stops the agent for the given network.
     *
     * @param networkId virtual network identifier
     */
    void stopAgent(NetworkId networkId);

    /**
     * Returns if the agent of the given network is active or not.
     *
     * @param networkId network id
     * @return true if the agent is active
     */
    boolean isActive(NetworkId networkId);
}
