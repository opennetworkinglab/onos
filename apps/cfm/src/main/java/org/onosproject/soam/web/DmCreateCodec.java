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
import static org.onlab.util.Tools.nullIsIllegal;

import java.time.Duration;

import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.incubator.net.l2monitoring.cfm.Mep.Priority;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.soam.SoamConfigException;
import org.onosproject.incubator.net.l2monitoring.soam.StartTime;
import org.onosproject.incubator.net.l2monitoring.soam.StopTime;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DefaultDelayMeasurementCreate;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementCreate;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementCreate.DmCreateBuilder;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementCreate.DmType;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementCreate.MeasurementOption;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementCreate.Version;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Encode and decode to/from JSON to DelayMeasurementCreate object.
 */
public class DmCreateCodec extends JsonCodec<DelayMeasurementCreate> {

    private static final String VERSION = "version";
    private static final String DM = "dm";
    private static final String DM_CFG_TYPE = "dmCfgType";
    private static final String DMDMM = "DMDMM";
    private static final String REMOTE_MEP_ID = "remoteMepId";
    private static final String PRIORITY = "priority";
    private static final String MEASUREMENTS_ENABLED = "measurementsEnabled";
    private static final String BINS_PER_FD_INTERVAL = "binsPerFdInterval";
    private static final String BINS_PER_IFDV_INTERVAL = "binsPerIfdvInterval";
    private static final String IFDV_SELECTION_OFFSET = "ifdvSelectionOffset";
    private static final String BINS_PER_FDR_INTERVAL = "binsPerFdrInterval";
    private static final String FRAME_SIZE = "frameSize";
    private static final String MESSAGE_PERIOD_MS = "messagePeriodMs";
    private static final String MEASUREMENT_INTERVAL_MINS = "measurementIntervalMins";
    private static final String ALIGN_MEASUREMENT_INTERVALS = "alignMeasurementIntervals";
    private static final String ALIGN_MEASUREMENT_OFFSET_MINS = "alignMeasurementOffsetMins";
    private static final String START_TIME = "startTime";
    private static final String STOP_TIME = "stopTime";

    @Override
    public DelayMeasurementCreate decode(ObjectNode json,
            CodecContext context) {

        if (json == null || !json.isObject()) {
            return null;
        }

        JsonNode dmNode = json.get(DM);
        Version version = Version.Y17312011;
        if (dmNode.get(VERSION) != null) {
            version = Version.valueOf(dmNode.get(VERSION).asText());
        }
        DmType dmCfgType = DmType.DMDMM;
        if (dmNode.get(DM_CFG_TYPE) != null) {
            dmCfgType = DmType.valueOf(dmNode.get(DM_CFG_TYPE).asText(DMDMM));
        }
        MepId remoteMepId = MepId.valueOf(
                nullIsIllegal(dmNode.get(REMOTE_MEP_ID), REMOTE_MEP_ID + " is required")
                .shortValue());
        Priority prio = Priority.valueOf(nullIsIllegal(dmNode.get(PRIORITY),
                PRIORITY + " is required in the format 'PRIOn'").asText());

        try {
            DmCreateBuilder builder = DefaultDelayMeasurementCreate
                    .builder(dmCfgType, version, remoteMepId, prio);

            if (dmNode.get(MEASUREMENTS_ENABLED) != null) {
                context.codec(MeasurementOption.class)
                    .decode((ArrayNode) (dmNode.get(MEASUREMENTS_ENABLED)), context)
                    .forEach(builder::addToMeasurementsEnabled);
            }

            if (dmNode.get(BINS_PER_FD_INTERVAL) != null) {
                builder = builder.binsPerFdInterval(
                        (short) dmNode.get(BINS_PER_FD_INTERVAL).asInt());
            }
            if (dmNode.get(BINS_PER_IFDV_INTERVAL) != null) {
                builder = builder.binsPerIfdvInterval(
                        (short) dmNode.get(BINS_PER_IFDV_INTERVAL).asInt());
            }
            if (dmNode.get(IFDV_SELECTION_OFFSET) != null) {
                builder = builder.ifdvSelectionOffset(
                        (short) dmNode.get(IFDV_SELECTION_OFFSET).asInt());
            }
            if (dmNode.get(BINS_PER_FDR_INTERVAL) != null) {
                builder = builder.binsPerFdrInterval(
                        (short) dmNode.get(BINS_PER_FDR_INTERVAL).asInt());
            }
            if (dmNode.get(FRAME_SIZE) != null) {
                builder = (DmCreateBuilder) builder.frameSize(
                        (short) dmNode.get(FRAME_SIZE).asInt());
            }
            if (dmNode.get(MESSAGE_PERIOD_MS) != null) {
                builder = (DmCreateBuilder) builder.messagePeriod(Duration.ofMillis(
                        dmNode.get(MESSAGE_PERIOD_MS).asInt()));
            }
            if (dmNode.get(MEASUREMENT_INTERVAL_MINS) != null) {
                builder = (DmCreateBuilder) builder.measurementInterval(
                        Duration.ofMinutes(
                        dmNode.get(MEASUREMENT_INTERVAL_MINS).asInt()));
            }
            if (dmNode.get(ALIGN_MEASUREMENT_INTERVALS) != null) {
                builder = (DmCreateBuilder) builder.alignMeasurementIntervals(
                        dmNode.get(ALIGN_MEASUREMENT_INTERVALS).asBoolean());
            }
            if (dmNode.get(ALIGN_MEASUREMENT_OFFSET_MINS) != null) {
                builder = (DmCreateBuilder) builder.alignMeasurementOffset(Duration.ofMinutes(
                        dmNode.get(ALIGN_MEASUREMENT_OFFSET_MINS).asInt()));
            }
            if (dmNode.get(START_TIME) != null) {
                builder = (DmCreateBuilder) builder.startTime(context.codec(StartTime.class)
                .decode((ObjectNode) dmNode.get(START_TIME), context));
            }
            if (dmNode.get(STOP_TIME) != null) {
                builder = (DmCreateBuilder) builder.stopTime(context.codec(StopTime.class)
                .decode((ObjectNode) dmNode.get(STOP_TIME), context));
            }

            return builder.build();
        } catch (SoamConfigException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public ObjectNode encode(DelayMeasurementCreate dm, CodecContext context) {
        checkNotNull(dm, "DM cannot be null");
        ObjectNode result = context.mapper().createObjectNode()
                .put(DM_CFG_TYPE, dm.dmCfgType().name())
                .put(VERSION, dm.version().name())
                .put(REMOTE_MEP_ID, dm.remoteMepId().id())
                .put(PRIORITY, dm.priority().name());

        if (dm.measurementsEnabled() != null) {
            result.set(MEASUREMENTS_ENABLED, new DmMeasurementOptionCodec()
                    .encode(dm.measurementsEnabled(), context));
        }

        if (dm.messagePeriod() != null) {
            result.put(MESSAGE_PERIOD_MS, dm.messagePeriod().toMillis());
        }
        if (dm.frameSize() != null) {
            result.put(FRAME_SIZE, dm.frameSize());
        }


        return result;
    }
}
