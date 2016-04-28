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

package org.onosproject.olt;

import org.onlab.packet.VlanId;
import org.onosproject.cordconfig.access.AccessDeviceData;
import org.onosproject.event.ListenerService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;

import java.util.Collection;
import java.util.Map;

/**
 * Service for interacting with an access device (OLT).
 */
public interface AccessDeviceService
        extends ListenerService<AccessDeviceEvent, AccessDeviceListener> {

    /**
     * Provisions connectivity for a subscriber on an access device.
     *
     * @param port subscriber's connection point
     * @param vlan VLAN ID to provision for subscriber
     */
    void provisionSubscriber(ConnectPoint port, VlanId vlan);

    /**
     * Removes provisioned connectivity for a subscriber from an access device.
     *
     * @param port subscriber's connection point
     */
    void removeSubscriber(ConnectPoint port);

    /**
     * Returns information about the provisioned subscribers.
     *
     * @return subscribers
     */
    Collection<Map.Entry<ConnectPoint, VlanId>> getSubscribers();

    /**
     * Returns the map of configured OLTs.
     *
     * @return a map
     */
    Map<DeviceId, AccessDeviceData> fetchOlts();

}
