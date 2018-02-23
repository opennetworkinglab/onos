/*
 * Copyright 2016-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licedses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.segmentrouting.storekey;

import java.util.Objects;

import org.onosproject.net.DeviceId;
import org.onosproject.segmentrouting.grouphandler.DestinationSet;

/**
 * Key of Destination set next objective store.
 */
public class DestinationSetNextObjectiveStoreKey {
    private final DeviceId deviceId;
    private final DestinationSet ds;

    /**
     * Constructs the key of destination set next objective store.
     *
     * @param deviceId device ID
     * @param ds destination set
     */
    public DestinationSetNextObjectiveStoreKey(DeviceId deviceId,
                                            DestinationSet ds) {
        this.deviceId = deviceId;
        this.ds = ds;
    }

    /**
     * Returns the device ID in the key.
     *
     * @return device ID
     */
    public DeviceId deviceId() {
        return this.deviceId;
    }

    /**
     * Returns the destination set in the key.
     *
     * @return destination set
     */
    public DestinationSet destinationSet() {
        return this.ds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DestinationSetNextObjectiveStoreKey)) {
            return false;
        }
        DestinationSetNextObjectiveStoreKey that =
                (DestinationSetNextObjectiveStoreKey) o;
        return (Objects.equals(this.deviceId, that.deviceId) &&
                Objects.equals(this.ds, that.ds));
    }

    // The list of destination ids and label are used for comparison.
    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + Objects.hashCode(this.deviceId)
                + Objects.hashCode(this.ds);

        return result;
    }

    @Override
    public String toString() {
        return "Device: " + deviceId + " " + ds;
    }
}
