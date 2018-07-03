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

import org.onosproject.event.ListenerService;
import org.onosproject.net.DeviceId;

import java.util.Set;

/**
 * Service for interacting with the inventory of vTap.
 */
public interface OpenstackVtapService
        extends ListenerService<OpenstackVtapEvent, OpenstackVtapListener> {

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
     * @return vTap or null if not exist
     */
    OpenstackVtap getVtap(OpenstackVtapId vTapId);

    /**
     * Returns a collection of vTaps which are associated with the given device.
     *
     * @param type               vTap type
     * @param deviceId           device identifier
     * @return a set of vTaps
     */
    Set<OpenstackVtap> getVtapsByDeviceId(OpenstackVtap.Type type, DeviceId deviceId);
}
