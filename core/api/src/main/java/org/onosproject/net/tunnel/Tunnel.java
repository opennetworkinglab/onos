/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.net.tunnel;

import org.onosproject.net.Annotated;
import org.onosproject.net.NetworkResource;
import org.onosproject.net.Provided;
import org.onosproject.net.resource.BandwidthResource;


/**
 * Abstraction of a generalized Tunnel entity (bandwidth pipe) for either L3/L2 networks or L1/L0 networks,
 * representation of e.g., VLAN, GRE tunnel, MPLS LSP, L1 ODUk connection, WDM OCH, etc.. Each Tunnel is
 * associated with at least two Label objects that model the logical ports essentially.
 * Note that it supports nested case.
 */

public interface Tunnel extends Annotated, Provided, NetworkResource {

    /**
     * Coarse representation of the Tunnel types.
     */
    public enum Type {
        /**
         * Signifies that this is a L2 tunnel.
         */
        VLAN,

        /**
         * Signifies that this is a DC L2 extension tunnel.
         */
        VXLAN,

        /**
         * Signifies that this is a L3 tunnel.
         */
        GRE,

        /**
         * Signifies that this is a MPLS tunnel.
         */
        LSP,

        /**
         * Signifies that this is a L1 OTN tunnel.
         */
        ODUk,

        /**
         * Signifies that this is a L0 OCH tunnel.
         */
        OCH
    }

    /**
     * Representation of the tunnel state.
     *
     */
    public enum State {

        /**
         * Signifies that a tunnel is currently in a initialized state.
         */
        INIT,

        /**
         * Signifies that a tunnel is currently established but no traffic.
         */
        ESTABLISHED,

        /**
         * Signifies that a tunnel is currently serving the traffic.
         */
        ACTIVE,

        /**
         * Signifies that a tunnel is currently out of service.
         */
        FAILED,

        /**
         * Signifies that a tunnel is currently in maintenance state.
         */
        INACTIVE

    }

    TunnelId id();


    /**
     * Returns the tunnel source point (source Label object).
     *
     * @return source Label object
     */
    Label src();

    /**
     * Returns the tunnel destination point (destination Label object).
     *
     * @return destination Label object
     */
    Label dst();

    /**
     * Returns the tunnel type.
     *
     * @return tunnel type
     */
    Type type();

    /**
     * Returns the tunnel state.
     *
     * @return tunnel state
     */
    State state();

    /**
     * Indicates if the tunnel is to be considered durable.
     *
     * @return true if the tunnel is durable
     */
    boolean isDurable();


    /**
     * Indicates if the tunnel is to be considered Bidirectional.
     *
     * @return true if the tunnel is Bidirectional
     */
    boolean isBidirectional();

    /**
     * Return the tunnel bandwidth.
     *
     * @return tunnel bandwidth
     */
    BandwidthResource bandwidth();
}





