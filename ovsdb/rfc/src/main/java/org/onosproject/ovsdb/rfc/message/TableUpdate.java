/*
 * Copyright 2015 Open Networking Laboratory
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

import java.util.Map;
import java.util.Objects;

import org.onosproject.ovsdb.rfc.notation.Row;
import org.onosproject.ovsdb.rfc.notation.UUID;

/**
 * TableUpdate is an object that maps from the row's UUID to a RowUpdate object.
 */
public final class TableUpdate {

    private final Map<UUID, RowUpdate> rows;

    /**
     * Constructs a TableUpdate object.
     * @param rows the parameter of TableUpdate entity
     */
    private TableUpdate(Map<UUID, RowUpdate> rows) {
        this.rows = rows;
    }

    /**
     * Get TableUpdate entity.
     * @param rows the parameter of TableUpdate entity
     * @return TableUpdate entity
     */
    public static TableUpdate tableUpdate(Map<UUID, RowUpdate> rows) {
        checkNotNull(rows, "rows cannot be null");
        return new TableUpdate(rows);
    }

    /**
     * Return old row.
     * @param uuid the key of rows
     * @return Row old row
     */
    public Row getOld(UUID uuid) {
        RowUpdate rowUpdate = rows.get(uuid);
        if (rowUpdate == null) {
            return null;
        }
        return rowUpdate.oldRow();
    }

    /**
     * Return new row.
     * @param uuid the key of rows
     * @return Row new row
     */
    public Row getNew(UUID uuid) {
        RowUpdate rowUpdate = rows.get(uuid);
        if (rowUpdate == null) {
            return null;
        }
        return rowUpdate.newRow();
    }

    /**
     * Return rows.
     * @return rows
     */
    public Map<UUID, RowUpdate> rows() {
        return rows;
    }

    @Override
    public int hashCode() {
        return Objects.hash(rows);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof TableUpdate) {
            final TableUpdate other = (TableUpdate) obj;
            return Objects.equals(this.rows, other.rows);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("rows", rows).toString();
    }
}
