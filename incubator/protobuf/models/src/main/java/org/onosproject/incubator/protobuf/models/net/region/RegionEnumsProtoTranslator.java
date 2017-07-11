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
package org.onosproject.incubator.protobuf.models.net.region;

import org.onosproject.grpc.net.region.models.RegionEnumsProto;
import org.onosproject.net.region.Region;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * gRPC RegionType message to equivalent ONOS enum conversion related utilities.
 */
public final class RegionEnumsProtoTranslator {

    private static final Logger log = LoggerFactory.getLogger(RegionEnumsProtoTranslator.class);

    /**
     * Translates gRPC enum RegionType to Optional of ONOS enum.
     *
     * @param type regiontype type in gRPC enum
     * @return Optional of equivalent ONOS enum or empty if not recognized
     */
    public static Optional<Region.Type> translate(RegionEnumsProto.RegionTypeProto type) {
        switch (type) {
            case CONTINENT:
                return Optional.of(Region.Type.CONTINENT);
            case COUNTRY:
                return Optional.of(Region.Type.COUNTRY);
            case METRO:
                return Optional.of(Region.Type.METRO);
            case CAMPUS:
                return Optional.of(Region.Type.CAMPUS);
            case BUILDING:
                return Optional.of(Region.Type.BUILDING);
            case DATA_CENTER:
                return Optional.of(Region.Type.DATA_CENTER);
            case FLOOR:
                return Optional.of(Region.Type.FLOOR);
            case ROOM:
                return Optional.of(Region.Type.ROOM);
            case RACK:
                return Optional.of(Region.Type.RACK);
            case LOGICAL_GROUP:
                return Optional.of(Region.Type.LOGICAL_GROUP);
            default:
                log.warn("Unrecognized Type gRPC message: {}", type);
                return Optional.empty();
        }
    }


    /**
     * Translates ONOS enum regionType to gRPC enum regionType.
     *
     * @param type ONOS' Type type
     * @return equivalent gRPC message enum
     */
    public static RegionEnumsProto.RegionTypeProto translate(Region.Type type) {
        switch (type) {
            case CONTINENT:
                return RegionEnumsProto.RegionTypeProto.CONTINENT;
            case COUNTRY:
                return RegionEnumsProto.RegionTypeProto.COUNTRY;
            case METRO:
                return RegionEnumsProto.RegionTypeProto.METRO;
            case CAMPUS:
                return RegionEnumsProto.RegionTypeProto.CAMPUS;
            case BUILDING:
                return RegionEnumsProto.RegionTypeProto.BUILDING;
            case DATA_CENTER:
                return RegionEnumsProto.RegionTypeProto.DATA_CENTER;
            case FLOOR:
                return RegionEnumsProto.RegionTypeProto.FLOOR;
            case ROOM:
                return RegionEnumsProto.RegionTypeProto.ROOM;
            case RACK:
                return RegionEnumsProto.RegionTypeProto.RACK;
            case LOGICAL_GROUP:
                return RegionEnumsProto.RegionTypeProto.LOGICAL_GROUP;
            default:
                log.warn("Unrecognized type", type);
                throw new IllegalArgumentException("Unrecognized Type");
        }
    }

    // Utility class not intended for instantiation.
    private RegionEnumsProtoTranslator() {}
}

