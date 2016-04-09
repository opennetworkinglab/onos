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
package org.onosproject.ovsdb.rfc.notation;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

/**
 * Column is the basic element of the OpenVswitch database.
 */
public final class Column {
    private final String columnName;
    private final Object data;

    /**
     * Column constructor.
     * @param columnName the column name
     * @param obj the data of the column
     */
    public Column(String columnName, Object obj) {
        checkNotNull(columnName, "columnName cannot be null");
        checkNotNull(obj, "data cannot be null");
        this.columnName = columnName;
        this.data = obj;
    }

    /**
     * Returns column data.
     * @return column data
     */
    public Object data() {
        return data;
    }

    /**
     * Returns columnName.
     * @return columnName
     */
    public String columnName() {
        return columnName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(columnName, data);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Column) {
            final Column other = (Column) obj;
            return Objects.equals(this.columnName, other.columnName)
                    && Objects.equals(this.data, other.data);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("columnName", columnName)
                .add("data", data).toString();
    }
}
