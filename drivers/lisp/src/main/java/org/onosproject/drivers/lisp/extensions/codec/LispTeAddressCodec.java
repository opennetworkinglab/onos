/*
 * Copyright 2017-present Open Networking Laboratory
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.drivers.lisp.extensions.LispTeAddress;

import java.util.List;
import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * LISP traffic engineering address codec.
 */
public final class LispTeAddressCodec extends JsonCodec<LispTeAddress> {

    protected static final String TE_RECORDS = "records";

    private static final String MISSING_MEMBER_MESSAGE =
                                " member is required in LispTeAddress";

    @Override
    public ObjectNode encode(LispTeAddress address, CodecContext context) {
        checkNotNull(address, "LispTeAddress cannot be null");

        final ObjectNode result = context.mapper().createObjectNode();
        final ArrayNode jsonRecords = result.putArray(TE_RECORDS);

        final JsonCodec<LispTeAddress.TeRecord> recordCodec =
                context.codec(LispTeAddress.TeRecord.class);

        for (final LispTeAddress.TeRecord record : address.getTeRecords()) {
            jsonRecords.add(recordCodec.encode(record, context));
        }

        return result;
    }

    @Override
    public LispTeAddress decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        final JsonCodec<LispTeAddress.TeRecord> recordCodec =
                context.codec(LispTeAddress.TeRecord.class);

        JsonNode recordsJson = nullIsIllegal(json.get(TE_RECORDS),
                TE_RECORDS + MISSING_MEMBER_MESSAGE);
        List<LispTeAddress.TeRecord> records = Lists.newArrayList();

        if (recordsJson != null) {
            IntStream.range(0, recordsJson.size())
                    .forEach(i -> records.add(
                            recordCodec.decode(get(recordsJson, i), context)));
        }

        return new LispTeAddress.Builder()
                        .withTeRecords(records)
                        .build();
    }
}
