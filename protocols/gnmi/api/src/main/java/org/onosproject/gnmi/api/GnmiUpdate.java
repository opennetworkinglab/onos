/*
 * Copyright 2018-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.gnmi.api;

import com.google.common.base.MoreObjects;
import gnmi.Gnmi.Notification;
import org.onosproject.net.DeviceId;

/**
 * Event class for gNMI update.
 */
public class GnmiUpdate implements GnmiEventSubject {
    private DeviceId deviceId;
    private Notification update;
    private boolean syncResponse;

    /**
     * Default constructor.
     *
     * @param deviceId the device id for this event
     * @param update the update for this event
     * @param syncResponse indicate target has sent all values associated with
     *                     the subscription at least once.
     */
    public GnmiUpdate(DeviceId deviceId, Notification update, boolean syncResponse) {
        this.deviceId = deviceId;
        this.update = update;
        this.syncResponse = syncResponse;
    }

    /**
     * Gets the update data.
     *
     * @return the update data
     */
    public Notification update() {
        return update;
    }

    /**
     * indicate target has sent all values associated with the subscription at
     * least once.
     *
     * @return true if all value from target has sent
     */
    public boolean syncResponse() {
        return syncResponse;
    }

    @Override
    public DeviceId deviceId() {
        return deviceId;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("deviceId", deviceId)
                .add("syncResponse", syncResponse)
                .add("update", update)
                .toString();
    }
}
