/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal
 */
package org.onosproject.ovsdb.lib.schema.typed;

import org.onosproject.ovsdb.lib.notation.Column;
import org.onosproject.ovsdb.lib.notation.Row;
import org.onosproject.ovsdb.lib.notation.UUID;
import org.onosproject.ovsdb.lib.schema.TableSchema;

public interface TypedBaseTable<E extends TableSchema<E>> {
    @TypedColumn(name = "", method = MethodType.GETTABLESCHEMA)
    E getSchema();

    @TypedColumn(name = "", method = MethodType.GETROW)
    Row<E> getRow();

    @TypedColumn(name = "_uuid", method = MethodType.GETDATA)
    public UUID getUuid();

    @TypedColumn(name = "_uuid", method = MethodType.GETCOLUMN)
    public Column<E, UUID> getUuidColumn();

    @TypedColumn(name = "_version", method = MethodType.GETDATA)
    public UUID getVersion();

    @TypedColumn(name = "_version", method = MethodType.GETCOLUMN)
    public Column<E, UUID> getVersionColumn();
}
