/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.isis.controller;

import org.onlab.packet.MacAddress;

/**
 * Representation of an ISIS neighbor.
 */
public interface IsisNeighbor {

    /**
     * Returns the MAC address of neighbor.
     *
     * @return MAC address of neighbor
     */
    MacAddress neighborMacAddress();

    /**
     * Returns the neighbor interface state.
     *
     * @return neighbor interface state
     */
    IsisInterfaceState interfaceState();

    /**
     * Sets the neighbor interface state.
     *
     * @param interfaceState the neighbor interface state
     */
    void setNeighborState(IsisInterfaceState interfaceState);

    /**
     * Sets the LAN ID.
     *
     * @param l1LanId LAN ID
     */
    void setL1LanId(String l1LanId);

    /**
     * Sets the LAN ID.
     *
     * @param l2LanId LAN ID
     */
    void setL2LanId(String l2LanId);

    /**
     * Returns neighbor system ID.
     *
     * @return neighbor system ID
     */
    String neighborSystemId();

    /**
     * Returns neighbor circuit ID.
     *
     * @return neighbor circuit ID
     */
    byte localCircuitId();

    /**
     * Returns neighbor extended circuit ID.
     *
     * @return neighbor extended circuit ID
     */
    int localExtendedCircuitId();

    /**
     * Sets neighbor extended circuit ID.
     *
     * @param localExtendedCircuitId neighbor extended circuit ID
     */
    void setLocalExtendedCircuitId(int localExtendedCircuitId);

    /**
     * Returns Holding time of neighbor.
     *
     * @return Holding time of neighbor
     */
    int holdingTime();

    /**
     * Sets Holding time of neighbor.
     *
     * @param holdingTime Holding time of neighbor
     */
    void setHoldingTime(int holdingTime);

    /**
     * Starts the inactivity timer for this neighbor.
     */
    void startInactivityTimeCheck();

    /**
     * Stops the inactivity timer.
     */
    void stopInactivityTimeCheck();

    /**
     * Stops the holding time check timer.
     */
    void stopHoldingTimeCheck();

    /**
     * Returns router type.
     *
     * @return router type
     */
    IsisRouterType routerType();
}