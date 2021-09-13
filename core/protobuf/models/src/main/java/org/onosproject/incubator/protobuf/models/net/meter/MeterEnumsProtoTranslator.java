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

import org.onosproject.grpc.net.meter.models.MeterEnumsProto;
import org.onosproject.net.meter.Meter;
import org.onosproject.net.meter.MeterState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * gRPC message conversion related utilities for meter enums.
 */
public final class MeterEnumsProtoTranslator {

    private static final Logger log = LoggerFactory.getLogger(MeterEnumsProtoTranslator.class);

    /**
     * Translates gRPC enum MeterUnit to ONOS enum.
     *
     * @param unit meterUnit in gRPC enum
     * @return equivalent in ONOS enum
     */
    public static Optional<Meter.Unit> translate(MeterEnumsProto.MeterUnitProto unit) {
        switch (unit) {
            case PKTS_PER_SEC:
                return Optional.of(Meter.Unit.PKTS_PER_SEC);
            case KB_PER_SEC:
                return Optional.of(Meter.Unit.KB_PER_SEC);
            case BYTES_PER_SEC:
                return Optional.of(Meter.Unit.BYTES_PER_SEC);
            default:
                log.warn("Unrecognized MeterUnit gRPC message: {}", unit);
                return Optional.empty();
        }
    }

    /**
     * Translates ONOS enum Meter.Unit Type to gRPC enum.
     *
     * @param unit Meter.Unit in ONOS enum
     * @return equivalent in gRPC enum
     */
    public static MeterEnumsProto.MeterUnitProto translate(Meter.Unit unit) {
        switch (unit) {
            case PKTS_PER_SEC:
                return MeterEnumsProto.MeterUnitProto.PKTS_PER_SEC;
            case KB_PER_SEC:
                return MeterEnumsProto.MeterUnitProto.KB_PER_SEC;
            case BYTES_PER_SEC:
                return MeterEnumsProto.MeterUnitProto.BYTES_PER_SEC;
            default:
                log.warn("Unrecognized MeterUnit ONOS message: {}", unit);
                return MeterEnumsProto.MeterUnitProto.UNRECOGNIZED;
        }
    }

    /**
     * Translates ONOS enum MeterState Type to gRPC enum.
     *
     * @param meterState MeterState in ONOS enum
     * @return equivalent in gRPC enum
     */
    public static MeterEnumsProto.MeterStateProto translate(MeterState meterState) {
        switch (meterState) {
            case PENDING_ADD:
                return MeterEnumsProto.MeterStateProto.PENDING_ADD;
            case ADDED:
                return MeterEnumsProto.MeterStateProto.ADDED;
            case PENDING_REMOVE:
                return MeterEnumsProto.MeterStateProto.PENDING_REMOVE;
            default:
                log.warn("Unrecognized MeterState ONOS message: {}", meterState);
                return MeterEnumsProto.MeterStateProto.UNRECOGNIZED;
        }
    }

    // Utility class not intended for instantiation.
    private MeterEnumsProtoTranslator() {}
}
