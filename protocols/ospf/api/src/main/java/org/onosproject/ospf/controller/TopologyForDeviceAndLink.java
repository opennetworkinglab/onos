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
import java.util.Map;

/**
 * Represents IP topology for OSPF device and link details.
 */
public interface TopologyForDeviceAndLink {

    /**
     * Gets the device information.
     *
     * @return device information
     */
    Map<String, DeviceInformation> deviceInformationMap();

    /**
     * Sets the device information.
     *
     * @param key                  key used to store in map
     * @param deviceInformationMap device information instance
     */
    void setDeviceInformationMap(String key, DeviceInformation deviceInformationMap);

    /**
     * Gets the link information.
     *
     * @return link information
     */
    Map<String, LinkInformation> linkInformationMap();

    /**
     * Sets link information.
     *
     * @param key                key used to store in map
     * @param linkInformationMap link information instance
     */
    void setLinkInformationMap(String key, LinkInformation linkInformationMap);

    /**
     * Removes link information.
     *
     * @param key key used to remove from map
     */
    void removeLinkInformationMap(String key);

    /**
     * Adds device information.
     *
     * @param ospfLsa       LSA instance
     * @param ospfInterface interface instance
     * @param ospfArea      area instance
     */
    void addLocalDevice(OspfLsa ospfLsa, OspfInterface ospfInterface, OspfArea ospfArea);

    /**
     * Removes device information.
     *
     * @param key key used to remove from map
     */
    void removeDeviceInformationMap(String key);

    /**
     * Removes links from linkInformationMap.
     *
     * @param routerId router's IP address
     */
    void removeLinks(Ip4Address routerId);

    /**
     * Gets OSPF link TED details.
     *
     * @param key key used to retrieve from map
     * @return links TED information
     */
    OspfLinkTed getOspfLinkTedHashMap(String key);

    /**
     * Gets all the router information to be deleted.
     *
     * @param ospfLsa  LSA instance
     * @param ospfArea area instance
     * @return list of router information which needs to delete from device list
     */
    List<String> getDeleteRouterInformation(OspfLsa ospfLsa, OspfArea ospfArea);

    /**
     * Updates the device and link information.
     *
     * @param ospfLsa  LSA instance
     * @param ospfArea area instance
     */
    void updateLinkInformation(OspfLsa ospfLsa, OspfArea ospfArea);
}