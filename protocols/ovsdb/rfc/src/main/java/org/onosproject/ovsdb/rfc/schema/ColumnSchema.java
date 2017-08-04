/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.ovsdb.rfc.schema;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import org.onosproject.ovsdb.rfc.schema.type.ColumnType;

/**
 * A schema for the column represented by column-schema.
 */
public final class ColumnSchema {
    private final String name;
    private final ColumnType type;

    /**
     * Constructs a ColumnSchema object.
     * @param name the column name
     * @param columnType the column type
     */
    public ColumnSchema(String name, ColumnType columnType) {
        checkNotNull(name, "name cannot be null");
        checkNotNull(columnType, "column type cannot be null");
        this.name = name;
        this.type = columnType;
    }

    /**
     * Returns the name of column.
     * @return the name of column
     */
    public String name() {
        return name;
    }

    /**
     * Returns the type of column.
     * @return the type of column
     */
    public ColumnType type() {
        return type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ColumnSchema) {
            final ColumnSchema other = (ColumnSchema) obj;
            return Objects.equals(this.name, other.name)
                    && Objects.equals(this.type, other.type);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("name", name).add("type", type)
                .toString();
    }
}
