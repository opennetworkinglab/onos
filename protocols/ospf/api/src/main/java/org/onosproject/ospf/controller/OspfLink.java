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

import org.onlab.packet.IpAddress;

import java.util.List;

/**
 * Abstraction of an OSPF Link.
 */
public interface OspfLink {

    /**
     * Gets IP address of the Router.
     *
     * @return IP address of router
     */
    IpAddress remoteRouterId();

    /**
     * Gets the area id for this device.
     *
     * @return the area id
     */
    int areaIdOfInterface();

    /**
     * Gets IP address of the interface.
     *
     * @return IP address of the interface
     */
    IpAddress interfaceIp();

    /**
     * Gets list of the link TED.
     *
     * @return list of the link TED
     */
    List<OspfLinkTed> linkTedLists();

    /**
     * Sets IP address of the router.
     *
     * @param routerIp router's IP address
     */
    void setRouterIp(IpAddress routerIp);

    /**
     * Sets the area id for this device.
     *
     * @param areaIdOfInterface area id
     */
    void setAreaIdOfInterface(int areaIdOfInterface);

    /**
     * Sets IP address of the interface.
     *
     * @param interfaceIp IP address of the interface.
     */
    void setInterfaceIp(IpAddress interfaceIp);

    /**
     * Sets list of the link TED.
     *
     * @param linkTedLists list of the link TED
     */
    void setLinkTedLists(List<OspfLinkTed> linkTedLists);
}