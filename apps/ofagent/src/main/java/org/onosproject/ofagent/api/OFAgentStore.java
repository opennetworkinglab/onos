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
package org.onosproject.ofagent.api;

import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.store.Store;

import java.util.Set;

/**
 * Manages inventory of OpenFlow agent states; not intended for direct use.
 */
public interface OFAgentStore extends Store<OFAgentEvent, OFAgentStoreDelegate>  {

    /**
     * Creates the new openflow agent.
     *
     * @param ofAgent the new ofagent
     */
    void createOfAgent(OFAgent ofAgent);

    /**
     * Updates the openflow agent.
     *
     * @param ofAgent the updated ofagent
     */
    void updateOfAgent(OFAgent ofAgent);

    /**
     * Removes the openflow agent for the supplied network ID.
     *
     * @param networkId virtual network identifier
     * @return removed agent; null if remove failed
     */
    OFAgent removeOfAgent(NetworkId networkId);

    /**
     * Returns the openflow agent with the supplied network ID.
     *
     * @param networkId virtual network identifier
     * @return ofagent; null if no ofagent exists for the network
     */
    OFAgent ofAgent(NetworkId networkId);

    /**
     * Returns all openflow agents.
     *
     * @return set of ofagents; empty set if no ofagents exist
     */
    Set<OFAgent> ofAgents();
}
