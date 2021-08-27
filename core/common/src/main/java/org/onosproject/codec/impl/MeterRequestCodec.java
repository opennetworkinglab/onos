/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.codec.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.meter.Band;
import org.onosproject.net.meter.DefaultMeterRequest;
import org.onosproject.net.meter.Meter;
import org.onosproject.net.meter.MeterRequest;
import org.onosproject.net.meter.MeterScope;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * MeterRequest JSON codec.
 */
public final class MeterRequestCodec extends JsonCodec<MeterRequest> {

    // JSON field names
    private static final String DEVICE_ID = "deviceId";
    private static final String UNIT = "unit";
    private static final String BANDS = "bands";
    private static final String SCOPE = "scope";
    private static final String INDEX = "index";
    private static final String REST_APP_ID = "org.onosproject.rest";
    private static final String MISSING_MEMBER_MESSAGE = " member is required in MeterRequest";

    private ApplicationId applicationId;

    @Override
    public MeterRequest decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        final JsonCodec<Band> meterBandCodec = context.codec(Band.class);


        // parse device id
        DeviceId deviceId = DeviceId.deviceId(nullIsIllegal(json.get(DEVICE_ID),
                DEVICE_ID + MISSING_MEMBER_MESSAGE).asText());

        // application id
        if (applicationId == null) {
            CoreService coreService = context.getService(CoreService.class);
            applicationId = coreService.registerApplication(REST_APP_ID);
        }

        // parse burst
        boolean burst = false;
        JsonNode burstJson = json.get("burst");
        if (burstJson != null) {
            burst = burstJson.asBoolean();
        }

        // parse unit type
        String unit = nullIsIllegal(json.get(UNIT), UNIT + MISSING_MEMBER_MESSAGE).asText();
        Meter.Unit meterUnit = null;

        switch (unit) {
            case "KB_PER_SEC":
                meterUnit = Meter.Unit.KB_PER_SEC;
                break;
            case "PKTS_PER_SEC":
                meterUnit = Meter.Unit.PKTS_PER_SEC;
                break;
            case "BYTES_PER_SEC":
                meterUnit = Meter.Unit.BYTES_PER_SEC;
                break;
            default:
                nullIsIllegal(meterUnit, "The requested unit " + unit + " is not defined for meter.");
        }

        // parse meter bands
        List<Band> bandList = new ArrayList<>();
        JsonNode bandsJson = json.get(BANDS);
        checkNotNull(bandsJson);
        if (bandsJson != null) {
            IntStream.range(0, bandsJson.size()).forEach(i -> {
                ObjectNode bandJson = get(bandsJson, i);
                bandList.add(meterBandCodec.decode(bandJson, context));
            });
        }

        // parse scope and index
        JsonNode scopeJson = json.get(SCOPE);
        MeterScope scope = null;
        if (scopeJson != null && !isNullOrEmpty(scopeJson.asText())) {
            scope = MeterScope.of(scopeJson.asText());
        }

        JsonNode indexJson = json.get(INDEX);
        Long index = null;
        if (indexJson != null && !isNullOrEmpty(indexJson.asText()) && scope != null) {
            index = indexJson.asLong();
        }

        // build the final request
        MeterRequest.Builder meterRequest = DefaultMeterRequest.builder();
        if (scope != null) {
            meterRequest.withScope(scope);
        }

        if (index != null) {
            meterRequest.withIndex(index);
        }

        meterRequest.fromApp(applicationId)
                .forDevice(deviceId)
                .withUnit(meterUnit)
                .withBands(bandList);

        if (burst) {
            meterRequest.burst();
        }

        return meterRequest.add();
    }

}
