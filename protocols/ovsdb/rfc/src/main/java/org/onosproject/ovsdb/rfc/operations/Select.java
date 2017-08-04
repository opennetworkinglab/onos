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
package org.onosproject.ovsdb.rfc.operations;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import org.onosproject.ovsdb.rfc.notation.Condition;
import org.onosproject.ovsdb.rfc.schema.TableSchema;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * select operation.Refer to RFC 7047 Section 5.2.
 */
public final class Select implements Operation {

    @JsonIgnore
    private final TableSchema tableSchema;
    private final String op;
    private final List<Condition> where;
    private final List<String> columns;

    /**
     * Constructs a Select object.
     * @param schema TableSchema entity
     * @param where the List of Condition entity
     * @param columns the List of column name
     */
    public Select(TableSchema schema, List<Condition> where, List<String> columns) {
        checkNotNull(schema, "TableSchema cannot be null");
        checkNotNull(where, "where cannot be null");
        checkNotNull(columns, "columns cannot be null");
        this.tableSchema = schema;
        this.op = Operations.SELECT.op();
        this.where = where;
        this.columns = columns;
    }

    /**
     * Returns the columns member of select operation.
     * @return the columns member of select operation
     */
    public List<String> getColumns() {
        return columns;
    }

    /**
     * Returns the where member of select operation.
     * @return the where member of select operation
     */
    public List<Condition> getWhere() {
        return where;
    }

    @Override
    public String getOp() {
        return op;
    }

    @Override
    public TableSchema getTableSchema() {
        return tableSchema;
    }

    /**
     * For the use of serialization.
     * @return the table member of update operation
     */
    @JsonProperty
    public String getTable() {
        return tableSchema.name();
    }
}
