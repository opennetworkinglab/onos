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

import org.onosproject.ovsdb.lib.notation.Row;
import org.onosproject.ovsdb.lib.schema.TableSchema;
import org.onosproject.ovsdb.lib.schema.typed.TypedBaseTable;

public class Operations {
    public static Operations op = new Operations();

    public <E extends TableSchema<E>> Insert<E> insert(TableSchema<E> schema) {
        return new Insert<>(schema);
    }

    public <E extends TableSchema<E>> Insert<E> insert(TypedBaseTable<E> typedTable) {
        return new Insert<>(typedTable);
    }

    public <E extends TableSchema<E>> Insert<E> insert(TableSchema<E> schema,
                                                       Row<E> row) {
        return new Insert<>(schema, row);
    }

    public <E extends TableSchema<E>> Update<E> update(TableSchema<E> schema) {
        return new Update<>(schema);
    }

    public <E extends TableSchema<E>> Update<E> update(TypedBaseTable<E> typedTable) {
        return new Update<>(typedTable);
    }

    public <E extends TableSchema<E>> Update<E> update(TableSchema<E> schema,
                                                       Row<E> row) {
        return new Update<>(schema, row);
    }

    public <E extends TableSchema<E>> Delete<E> delete(TableSchema<E> schema) {
        return new Delete<>(schema);
    }

    public <E extends TableSchema<E>> Mutate<E> mutate(TableSchema<E> schema) {
        return new Mutate<>(schema);
    }

    public Commit commit(Boolean durable) {
        return new Commit(durable);
    }

    public Abort abort() {
        return new Abort();
    }

    public <E extends TableSchema<E>> Select<E> select(TableSchema<E> schema) {
        return new Select<>(schema);
    }

    public Comment comment(String comment) {
        return new Comment(comment);
    }

    /*
     * Could not use Java keyword "assert" which clashes with the ovsdb json-rpc
     * method. using assertion instead.
     */
    public Assert assertion(String lock) {
        return new Assert(lock);
    }
}
