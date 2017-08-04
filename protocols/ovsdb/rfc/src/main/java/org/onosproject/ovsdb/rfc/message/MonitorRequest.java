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
package org.onosproject.ovsdb.rfc.message;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Monitor Requst information that need to monitor table.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class MonitorRequest {
    @JsonIgnore
    private final String tableName;
    private final Set<String> columns;
    private final MonitorSelect select;

    /**
     * Constructs a MonitorRequest object.
     * @param tableName table name
     * @param columns a set of column name
     * @param select monitor action
     */
    public MonitorRequest(String tableName, Set<String> columns,
                          MonitorSelect select) {
        checkNotNull(tableName, "table name cannot be null");
        checkNotNull(columns, "columns cannot be null");
        checkNotNull(select, "select cannot be null");
        this.tableName = tableName;
        this.columns = columns;
        this.select = select;
    }

    /**
     * Returns tableName.
     * @return tableName
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Returns select.
     * @return select
     */
    public MonitorSelect getSelect() {
        return select;
    }

    /**
     * Returns columns.
     * @return columns
     */
    public Set<String> getColumns() {
        return columns;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tableName, select, columns);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof MonitorRequest) {
            final MonitorRequest other = (MonitorRequest) obj;
            return Objects.equals(this.tableName, other.tableName)
                    && Objects.equals(this.select, other.select)
                    && Objects.equals(this.columns, other.columns);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("tableName", tableName)
                .add("select", select).add("columns", columns).toString();
    }
}
