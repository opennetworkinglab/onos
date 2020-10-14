/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.inbandtelemetry.app.ui;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableSet;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.TpPort;
import org.onosproject.inbandtelemetry.api.IntIntent;
import org.onosproject.inbandtelemetry.api.IntIntentId;
import org.onosproject.net.behaviour.inbandtelemetry.IntMetadataType;
import org.onosproject.inbandtelemetry.api.IntService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

import static org.onosproject.inbandtelemetry.api.IntIntent.IntHeaderType.HOP_BY_HOP;

public class IntAppUiMessageHandler extends UiMessageHandler {

    private static final String INT_INTENT_ADD_REQUEST = "intIntentAddRequest";
    private static final String INT_INTENT_DEL_REQUEST = "intIntentDelRequest";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private IntService intService;

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
                new IntIntentAddRequestHandler(),
                new IntIntentDelRequestHandler()
        );
    }

    private final class IntIntentDelRequestHandler extends RequestHandler {
        private IntIntentDelRequestHandler() {
            super(INT_INTENT_DEL_REQUEST);
        }

        @Override
        public void process(ObjectNode payload) {
            log.debug("intIntentDelRequest: {}", payload);
            intService = get(IntService.class);
            if (payload.get("intentId") != null) {
                intService.removeIntIntent(IntIntentId.valueOf(payload.get("intentId").asLong()));
            }
        }
    }

    private final class IntIntentAddRequestHandler extends RequestHandler {
        private IntIntentAddRequestHandler() {
            super(INT_INTENT_ADD_REQUEST);
        }

        @Override
        public void process(ObjectNode payload) {
            log.debug("intIntentAddRequest: {}", payload);

            intService = get(IntService.class);

            TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
            IntIntent.Builder builder = IntIntent.builder();

            JsonNode jsonNodeVal = payload.get("ip4SrcPrefix");
            if (jsonNodeVal != null && !jsonNodeVal.asText().isEmpty()) {
                sBuilder.matchIPSrc(parseIp4Prefix(jsonNodeVal.asText()));
            }

            jsonNodeVal = payload.get("ip4DstPrefix");
            if (jsonNodeVal != null && !jsonNodeVal.asText().isEmpty()) {
                sBuilder.matchIPDst(parseIp4Prefix(jsonNodeVal.asText()));
            }

            jsonNodeVal = payload.get("protocol");
            byte ipProtocol = 0;
            if (jsonNodeVal != null) {
                if (jsonNodeVal.asText().equalsIgnoreCase("TCP")) {
                    ipProtocol = IPv4.PROTOCOL_TCP;
                } else if (jsonNodeVal.asText().equalsIgnoreCase("UDP")) {
                    ipProtocol = IPv4.PROTOCOL_UDP;
                }
            }

            jsonNodeVal = payload.get("l4SrcPort");
            if (jsonNodeVal != null) {
                int portNo = jsonNodeVal.asInt(0);
                if (portNo != 0 && ipProtocol == IPv4.PROTOCOL_TCP) {
                    sBuilder.matchTcpSrc(TpPort.tpPort(portNo));
                } else if (portNo != 0 && ipProtocol == IPv4.PROTOCOL_UDP) {
                    sBuilder.matchUdpSrc(TpPort.tpPort(portNo));
                }
            }

            jsonNodeVal = payload.get("l4DstPort");
            if (jsonNodeVal != null) {
                int portNo = jsonNodeVal.asInt(0);
                if (portNo != 0 && ipProtocol == IPv4.PROTOCOL_TCP) {
                    sBuilder.matchTcpDst(TpPort.tpPort(portNo));
                } else if (portNo != 0 && ipProtocol == IPv4.PROTOCOL_UDP) {
                    sBuilder.matchUdpDst(TpPort.tpPort(portNo));
                }
            }

            jsonNodeVal = payload.get("metadata");
            if (jsonNodeVal != null && jsonNodeVal.isArray()) {
                for (final JsonNode json : jsonNodeVal) {
                    switch (json.asText()) {
                        case "SWITCH_ID":
                            builder.withMetadataType(IntMetadataType.SWITCH_ID);
                            break;
                        case "PORT_ID":
                            builder.withMetadataType(IntMetadataType.L1_PORT_ID);
                            break;
                        case "HOP_LATENCY":
                            builder.withMetadataType(IntMetadataType.HOP_LATENCY);
                            break;
                        case "QUEUE_OCCUPANCY":
                            builder.withMetadataType(IntMetadataType.QUEUE_OCCUPANCY);
                            break;
                        case "INGRESS_TIMESTAMP":
                            builder.withMetadataType(IntMetadataType.INGRESS_TIMESTAMP);
                            break;
                        case "EGRESS_TIMESTAMP":
                            builder.withMetadataType(IntMetadataType.EGRESS_TIMESTAMP);
                            break;
                        case "EGRESS_TX_UTIL":
                            builder.withMetadataType(IntMetadataType.EGRESS_TX_UTIL);
                            break;
                        default:
                            break;
                    }
                }
            }

            jsonNodeVal = payload.get("telemetryMode");
            if (jsonNodeVal != null) {
                if (jsonNodeVal.asText()
                        .equalsIgnoreCase(IntIntent.TelemetryMode.POSTCARD.toString())) {
                    builder.withTelemetryMode(IntIntent.TelemetryMode.POSTCARD);
                } else if (jsonNodeVal.asText()
                        .equalsIgnoreCase(IntIntent.TelemetryMode.INBAND_TELEMETRY.toString())) {
                    builder.withTelemetryMode(IntIntent.TelemetryMode.INBAND_TELEMETRY);
                } else {
                    log.warn("Unsupport telemetry mode {}", jsonNodeVal.asText());
                    return;
                }
            }

            builder.withSelector(sBuilder.build())
                    .withHeaderType(HOP_BY_HOP)
                    .withReportType(IntIntent.IntReportType.TRACKED_FLOW);
            intService.installIntIntent(builder.build());
        }

        private Ip4Prefix parseIp4Prefix(String prefixString) {
            if (prefixString == null) {
                return null;
            }
            String[] splitString = prefixString.split("/");
            Ip4Address ip4Address = Ip4Address.valueOf(splitString[0]);
            int mask = splitString.length > 1 ? Integer.parseInt(splitString[1]) : 32;
            return Ip4Prefix.valueOf(ip4Address, mask);
        }
    }
}
