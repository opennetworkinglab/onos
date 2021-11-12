/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.inbandtelemetry.rest;


import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.inbandtelemetry.api.IntIntent;
import org.onosproject.net.behaviour.inbandtelemetry.IntMetadataType;
import org.onosproject.net.flow.TrafficSelector;


import static com.google.common.base.Preconditions.checkNotNull;


/**
 * IntIntent JSON codec.
 */
public final class IntIntentCodec extends JsonCodec<IntIntent> {

    // JSON field names
    private static final String METADATA_TYPES = "metadataTypes";
    private static final String HEADER_TYPE = "headerType";
    private static final String TELEMETRY_MODE = "telemetryMode";
    private static final String REPORT_TYPES = "reportTypes";
    private static final String SELECTOR = "selector";


    @Override
    public ObjectNode encode(IntIntent intIntent, CodecContext context) {
        checkNotNull(intIntent, "intIntent cannot be null");
        ObjectNode result = context.mapper().createObjectNode();
        ArrayNode metadataTypes = context.mapper().createArrayNode();
        ArrayNode reportTypes = context.mapper().createArrayNode();
        intIntent.reportTypes().forEach(reportType -> {
            reportTypes.add(reportType.toString());
        });
        intIntent.metadataTypes().forEach(intMetadataType -> {
            metadataTypes.add(intMetadataType.toString());
        });
        result.put(HEADER_TYPE, intIntent.headerType().toString())
                .put(TELEMETRY_MODE, intIntent.telemetryMode().toString());
        result.set(REPORT_TYPES, reportTypes);
        result.set(METADATA_TYPES, metadataTypes);
        result.set(SELECTOR, context.codec(TrafficSelector.class).encode(intIntent.selector(), context));
        return result;
    }
    @Override
    public IntIntent decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }
        IntIntent.Builder resultBuilder = new IntIntent.Builder();
        resultBuilder.withHeaderType(IntIntent.IntHeaderType.valueOf(json.findValue(HEADER_TYPE).asText()));
        resultBuilder.withTelemetryMode(IntIntent.TelemetryMode.valueOf(json.findValue(TELEMETRY_MODE).asText()));

        JsonCodec<TrafficSelector> selectorCodec = context.codec(TrafficSelector.class);
        resultBuilder.withSelector(selectorCodec.decode((ObjectNode) json.findValue(SELECTOR), context));
        json.findValue(METADATA_TYPES).forEach(metaNode -> {
                resultBuilder.withMetadataType(IntMetadataType.valueOf(metaNode.asText()));
        });
        json.findValues(REPORT_TYPES).forEach(jsonNode -> {
            resultBuilder.withReportType(IntIntent.IntReportType.valueOf(jsonNode.asText()));
        });
        return resultBuilder.build();
    }

}

