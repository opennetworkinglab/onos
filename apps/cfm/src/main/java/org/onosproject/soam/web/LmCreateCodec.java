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
import org.onosproject.incubator.net.l2monitoring.cfm.Mep.Priority;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.soam.MilliPct;
import org.onosproject.incubator.net.l2monitoring.soam.SoamConfigException;
import org.onosproject.incubator.net.l2monitoring.soam.StartTime;
import org.onosproject.incubator.net.l2monitoring.soam.StopTime;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementCreate.Version;
import org.onosproject.incubator.net.l2monitoring.soam.loss.DefaultLmCreate;
import org.onosproject.incubator.net.l2monitoring.soam.loss.LossMeasurementCreate;
import org.onosproject.incubator.net.l2monitoring.soam.loss.LossMeasurementThreshold;

import java.time.Duration;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;
import static org.onosproject.incubator.net.l2monitoring.soam.loss.LossMeasurementCreate.CounterOption;
import static org.onosproject.incubator.net.l2monitoring.soam.loss.LossMeasurementCreate.LmType;
import static org.onosproject.incubator.net.l2monitoring.soam.loss.LossMeasurementCreate.LmCreateBuilder;

/**
 * Encode and decode to/from JSON to LossMeasurementCreate object.
 */
public class LmCreateCodec extends JsonCodec<LossMeasurementCreate> {

    public static final String LM = "lm";
    public static final String VERSION = "version";
    public static final String LM_CFG_TYPE = "lmCfgType";
    public static final String LMLMM = "LMLMM";
    public static final String REMOTE_MEP_ID = "remoteMepId";
    public static final String PRIORITY = "priority";
    public static final String COUNTERS_ENABLED = "countersEnabled";
    public static final String THRESHOLDS = "thresholds";
    public static final String AVAILABILITY_MEASUREMENT_INTERVAL_MINS =
                                "availabilityMeasurementIntervalMins";
    public static final String AVAILABILITY_NUMBER_CONSECUTIVE_FLR_MEASUREMENTS =
                                "availabilityNumberConsecutiveFlrMeasurements";
    public static final String AVAILABILITY_FLR_THRESHOLD_PCT =
                                "availabilityFlrThresholdPct";
    public static final String AVAILABILITY_NUMBER_CONSECUTIVE_INTERVALS =
                                "availabilityNumberConsecutiveIntervals";
    public static final String AVAILABILITY_NUMBER_CONSECUTIVE_HIGH_FLR =
                                "availabilityNumberConsecutiveHighFlr";
    public static final String FRAME_SIZE = "frameSize";
    public static final String MESSAGE_PERIOD_MS = "messagePeriodMs";
    public static final String MEASUREMENT_INTERVAL_MINS =
                                "measurementIntervalMins";
    public static final String ALIGN_MEASUREMENT_INTERVALS =
                                "alignMeasurementIntervals";
    public static final String ALIGN_MEASUREMENT_OFFSET_MINS =
                                "alignMeasurementOffsetMins";
    public static final String START_TIME = "startTime";
    public static final String STOP_TIME = "stopTime";

