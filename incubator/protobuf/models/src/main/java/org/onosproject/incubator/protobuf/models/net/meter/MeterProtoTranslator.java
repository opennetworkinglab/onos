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

import com.google.common.annotations.Beta;
import org.onosproject.grpc.net.meter.models.BandProtoOuterClass.BandProto;
import org.onosproject.grpc.net.meter.models.MeterProtoOuterClass.MeterProto;
import org.onosproject.incubator.protobuf.models.core.ApplicationIdProtoTranslator;
import org.onosproject.net.meter.Meter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

/**
 * gRPC message conversion related utilities for meter service.
 */
@Beta
public final class MeterProtoTranslator {
    private static final Logger log = LoggerFactory.getLogger(MeterProtoTranslator.class);

    /**
     * Translates {@link Meter} to gRPC MeterCore message.
     *
     * @param meter {@link Meter}
     * @return gRPC MeterCore message
     */
    public static MeterProto translate(Meter meter) {
        return MeterProto.newBuilder()
                .setDeviceId(meter.deviceId().toString())
                .setApplicationId(ApplicationIdProtoTranslator.translate(meter.appId()))
                .setUnit(MeterEnumsProtoTranslator.translate(meter.unit()))
                .setIsBurst(meter.isBurst())
                .addAllBands(meter.bands().stream()
                        .map(b -> BandProto.newBuilder()
                                .setRate(b.rate())
                                .setBurst(b.burst())
                                .setDropPrecedence(b.dropPrecedence())
                                .setType(BandProtoTranslator.translate(b.type()))
                                .setPackets(b.packets())
                                .setBytes(b.bytes())
                                .build())
                        .collect(Collectors.toList()))
                .setState(MeterEnumsProtoTranslator.translate(meter.state()))
                .setLife(meter.life())
                .setReferenceCount(meter.referenceCount())
                .setPacketsSeen(meter.packetsSeen())
                .setBytesSeen(meter.bytesSeen())
                .build();
    }

    // Utility class not intended for instantiation.
    private MeterProtoTranslator() {}
}
