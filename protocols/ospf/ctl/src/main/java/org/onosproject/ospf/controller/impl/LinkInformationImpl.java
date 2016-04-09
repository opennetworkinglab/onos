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
package org.onosproject.ospf.controller.impl;

import org.onlab.packet.Ip4Address;
import org.onosproject.ospf.controller.LinkInformation;

/**
 * Representation of an OSPF link information..
 */
public class LinkInformationImpl implements LinkInformation {

    String linkId;
    Ip4Address linkSourceId;
    Ip4Address linkDestinationId;
    Ip4Address interfaceIp;
    boolean linkSrcIdNotRouterId;
    boolean alreadyCreated;
    Ip4Address linkSourceIpAddress;
    Ip4Address linkDestinationIpAddress;

    /**
     * Gets link id.
     *
     * @return link id
     */
    public String linkId() {
        return linkId;
    }

    /**
     * Sets link id.
     *
     * @param linkId link id
     */
    public void setLinkId(String linkId) {
        this.linkId = linkId;
    }

    /**
     * Gets is already created or not.
     *
     * @return true if already created else false
     */
    public boolean isAlreadyCreated() {
        return alreadyCreated;
    }

    /**
     * Sets is already created or not.
     *
     * @param alreadyCreated true or false
     */
    public void setAlreadyCreated(boolean alreadyCreated) {
        this.alreadyCreated = alreadyCreated;
    }

    /**
     * Gets is link source id is not router id.
     *
     * @return true if link source id is router id else false
     */
    public boolean isLinkSrcIdNotRouterId() {
        return linkSrcIdNotRouterId;
    }

    /**
     * Sets is link source id is not router id.
     *
     * @param linkSrcIdNotRouterId true or false
     */
    public void setLinkSrcIdNotRouterId(boolean linkSrcIdNotRouterId) {
        this.linkSrcIdNotRouterId = linkSrcIdNotRouterId;
    }

    /**
     * Gets link destination id.
     *
     * @return link destination id
     */
    public Ip4Address linkDestinationId() {
        return linkDestinationId;
    }

    /**
     * Sets link destination id.
     *
     * @param linkDestinationId link destination id
     */
    public void setLinkDestinationId(Ip4Address linkDestinationId) {
        this.linkDestinationId = linkDestinationId;
    }

    /**
     * Gets link source id.
     *
     * @return link source id
     */
    public Ip4Address linkSourceId() {
        return linkSourceId;
    }

    /**
     * Sets link source id.
     *
     * @param linkSourceId link source id
     */
    public void setLinkSourceId(Ip4Address linkSourceId) {
        this.linkSourceId = linkSourceId;
    }

    /**
     * Gets interface IP address.
     *
     * @return interface IP address
     */
    public Ip4Address interfaceIp() {
        return interfaceIp;
    }

    /**
     * Sets interface IP address.
     *
     * @param interfaceIp interface IP address
     */
    public void setInterfaceIp(Ip4Address interfaceIp) {
        this.interfaceIp = interfaceIp;
    }

    /**
     * Gets link source IP address.
     *
     * @return link source IP address
     */
    public Ip4Address linkSourceIpAddress() {
        return linkSourceIpAddress;
    }

    /**
     * Sets link source IP address.
     *
     * @param linkSourceIpAddress link source IP address
     */
    public void setLinkSourceIpAddress(Ip4Address linkSourceIpAddress) {
        this.linkSourceIpAddress = linkSourceIpAddress;
    }

    /**
     * Gets link destination IP address.
     *
     * @return link destination IP address
     */
    public Ip4Address linkDestinationIpAddress() {
        return linkDestinationIpAddress;
    }

    /**
     * Sets link destination IP address.
     *
     * @param linkDestinationIpAddress link destination IP address
     */
    public void setLinkDestinationIpAddress(Ip4Address linkDestinationIpAddress) {
        this.linkDestinationIpAddress = linkDestinationIpAddress;
    }
}