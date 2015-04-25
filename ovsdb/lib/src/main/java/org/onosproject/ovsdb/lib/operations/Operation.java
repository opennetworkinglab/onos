/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Ashwin Raveendran
 */
package org.onosproject.ovsdb.lib.operations;

import org.onosproject.ovsdb.lib.schema.TableSchema;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class Operation<E extends TableSchema<E>> {

    @JsonIgnore
    private TableSchema<E> tableSchema;

    private String op;

    @JsonIgnore
    // todo(Ashwin): remove this
    // Just a simple way to retain the result of a transact operation which the
    // client can refer to.
    private OperationResult result;

    protected Operation() {
    }

    protected Operation(TableSchema<E> tableSchema) {
        this.tableSchema = tableSchema;
    }

    public Operation(TableSchema<E> schema, String operation) {
        this.tableSchema = schema;
        this.op = operation;
    }

    public String getOp() {
        return op;
    }

    public void setOp(String op) {
        this.op = op;
    }

    public OperationResult getResult() {
        return result;
    }

    public void setResult(OperationResult result) {
        this.result = result;
    }

    public TableSchema<E> getTableSchema() {
        return tableSchema;
    }

    public void setTableSchema(TableSchema<E> tableSchema) {
        this.tableSchema = tableSchema;
    }

    @JsonProperty
    public String getTable() {
        return (tableSchema == null) ? null : tableSchema.getName();
    }

    @Override
    public String toString() {
        return "Operation [op=" + op + ", result=" + result + "]";
    }

}
