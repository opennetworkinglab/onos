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
 * Manages inventory of OpenstackVtap states; not intended for direct use.
 */
public interface OpenstackVtapStore
        extends Store<OpenstackVtapEvent, OpenstackVtapStoreDelegate> {

    /**
     * Creates a new vTap or updates the existing one based on the specified
     * description.
     *
     * @param vTapId             vTap identifier
     * @param description        vTap description data
     * @param replaceFlag        replace device set if true, merge device set otherwise
     * @return created or updated vTap object or null if error occurred
     */
    OpenstackVtap createOrUpdateVtap(OpenstackVtapId vTapId, OpenstackVtap description, boolean replaceFlag);

    /**
     * Removes the specified vTap from the inventory by the given vTap identifier.
     *
     * @param vTapId             vTap identifier
     * @return removed vTap object or null if error occurred
     */
    OpenstackVtap removeVtapById(OpenstackVtapId vTapId);

    /**
     * Adds the specified device id from the vTap entry.
     *
     * @param vTapId             vTap identification
     * @param type               vTap type
     * @param deviceId           device identifier to be added
     * @return true on success, false otherwise
     */
    boolean addDeviceToVtap(OpenstackVtapId vTapId, OpenstackVtap.Type type, DeviceId deviceId);

    /**
     * Removes the specified device id from the vTap entry.
     *
     * @param vTapId             vTap identification
     * @param type               vTap type
     * @param deviceId           device identifier to be removed
     * @return true on success, false otherwise
     */
    boolean removeDeviceFromVtap(OpenstackVtapId vTapId, OpenstackVtap.Type type, DeviceId deviceId);

    /**
     * Adds the specified device id from the vTap entry.
     *
     * @param vTapId             vTap identification
     * @param txDeviceIds        TX device identifiers to be updated
     * @param rxDeviceIds        RX device identifiers to be updated
     * @param replaceFlag        replace device set if true, merge device set otherwise
     * @return true on success, false otherwise
     */
    boolean updateDeviceForVtap(OpenstackVtapId vTapId, Set<DeviceId> txDeviceIds,
                                Set<DeviceId> rxDeviceIds, boolean replaceFlag);

    /**
     * Returns the number of vTaps in the store.
     *
     * @param type               vTap type
     * @return vTap count
     */
    int getVtapCount(OpenstackVtap.Type type);

    /**
     * Returns a collection of selected vTaps in the store.
     *
     * @param type               vTap type
     * @return iterable collection of selected vTaps
     */
    Set<OpenstackVtap> getVtaps(OpenstackVtap.Type type);

    /**
     * Returns the vTap with the specified identifier.
     *
     * @param vTapId             vTap identifier
     * @return vtap or null if not found
     */
    OpenstackVtap getVtap(OpenstackVtapId vTapId);

    /**
     * Returns the set of vTaps whose included on device.
     *
     * @param type               vTap type
     * @param deviceId           device identifier
     * @return set of vTaps
     */
    Set<OpenstackVtap> getVtapsByDeviceId(OpenstackVtap.Type type, DeviceId deviceId);
}
