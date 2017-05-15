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

package org.onosproject.segmentrouting.storekey;

import java.util.Objects;

import org.onosproject.net.DeviceId;
import org.onosproject.segmentrouting.grouphandler.NeighborSet;

/**
 * Key of Neighborset next objective store.
 */
public class NeighborSetNextObjectiveStoreKey {
    private final DeviceId deviceId;
    private final NeighborSet ns;

    /**
     * Constructs the key of neighbor set next objective store.
     *
     * @param deviceId device ID
     * @param ns neighbor set
     */
    public NeighborSetNextObjectiveStoreKey(DeviceId deviceId,
                                            NeighborSet ns) {
        this.deviceId = deviceId;
        this.ns = ns;
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
     * Returns the neighbor set in the key.
     *
     * @return neighbor set
     */
    public NeighborSet neighborSet() {
        return this.ns;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NeighborSetNextObjectiveStoreKey)) {
            return false;
        }
        NeighborSetNextObjectiveStoreKey that =
                (NeighborSetNextObjectiveStoreKey) o;
        return (Objects.equals(this.deviceId, that.deviceId) &&
                Objects.equals(this.ns, that.ns));
    }

    // The list of neighbor ids and label are used for comparison.
    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + Objects.hashCode(this.deviceId)
                + Objects.hashCode(this.ns);

        return result;
    }

    @Override
    public String toString() {
        return "Device: " + deviceId + " " + ns;
    }
}
