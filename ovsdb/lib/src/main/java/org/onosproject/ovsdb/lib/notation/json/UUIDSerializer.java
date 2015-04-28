/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Ashwin Raveendran
 */
package org.onosproject.ovsdb.lib.notation.json;

import java.io.IOException;

import org.onosproject.ovsdb.lib.notation.UUID;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class UUIDSerializer extends JsonSerializer<UUID> {
    @Override
    public void serialize(UUID value, JsonGenerator generator,
                          SerializerProvider provider)
            throws IOException, JsonProcessingException {
        generator.writeStartArray();
        try {
            java.util.UUID.fromString(value.toString());
            generator.writeString("uuid");
        } catch (IllegalArgumentException ex) {
            generator.writeString("named-uuid");
        }
        generator.writeString(value.toString());
        generator.writeEndArray();
    }
}
