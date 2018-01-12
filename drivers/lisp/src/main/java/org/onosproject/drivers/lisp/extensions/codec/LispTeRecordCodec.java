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
package org.onosproject.drivers.lisp.extensions.codec;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.drivers.lisp.extensions.LispTeAddress;
import org.onosproject.mapping.addresses.MappingAddress;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * LISP traffic engineering record codec.
 */
public final class LispTeRecordCodec extends JsonCodec<LispTeAddress.TeRecord> {

    static final String LOOKUP = "lookup";
    static final String RLOC_PROBE = "rlocProbe";
    static final String STRICT = "strict";
    static final String ADDRESS = "address";

    private static final String MISSING_MEMBER_MESSAGE =
                                " member is required in LispTeRecord";

    @Override
    public ObjectNode encode(LispTeAddress.TeRecord record, CodecContext context) {
        checkNotNull(record, "LispTeRecord cannot be null");

        final ObjectNode result = context.mapper().createObjectNode()
                .put(LOOKUP, record.isLookup())
                .put(RLOC_PROBE, record.isRlocProbe())
                .put(STRICT, record.isStrict());

        if (record.getAddress() != null) {
            final JsonCodec<MappingAddress> addressCodec =
                    context.codec(MappingAddress.class);
            ObjectNode address = addressCodec.encode(record.getAddress(), context);
            result.set(ADDRESS, address);
        }

        return result;
    }

    @Override
    public LispTeAddress.TeRecord decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        boolean isLookup = nullIsIllegal(json.get(LOOKUP),
                LOOKUP + MISSING_MEMBER_MESSAGE).asBoolean();
        boolean isRlocProbe = nullIsIllegal(json.get(RLOC_PROBE),
                RLOC_PROBE + MISSING_MEMBER_MESSAGE).asBoolean();
        boolean isStrict = nullIsIllegal(json.get(STRICT),
                STRICT + MISSING_MEMBER_MESSAGE).asBoolean();

        ObjectNode addressJson = get(json, ADDRESS);
        MappingAddress mappingAddress = null;

        if (addressJson != null) {
            final JsonCodec<MappingAddress> addressCodec =
                    context.codec(MappingAddress.class);
            mappingAddress = addressCodec.decode(addressJson, context);
        }

        return new LispTeAddress.TeRecord.Builder()
                        .withIsLookup(isLookup)
                        .withIsRlocProbe(isRlocProbe)
                        .withIsStrict(isStrict)
                        .withRtrRlocAddress(mappingAddress)
                        .build();
    }

}
