/*
 * Copyright (C) 2014 Red Hat Inc,
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Dave Tucker
 *
 */
package org.onosproject.ovsdb.lib.operations;

import java.util.List;

import org.onosproject.ovsdb.lib.notation.Condition;
import org.onosproject.ovsdb.lib.schema.ColumnSchema;
import org.onosproject.ovsdb.lib.schema.TableSchema;

import com.google.common.collect.Lists;

public class Select<E extends TableSchema<E>> extends Operation<E>
        implements ConditionalOperation {

    public static final String SELECT = "select";
    List<Condition> where = Lists.newArrayList();
    private List<String> columns = Lists.newArrayList();

    public Select on(TableSchema schema) {
        this.setTableSchema(schema);
        return this;
    }

    public Select(TableSchema<E> schema) {
        super(schema, SELECT);
    }

    public <D, C extends TableSchema<C>> Select<E> column(ColumnSchema<C, D> columnSchema) {
        columns.add(columnSchema.getName());
        return this;
    }

    public Where where(Condition condition) {
        where.add(condition);
        return new Where(this);
    }

    public List<String> getColumns() {
        return columns;
    }

    public void setColumns(List<String> columns) {
        this.columns = columns;
    }

    @Override
    public void addCondition(Condition condition) {
        this.where.add(condition);
    }

    public List<Condition> getWhere() {
        return where;
    }

    public void setWhere(List<Condition> where) {
        this.where = where;
    }
}
