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

import org.onlab.packet.IpAddress;

import java.util.List;

/**
 * Abstraction of an ISIS Link.
 */
public interface IsisLink {

    /**
     * Returns IP address of the Router.
     *
     * @return IP address of router
     */
    IpAddress remoteRouterId();

    /**
     * Returns the area ID for this device.
     *
     * @return the area ID
     */
    int areaIdOfInterface();

    /**
     * Returns IP address of the interface.
     *
     * @return IP address of the interface
     */
    IpAddress interfaceIp();

    /**
     * Returns the list of link TED details.
     *
     * @return linkTed list of link TED
     */
    List<IsisLinkTed> linkTed();

    /**
     * Sets IP address of the router.
     *
     * @param routerIp router's IP address
     */
    void setRouterIp(IpAddress routerIp);

    /**
     * Sets the area ID for this device.
     *
     * @param areaIdOfInterface area ID
     */
    void setAreaIdOfInterface(int areaIdOfInterface);

    /**
     * Sets IP address of the interface.
     *
     * @param interfaceIp IP address of the interface
     */
    void setInterfaceIp(IpAddress interfaceIp);

    /**
     * Sets the list of link TED.
     *
     * @param linkTed list of link TED
     */
    void setLinkTed(List<IsisLinkTed> linkTed);
}
