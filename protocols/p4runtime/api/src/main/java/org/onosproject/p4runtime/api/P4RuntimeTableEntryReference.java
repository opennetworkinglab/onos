/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.p4runtime.api;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.onosproject.net.DeviceId;
import org.onosproject.net.pi.runtime.PiMatchKey;
import org.onosproject.net.pi.runtime.PiTableId;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Class containing the reference for a table entry in P4Runtime.
 */
public final class P4RuntimeTableEntryReference {

    private final DeviceId deviceId;
    private final PiTableId tableId;
    private final PiMatchKey matchKey;

    /**
     * Creates a new table entry reference.
     *
     * @param deviceId a device ID
     * @param tableId  a table name
     * @param matchKey a match key
     */
    public P4RuntimeTableEntryReference(DeviceId deviceId, PiTableId tableId, PiMatchKey matchKey) {
        this.deviceId = checkNotNull(deviceId);
        this.tableId = checkNotNull(tableId);
        this.matchKey = checkNotNull(matchKey);
    }

    /**
     * Returns the device ID of this table entry reference.
     *
     * @return a device ID
     */
    public DeviceId deviceId() {
        return deviceId;
    }

    /**
     * Returns the table id of this table entry reference.
     *
     * @return a table name
     */
    public PiTableId tableId() {
        return tableId;
    }

    /**
     * Returns the match key of this table entry reference.
     *
     * @return a match key
     */
    public PiMatchKey matchKey() {
        return matchKey;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(deviceId, tableId, matchKey);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final P4RuntimeTableEntryReference other = (P4RuntimeTableEntryReference) obj;
        return Objects.equal(this.deviceId, other.deviceId)
                && Objects.equal(this.tableId, other.tableId)
                && Objects.equal(this.matchKey, other.matchKey);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("deviceId", deviceId)
                .add("tableId", tableId)
                .add("matchKey", matchKey)
                .toString();
    }
}
