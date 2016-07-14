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
 * Abstraction of an ISIS Link.
 */
public interface IsisLink {

    /**
     * Returns the remote system ID.
     *
     * @return remote system ID
     */
    String remoteSystemId();

    /**
     * Returns the local system ID.
     *
     * @return local system ID
     */
    String localSystemId();

    /**
     * Returns IP address of the interface.
     *
     * @return IP address of the interface
     */
    Ip4Address interfaceIp();

    /**
     * Returns IP address of the neighbor.
     *
     * @return IP address of the neighbor
     */
    Ip4Address neighborIp();

    /**
     * Returns the link TED details.
     *
     * @return linkTed link TED
     */
    IsisLinkTed linkTed();

    /**
     * Sets remote system ID.
     *
     * @param remoteSystemId remote system ID
     */
    void setRemoteSystemId(String remoteSystemId);

    /**
     * Sets local system ID.
     *
     * @param localSystemId remote system ID
     */
    void setLocalSystemId(String localSystemId);

    /**
     * Sets IP address of the interface.
     *
     * @param interfaceIp IP address of the interface
     */
    void setInterfaceIp(Ip4Address interfaceIp);

    /**
     * Sets IP address of the neighbor.
     *
     * @param neighborIp IP address of the neighbor
     */
    void setNeighborIp(Ip4Address neighborIp);

    /**
     * Sets the link TED information.
     *
     * @param linkTed link TED
     */
    void setLinkTed(IsisLinkTed linkTed);
}
