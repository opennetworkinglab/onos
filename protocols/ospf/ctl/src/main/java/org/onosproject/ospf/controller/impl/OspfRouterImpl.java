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
import org.onosproject.ospf.controller.OspfDeviceTed;
import org.onosproject.ospf.controller.OspfRouter;

/**
 * Representation of an OSPF Router.
 */
public class OspfRouterImpl implements OspfRouter {

    private Ip4Address routerIp;
    private Ip4Address areaIdOfInterface;
    private Ip4Address neighborRouterId;
    private Ip4Address interfaceId;
    private OspfDeviceTed deviceTed;
    private boolean isOpaque;
    private boolean isDr;

    /**
     * Gets IP address of the Router.
     *
     * @return IP address router
     */
    public Ip4Address routerIp() {
        return routerIp;
    }

    /**
     * Sets IP address of the Router.
     *
     * @param routerIp IP address of the router
     */
    public void setRouterIp(Ip4Address routerIp) {
        this.routerIp = routerIp;
    }

    /**
     * Gets the area id of this device.
     *
     * @return the area id od this device
     */
    public Ip4Address areaIdOfInterface() {
        return areaIdOfInterface;
    }

    /**
     * Sets the area id for this device.
     *
     * @param areaIdOfInterface area identifier for the device
     */
    public void setAreaIdOfInterface(Ip4Address areaIdOfInterface) {
        this.areaIdOfInterface = areaIdOfInterface;
    }

    /**
     * Gets IP address of the interface.
     *
     * @return IP address of the interface
     */
    public Ip4Address interfaceId() {
        return interfaceId;
    }

    /**
     * Gets IP address of the interface.
     *
     * @param interfaceId IP address of the interface
     */
    public void setInterfaceId(Ip4Address interfaceId) {
        this.interfaceId = interfaceId;
    }

    /**
     * Gets List of the device ted.
     *
     * @return List of the device ted.
     */
    public OspfDeviceTed deviceTed() {
        return deviceTed;
    }

    /**
     * Sets List of the device TED.
     *
     * @param deviceTed of the device TED.
     */
    public void setDeviceTed(OspfDeviceTed deviceTed) {
        this.deviceTed = deviceTed;
    }

    /**
     * Gets boolean value.
     *
     * @return boolean value.
     */
    public boolean isOpaque() {
        return isOpaque;
    }

    /**
     * Sets boolean value.
     *
     * @param opaque true if opaque else false
     */
    public void setOpaque(boolean opaque) {
        isOpaque = opaque;
    }

    /**
     * Gets neighbor's Router id.
     *
     * @return neighbor's Router id
     */
    public Ip4Address neighborRouterId() {
        return neighborRouterId;
    }

    /**
     * Sets neighbor's Router id.
     *
     * @param advertisingRouterId neighbor's Router id
     */
    public void setNeighborRouterId(Ip4Address advertisingRouterId) {
        this.neighborRouterId = advertisingRouterId;
    }

    /**
     * Gets if DR or not.
     *
     * @return true if DR else false
     */
    public boolean isDr() {
        return isDr;
    }

    /**
     * Sets dr or not.
     *
     * @param dr true if DR else false
     */
    public void setDr(boolean dr) {
        isDr = dr;
    }
}
