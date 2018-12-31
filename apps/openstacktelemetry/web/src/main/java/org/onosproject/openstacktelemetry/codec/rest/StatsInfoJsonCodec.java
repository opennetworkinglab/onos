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
package org.onosproject.openstacktelemetry.codec.rest;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.openstacktelemetry.api.StatsInfo;
import org.onosproject.openstacktelemetry.api.DefaultStatsInfo;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Openstack telemetry codec used for serializing and de-serializing JSON string.
 */
public final class StatsInfoJsonCodec extends JsonCodec<StatsInfo> {

    private static final String STARTUP_TIME = "startupTime";
    private static final String FST_PKT_ARR_TIME = "fstPktArrTime";
    private static final String LST_PKT_OFFSET = "lstPktOffset";
    private static final String PREV_ACC_BYTES = "prevAccBytes";
    private static final String PREV_ACC_PKTS = "prevAccPkts";
    private static final String CURR_ACC_BYTES = "currAccBytes";
    private static final String CURR_ACC_PKTS = "currAccPkts";
    private static final String ERROR_PKTS = "errorPkts";
    private static final String DROP_PKTS = "dropPkts";

    @Override
    public ObjectNode encode(StatsInfo info, CodecContext context) {
        checkNotNull(info, "StatsInfo cannot be null");

        return context.mapper().createObjectNode()
                .put(STARTUP_TIME, info.startupTime())
                .put(FST_PKT_ARR_TIME, info.fstPktArrTime())
                .put(LST_PKT_OFFSET, info.lstPktOffset())
                .put(PREV_ACC_BYTES, info.prevAccBytes())
                .put(PREV_ACC_PKTS, info.prevAccPkts())
                .put(CURR_ACC_BYTES, info.prevAccBytes())
                .put(CURR_ACC_PKTS, info.prevAccPkts())
                .put(ERROR_PKTS, info.errorPkts())
                .put(DROP_PKTS, info.dropPkts());
    }

    @Override
    public StatsInfo decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        return new DefaultStatsInfo.DefaultBuilder()
                .withStartupTime(json.get(STARTUP_TIME).asLong())
                .withFstPktArrTime(json.get(FST_PKT_ARR_TIME).asLong())
                .withLstPktOffset(json.get(LST_PKT_OFFSET).asInt())
                .withPrevAccBytes(json.get(PREV_ACC_BYTES).asLong())
                .withPrevAccPkts(json.get(PREV_ACC_PKTS).asInt())
                .withCurrAccBytes(json.get(CURR_ACC_BYTES).asLong())
                .withCurrAccPkts(json.get(CURR_ACC_PKTS).asInt())
                .withErrorPkts((short) json.get(ERROR_PKTS).asInt())
                .withDropPkts((short) json.get(DROP_PKTS).asInt())
                .build();
    }
}
