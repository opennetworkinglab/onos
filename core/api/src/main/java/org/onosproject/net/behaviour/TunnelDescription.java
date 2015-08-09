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
package org.onosproject.net.behaviour;

import org.onosproject.net.Annotated;
import org.onosproject.net.Description;

import com.google.common.annotations.Beta;

/**
 * Describes a tunnel.
 */
@Beta
public interface TunnelDescription extends Description, Annotated {

    /**
     * Tunnel technology type.
     */
    enum Type {
        /**
         * Signifies that this is a MPLS tunnel.
         */
        MPLS,
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
         * Signifies that this is a L1 OTN tunnel.
         */
        ODUK,
        /**
         * Signifies that this is a L0 OCH tunnel.
         */
        OCH
    }

    /**
     * Returns the connection point source.
     *
     * @return tunnel source ConnectionPoint
     */
    TunnelEndPoint src();

    /**
     * Returns the connection point destination.
     *
     * @return tunnel destination
     */
    TunnelEndPoint dst();

    /**
     * Returns the tunnel type.
     *
     * @return tunnel type
     */
    Type type();

    /**
     * Return the name of a tunnel.
     *
     * @return Tunnel Name
     */
    TunnelName tunnelName();
}
