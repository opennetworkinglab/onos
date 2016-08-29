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
package org.onosproject.codec.impl;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.meter.Band;
import org.onosproject.net.meter.Band.Builder;
import org.onosproject.net.meter.DefaultBand;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Meter band JSON codec.
 */
public final class MeterBandCodec extends JsonCodec<Band> {
    private final Logger log = getLogger(getClass());

    // JSON field names
    private static final String TYPE = "type";
    private static final String RATE = "rate";
    private static final String BURST_SIZE = "burstSize";
    private static final String PREC = "prec";
    private static final String PACKETS = "packets";
    private static final String BYTES = "bytes";
    private static final String MISSING_MEMBER_MESSAGE = " member is required in Band";

    @Override
    public ObjectNode encode(Band band, CodecContext context) {
        checkNotNull(band, "Band cannot be null");

        ObjectNode result = context.mapper().createObjectNode()
                .put(TYPE, band.type().toString())
                .put(RATE, band.rate())
                .put(PACKETS, band.packets())
                .put(BYTES, band.bytes())
                .put(BURST_SIZE, band.burst());

        if (band.dropPrecedence() != null) {
            result.put(PREC, band.dropPrecedence());
        }

        return result;
    }

    @Override
    public Band decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        Builder builder = DefaultBand.builder();

        // parse rate
        long rate = nullIsIllegal(json.get(RATE), RATE + MISSING_MEMBER_MESSAGE).asLong();
        builder.withRate(rate);

        // parse burst size
        long burstSize = nullIsIllegal(json.get(BURST_SIZE), BURST_SIZE + MISSING_MEMBER_MESSAGE).asLong();
        builder.burstSize(burstSize);

        // parse precedence
        Short precedence = null;

        // parse band type
        String typeStr = nullIsIllegal(json.get(TYPE), TYPE + MISSING_MEMBER_MESSAGE).asText();
        Band.Type type = null;
        switch (typeStr) {
            case "DROP":
                type = Band.Type.DROP;
                builder.ofType(type);
                break;
            case "REMARK":
                type = Band.Type.REMARK;
                precedence = (short) nullIsIllegal(json.get(PREC), PREC + MISSING_MEMBER_MESSAGE).asInt();
                builder.ofType(type);
                builder.dropPrecedence(precedence);
                break;
            default:
                nullIsIllegal(type, "The requested type " + typeStr + " is not defined for band.");
        }

        Band band = builder.build();
        return band;
    }
}
