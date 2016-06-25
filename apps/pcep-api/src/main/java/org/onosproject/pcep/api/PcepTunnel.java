/*
 * Copyright 2015-present Open Networking Laboratory
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

package org.onosproject.pcep.api;

import java.util.List;

/**
 * Abstraction of a generalized PCEP Tunnel entity (bandwidth pipe) for L2
 * networks or L1/L0 networks, representation of e.g., VLAN, L1 ODUk connection,
 * WDM OCH, etc..
 */
public interface PcepTunnel extends PcepOperator {

    /**
     * Describe the type of a tunnel.
     */
    enum Type {

        /**
         * Signifies that this is a L0 OCH tunnel.
         */
        OCH,

        /**
         * Signifies that this is a L1 OTN tunnel.
         */
        OTN,

        /**
         * Signifies that this is a L2 tunnel.
         */
        UNI,
    }

    /**
     * The ability of a tunnel.
     */
    enum Ability {
        /**
         * no protected tunnel,if the tunnel is broken ,then the user is out of
         * service.
         */
        NOPROTECTED,

        /**
         * tunnel with rerouter ability.if a tunnel is broken, the tunnel will
         * try to find another path to provider service.
         */
        SILVER,

        /**
         * tunnel with 1 + 1 rerouter ability.if a tunnel is broken, there'll be
         * another tunnel providing service at once.
         */
        DIAMOND
    }

    enum PathType {

        /**
         * Indicates path is the preferred path.
         */
        FIRST,

        /**
         * Indicates path is the alternate path.
         */
        SECOND
    }

    /**
     * Represents state of the path, work normally or broken down.
     */
    enum PathState {
        NORMAL, BROKEN
    }

    /**
     * Returns the type of a tunnel.
     *
     * @return tunnel type
     */
    Type type();

    /**
     * Returns the name of a tunnel.
     *
     * @return tunnel name
     */
    String name();

    /**
     * Returns the device id of destination endpoint of a tunnel.
     *
     * @return device id
     */
    PcepDpid srcDeviceID();

    /**
     * Returns the device id of source endpoint of a tunnel.
     *
     * @return device id
     */
    PcepDpid dstDeviceId();

    /**
     * Returns source port of a tunnel.
     *
     * @return port number
     */
    long srcPort();

    /**
     * Returns destination port of a tunnel.
     *
     * @return port number
     */
    long dstPort();

    /**
     * Returns the bandwidth of a tunnel.
     *
     * @return bandwidth
     */
    long bandWidth();

    /**
     * Returns the tunnel id.
     *
     * @return id of the PCEP tunnel
     */
    long id();

    /**
     * Returns the detail hop list of a tunnel.
     *
     * @return hop list
     */
    List<PcepHopNodeDescription> getHopList();

    /**
     * Returns the instance of a pcep tunnel,a instance is used to mark the times of
     * a tunnel created. instance and id identify a tunnel together.
     *
     * @return the instance of a tunnel.
     */
    int getInstance();

    /**
     * Returns the state of a path.
     *
     * @return normal or broken
     */
    PathState getPathState();

    /**
     * Returns the ability of a tunnel.
     *
     * @return ability of the tunenl
     */
    Ability getSla();

    /**
     * Returns the path type of a path if the tunnel's ability is diamond .
     *
     * @return the type of a path, the preferred or alternate.
     */
    PathType getPathType();

    /**
     * Get the under lay tunnel id of VLAN tunnel.
     *
     * @return the tunnel id of a OCH tunnel under lay of a VLAN tunnel.
     */
    long underlayTunnelId();

}
