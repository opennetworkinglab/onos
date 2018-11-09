/*
 * Copyright 2018-present Open Networking Foundation
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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of encoder for TrafficStatInfo codec.
 */
public final class TrafficStatInfoCodec extends JsonCodec<TrafficStatInfo> {

    private final Logger log = getLogger(getClass());

    @Override
    public ObjectNode encode(TrafficStatInfo tsi, CodecContext context) {
        checkNotNull(tsi, "TrafficStatInfo cannot be null");

        return context.mapper().createObjectNode()
                .put("ethernetBytes", tsi.ethernetBytes())
                .put("discardedBytes", tsi.discardedBytes())
                .put("ipPackets", tsi.ipPackets())
                .put("totalPackets", tsi.totalPackets())
                .put("ipBytes", tsi.ipBytes())
                .put("avgPktSize", tsi.avgPktSize())
                .put("uniqueFlows", tsi.uniqueFlows())
                .put("tcpPackets", tsi.tcpPackets())
                .put("udpPackets", tsi.udpPackets())
                .put("dpiThroughputPps", tsi.dpiThroughputPps())
                .put("dpiThroughputBps", tsi.dpiThroughputBps())
                .put("trafficThroughputPps", tsi.trafficThroughputPps())
                .put("trafficThroughputBps", tsi.trafficThroughputBps())
                .put("trafficDurationSec", tsi.trafficDurationSec())
                .put("guessedFlowProtos", tsi.guessedFlowProtos());
    }

    @Override
    public TrafficStatInfo decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        log.debug("ethernetBytes={}, full json={} ", json.get("ethernetBytes"), json);
        final Long ethernetBytes = json.get("ethernetBytes").asLong();
        final Long discardedBytes = json.get("discardedBytes").asLong();
        final Long ipPackets = json.get("ipPackets").asLong();
        final Long totalPackets = json.get("totalPackets").asLong();
        final Long ipBytes = json.get("ipBytes").asLong();
        final int avgPktSize = json.get("avgPktSize").asInt();
        final int uniqueFlows = json.get("uniqueFlows").asInt();
        final Long tcpPackets = json.get("tcpPackets").asLong();
        final Long udpPackets = json.get("udpPackets").asLong();
        final double dpiThroughputPps = json.get("dpiThroughputPps").asDouble();
        final double dpiThroughputBps = json.get("dpiThroughputBps").asDouble();
        final double trafficThroughputPps = json.get("trafficThroughputPps").asDouble();
        final double trafficThroughputBps = json.get("trafficThroughputBps").asDouble();
        final double trafficDurationSec = json.get("trafficDurationSec").asDouble();
        final int guessedFlowProtos = json.get("guessedFlowProtos").asInt();

        return new TrafficStatInfo(ethernetBytes,
                                   discardedBytes,
                                   ipPackets, totalPackets,
                                   ipBytes, avgPktSize,
                                   uniqueFlows,
                                   tcpPackets, udpPackets,
                                   dpiThroughputPps, dpiThroughputBps,
                                   trafficThroughputPps, trafficThroughputBps,
                                   trafficDurationSec,
                                   guessedFlowProtos);
    }
}
