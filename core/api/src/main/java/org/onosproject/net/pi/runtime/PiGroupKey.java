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

import com.google.common.base.Objects;
import org.onosproject.net.group.GroupKey;
import org.onosproject.net.pi.model.PiActionProfileId;
import org.onosproject.net.pi.model.PiTableId;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of GroupKey for the case of a protocol-independent pipeline.
 */
public final class PiGroupKey implements GroupKey {

    private final PiTableId tableId;
    private final PiActionProfileId piActionProfileId;
    private final int groupId;

    /**
     * Returns a new group key for the given table ID, action profile ID, and group ID.
     *
     * @param tableId           table ID
     * @param piActionProfileId action profile ID
     * @param groupId           group ID
     */
    public PiGroupKey(PiTableId tableId, PiActionProfileId piActionProfileId, int groupId) {
        this.tableId = checkNotNull(tableId);
        this.piActionProfileId = checkNotNull(piActionProfileId);
        this.groupId = groupId;
    }

    /**
     * Returns the table ID defined by this key.
     *
     * @return table ID
     */
    public PiTableId tableId() {
        return tableId;
    }

    /**
     * Returns the group ID defined by this key.
     *
     * @return group ID
     */
    public int groupId() {
        return groupId;
    }

    /**
     * Returns the action profile ID defined by this key.
     *
     * @return action profile ID
     */
    public PiActionProfileId actionProfileId() {
        return piActionProfileId;
    }

    @Override
    public byte[] key() {
        return toString().getBytes();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PiGroupKey)) {
            return false;
        }
        PiGroupKey that = (PiGroupKey) o;
        return groupId == that.groupId &&
                Objects.equal(tableId, that.tableId) &&
                Objects.equal(piActionProfileId, that.piActionProfileId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(tableId, piActionProfileId, groupId);
    }

    @Override
    public String toString() {
        return tableId.id() + "-" + piActionProfileId.id() + "-" + String.valueOf(groupId);
    }
}
