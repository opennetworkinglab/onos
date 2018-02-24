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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.incubator.net.l2monitoring.soam.MilliPct;
import org.onosproject.incubator.net.l2monitoring.soam.SoamId;
import org.onosproject.incubator.net.l2monitoring.soam.loss.LossMeasurementThreshold;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

import static org.onosproject.incubator.net.l2monitoring.soam.loss.DefaultLmThreshold.*;

/**
 * Encode and decode to/from JSON to LossMeasurementThreshold object.
 */
public class LossMeasurementThresholdCodec extends JsonCodec<LossMeasurementThreshold> {
    static final String MEASUREDFLRFORWARD = "measuredFlrForward";
    static final String MAXFLRFORWARD = "maxFlrForward";
    static final String AVERAGEFLRFORWARD = "averageFlrForward";
    static final String MEASUREDFLRBACKWARD = "measuredFlrBackward";
    static final String MAXFLRBACKWARD = "maxFlrBackward";
    static final String AVERAGEFLRBACKWARD = "averageFlrBackward";
    static final String FORWARDHIGHLOSS = "forwardHighLoss";
    static final String FORWARDCONSECUTIVEHIGHLOSS = "forwardConsecutiveHighLoss";
    static final String BACKWARDHIGHLOSS = "backwardHighLoss";
    static final String BACKWARDCONSECUTIVEHIGHLOSS = "backwardConsecutiveHighLoss";
    static final String FORWARDUNAVAILABLECOUNT = "forwardUnavailableCount";
    static final String FORWARDAVAILABLERATIO = "forwardAvailableRatio";
    static final String BACKWARDUNAVAILABLECOUNT = "backwardUnavailableCount";
    static final String BACKWARDAVAILABLERATIO = "backwardAvailableRatio";
    static final String THRESHOLDOPTIONS = "thresholdOptions";

    @Override
    public ObjectNode encode(LossMeasurementThreshold lmt, CodecContext context) {
        checkNotNull(lmt, "LM thresholds cannot be null");
        ObjectNode result = context.mapper().createObjectNode()
                .put("id", lmt.thresholdId().value());

        if (lmt.thresholds() != null) {
            result.set(THRESHOLDOPTIONS, new LmThresholdOptionCodec()
                    .encode(lmt.thresholds(), context));
        }

        if (lmt.measuredFlrForward() != null) {
            result.put(MEASUREDFLRFORWARD, lmt.measuredFlrForward().percentValue());
        }
        if (lmt.maxFlrForward() != null) {
            result.put(MAXFLRFORWARD, lmt.maxFlrForward().percentValue());
        }
        if (lmt.averageFlrForward() != null) {
            result.put(AVERAGEFLRFORWARD, lmt.averageFlrForward().percentValue());
        }
        if (lmt.measuredFlrBackward() != null) {
            result.put(MEASUREDFLRBACKWARD, lmt.measuredFlrBackward().percentValue());
        }
        if (lmt.maxFlrBackward() != null) {
            result.put(MAXFLRBACKWARD, lmt.maxFlrBackward().percentValue());
        }
        if (lmt.averageFlrBackward() != null) {
            result.put(AVERAGEFLRBACKWARD, lmt.averageFlrBackward().percentValue());
        }
        if (lmt.forwardHighLoss() != null) {
            result.put(FORWARDHIGHLOSS, lmt.forwardHighLoss().longValue());
        }
        if (lmt.forwardConsecutiveHighLoss() != null) {
            result.put(FORWARDCONSECUTIVEHIGHLOSS, lmt.measuredFlrForward().longValue());
        }
        if (lmt.backwardHighLoss() != null) {
            result.put(BACKWARDHIGHLOSS, lmt.backwardHighLoss().longValue());
        }
        if (lmt.backwardConsecutiveHighLoss() != null) {
            result.put(BACKWARDCONSECUTIVEHIGHLOSS, lmt.backwardConsecutiveHighLoss().longValue());
        }
        if (lmt.forwardUnavailableCount() != null) {
            result.put(FORWARDUNAVAILABLECOUNT, lmt.forwardUnavailableCount().longValue());
        }
        if (lmt.forwardAvailableRatio() != null) {
            result.put(FORWARDAVAILABLERATIO, lmt.forwardAvailableRatio().percentValue());
        }
        if (lmt.backwardUnavailableCount() != null) {
            result.put(BACKWARDUNAVAILABLECOUNT, lmt.backwardUnavailableCount().longValue());
        }
        if (lmt.backwardAvailableRatio() != null) {
            result.put(BACKWARDAVAILABLERATIO, lmt.backwardAvailableRatio().percentValue());
        }

        return result;
    }

