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
import org.onosproject.mapping.DefaultMappingKey;
import org.onosproject.mapping.MappingKey;
import org.onosproject.mapping.addresses.MappingAddress;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Mapping key codec.
 */
public final class MappingKeyCodec extends JsonCodec<MappingKey> {

    protected static final String ADDRESS = "address";

    @Override
    public ObjectNode encode(MappingKey key, CodecContext context) {
        checkNotNull(key, "Mapping key cannot be null");

        final ObjectNode result = context.mapper().createObjectNode();
        final JsonCodec<MappingAddress> addressCodec =
                context.codec(MappingAddress.class);

        result.set(ADDRESS, addressCodec.encode(key.address(), context));

        return result;
    }

    @Override
    public MappingKey decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        MappingKey.Builder builder = DefaultMappingKey.builder();

        ObjectNode addressJson = get(json, ADDRESS);
        if (addressJson != null) {
            final JsonCodec<MappingAddress> addressCodec =
                    context.codec(MappingAddress.class);
            builder.withAddress(addressCodec.decode(addressJson, context));
        }

        return builder.build();
    }
}
