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
 * Representation of an OpenFlow agent, which holds the mapping between the virtual
 * network and the external OpenFlow controllers.
 */
public interface OFAgent {

    enum State {

        /**
         * Specifies that the ofagent state is started.
         */
        STARTED,

        /**
         * Specifies that the ofagent state is stopped.
         */
        STOPPED
    }

    /**
     * Returns the identifier of the virtual network that this agent cares for.
     *
     * @return id of the virtual network
     */
    NetworkId networkId();

    /**
     * Returns the external OpenFlow controllers of the virtual network.
     *
     * @return set of openflow controllers
     */
    Set<OFController> controllers();

    /**
     * Returns the admin state of the agent.
     *
     * @return state
     */
    State state();

    /**
     * Builder of OF agent entities.
     */
    interface Builder {

        /**
         * Returns new OF agent.
         *
         * @return of agent
         */
        OFAgent build();


        /**
         * Returns OF agent builder with the supplied OF agent.
         *
         * @param ofAgent ofagent
         * @return of agent builder
         */
        Builder from(OFAgent ofAgent);

        /**
         * Returns OF agent builder with the supplied network ID.
         *
         * @param networkId id of the virtual network
         * @return of agent builder
         */
        Builder networkId(NetworkId networkId);

        /**
         * Returns OF agent builder with the supplied controllers.
         *
         * @param controllers set of openflow controllers
         * @return of agent builder
         */
        Builder controllers(Set<OFController> controllers);

        /**
         * Returns OF agent builder with the supplied additional controller.
         *
         * @param controller additional controller
         * @return of agent builder
         */
        Builder addController(OFController controller);

        /**
         * Returns OF agent builder with the supplied controller removed.
         *
         * @param controller controller to delete
         * @return of agent builder
         */
        Builder deleteController(OFController controller);

        /**
         * Returns OF agent builder with the supplied state.
         *
         * @param state state of the agent
         * @return of agent builder
         */
        Builder state(State state);
    }
}
