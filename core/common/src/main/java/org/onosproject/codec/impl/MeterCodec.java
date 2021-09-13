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
package org.onosproject.codec.impl;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.meter.Band;
import org.onosproject.net.meter.Meter;

import static com.google.common.base.Preconditions.checkNotNull;


/**
 * Meter JSON codec.
 */
public final class MeterCodec extends JsonCodec<Meter> {

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

    @Override
    public ObjectNode encode(Meter meter, CodecContext context) {
        checkNotNull(meter, "Meter cannot be null");
        ObjectNode result = context.mapper().createObjectNode()
                .put(ID, meter.meterCellId().toString())
                .put(LIFE, meter.life())
                .put(PACKETS, meter.packetsSeen())
                .put(BYTES, meter.bytesSeen())
                .put(REFERENCE_COUNT, meter.referenceCount())
                .put(UNIT, meter.unit().toString())
                .put(BURST, meter.isBurst())
                .put(DEVICE_ID, meter.deviceId().toString());

        if (meter.appId() != null) {
            result.put(APP_ID, meter.appId().name());
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
}
