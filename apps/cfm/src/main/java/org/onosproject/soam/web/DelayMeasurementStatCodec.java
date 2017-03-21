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
package org.onosproject.soam.web;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Duration;
import java.util.Map;

import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementStat;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Encode and decode to/from JSON to DelayMeasurementStat object.
 */
public class DelayMeasurementStatCodec extends JsonCodec<DelayMeasurementStat> {

    private static final String LOWER_LIMIT = "lowerLimit";
    private static final String COUNT = "count";
    private static final String BINS = "bins";
    private static final String SOAM_PDUS_SENT = "soamPdusSent";
    private static final String SOAM_PDUS_RECEIVED = "soamPdusReceived";

    @Override
    public ObjectNode encode(DelayMeasurementStat dmStat, CodecContext context) {
        checkNotNull(dmStat, "DM stat cannot be null");
        ObjectNode result = context.mapper().createObjectNode()
                .put("elapsedTime", dmStat.elapsedTime().toString())
                .put("suspectStatus", String.valueOf(dmStat.suspectStatus()));

        if (dmStat.frameDelayTwoWayMin() != null) {
            result = result.put("frameDelayTwoWayMin",
                    dmStat.frameDelayTwoWayMin().toString());
        }
        if (dmStat.frameDelayTwoWayMax() != null) {
            result = result.put("frameDelayTwoWayMax",
                    dmStat.frameDelayTwoWayMax().toString());
        }
        if (dmStat.frameDelayTwoWayAvg() != null) {
            result = result.put("frameDelayTwoWayAvg",
                    dmStat.frameDelayTwoWayAvg().toString());
        }
        if (dmStat.frameDelayTwoWayBins() != null) {
            result = (ObjectNode) result.set("frameDelayTwoWayBins",
                    encode(dmStat.frameDelayTwoWayBins(), context));
        }
        if (dmStat.frameDelayForwardMin() != null) {
            result = result.put("frameDelayForwardMin",
                    dmStat.frameDelayForwardMin().toString());
        }
        if (dmStat.frameDelayForwardMax() != null) {
            result = result.put("frameDelayForwardMax",
                    dmStat.frameDelayForwardMax().toString());
        }
        if (dmStat.frameDelayForwardAvg() != null) {
            result = result.put("frameDelayForwardAvg",
                    dmStat.frameDelayForwardAvg().toString());
        }
        if (dmStat.frameDelayForwardBins() != null) {
            result = (ObjectNode) result.set("frameDelayForwardBins",
                    encode(dmStat.frameDelayForwardBins(), context));
        }
        if (dmStat.frameDelayBackwardMin() != null) {
            result = result.put("frameDelayBackwardMin",
                    dmStat.frameDelayBackwardMin().toString());
        }
        if (dmStat.frameDelayBackwardMax() != null) {
            result = result.put("frameDelayBackwardMax",
                    dmStat.frameDelayBackwardMax().toString());
        }
        if (dmStat.frameDelayBackwardAvg() != null) {
            result = result.put("frameDelayBackwardAvg",
                    dmStat.frameDelayBackwardAvg().toString());
        }
        if (dmStat.frameDelayBackwardBins() != null) {
            result = (ObjectNode) result.set("frameDelayBackwardBins",
                    encode(dmStat.frameDelayBackwardBins(), context));
        }
        if (dmStat.interFrameDelayVariationTwoWayMin() != null) {
            result = result.put("interFrameDelayVariationTwoWayMin",
                    dmStat.interFrameDelayVariationTwoWayMin().toString());
        }
        if (dmStat.interFrameDelayVariationTwoWayMax() != null) {
            result.put("interFrameDelayVariationTwoWayMax",
                    dmStat.interFrameDelayVariationTwoWayMax().toString());
        }
        if (dmStat.interFrameDelayVariationTwoWayAvg() != null) {
            result.put("interFrameDelayVariationTwoWayAvg",
                    dmStat.interFrameDelayVariationTwoWayAvg().toString());
        }
        if (dmStat.interFrameDelayVariationTwoWayBins() != null) {
            result = (ObjectNode) result.set("interFrameDelayVariationTwoWayBins",
                    encode(dmStat.interFrameDelayVariationTwoWayBins(), context));
        }
        if (dmStat.interFrameDelayVariationForwardMin() != null) {
            result = result.put("interFrameDelayVariationForwardMin",
                    dmStat.interFrameDelayVariationForwardMin().toString());
        }
        if (dmStat.interFrameDelayVariationForwardMax() != null) {
            result = result.put("interFrameDelayVariationForwardMax",
                    dmStat.interFrameDelayVariationForwardMax().toString());
        }
        if (dmStat.interFrameDelayVariationForwardAvg() != null) {
            result = result.put("interFrameDelayVariationForwardAvg",
                    dmStat.interFrameDelayVariationForwardAvg().toString());
        }
        if (dmStat.interFrameDelayVariationForwardBins() != null) {
            result = (ObjectNode) result.set("interFrameDelayVariationForwardBins",
                    encode(dmStat.interFrameDelayVariationForwardBins(), context));
        }
        if (dmStat.interFrameDelayVariationBackwardMin() != null) {
            result = result.put("interFrameDelayVariationBackwardMin",
                    dmStat.interFrameDelayVariationBackwardMin().toString());
        }
        if (dmStat.interFrameDelayVariationBackwardMax() != null) {
            result = result.put("interFrameDelayVariationBackwardMax",
                    dmStat.interFrameDelayVariationBackwardMax().toString());
        }
        if (dmStat.interFrameDelayVariationBackwardAvg() != null) {
            result = result.put("interFrameDelayVariationBackwardAvg",
                    dmStat.interFrameDelayVariationBackwardAvg().toString());
        }
        if (dmStat.interFrameDelayVariationBackwardBins() != null) {
            result = (ObjectNode) result.set("interFrameDelayVariationBackwardBins",
                    encode(dmStat.interFrameDelayVariationBackwardBins(), context));
        }
        if (dmStat.frameDelayRangeTwoWayMax() != null) {
            result = result.put("frameDelayRangeTwoWayMax",
                    dmStat.frameDelayRangeTwoWayMax().toString());
        }
        if (dmStat.frameDelayRangeTwoWayAvg() != null) {
            result = result.put("frameDelayRangeTwoWayAvg",
                    dmStat.frameDelayRangeTwoWayAvg().toString());
        }
        if (dmStat.frameDelayRangeTwoWayBins() != null) {
            result = (ObjectNode) result.set("frameDelayRangeTwoWayBins",
                    encode(dmStat.frameDelayRangeTwoWayBins(), context));
        }
        if (dmStat.frameDelayRangeForwardMax() != null) {
            result = result.put("frameDelayRangeForwardMax",
                    dmStat.frameDelayRangeForwardMax().toString());
        }
        if (dmStat.frameDelayRangeForwardAvg() != null) {
            result = result.put("frameDelayRangeForwardAvg",
                    dmStat.frameDelayRangeForwardAvg().toString());
        }
        if (dmStat.frameDelayRangeForwardBins() != null) {
            result = (ObjectNode) result.set("frameDelayRangeForwardBins",
                    encode(dmStat.frameDelayRangeForwardBins(), context));
        }
        if (dmStat.frameDelayRangeBackwardMax() != null) {
            result = result.put("frameDelayRangeBackwardMax",
                    dmStat.frameDelayRangeBackwardMax().toString());
        }
        if (dmStat.frameDelayRangeBackwardAvg() != null) {
            result = result.put("frameDelayRangeBackwardAvg",
                    dmStat.frameDelayRangeBackwardAvg().toString());
        }
        if (dmStat.frameDelayRangeBackwardBins() != null) {
            result = (ObjectNode) result.set("frameDelayRangeBackwardBins",
                    encode(dmStat.frameDelayRangeBackwardBins(), context));
        }

        if (dmStat.soamPdusReceived() != null) {
            result = result.put(SOAM_PDUS_RECEIVED, dmStat.soamPdusReceived().toString());
        }

        if (dmStat.soamPdusSent() != null) {
            result = result.put(SOAM_PDUS_SENT, dmStat.soamPdusSent().toString());
        }

        return result;
    }

    private ObjectNode encode(Map<Duration, Integer> bins, CodecContext context) {
        checkNotNull(bins, "Bins cannot be null");
        ArrayNode binsResult = context.mapper().createArrayNode();
        bins.keySet().forEach(lwrLimit -> binsResult.add(encode(lwrLimit, bins.get(lwrLimit), context)));

        return (ObjectNode) context.mapper().createObjectNode().set(BINS, binsResult);
    }

    private ObjectNode encode(Duration duration, Integer count, CodecContext context) {
        return context.mapper().createObjectNode()
            .put(LOWER_LIMIT, duration.toString())
            .put(COUNT, count);
    }
}
