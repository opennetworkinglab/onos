/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.segmentrouting.xconnect.api;

import com.google.common.collect.ImmutableMap;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import java.util.List;
import java.util.Set;

/**
 * VLAN cross connect between exactly two ports.
 */
@Service
public interface XconnectService {

    /**
     * VLAN cross-connect ACL priority.
     */
    int XCONNECT_ACL_PRIORITY = 60000;

    /**
     * VLAN cross-connect Bridging priority.
     */
    int XCONNECT_PRIORITY = 1000;

    /**
     * Creates or updates Xconnect.
     *
     * @param deviceId device ID
     * @param vlanId VLAN ID
     * @param ports set of ports
     */
    void addOrUpdateXconnect(DeviceId deviceId, VlanId vlanId, Set<PortNumber> ports);

    /**
     * Deletes Xconnect.
     *
     * @param deviceId device ID
     * @param vlanId VLAN ID
     */
    void removeXonnect(DeviceId deviceId, VlanId vlanId);

    /**
     * Gets Xconnects.
     *
     * @return set of Xconnect descriptions
     */
    Set<XconnectDesc> getXconnects();

    /**
     * Check if there is Xconnect configured on given connect point.
     *
     * @param cp connect point
     * @return true if there is Xconnect configured on the connect point
     */
    boolean hasXconnect(ConnectPoint cp);

    /**
     * Gives xconnect VLAN of given port of a device.
     *
     * @param deviceId Device ID
     * @param port Port number
     * @return true if given VLAN vlanId is XConnect VLAN on device deviceId.
     */
    List<VlanId> getXconnectVlans(DeviceId deviceId, PortNumber port);

    /**
     * Checks given VLAN is XConnect VLAN in given device.
     *
     * @param deviceId Device ID
     * @param vlanId VLAN ID
     * @return true if given VLAN vlanId is XConnect VLAN on device deviceId.
     */
    boolean isXconnectVlan(DeviceId deviceId, VlanId vlanId);

    /**
     * Returns the Xconnect next objective store.
     *
     * @return current contents of the xconnectNextObjStore
     */
    ImmutableMap<XconnectKey, Integer> getNext();

    /**
     * Removes given next ID from Xconnect next objective store.
     *
     * @param nextId next ID
     */
    void removeNextId(int nextId);

    /**
     * Returns Xconnect next objective ID associated with group device + vlan.
     *
     * @param deviceId - Device ID
     * @param vlanId - VLAN ID
     * @return Current associated group ID
     */
    int getNextId(DeviceId deviceId, VlanId vlanId);
}
