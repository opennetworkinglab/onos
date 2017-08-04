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

import org.onosproject.event.ListenerService;
import org.onosproject.incubator.net.virtual.NetworkId;

import java.util.Set;

/**
 * Service for administering OF agents for a virtual network.
 */
public interface OFAgentService extends ListenerService<OFAgentEvent, OFAgentListener> {

    String APPLICATION_NAME = "org.onosproject.ofagent";

    /**
     * Returns the OpenFlow agent list.
     *
     * @return set of openflow agents
     */
    Set<OFAgent> agents();

    /**
     * Returns the agent for the given network.
     *
     * @param networkId network id
     * @return ofagent; null if no ofagent exists for the network
     */
    OFAgent agent(NetworkId networkId);
}
