/*
 * Copyright (C) 2014 EBay Software Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Ashwin Raveendran
 */
package org.onosproject.ovsdb.lib.schema;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.onosproject.ovsdb.lib.message.TableUpdate;
import org.onosproject.ovsdb.lib.notation.Column;
import org.onosproject.ovsdb.lib.notation.Row;
import org.onosproject.ovsdb.lib.notation.UUID;
import org.onosproject.ovsdb.lib.operations.Insert;
import org.onosproject.ovsdb.lib.schema.BaseType.UuidBaseType;
import org.onosproject.ovsdb.lib.schema.ColumnType.AtomicColumnType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

public abstract class TableSchema<E extends TableSchema<E>> {

    protected static final Logger log = LoggerFactory
            .getLogger(TableSchema.class);
    private String name;
    private Map<String, ColumnSchema> columns;

    public TableSchema() {
    }

    protected TableSchema(String name) {
        this.name = name;
    }

    public TableSchema(String name, Map<String, ColumnSchema> columns) {
        this.name = name;
        this.columns = columns;
    }

    public Set<String> getColumns() {
        return this.columns.keySet();
    }

    public Map<String, ColumnSchema> getColumnSchemas() {
        return columns;
    }

    public boolean hasColumn(String column) {
        return this.getColumns().contains(column);
    }

    public ColumnType getColumnType(String column) {
        return this.columns.get(column).getType();
    }

    public <E extends TableSchema<E>> E as(Class<E> clazz) {
        try {
            Constructor<E> e = clazz.getConstructor(TableSchema.class);
            return e.newInstance(this);
        } catch (Exception e) {
            throw new RuntimeException(
                                       "exception constructing instance of clazz "
                                               + clazz, e);
        }
    }

    public Insert<E> insert() {
        return new Insert<>(this);
    }

    public <D> ColumnSchema<E, Set<D>> multiValuedColumn(String column,
                                                         Class<D> type) {
        // todo exception handling

        ColumnSchema columnSchema = columns.get(column);
        columnSchema.validateType(type);
        return columnSchema;
    }

    public <K, V> ColumnSchema<E, Map<K, V>> multiValuedColumn(String column,
                                                               Class<K> keyType,
                                                               Class<V> valueType) {
        // todo exception handling

        ColumnSchema columnSchema = columns.get(column);
        columnSchema.validateType(valueType);
        return columnSchema;
    }

    public <D> ColumnSchema<E, D> column(String column, Class<D> type) {
        // todo exception handling

        ColumnSchema columnSchema = columns.get(column);
        if (columnSchema != null) {
            columnSchema.validateType(type);
        }
        return columnSchema;
    }

    public ColumnSchema column(String column) {
        return this.columns.get(column);
    }

    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    protected void setColumns(Map<String, ColumnSchema> columns) {
        this.columns = columns;
    }

    public TableUpdate<E> updatesFromJson(JsonNode value) {
        TableUpdate<E> tableUpdate = new TableUpdate<>();
        Iterator<Entry<String, JsonNode>> fields = value.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> idOldNew = fields.next();
            String uuid = idOldNew.getKey();

            ObjectNode newN = (ObjectNode) idOldNew.getValue().get("new");
            ObjectNode old = (ObjectNode) idOldNew.getValue().get("old");

            Row<E> newRow = newN != null ? createRow(newN) : null;
            Row<E> oldRow = old != null ? createRow(old) : null;

            tableUpdate.addRow(new UUID(uuid), oldRow, newRow);
        }
        return tableUpdate;
    }

    public Row<E> createRow(ObjectNode rowNode) {
        List<Column<E, ?>> columns = Lists.newArrayList();
        for (Iterator<Map.Entry<String, JsonNode>> iter = rowNode.fields(); iter
                .hasNext();) {
            Map.Entry<String, JsonNode> next = iter.next();
            ColumnSchema<E, Object> schema = column(next.getKey(), Object.class);
            /*
             * Ideally the ColumnSchema shouldn't be null at this stage. But
             * there can be cases in which the OVSDB manager Schema
             * implementation might decide to include some "hidden" columns that
             * are NOT reported in getSchema, but decide to report it in
             * unfiltered monitor. Hence adding some safety checks around that.
             */
            if (schema != null) {
                Object o = schema.valueFromJson(next.getValue());
                columns.add(new Column<>(schema, o));
            }
        }
        return new Row<>(this, columns);
    }

    public ArrayList<Row<E>> createRows(JsonNode rowsNode) {
        ArrayList<Row<E>> rows = Lists.newArrayList();
        for (JsonNode rowNode : rowsNode.get("rows")) {
            rows.add(createRow((ObjectNode) rowNode));
        }

        return rows;
    }

    /*
     * RFC 7047 Section 3.2 specifies 2 internally generated columns in each
     * table namely _uuid and _version which are not exposed in get_schema call.
     * Since these 2 columns are extremely useful for Mutate, update and select
     * operations, the ColumnSchema for these 2 columns are manually populated.
     *
     * It is to be noted that these 2 columns are specified as part of the
     * RFC7047 and not a specific Schema implementation detail & hence adding it
     * by default in the Library for better application experience using the
     * library.
     */
    public void populateInternallyGeneratedColumns() {
        columns.put("_uuid",
                    new ColumnSchema("_uuid",
                                     new AtomicColumnType(new UuidBaseType())));
        columns.put("_version",
                    new ColumnSchema("_version",
                                     new AtomicColumnType(new UuidBaseType())));
    }
}
