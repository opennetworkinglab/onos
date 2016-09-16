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

import java.util.Map;

/**
 * Represents IP topology for ISIS device and link details.
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
     * @param key                  system ID of the device as key
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
     * @param key                system ID of the device as key
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
     * Removes device information.
     *
     * @param key key used to remove from map
     */
    void removeDeviceInformationMap(String key);

    /**
     * Removes links from linkInformationMap.
     *
     * @param linkId ID
     */
    void removeLinks(String linkId);

    /**
     * Gets deviceInformation as map.
     *
     * @return deviceInformationMap to delete from core
     */
    Map<String, DeviceInformation> deviceInformationMapToDelete();

    /**
     * Sets deviceInformation as map.
     *
     * @param key                          key used to add in map
     * @param deviceInformationMapToDelete device information to delete from map
     */
    void setDeviceInformationMapToDelete(String key, DeviceInformation deviceInformationMapToDelete);

    /**
     * Removes Device Information from deviceInformationMapToDelete.
     *
     * @param key key to remove from map
     */
    void removeDeviceInformationMapFromDeleteMap(String key);

    /**
     * Gets deviceInformation as map for Point-To-Point.
     *
     * @return deviceInformationMap
     */
    Map<String, DeviceInformation> deviceInformationMapForPointToPoint();

    /**
     * Sets deviceInformation as map for Point-To-Point..
     *
     * @param key                  key to add to map
     * @param deviceInformationMap device information map
     */
    void setDeviceInformationMapForPointToPoint(String key, DeviceInformation deviceInformationMap);

    /**
     * Gets linkInformation as map for PointToPoint.
     *
     * @return linkInformationMap
     */
    Map<String, LinkInformation> linkInformationMapForPointToPoint();

    /**
     * Sets linkInformation as map for PointToPoint.
     *
     * @param key                key to add link information to map
     * @param linkInformationMap link information to add
     */
    void setLinkInformationMapForPointToPoint(String key, LinkInformation linkInformationMap);
}