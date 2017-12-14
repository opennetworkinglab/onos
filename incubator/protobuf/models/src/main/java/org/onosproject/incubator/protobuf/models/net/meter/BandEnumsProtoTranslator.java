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

import org.onosproject.grpc.net.meter.models.BandEnumsProto;
import org.onosproject.net.meter.Band;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * gRPC message conversion related utilities for band enums.
 */
public final class BandEnumsProtoTranslator {

    private static final Logger log = LoggerFactory.getLogger(BandEnumsProtoTranslator.class);

    /**
     * Translates gRPC enum Band Type to ONOS enum.
     *
     * @param bandType BandType in gRPC enum
     * @return equivalent in ONOS enum
     */
    public static Optional<Band.Type> translate(BandEnumsProto.BandTypeProto bandType) {
        switch (bandType) {
            case DROP:
                return Optional.of(Band.Type.DROP);
            case REMARK:
                return Optional.of(Band.Type.REMARK);
            case EXPERIMENTAL:
                return Optional.of(Band.Type.EXPERIMENTAL);
            default:
                log.warn("Unrecognized BandType gRPC message: {}", bandType);
                return Optional.empty();
        }
    }

    // Utility class not intended for instantiation.
    private BandEnumsProtoTranslator() {}
}
