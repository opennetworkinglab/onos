/*
 * Copyright 2015-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.ovsdb.rfc.notation.json;

import java.io.IOException;
import java.util.Set;

import org.onosproject.ovsdb.rfc.notation.OvsdbSet;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * OvsdbSet Serializer.
 */
public class OvsdbSetSerializer extends JsonSerializer<OvsdbSet> {
    @Override
    public void serialize(OvsdbSet set, JsonGenerator generator,
                          SerializerProvider provider)
            throws IOException, JsonProcessingException {
        generator.writeStartArray();
        generator.writeString("set");
        generator.writeStartArray();
        Set javaSet = set.set();
        for (Object key : javaSet) {
            generator.writeObject(key);
        }
        generator.writeEndArray();
        generator.writeEndArray();
    }
}