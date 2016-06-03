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
package org.onosproject.isis.controller.topology;

import org.onlab.packet.Ip4Address;

/**
 * Abstraction of an ISIS Router.
 */
public interface IsisRouter {

    /**
     * Returns system ID for the router.
     *
     * @return system ID of the router
     */
    String systemId();

    /**
     * Returns IP address of the interface.
     *
     * @return IP address of the interface
     */
    Ip4Address interfaceId();

    /**
     * Gets IP address of the interface.
     *
     * @param interfaceId IP address of the interface
     */
    void setInterfaceId(Ip4Address interfaceId);

    /**
     * Sets system ID of the Router.
     *
     * @param systemId system ID of the router
     */
    void setSystemId(String systemId);

    /**
     * Gets neighbours ID.
     *
     * @return neighbour ID
     */
    Ip4Address neighborRouterId();

    /**
     * Sets the neighbour Id.
     *
     * @param neighbourId neighbour Id
     */
    void setNeighborRouterId(Ip4Address neighbourId);

    /**
     * Gets if the router id DIS or not.
     *
     * @return true if the router is DIS else false
     */
    boolean isDis();

    /**
     * Sets if the router id DIS or not.
     *
     * @param dis true if the router is DIS else false
     */
    void setDis(boolean dis);
}
