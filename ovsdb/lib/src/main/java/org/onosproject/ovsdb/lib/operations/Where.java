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

package org.onosproject.ovsdb.lib.operations;

import org.onosproject.ovsdb.lib.notation.Condition;
import org.onosproject.ovsdb.lib.notation.Function;
import org.onosproject.ovsdb.lib.schema.ColumnSchema;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Where {

    @JsonIgnore
    ConditionalOperation operation;

    public Where() {
    }

    public Where(ConditionalOperation operation) {
        this.operation = operation;
    }

    public Where condition(Condition condition) {
        operation.addCondition(condition);
        return this;
    }

    public Where condition(ColumnSchema column, Function function, Object value) {
        this.condition(new Condition(column.getName(), function, value));
        return this;
    }

    public Where and(ColumnSchema column, Function function, Object value) {
        condition(column, function, value);
        return this;
    }

    public Where and(Condition condition) {
        condition(condition);
        return this;
    }

    public Operation build() {
        return (Operation) this.operation;
    }

}
