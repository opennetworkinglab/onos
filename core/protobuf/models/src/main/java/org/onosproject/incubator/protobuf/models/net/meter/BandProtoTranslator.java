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
import org.onosproject.grpc.net.meter.models.BandProtoOuterClass.BandProto;
import org.onosproject.net.meter.Band;
import org.onosproject.net.meter.DefaultBand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * gRPC message conversion related utilities for band service.
 */
public final class BandProtoTranslator {

    private static final Logger log = LoggerFactory.getLogger(BandProtoTranslator.class);

    /**
     * Translates gRPC Band to {@link Band}.
     *
     * @param gBand gRPC message
     * @return {@link Band}
     */
    public static Band translate(BandProto gBand) {
        Band.Type type = BandEnumsProtoTranslator.translate(gBand.getType()).get();
        long rate = gBand.getRate();
        long burstSize = gBand.getBurst();
        short prec = (short) gBand.getDropPrecedence();
        Band band = new DefaultBand(type, rate, burstSize, prec);
        return band;
    }

    /**
     * Translates gRPC List Bands to Collection Band.
     *
     * @param listBands gRPC message
     * @return Collection Band
     */
    public static Collection<Band> translate(List<BandProto>  listBands) {
        Collection<Band> bands = new ArrayList<>();
        listBands.forEach(d -> bands.add(translate(d)));
        return bands;
    }

    /**
     * Translates ONOS enum Band Type to gRPC enum.
     *
     * @param bandType BandType in ONOS enum
     * @return equivalent in gRPC enum
     */
    public static BandEnumsProto.BandTypeProto translate(Band.Type bandType) {
        switch (bandType) {
            case DROP:
                return BandEnumsProto.BandTypeProto.DROP;
            case REMARK:
                return BandEnumsProto.BandTypeProto.REMARK;
            case EXPERIMENTAL:
                return BandEnumsProto.BandTypeProto.EXPERIMENTAL;
            default:
                log.warn("Unrecognized BandType ONOS message: {}", bandType);
                return BandEnumsProto.BandTypeProto.UNRECOGNIZED;
        }
    }

    // Utility class not intended for instantiation.
    private BandProtoTranslator() {}
}
