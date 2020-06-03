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
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TpPort;
import org.onosproject.inbandtelemetry.api.IntIntent;
import org.onosproject.inbandtelemetry.api.IntIntentId;
import org.onosproject.net.behaviour.inbandtelemetry.IntMetadataType;
import org.onosproject.inbandtelemetry.api.IntService;
import org.onosproject.net.behaviour.inbandtelemetry.IntDeviceConfig;
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
    private static final String INT_CONFIG_ADD_REQUEST = "intConfigAddRequest";
//    private static final String INT_CONFIG_DEL_REQUEST = "intConfigDelRequest";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private IntService intService;

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
                new IntIntentAddRequestHandler(),
                new IntIntentDelRequestHandler(),
                new IntConfigAddRequestHandler()
                //new intConfigDelRequestHandler()
        );
    }

    private final class IntConfigAddRequestHandler extends RequestHandler {
        private IntConfigAddRequestHandler() {
            super(INT_CONFIG_ADD_REQUEST);
        }

        @Override
        public void process(ObjectNode payload) {
            log.info("intConfigAddRequest: {}", payload);

            intService = get(IntService.class);
            IntDeviceConfig.Builder builder = IntDeviceConfig.builder();

            if (payload.get("collectorIp") != null) {
                builder.withCollectorIp(IpAddress.valueOf(payload.get("collectorIp").asText()));
            } else {
                builder.withCollectorIp(IpAddress.valueOf("127.0.0.1"));
            }

            if (payload.get("collectorPort") != null) {
                builder.withCollectorPort(TpPort.tpPort(
                        payload.get("collectorPort").asInt()));
            } else {
                builder.withCollectorPort(TpPort.tpPort(1234));
            }

            builder.enabled(true)
                    .withSinkIp(IpAddress.valueOf("10.192.19.180"))
                    .withSinkMac(MacAddress.NONE)
                    .withCollectorNextHopMac(MacAddress.BROADCAST);

            intService.setConfig(builder.build());
        }
    }

    private final class IntIntentDelRequestHandler extends RequestHandler {
        private IntIntentDelRequestHandler() {
            super(INT_INTENT_DEL_REQUEST);
        }

        @Override
        public void process(ObjectNode payload) {
            log.info("intIntentDelRequest: {}", payload);

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
            log.info("intIntentAddRequest: {}", payload);

            intService = get(IntService.class);

            TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
            IntIntent.Builder builder = IntIntent.builder();

            if (payload.get("ip4SrcPrefix") != null) {
                sBuilder.matchIPSrc(parseIp4Prefix(payload.get("ip4SrcPrefix").asText()));
            }

            if (payload.get("ip4DstPrefix") != null) {
                sBuilder.matchIPDst(parseIp4Prefix(payload.get("ip4DstPrefix").asText()));
            }

            if (payload.get("l4SrcPort") != null) {
                if (payload.get("protocol") != null && payload.get("protocol").asText().equalsIgnoreCase("TCP")) {
                    sBuilder.matchTcpSrc(TpPort.tpPort(payload.get("l4SrcPort").asInt()));
                } else {
                    sBuilder.matchUdpSrc(TpPort.tpPort(payload.get("l4SrcPort").asInt()));
                }
            }

            if (payload.get("l4DstPort") != null) {
                if (payload.get("protocol") != null && payload.get("protocol").asText().equalsIgnoreCase("TCP")) {
                    sBuilder.matchTcpDst(TpPort.tpPort(payload.get("l4DstPort").asInt()));
                } else {
                    sBuilder.matchUdpDst(TpPort.tpPort(payload.get("l4DstPort").asInt()));
                }
            }

            if (payload.get("metadata") != null) {
                JsonNode meta = payload.get("metadata");
                if (meta.isArray()) {
                    for (final JsonNode json : meta) {
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
            }

            builder.withSelector(sBuilder.build())
                    .withHeaderType(HOP_BY_HOP)
                    .withReportType(IntIntent.IntReportType.TRACKED_FLOW)
                    .withTelemetryMode(IntIntent.TelemetryMode.INBAND_TELEMETRY);
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
