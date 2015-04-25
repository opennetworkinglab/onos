/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal
 */
package org.onosproject.ovsdb.lib.notation.json;

import java.io.IOException;
import java.util.Collection;

import org.onosproject.ovsdb.lib.notation.Column;
import org.onosproject.ovsdb.lib.notation.Row;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class RowSerializer extends JsonSerializer<Row> {
    @Override
    public void serialize(Row row, JsonGenerator generator,
                          SerializerProvider provider)
            throws IOException, JsonProcessingException {
        generator.writeStartObject();
        Collection<Column> columns = row.getColumns();
        for (Column<?, ?> column : columns) {
            generator.writeObjectField(column.getSchema().getName(),
                                       column.getData());
        }
        generator.writeEndObject();
    }
}
