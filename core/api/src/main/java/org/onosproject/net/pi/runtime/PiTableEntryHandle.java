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

package org.onosproject.net.pi.runtime;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.onosproject.net.DeviceId;
import org.onosproject.net.pi.model.PiTableId;

import java.util.OptionalInt;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Global identifier of a PI table entry applied on a device, uniquely defined
 * by a device ID, table ID and match key.
 */
@Beta
public final class PiTableEntryHandle extends PiHandle {

    private static final int NO_PRIORITY = -1;

    private final PiTableId tableId;
    private final PiMatchKey matchKey;
    private final int priority;

    private PiTableEntryHandle(DeviceId deviceId, PiTableId tableId, PiMatchKey matchKey,
                               Integer priority) {
        super(deviceId);
        this.tableId = tableId;
        this.matchKey = matchKey;
        this.priority = priority;
    }

    /**
     * Creates a new handle for the given PI table entry and device ID.
     *
     * @param deviceId device ID
     * @param entry    PI table entry
     * @return PI table entry handle
     */
    public static PiTableEntryHandle of(DeviceId deviceId, PiTableEntry entry) {
        checkNotNull(entry);
        return new PiTableEntryHandle(
                deviceId, entry.table(), entry.matchKey(),
                entry.priority().orElse(NO_PRIORITY));
    }

    /**
     * Returns the table ID associated with this handle.
     *
     * @return table ID
     */
    public PiTableId tableId() {
        return tableId;
    }

    /**
     * Returns the match key associated with this handle.
     *
     * @return match key
     */
    public PiMatchKey matchKey() {
        return matchKey;
    }

    /**
     * Returns the optional priority associated with this handle.
     *
     * @return optional priority
     */
    public OptionalInt priority() {
        return priority == NO_PRIORITY
                ? OptionalInt.empty() : OptionalInt.of(priority);
    }

    @Override
    public PiEntityType entityType() {
        return PiEntityType.TABLE_ENTRY;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(deviceId(), tableId, matchKey, priority().orElse(NO_PRIORITY));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final PiTableEntryHandle other = (PiTableEntryHandle) obj;
        return Objects.equal(this.deviceId(), other.deviceId())
                && Objects.equal(this.tableId, other.tableId)
                && Objects.equal(this.matchKey, other.matchKey)
                && Objects.equal(this.priority(), other.priority());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("deviceId", deviceId())
                .add("tableId", tableId)
                .add("matchKey", matchKey)
                .add("priority", priority == NO_PRIORITY ? "N/A" : priority)
                .toString();
    }
}
