/*
 *
 *  * Copyright (C) 2014 EBay Software Foundation
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *  *
 *  * Authors : Ashwin Raveendran
 *
 */

package org.onosproject.ovsdb.lib.schema;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.onosproject.ovsdb.lib.error.BadSchemaException;

import com.fasterxml.jackson.databind.JsonNode;

public class GenericTableSchema extends TableSchema<GenericTableSchema> {

    public GenericTableSchema() {
    }

    public GenericTableSchema(String tableName) {
        super(tableName);
    }

    public GenericTableSchema(TableSchema tableSchema) {
        super(tableSchema.getName(), tableSchema.getColumnSchemas());
    }

    public GenericTableSchema fromJson(String tableName, JsonNode json) {

        if (!json.isObject() || !json.has("columns")) {
            throw new BadSchemaException(
                                         "bad tableschema root, expected \"columns\" as child");
        }

        Map<String, ColumnSchema> columns = new HashMap<>();
        for (Iterator<Map.Entry<String, JsonNode>> iter = json.get("columns")
                .fields(); iter.hasNext();) {
            Map.Entry<String, JsonNode> column = iter.next();
            log.trace("{}:{}", tableName, column.getKey());
            columns.put(column.getKey(),
                        ColumnSchema.fromJson(column.getKey(),
                                              column.getValue()));
        }

        this.setName(tableName);
        this.setColumns(columns);
        return this;
    }

    // public TableUpdate<GenericTableSchema> updatesFromJson(JsonNode value) {
    // ObjectNode new_ = (ObjectNode) value.get("new");
    // ObjectNode old = (ObjectNode) value.get("new");
    //
    // Row<GenericTableSchema> newRow = createRow(new_);
    // Row<GenericTableSchema> oldRow = createRow(old);
    //
    // TableUpdate<GenericTableSchema> tableUpdate = new
    // TableUpdate<GenericTableSchema>();
    // tableUpdate.setNew(newRow);
    // tableUpdate.setNew(oldRow);
    //
    //
    // return null;
    // }
    //
    // protected Row<GenericTableSchema> createRow(ObjectNode rowNode) {
    // List<Column<GenericTableSchema, ?>> columns = Lists.newArrayList();
    // for (Iterator<Map.Entry<String, JsonNode>> iter = rowNode.fields();
    // iter.hasNext();) {
    // Map.Entry<String, JsonNode> next = iter.next();
    // ColumnSchema schema = column(next.getKey());
    // Object o = schema.valueFromJson(next.getValue());
    // columns.add(new Column(schema, o));
    // }
    // return new Row<>(columns);
    // }
}
