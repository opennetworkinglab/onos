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
package org.onosproject.ospf.controller;

import org.onlab.packet.Ip4Address;

/**
 * Representation of an OSPF link information.
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
     * Gets link information is already created or not.
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
     * Gets is link source id is same as router id or not.
     *
     * @return true if link source id is not same as router id else false
     */
    boolean isLinkSrcIdNotRouterId();

    /**
     * Sets is link source id is same as router id or not.
     *
     * @param linkSrcIdNotRouterId true if link source id is not same as router id else false
     */
    void setLinkSrcIdNotRouterId(boolean linkSrcIdNotRouterId);

    /**
     * Gets link destination id.
     *
     * @return link destination id
     */
    Ip4Address linkDestinationId();

    /**
     * Sets link destination id.
     *
     * @param linkDestinationId link destination id
     */
    void setLinkDestinationId(Ip4Address linkDestinationId);

    /**
     * Gets link source id.
     *
     * @return link source id
     */
    Ip4Address linkSourceId();

    /**
     * Sets link source id.
     *
     * @param linkSourceId link source id
     */
    void setLinkSourceId(Ip4Address linkSourceId);

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
     * Gets link source ip address.
     *
     * @return link source ip address
     */
    Ip4Address linkSourceIpAddress();

    /**
     * Sets link source ip address.
     *
     * @param linkSourceIpAddress link source ip address
     */
    void setLinkSourceIpAddress(Ip4Address linkSourceIpAddress);

    /**
     * Gets link destination ip address.
     *
     * @return link destination ip address
     */
    Ip4Address linkDestinationIpAddress();

    /**
     * Sets link destination ip address.
     *
     * @param linkDestinationIpAddress link destination ip address
     */
    void setLinkDestinationIpAddress(Ip4Address linkDestinationIpAddress);
}