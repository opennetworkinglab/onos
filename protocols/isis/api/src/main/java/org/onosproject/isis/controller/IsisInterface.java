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

/**
 * Representation of an ISIS interface.
 */
public interface IsisInterface {

    /**
     * Sets interface index.
     *
     * @param interfaceIndex interface index
     */
    void setInterfaceIndex(int interfaceIndex);

    /**
     * Sets intermediate system name.
     *
     * @param intermediateSystemName intermediate system name
     */
    void setIntermediateSystemName(String intermediateSystemName);

    /**
     * Sets system ID.
     *
     * @param systemId system ID
     */
    void setSystemId(String systemId);

    /**
     * Sets LAN ID.
     *
     * @param lanId LAN ID
     */
    void setLanId(String lanId);

    /**
     * Sets ID length.
     *
     * @param idLength ID length
     */
    void setIdLength(int idLength);

    /**
     * Sets max area addresses.
     *
     * @param maxAreaAddresses max area addresses
     */
    void setMaxAreaAddresses(int maxAreaAddresses);

    /**
     * Sets reserved packet circuit type.
     *
     * @param reservedPacketCircuitType reserved packet circuit type
     */
    void setReservedPacketCircuitType(int reservedPacketCircuitType);

    /**
     * Sets point to point.
     *
     * @param p2p point to point
     */
    void setP2p(int p2p);

    /**
     * Sets area address.
     *
     * @param areaAddress area address
     */
    void setAreaAddress(String areaAddress);

    /**
     * Sets area length.
     *
     * @param areaLength area length
     */
    void setAreaLength(int areaLength);

    /**
     * Sets link state packet ID.
     *
     * @param lspId link state packet ID
     */
    void setLspId(String lspId);

    /**
     * Sets holding time.
     *
     * @param holdingTime holding time
     */
    void setHoldingTime(int holdingTime);

    /**
     * Sets priority.
     *
     * @param priority priority
     */
    void setPriority(int priority);

    /**
     * Sets hello interval.
     *
     * @param helloInterval hello interval
     */
    void setHelloInterval(int helloInterval);
}