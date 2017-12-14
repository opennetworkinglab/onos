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
package org.onosproject.incubator.protobuf.models.net.meter;

import org.onosproject.core.ApplicationId;
import org.onosproject.grpc.net.meter.models.MeterEnumsProto;
import org.onosproject.grpc.net.meter.models.MeterRequestProtoOuterClass;
import org.onosproject.incubator.protobuf.models.core.ApplicationIdProtoTranslator;
import org.onosproject.net.DeviceId;
import org.onosproject.net.meter.Band;
import org.onosproject.net.meter.DefaultMeterRequest;
import org.onosproject.net.meter.Meter;
import org.onosproject.net.meter.MeterRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Optional;

/**
 * gRPC message conversion related utilities for meter request service.
 */
public final class MeterRequestProtoTranslator {

    private static final Logger log = LoggerFactory.getLogger(MeterRequestProtoTranslator.class);

    /**
     * Translates gRPC MeterRequestType to {@link MeterRequest.Type}.
     *
     * @param type gRPC message
     * @return {@link MeterRequest.Type}
     */
    public static Optional<Object> translate(MeterEnumsProto.MeterRequestTypeProto type) {
        switch (type) {
            case ADD:
                return Optional.of(MeterRequest.Type.ADD);
            case MODIFY:
                return Optional.of(MeterRequest.Type.MODIFY);
            case REMOVE:
                return Optional.of(MeterRequest.Type.REMOVE);
            default:
                log.warn("Unrecognized MeterRequest type gRPC message: {}", type);
                return Optional.empty();
        }
    }

    /**
     * Translates gRPC MeterRequest to {@link MeterRequest}.
     *
     * @param meterRequest gRPC message
     * @return {@link MeterRequest}
     */
    public static MeterRequest translate(MeterRequestProtoOuterClass.MeterRequestProto meterRequest) {

        DeviceId deviceid = DeviceId.deviceId(meterRequest.getDeviceId());
        ApplicationId appId = ApplicationIdProtoTranslator.translate(meterRequest.getApplicationId());
        Meter.Unit unit = MeterEnumsProtoTranslator.translate(meterRequest.getUnit()).get();
        boolean burst = meterRequest.getIsBurst();
        Collection<Band> bands = BandProtoTranslator.translate(meterRequest.getBandsList());
        MeterRequest.Type type = (MeterRequest.Type) translate(meterRequest.getType()).get();
        if (type == MeterRequest.Type.ADD) {
            return DefaultMeterRequest.builder()
                    .forDevice(deviceid)
                    .fromApp(appId)
                    .withUnit(unit)
                    .withBands(bands)
                    .add();
        } else {
            return DefaultMeterRequest.builder()
                    .forDevice(deviceid)
                    .fromApp(appId)
                    .withUnit(unit)
                    .withBands(bands)
                    .remove();
        }
    }

    // Utility class not intended for instantiation.
    private MeterRequestProtoTranslator() {}
}
