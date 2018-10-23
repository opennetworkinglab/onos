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
package org.onosproject.openstackvtap.api;

import org.onlab.packet.IpAddress;
import org.onosproject.event.ListenerService;
import org.onosproject.net.DeviceId;

import java.util.Set;

/**
 * Service for interacting with the inventory of openstack vtap.
 */
public interface OpenstackVtapService
        extends ListenerService<OpenstackVtapEvent, OpenstackVtapListener> {

    /**
     * Creates a new openstack vtap network based on the specified description.
     *
     * @param mode      mode of vtap network
     * @param networkId network id of the vtap tunneling network
     * @param serverIp  server IP address used for tunneling
     * @return created openstack vtap network object or null if error occurred
     */
    OpenstackVtapNetwork createVtapNetwork(OpenstackVtapNetwork.Mode mode, Integer networkId, IpAddress serverIp);

    /**
     * Updates the openstack vtap network based on the specified description.
     *
     * @param description description of vtap network
     * @return updated openstack vtap network object or null if error occurred
     */
    OpenstackVtapNetwork updateVtapNetwork(OpenstackVtapNetwork description);

    /**
     * Removes the specified openstack vtap network.
     *
     * @return removed openstack vtap network object or null if error occurred
     */
    OpenstackVtapNetwork removeVtapNetwork();

    /**
     * Returns the openstack vtap network.
     *
     * @return openstack vtap network or null if not exists
     */
    OpenstackVtapNetwork getVtapNetwork();

    /**
     * Returns a set of devices which are associated with the openstack vtap network.
     *
     * @return set of devices
     */
    Set<DeviceId> getVtapNetworkDevices();

    /**
     * Creates a new openstack vtap based on the specified type and criterion.
     *
     * @param type          type of vtap (all,rx,tx)
     * @param vtapCriterion criterion of vtap
     * @return created openstack vtap object or null if error occurred
     */
    OpenstackVtap createVtap(OpenstackVtap.Type type, OpenstackVtapCriterion vtapCriterion);

    /**
     * Updates the openstack vtap with specified description.
     *
     * @param description description of vtap
     * @return updated openstack vtap object or null if error occurred
     */
    OpenstackVtap updateVtap(OpenstackVtap description);

    /**
     * Removes the specified openstack vtap with given vtap identifier.
     *
     * @param vtapId vtap identifier
     * @return removed openstack vtap object or null if error occurred
     */
    OpenstackVtap removeVtap(OpenstackVtapId vtapId);

    /**
     * Returns the number of openstack vtaps for the given type.
     *
     * @param type type of vtap (any,rx,tx,all)
     * @return number of openstack vtaps
     */
    int getVtapCount(OpenstackVtap.Type type);

    /**
     * Returns a set of openstack vtaps for the given type.
     *
     * @param type type of vtap (any,rx,tx,all)
     * @return set of openstack vtaps
     */
    Set<OpenstackVtap> getVtaps(OpenstackVtap.Type type);

    /**
     * Returns the openstack vtap with the specified identifier.
     *
     * @param vtapId vtap identifier
     * @return openstack vtap or null if one with the given identifier is not known
     */
    OpenstackVtap getVtap(OpenstackVtapId vtapId);

    /**
     * Returns a set of openstack vtaps which are associated with the given device.
     *
     * @param deviceId device identifier
     * @return set of openstack vtaps
     */
    Set<OpenstackVtap> getVtapsByDeviceId(DeviceId deviceId);
}
