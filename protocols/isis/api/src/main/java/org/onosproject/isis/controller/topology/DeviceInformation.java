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
 * Representation of an ISIS device information.
 */
public interface DeviceInformation {

    /**
     * Gets system id.
     *
     * @return system id
     */
    String systemId();

    /**
     * Sets system id.
     *
     * @param systemId system id
     */
    void setSystemId(String systemId);

    /**
     * Gets interface ids.
     *
     * @return interface ids
     */
    Ip4Address interfaceId();

    /**
     * Sets interface id.
     *
     * @param interfaceId interface id
     */
    void setInterfaceId(Ip4Address interfaceId);

    /**
     * Gets area id.
     *
     * @return area id
     */
    String areaId();

    /**
     * Sets area id.
     *
     * @param areaId area id
     */
    void setAreaId(String areaId);

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
     * Gets device is dis or not.
     *
     * @return true if device is dis else false
     */
    boolean isDis();

    /**
     * Sets device is dis or not.
     *
     * @param dis true if device is dr else false
     */
    void setDis(boolean dis);

    /**
     * Gets neighbor id.
     *
     * @return neighbor id
     */
    String neighborId();

    /**
     * Sets neighbor id.
     *
     * @param neighborId neighbor id
     */
    void setNeighborId(String neighborId);
}