    @Override
    public List<LossMeasurementThreshold> decode(ArrayNode json, CodecContext context) {
        if (json == null) {
            return null;
        }
        List<LossMeasurementThreshold> thrList = new ArrayList<>();
        json.forEach(node -> thrList.add(decode((ObjectNode) node, context)));
        return thrList;
    }

    @Override
    public LossMeasurementThreshold decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        JsonNode thrNode = json.get("threshold");

        SoamId thresholdId = SoamId.valueOf(nullIsIllegal(thrNode.get("id"),
                "thresholdId must not be null").asInt());
        LossMeasurementThreshold.LmThresholdBuilder builder = builder(thresholdId);

        if (thrNode.get("thresholds") != null) {
            context.codec(LossMeasurementThreshold.ThresholdOption.class)
                    .decode((ArrayNode) (thrNode.get("thresholds")), context)
                    .forEach(builder::addToThreshold);
        }

        if (thrNode.get(MEASUREDFLRFORWARD) != null) {
            builder.measuredFlrForward(MilliPct.ofPercent(
                    (float) thrNode.get(MEASUREDFLRFORWARD).asDouble()));
        }
        if (thrNode.get(MAXFLRFORWARD) != null) {
            builder.maxFlrForward(MilliPct.ofPercent(
                    (float) thrNode.get(MAXFLRFORWARD).asDouble()));
        }
        if (thrNode.get(AVERAGEFLRFORWARD) != null) {
            builder.averageFlrForward(MilliPct.ofPercent(
                    (float) thrNode.get(AVERAGEFLRFORWARD).asDouble()));
        }
        if (thrNode.get(MEASUREDFLRBACKWARD) != null) {
            builder.measuredFlrBackward(MilliPct.ofPercent(
                    (float) thrNode.get(MEASUREDFLRBACKWARD).asDouble()));
        }
        if (thrNode.get(MAXFLRBACKWARD) != null) {
            builder.maxFlrBackward(MilliPct.ofPercent(
                    (float) thrNode.get(MAXFLRBACKWARD).asDouble()));
        }
        if (thrNode.get(AVERAGEFLRBACKWARD) != null) {
            builder.averageFlrBackward(MilliPct.ofPercent(
                    (float) thrNode.get(AVERAGEFLRBACKWARD).asDouble()));
        }
        if (thrNode.get(FORWARDHIGHLOSS) != null) {
            builder.forwardHighLoss(thrNode.get(FORWARDHIGHLOSS).asLong());
        }
        if (thrNode.get(FORWARDCONSECUTIVEHIGHLOSS) != null) {
            builder.forwardConsecutiveHighLoss(thrNode.get(FORWARDCONSECUTIVEHIGHLOSS).asLong());
        }
        if (thrNode.get(BACKWARDHIGHLOSS) != null) {
            builder.backwardHighLoss(thrNode.get(BACKWARDHIGHLOSS).asLong());
        }
        if (thrNode.get(BACKWARDCONSECUTIVEHIGHLOSS) != null) {
            builder.backwardConsecutiveHighLoss(thrNode.get(BACKWARDCONSECUTIVEHIGHLOSS).asLong());
        }
        if (thrNode.get(FORWARDUNAVAILABLECOUNT) != null) {
            builder.forwardUnavailableCount(thrNode.get(FORWARDUNAVAILABLECOUNT).asLong());
        }
        if (thrNode.get(FORWARDAVAILABLERATIO) != null) {
            builder.forwardAvailableRatio(MilliPct.ofPercent(
                    (float) thrNode.get(FORWARDAVAILABLERATIO).asDouble()));
        }
        if (thrNode.get(BACKWARDUNAVAILABLECOUNT) != null) {
            builder.backwardUnavailableCount(thrNode.get(BACKWARDUNAVAILABLECOUNT).asLong());
        }
        if (thrNode.get(BACKWARDAVAILABLERATIO) != null) {
            builder.backwardAvailableRatio(MilliPct.ofPercent(
                    (float) thrNode.get(BACKWARDAVAILABLERATIO).asDouble()));
        }

        return builder.build();
    }
}
