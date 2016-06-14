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

import java.util.List;

/**
 * Representation of an OSPF device information.
 */
public interface DeviceInformation {

    /**
     * Gets router id.
     *
     * @return router id
     */
    Ip4Address routerId();

    /**
     * Sets router id.
     *
     * @param routId router id
     */
    void setRouterId(Ip4Address routId);

    /**
     * Gets device id.
     *
     * @return device id
     */
    Ip4Address deviceId();

    /**
     * Sets device id.
     *
     * @param deviceId device id
     */
    void setDeviceId(Ip4Address deviceId);

    /**
     * Gets list of interface ids.
     *
     * @return list of interface ids
     */
    List<Ip4Address> interfaceId();

    /**
     * Adds interface id to list.
     *
     * @param interfaceId interface id
     */
    void addInterfaceId(Ip4Address interfaceId);

    /**
     * Gets area id.
     *
     * @return area id
     */
    Ip4Address areaId();

    /**
     * Sets area id.
     *
     * @param areaId area id
     */
    void setAreaId(Ip4Address areaId);

    /**
     * Gets device information is already created or not.
     *
     * @return true if device information is already created else false
     */
    boolean isAlreadyCreated();

    /**
     * Sets device information is already created or not.
     *
     * @param alreadyCreated true if device information is already created else false
     */
    void setAlreadyCreated(boolean alreadyCreated);

    /**
     * Gets device is dr or not.
     *
     * @return true if device is dr else false
     */
    boolean isDr();

    /**
     * Sets device is dr or not.
     *
     * @param dr true if device is dr else false
     */
    void setDr(boolean dr);

    /**
     * Gets neighbor id.
     *
     * @return neighbor id
     */
    Ip4Address neighborId();

    /**
     * Sets neighbor id.
     *
     * @param neighborId neighbor id
     */
    void setNeighborId(Ip4Address neighborId);
}