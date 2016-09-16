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
 * Representation of an ISIS link information.
 */
public interface LinkInformation {

    /**
     * Gets link id.
     *
     * @return link id
     */
    String linkId();

    /**
     * Sets link id.
     *
     * @param linkId link id
     */
    void setLinkId(String linkId);

    /**
     * Gets whether link information is already created or not.
     *
     * @return true if link information is already created else false
     */
    boolean isAlreadyCreated();

    /**
     * Sets link information is already created or not.
     *
     * @param alreadyCreated true if link information is already created else false
     */
    void setAlreadyCreated(boolean alreadyCreated);


    /**
     * Returns link destination ID.
     *
     * @return link destination ID
     */
    String linkDestinationId();

    /**
     * Sets link destination id.
     *
     * @param linkDestinationId link destination id
     */
    void setLinkDestinationId(String linkDestinationId);

    /**
     * Gets link source id.
     *
     * @return link source id
     */
    String linkSourceId();

    /**
     * Sets link source id.
     *
     * @param linkSourceId link source id
     */
    void setLinkSourceId(String linkSourceId);

    /**
     * Gets interface ip address.
     *
     * @return interface ip address
     */
    Ip4Address interfaceIp();

    /**
     * Sets interface ip address.
     *
     * @param interfaceIp interface ip address
     */
    void setInterfaceIp(Ip4Address interfaceIp);

    /**
     * Gets neighbor ip address.
     *
     * @return neighbor ip address
     */
    Ip4Address neighborIp();

    /**
     * Sets neighbor ip address.
     *
     * @param neighborIp neighbor ip address
     */
    void setNeighborIp(Ip4Address neighborIp);
}