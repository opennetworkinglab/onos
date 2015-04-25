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
import java.util.Set;

import org.onosproject.ovsdb.lib.notation.OvsdbSet;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class OvsdbSetSerializer extends JsonSerializer<OvsdbSet<?>> {
    @Override
    public void serialize(OvsdbSet<?> set, JsonGenerator generator,
        SerializerProvider provider) throws IOException,
            JsonProcessingException {
        generator.writeStartArray();
        generator.writeString("set");
        generator.writeStartArray();
        Set<?> javaSet = set.delegate();
        for (Object t : javaSet) {
            generator.writeObject(t);
        }
        generator.writeEndArray();
        generator.writeEndArray();
    }
}