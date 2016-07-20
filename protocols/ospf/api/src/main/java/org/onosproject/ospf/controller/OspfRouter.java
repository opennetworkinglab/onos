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
 * Abstraction of an OSPF Router.
 */
public interface OspfRouter {

    /**
     * Gets IP address of the router.
     *
     * @return IP address of the router
     */
    Ip4Address routerIp();

    /**
     * Gets the area id for this device.
     *
     * @return the area id for this device
     */
    Ip4Address areaIdOfInterface();

    /**
     * Gets IP address of the interface.
     *
     * @return IP address of the interface
     */
    Ip4Address interfaceId();

    /**
     * Gets list of device TED.
     *
     * @return list of device TED.
     */
    OspfDeviceTed deviceTed();

    /**
     * Sets IP address of the Router.
     *
     * @param routerIp IP address of the router
     */
    void setRouterIp(Ip4Address routerIp);

    /**
     * Sets area id in which this device belongs to.
     *
     * @param areaIdOfInterface area id in which this device belongs to
     */
    void setAreaIdOfInterface(Ip4Address areaIdOfInterface);

    /**
     * Sets IP address of the interface.
     *
     * @param interfaceId IP address of the interface
     */
    void setInterfaceId(Ip4Address interfaceId);

    /**
     * Sets the device TED information.
     *
     * @param deviceTed device TED instance
     */
    void setDeviceTed(OspfDeviceTed deviceTed);

    /**
     * Gets if router is opaque enabled.
     *
     * @return true if router is opaque enabled else false.
     */
    boolean isOpaque();

    /**
     * Sets true if device is opaque enable if not sets false.
     *
     * @param opaque true if device is opaque enable if not sets false
     */
    void setOpaque(boolean opaque);

    /**
     * Gets IP address of the advertising router.
     *
     * @return IP address of the advertising router
     */
    Ip4Address neighborRouterId();

    /**
     * Sets IP address of the advertising router.
     *
     * @param advertisingRouterId IP address of the advertising router
     */
    void setNeighborRouterId(Ip4Address advertisingRouterId);


    /**
     * Gets if the router id DR or not.
     *
     * @return true if the router is DR else false
     */
    boolean isDr();

    /**
     * Sets if the router id DR or not.
     *
     * @param dr true if the router is DR else false
     */
    void setDr(boolean dr);
}