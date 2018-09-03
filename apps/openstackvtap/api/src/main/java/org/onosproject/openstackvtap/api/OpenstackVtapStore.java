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

import org.onosproject.net.DeviceId;
import org.onosproject.store.Store;

import java.util.Set;

/**
 * Manages inventory of openstack vtap datum; not intended for direct use.
 */
public interface OpenstackVtapStore
        extends Store<OpenstackVtapEvent, OpenstackVtapStoreDelegate> {

    /**
     * Creates a new openstack vtap network based on the specified description.
     *
     * @param key         vtap network identifier
     * @param description description of vtap network
     * @return created openstack vtap network object or null if error occurred
     */
    OpenstackVtapNetwork createVtapNetwork(Integer key, OpenstackVtapNetwork description);

    /**
     * Updates the openstack vtap network based on the specified description.
     *
     * @param key         vtap network identifier
     * @param description description of vtap network
     * @return updated openstack vtap network object or null if error occurred
     */
    OpenstackVtapNetwork updateVtapNetwork(Integer key, OpenstackVtapNetwork description);

    /**
     * Removes the specified openstack vtap network with the specified identifier.
     *
     * @param key vtap network identifier
     * @return removed openstack vtap network object or null if error occurred
     */
    OpenstackVtapNetwork removeVtapNetwork(Integer key);

    /**
     * Removes all openstack vtap networks.
     */
    void clearVtapNetworks();

    /**
     * Returns the number of openstack vtap networks.
     *
     * @return number of openstack vtap networks
     */
    int getVtapNetworkCount();

    /**
     * Returns the openstack vtap network with the specified identifier.
     *
     * @param key vtap network identifier
     * @return openstack vtap or null if one with the given identifier is not known
     */
    OpenstackVtapNetwork getVtapNetwork(Integer key);

    /**
     * Adds the specified device id to the specified vtap network entry.
     *
     * @param key      vtap network identifier
     * @param deviceId device identifier to be added
     * @return true on success, false otherwise
     */
    boolean addDeviceToVtapNetwork(Integer key, DeviceId deviceId);

    /**
     * Removes the specified device id from the specified vtap network entry.
     *
     * @param key      vtap network identifier
     * @param deviceId device identifier to be removed
     * @return true on success, false otherwise
     */
    boolean removeDeviceFromVtapNetwork(Integer key, DeviceId deviceId);

    /**
     * Returns a set of device identifiers from the specified vtap network entry.
     *
     * @param key vtap network identifier
     * @return set of device identifier
     */
    Set<DeviceId> getVtapNetworkDevices(Integer key);

    /**
     * Creates a new openstack vtap based on the specified description.
     *
     * @param description description of vtap
     * @return created openstack vtap object or null if error occurred
     */
    OpenstackVtap createVtap(OpenstackVtap description);

    /**
     * Updates the openstack vtap with specified description.
     *
     * @param description    description of vtap
     * @param replaceDevices replace device set if true, merge device set otherwise
     * @return updated openstack vtap object or null if error occurred
     */
    OpenstackVtap updateVtap(OpenstackVtap description, boolean replaceDevices);

    /**
     * Removes the specified openstack vtap with given vtap identifier.
     *
     * @param vtapId vtap identifier
     * @return removed openstack vtap object or null if error occurred
     */
    OpenstackVtap removeVtap(OpenstackVtapId vtapId);

    /**
     * Removes all openstack vtaps.
     */
    void clearVtaps();

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
     * Adds the specified device id to the specified vtap entry.
     *
     * @param vtapId   vtap identifier
     * @param type     type of vtap (any,rx,tx,all)
     * @param deviceId device identifier to be added
     * @return true on success, false otherwise
     */
    boolean addDeviceToVtap(OpenstackVtapId vtapId, OpenstackVtap.Type type, DeviceId deviceId);

    /**
     * Removes the specified device id from the vtap entry.
     *
     * @param vtapId   vtap identifier
     * @param type     type of vtap (any,rx,tx,all)
     * @param deviceId device identifier to be removed
     * @return true on success, false otherwise
     */
    boolean removeDeviceFromVtap(OpenstackVtapId vtapId, OpenstackVtap.Type type, DeviceId deviceId);

    /**
     * Returns a set of openstack vtaps which are associated with the given device.
     *
     * @param deviceId device identifier
     * @return set of openstack vtaps
     */
    Set<OpenstackVtap> getVtapsByDeviceId(DeviceId deviceId);
}