    @Override
    public LossMeasurementCreate decode(ObjectNode json,
            CodecContext context) {

        if (json == null || !json.isObject()) {
            return null;
        }

        JsonNode lmNode = json.get(LM);
        Version version = Version.Y17312011;
        if (lmNode.get(VERSION) != null) {
            version = Version.valueOf(lmNode.get(VERSION).asText());
        }
        LmType lmCfgType = LmType.LMLMM;
        if (lmNode.get(LM_CFG_TYPE) != null) {
            lmCfgType = LmType.valueOf(lmNode.get(LM_CFG_TYPE).asText(LMLMM));
        }
        MepId remoteMepId = MepId.valueOf(
                nullIsIllegal(lmNode.get(REMOTE_MEP_ID), REMOTE_MEP_ID + " is required")
                .shortValue());
        Priority prio = Priority.valueOf(nullIsIllegal(lmNode.get(PRIORITY),
                PRIORITY + " is required in the format 'PRIOn'").asText());

        try {
            LmCreateBuilder builder = DefaultLmCreate
                    .builder(version, remoteMepId, prio, lmCfgType);

            if (lmNode.get(COUNTERS_ENABLED) != null) {
                context.codec(CounterOption.class)
                    .decode((ArrayNode) (lmNode.get(COUNTERS_ENABLED)), context)
                    .forEach(builder::addToCountersEnabled);
            }

            if (lmNode.get(THRESHOLDS) != null) {
                context.codec(LossMeasurementThreshold.class)
                        .decode((ArrayNode) (lmNode.get(THRESHOLDS)), context)
                        .forEach(builder::addToLossMeasurementThreshold);
            }

            if (lmNode.get(AVAILABILITY_MEASUREMENT_INTERVAL_MINS) != null) {
                builder = builder.availabilityMeasurementInterval(
                        Duration.ofMinutes(lmNode.get(AVAILABILITY_MEASUREMENT_INTERVAL_MINS).asInt()));
            }
            if (lmNode.get(AVAILABILITY_NUMBER_CONSECUTIVE_FLR_MEASUREMENTS) != null) {
                builder = builder.availabilityNumberConsecutiveFlrMeasurements(
                        lmNode.get(AVAILABILITY_NUMBER_CONSECUTIVE_FLR_MEASUREMENTS).asInt());
            }
            if (lmNode.get(AVAILABILITY_FLR_THRESHOLD_PCT) != null) {
                builder = builder.availabilityFlrThreshold(
                        MilliPct.ofPercent((float) lmNode.get(AVAILABILITY_FLR_THRESHOLD_PCT).asDouble()));
            }
            if (lmNode.get(AVAILABILITY_NUMBER_CONSECUTIVE_INTERVALS) != null) {
                builder = builder.availabilityNumberConsecutiveIntervals(
                        (short) lmNode.get(AVAILABILITY_NUMBER_CONSECUTIVE_INTERVALS).asInt());
            }
            if (lmNode.get(AVAILABILITY_NUMBER_CONSECUTIVE_HIGH_FLR) != null) {
                builder = builder.availabilityNumberConsecutiveHighFlr(
                        (short) lmNode.get(AVAILABILITY_NUMBER_CONSECUTIVE_HIGH_FLR).asInt());
            }
            if (lmNode.get(FRAME_SIZE) != null) {
                builder = (LmCreateBuilder) builder.frameSize(
                        (short) lmNode.get(FRAME_SIZE).asInt());
            }
            if (lmNode.get(MESSAGE_PERIOD_MS) != null) {
                builder = (LmCreateBuilder) builder.messagePeriod(Duration.ofMillis(
                        lmNode.get(MESSAGE_PERIOD_MS).asInt()));
            }
            if (lmNode.get(MEASUREMENT_INTERVAL_MINS) != null) {
                builder = (LmCreateBuilder) builder.measurementInterval(
                        Duration.ofMinutes(
                                lmNode.get(MEASUREMENT_INTERVAL_MINS).asInt()));
            }
            if (lmNode.get(ALIGN_MEASUREMENT_INTERVALS) != null) {
                builder = (LmCreateBuilder) builder.alignMeasurementIntervals(
                        lmNode.get(ALIGN_MEASUREMENT_INTERVALS).asBoolean());
            }
            if (lmNode.get(ALIGN_MEASUREMENT_OFFSET_MINS) != null) {
                builder = (LmCreateBuilder) builder.alignMeasurementOffset(Duration.ofMinutes(
                        lmNode.get(ALIGN_MEASUREMENT_OFFSET_MINS).asInt()));
            }
            if (lmNode.get(START_TIME) != null) {
                builder = (LmCreateBuilder) builder.startTime(context.codec(StartTime.class)
                        .decode((ObjectNode) lmNode.get(START_TIME), context));
            }
            if (lmNode.get(STOP_TIME) != null) {
                builder = (LmCreateBuilder) builder.stopTime(context.codec(StopTime.class)
                        .decode((ObjectNode) lmNode.get(STOP_TIME), context));
            }


            return builder.build();
        } catch (SoamConfigException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public ObjectNode encode(LossMeasurementCreate lm, CodecContext context) {
        checkNotNull(lm, "LM cannot be null");
        ObjectNode result = context.mapper().createObjectNode()
                .put(LM_CFG_TYPE, lm.lmCfgType().name())
                .put(VERSION, lm.version().name())
                .put(REMOTE_MEP_ID, lm.remoteMepId().id())
                .put(PRIORITY, lm.priority().name());

        if (lm.countersEnabled() != null) {
            result.set(COUNTERS_ENABLED, new LmCounterOptionCodec()
                    .encode(lm.countersEnabled(), context));
        }

        if (lm.messagePeriod() != null) {
            result.put(MESSAGE_PERIOD_MS, lm.messagePeriod().toMillis());
        }
        if (lm.frameSize() != null) {
            result.put(FRAME_SIZE, lm.frameSize());
        }


        return result;
    }
}
