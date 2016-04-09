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
import org.onosproject.ospf.controller.DeviceInformation;

import java.util.ArrayList;
import java.util.List;

/**
 * Representation of an OSPF device information.
 */
public class DeviceInformationImpl implements DeviceInformation {

    Ip4Address deviceId;
    Ip4Address routerId;
    List<Ip4Address> interfaceId = new ArrayList<>();
    Ip4Address areaId;
    boolean alreadyCreated;
    boolean isDr;

    Ip4Address neighborId;

    /**
     * Gets router id.
     *
     * @return router id
     */
    public Ip4Address routerId() {
        return routerId;
    }

    /**
     * Sets router id.
     *
     * @param routerId router id
     */
    public void setRouterId(Ip4Address routerId) {
        this.routerId = routerId;
    }

    /**
     * Gets device id.
     *
     * @return device id
     */
    public Ip4Address deviceId() {
        return deviceId;
    }

    /**
     * Sets device id.
     *
     * @param deviceId device id
     */
    public void setDeviceId(Ip4Address deviceId) {
        this.deviceId = deviceId;
    }

    /**
     * Gets interface id list.
     *
     * @return interface id list
     */
    public List<Ip4Address> interfaceId() {
        return this.interfaceId;
    }

    /**
     * Adds interface id to list.
     *
     * @param interfaceId interface id
     */
    public void addInterfaceId(Ip4Address interfaceId) {
        this.interfaceId.add(interfaceId);
    }

    /**
     * Gets area id.
     *
     * @return area id
     */
    public Ip4Address areaId() {
        return areaId;
    }

    /**
     * Sets area id.
     *
     * @param areaId area id
     */
    public void setAreaId(Ip4Address areaId) {
        this.areaId = areaId;
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
     * Gets is DR or not.
     *
     * @return true if DR else false
     */
    public boolean isDr() {
        return isDr;
    }

    /**
     * Stes DR or not.
     *
     * @param dr true or false
     */
    public void setDr(boolean dr) {
        this.isDr = dr;
    }

    /**
     * Gets neighbor id.
     *
     * @return neighbor id
     */
    public Ip4Address neighborId() {
        return neighborId;
    }

    /**
     * Sets neighbor id.
     *
     * @param neighborId neighbor id
     */
    public void setNeighborId(Ip4Address neighborId) {
        this.neighborId = neighborId;
    }
}
