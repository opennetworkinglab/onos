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

import org.onosproject.ovsdb.rfc.notation.Condition;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * Condition Serializer.
 */
public class ConditionSerializer extends JsonSerializer<Condition> {
    @Override
    public void serialize(Condition condition, JsonGenerator generator,
                          SerializerProvider provider)
            throws IOException, JsonProcessingException {
        generator.writeStartArray();
        generator.writeString(condition.getColumn());
        generator.writeString(condition.getFunction().function());
        generator.writeObject(condition.getValue());
        generator.writeEndArray();
    }
}
