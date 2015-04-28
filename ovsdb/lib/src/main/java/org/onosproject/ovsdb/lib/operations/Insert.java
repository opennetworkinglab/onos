/*
 * Copyright (C) 2014 EBay Software Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Ashwin Raveendran
 */
package org.onosproject.ovsdb.lib.operations;

import java.util.Collection;
import java.util.Map;

import org.onosproject.ovsdb.lib.notation.Column;
import org.onosproject.ovsdb.lib.notation.Row;
import org.onosproject.ovsdb.lib.schema.ColumnSchema;
import org.onosproject.ovsdb.lib.schema.TableSchema;
import org.onosproject.ovsdb.lib.schema.typed.TypedBaseTable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Maps;

public class Insert<E extends TableSchema<E>> extends Operation<E> {

    public static final String INSERT = "insert";

    String uuid;

    @JsonProperty("uuid-name")
    private String uuidName;

    private Map<String, Object> row = Maps.newHashMap();

    public Insert<E> on(TableSchema<E> schema) {
        this.setTableSchema(schema);
        return this;
    }

    public Insert<E> withId(String name) {
        this.uuidName = name;
        this.setOp(INSERT);
        return this;
    }

    public Insert(TableSchema<E> schema) {
        super(schema, INSERT);
    }

    public Insert(TableSchema<E> schema, Row<E> row) {
        super(schema, INSERT);
        Collection<Column<E, ?>> columns = row.getColumns();
        for (Column<E, ?> column : columns) {
            this.value(column);
        }
    }

    public Insert(TypedBaseTable<E> typedTable) {
        this(typedTable.getSchema(), typedTable.getRow());
    }

    public <D, C extends TableSchema<C>> Insert<E> value(ColumnSchema<C, D> columnSchema,
                                                         D value) {
        Object untypedValue = columnSchema.getNormalizeData(value);
        row.put(columnSchema.getName(), untypedValue);
        return this;
    }

    public <D, C extends TableSchema<C>> Insert<E> value(Column<C, D> column) {
        ColumnSchema<C, D> columnSchema = column.getSchema();
        D value = column.getData();
        return this.value(columnSchema, value);
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUuidName() {
        return uuidName;
    }

    public void setUuidName(String uuidName) {
        this.uuidName = uuidName;
    }

    public Map<String, Object> getRow() {
        return row;
    }

    public void setRow(Map<String, Object> row) {
        this.row = row;
    }

}
