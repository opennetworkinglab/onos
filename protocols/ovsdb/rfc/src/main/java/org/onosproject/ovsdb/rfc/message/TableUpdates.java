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

import java.util.Map;
import java.util.Objects;

import org.onosproject.ovsdb.rfc.schema.TableSchema;

/**
 * TableUpdates is an object that maps from a table name to a TableUpdate.
 */
public final class TableUpdates {

    private final Map<String, TableUpdate> result;

    /**
     * Constructs a TableUpdates object.
     * @param result the parameter of TableUpdates entity
     */
    private TableUpdates(Map<String, TableUpdate> result) {
        this.result = result;
    }

    /**
     * Get TableUpdates.
     * @param result the parameter of TableUpdates entity
     * @return TableUpdates
     */
    public static TableUpdates tableUpdates(Map<String, TableUpdate> result) {
        checkNotNull(result, "result cannot be null");
        return new TableUpdates(result);
    }

    /**
     * Return TableUpdate.
     * @param table the TableSchema of TableUpdates
     * @return TableUpdate
     */
    public TableUpdate tableUpdate(TableSchema table) {
        return this.result.get(table.name());
    }

    /**
     * Return the map of TableUpdate.
     * @return result
     */
    public Map<String, TableUpdate> result() {
        return result;
    }

    @Override
    public int hashCode() {
        return result.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof TableUpdates) {
            final TableUpdates other = (TableUpdates) obj;
            return Objects.equals(this.result, other.result);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("result", result).toString();
    }
}
