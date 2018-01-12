/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.mapping.codec;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.mapping.MappingEntry;
import org.onosproject.mapping.MappingKey;
import org.onosproject.mapping.MappingValue;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Mapping entry JSON codec.
 */
public final class MappingEntryCodec extends JsonCodec<MappingEntry> {

    static final String KEY = "key";
    static final String VALUE = "value";
    static final String ID = "id";
    static final String DEVICE_ID = "deviceId";
    static final String STATE = "state";

    @Override
    public ObjectNode encode(MappingEntry mappingEntry, CodecContext context) {

        checkNotNull(mappingEntry, "Mapping entry cannot be null");

        final ObjectNode result = context.mapper().createObjectNode()
                .put(ID, Long.toString(mappingEntry.id().value()))
                .put(DEVICE_ID, mappingEntry.deviceId().toString())
                .put(STATE, mappingEntry.state().toString());

        if (mappingEntry.key() != null) {
            final JsonCodec<MappingKey> keyCodec =
                    context.codec(MappingKey.class);
            result.set(KEY, keyCodec.encode(mappingEntry.key(), context));
        }

        if (mappingEntry.value() != null) {
            final JsonCodec<MappingValue> valueCodec =
                    context.codec(MappingValue.class);
            result.set(VALUE, valueCodec.encode(mappingEntry.value(), context));
        }

        return result;
    }
}
