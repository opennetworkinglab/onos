/*
 * Copyright 2014-2015 Open Networking Laboratory
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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.meter.Band;
import org.onosproject.net.meter.DefaultMeter;
import org.onosproject.net.meter.Meter;
import org.onosproject.net.meter.MeterId;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;
import static org.slf4j.LoggerFactory.getLogger;


/**
 * Meter JSON codec.
 */
public final class MeterCodec extends JsonCodec<Meter> {
    private final Logger log = getLogger(getClass());

    // JSON field names
    private static final String ID = "id";
    private static final String STATE = "state";
    private static final String LIFE = "life";
    private static final String PACKETS = "packets";
    private static final String BYTES = "bytes";
    private static final String REFERENCE_COUNT = "referenceCount";
    private static final String APP_ID = "appId";
    private static final String BURST = "burst";
    private static final String DEVICE_ID = "deviceId";
    private static final String UNIT = "unit";
    private static final String BANDS = "bands";
    public static final String REST_APP_ID = "org.onosproject.rest";
    private static final String MISSING_MEMBER_MESSAGE = " member is required in Meter";

    @Override
    public ObjectNode encode(Meter meter, CodecContext context) {
        checkNotNull(meter, "Meter cannot be null");
        ObjectNode result = context.mapper().createObjectNode()
                .put(ID, meter.id().toString())
                .put(LIFE, meter.life())
                .put(PACKETS, meter.packetsSeen())
                .put(BYTES, meter.bytesSeen())
                .put(REFERENCE_COUNT, meter.referenceCount())
                .put(UNIT, meter.unit().toString())
                .put(BURST, meter.isBurst())
                .put(DEVICE_ID, meter.deviceId().toString());

        if (meter.appId() != null) {
            result.put(APP_ID, meter.appId().toString());
        }

        if (meter.state() != null) {
            result.put(STATE, meter.state().toString());
        }

        ArrayNode bands = context.mapper().createArrayNode();
        meter.bands().forEach(band -> {
            ObjectNode bandJson = context.codec(Band.class).encode(band, context);
            bands.add(bandJson);
        });
        result.set(BANDS, bands);
        return result;
    }

    @Override
    public Meter decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        final JsonCodec<Band> meterBandCodec = context.codec(Band.class);
        CoreService coreService = context.getService(CoreService.class);

        // parse meter id
        int meterIdInt = nullIsIllegal(json.get(ID), ID + MISSING_MEMBER_MESSAGE).asInt();
        MeterId meterId = MeterId.meterId(meterIdInt);

        // parse device id
        DeviceId deviceId = DeviceId.deviceId(nullIsIllegal(json.get(DEVICE_ID),
                DEVICE_ID + MISSING_MEMBER_MESSAGE).asText());

        // application id
        ApplicationId appId = coreService.registerApplication(REST_APP_ID);

        // parse burst
        boolean burst = false;
        JsonNode burstJson = json.get("burst");
        if (burstJson != null) {
            burst = burstJson.asBoolean();
        }

        // parse unit type
        String unit = nullIsIllegal(json.get(UNIT), UNIT + MISSING_MEMBER_MESSAGE).asText();
        Meter.Unit meterUnit;

        switch (unit) {
            case "KB_PER_SEC":
                meterUnit = Meter.Unit.KB_PER_SEC;
                break;
            case "PKTS_PER_SEC":
                meterUnit = Meter.Unit.PKTS_PER_SEC;
                break;
            default:
                log.warn("The requested unit {} is not defined for meter.", unit);
                return null;
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

        Meter meter;
        if (burst) {
            meter = DefaultMeter.builder()
                    .withId(meterId)
                    .fromApp(appId)
                    .forDevice(deviceId)
                    .withUnit(meterUnit)
                    .withBands(bandList)
                    .burst().build();
        } else {
            meter = DefaultMeter.builder()
                    .withId(meterId)
                    .fromApp(appId)
                    .forDevice(deviceId)
                    .withUnit(meterUnit)
                    .withBands(bandList).build();
        }

        return meter;
    }
}
