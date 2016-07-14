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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.onosproject.ovsdb.rfc.notation.Uuid;

import java.io.IOException;

/**
 * UUID Serializer.
 */
public class UuidSerializer extends JsonSerializer<Uuid> {
    @Override
    public void serialize(Uuid value, JsonGenerator generator,
                          SerializerProvider provider) throws IOException {
        generator.writeStartArray();
        String reg = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$";
        if (value.value().matches(reg)) {
            generator.writeString("uuid");
        } else {
            generator.writeString("named-uuid");
        }
        generator.writeString(value.value());
        generator.writeEndArray();
    }
}
