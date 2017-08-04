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

import java.util.Collection;
import java.util.Map;

import org.onosproject.ovsdb.rfc.notation.Column;
import org.onosproject.ovsdb.rfc.notation.Row;
import org.onosproject.ovsdb.rfc.schema.TableSchema;
import org.onosproject.ovsdb.rfc.utils.TransValueUtil;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Maps;

/**
 * insert operation.Refer to RFC 7047 Section 5.2.
 */
public final class Insert implements Operation {

    @JsonIgnore
    private final TableSchema tableSchema;
    private final String op;
    @JsonProperty("uuid-name")
    private final String uuidName;
    private final Map<String, Object> row;

    /**
     * Constructs a Insert object.
     * @param schema TableSchema entity
     * @param uuidName uuid-name
     * @param row Row entity
     */
    public Insert(TableSchema schema, String uuidName, Row row) {
        checkNotNull(schema, "TableSchema cannot be null");
        checkNotNull(uuidName, "uuid name cannot be null");
        checkNotNull(row, "row cannot be null");
        this.tableSchema = schema;
        this.op = Operations.INSERT.op();
        this.uuidName = uuidName;
        this.row = Maps.newHashMap();
        generateOperationRow(row);
    }

    /**
     * Row entity convert into the row format of insert operation. Refer to RFC
     * 7047 Section 5.2.
     * @param row Row entity
     */
    private void generateOperationRow(Row row) {
        Collection<Column> columns = row.getColumns();
        for (Column column : columns) {
            String columnName = column.columnName();
            Object value = column.data();
            Object formatValue = TransValueUtil.getFormatData(value);
            this.row.put(columnName, formatValue);
        }
    }

    /**
     * Returns the uuid-name member of insert operation.
     * @return the uuid-name member of insert operation
     */
    public String getUuidName() {
        return uuidName;
    }

    /**
     * Returns the row member of insert operation.
     * @return the row member of insert operation
     */
    public Map<String, Object> getRow() {
        return row;
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
