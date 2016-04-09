/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.ovsdb.rfc.message;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import org.onosproject.ovsdb.rfc.notation.Row;
import org.onosproject.ovsdb.rfc.notation.Uuid;

/**
 * A TableUpdate is an object that maps from the row's UUID to a RowUpdate object.
 * A RowUpdate is an object with the following members: "old": row, "new": row.
 * Refer to RFC 7047 Section 4.1.6.
 */
public final class RowUpdate {
    private final Uuid uuid;
    private final Row oldRow;
    private final Row newRow;

    /**
     * Constructs a RowUpdate object.
     * @param uuid UUID
     * @param oldRow present for "delete" and "modify" updates
     * @param newRow present for "initial", "insert", and "modify" updates
     */
    public RowUpdate(Uuid uuid, Row oldRow, Row newRow) {
        checkNotNull(uuid, "uuid cannot be null");
        this.uuid = uuid;
        this.oldRow = oldRow;
        this.newRow = newRow;
    }

    /**
     * Return uuid.
     * @return uuid
     */
    public Uuid uuid() {
        return this.uuid;
    }

    /**
     * Return oldRow.
     * @return oldRow
     */
    public Row oldRow() {
        return oldRow;
    }

    /**
     * Return newRow.
     * @return newRow
     */
    public Row newRow() {
        return newRow;
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, oldRow, newRow);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof RowUpdate) {
            final RowUpdate other = (RowUpdate) obj;
            return Objects.equals(this.uuid, other.uuid)
                    && Objects.equals(this.oldRow, other.oldRow)
                    && Objects.equals(this.newRow, other.newRow);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("uuid", uuid).add("oldRow", oldRow)
                .add("newRow", newRow).toString();
    }
}
