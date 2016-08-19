/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.incubator.net.dpi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of encoder for DpiStatInfo codec.
 */
public final class DpiStatInfoCodec extends JsonCodec<DpiStatInfo> {

    private static final String TRAFFIC_STATISTICS = "trafficStatistics";
    private static final String DETECTED_PROTOS = "detectedProtos";
    private static final String KNOWN_FLOWS = "knownFlows";
    private static final String UNKNOWN_FLOWS = "unknownFlows";

    private final Logger log = getLogger(getClass());

    @Override
    public ObjectNode encode(DpiStatInfo dsi, CodecContext context) {
        checkNotNull(dsi, "DpiStatInfo cannot be null");

        final ObjectNode result = context.mapper().createObjectNode();

        final JsonCodec<TrafficStatInfo> trafficStatInfoCodec =
                context.codec(TrafficStatInfo.class);


        final TrafficStatInfo tsi = dsi.trafficStatistics();
        if (tsi != null) {
            final ObjectNode jsonTrafficStatistics = trafficStatInfoCodec.encode(tsi, context);
            result.set(TRAFFIC_STATISTICS, jsonTrafficStatistics);
        }


        final List<ProtocolStatInfo> psi = dsi.detectedProtos();
        if (psi != null) {
            final ArrayNode jsonDetectedProtos = result.putArray(DETECTED_PROTOS);
            final JsonCodec<ProtocolStatInfo> protocolStatInfoCodec =
                    context.codec(ProtocolStatInfo.class);

            for (final ProtocolStatInfo protocolStatInfo : psi) {
                jsonDetectedProtos.add(protocolStatInfoCodec.encode(protocolStatInfo, context));
            }
        }

        List<FlowStatInfo> fsi = dsi.knownFlows();
        if (fsi != null) {
            final ArrayNode jsonKnownFlows = result.putArray(KNOWN_FLOWS);
            final JsonCodec<FlowStatInfo> flowStatInfoCodec =
                    context.codec(FlowStatInfo.class);

            for (final FlowStatInfo flowStatInfo : fsi) {
                jsonKnownFlows.add(flowStatInfoCodec.encode(flowStatInfo, context));
            }
        }

        fsi = dsi.unknownFlows();
        if (fsi != null) {
            final ArrayNode jsonUnknownFlows = result.putArray(UNKNOWN_FLOWS);
            final JsonCodec<FlowStatInfo> flowStatInfoCodec =
                    context.codec(FlowStatInfo.class);

            for (final FlowStatInfo flowStatInfo : fsi) {
                jsonUnknownFlows.add(flowStatInfoCodec.encode(flowStatInfo, context));
            }
        }

        return result;
    }

    @Override
    public DpiStatInfo decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        log.debug("trafficStatistics={}, full json={} ", json.get("trafficStatistics"), json);
        TrafficStatInfo trafficStatInfo = null;
        ObjectNode tsJson = get(json, TRAFFIC_STATISTICS);
        if (tsJson != null) {
            JsonCodec<TrafficStatInfo> trafficStatInfoJsonCodec =
                    context.codec(TrafficStatInfo.class);
            trafficStatInfo = trafficStatInfoJsonCodec.decode(tsJson, context);
        }

        final JsonCodec<ProtocolStatInfo> protocolStatInfoCodec =
                context.codec(ProtocolStatInfo.class);

        List<ProtocolStatInfo> detectedProtos = new ArrayList<>();
        JsonNode dpJson = json.get(DETECTED_PROTOS);
        if (dpJson != null) {
            IntStream.range(0, dpJson.size())
                    .forEach(i -> detectedProtos.add(
                            protocolStatInfoCodec.decode(get(dpJson, i),
                                                     context)));
        }

        final JsonCodec<FlowStatInfo> flowStatInfoCodec =
                context.codec(FlowStatInfo.class);

        List<FlowStatInfo> knownFlows = new ArrayList<>();
        JsonNode kfJson = json.get(KNOWN_FLOWS);
        if (kfJson != null) {
            IntStream.range(0, kfJson.size())
                    .forEach(i -> knownFlows.add(
                            flowStatInfoCodec.decode(get(kfJson, i),
                                                         context)));
        }

        List<FlowStatInfo> unknownFlows = new ArrayList<>();
        JsonNode ufJson = json.get(UNKNOWN_FLOWS);
        if (ufJson != null) {
            IntStream.range(0, ufJson.size())
                    .forEach(i -> unknownFlows.add(
                            flowStatInfoCodec.decode(get(ufJson, i),
                                                     context)));
        }

        return new DpiStatInfo(trafficStatInfo,
                               detectedProtos,
                               knownFlows,
                               unknownFlows);
    }
}
