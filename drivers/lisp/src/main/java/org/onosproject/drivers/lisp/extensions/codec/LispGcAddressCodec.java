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
package org.onosproject.drivers.lisp.extensions.codec;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.drivers.lisp.extensions.LispGcAddress;
import org.onosproject.mapping.addresses.MappingAddress;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * LISP geo coordinate address codec.
 */
public final class LispGcAddressCodec extends JsonCodec<LispGcAddress> {

    static final String NORTH = "north";
    static final String LATITUDE_DEGREE = "latitudeDegree";
    static final String LATITUDE_MINUTE = "latitudeMinute";
    static final String LATITUDE_SECOND = "latitudeSecond";
    static final String EAST = "east";
    static final String LONGITUDE_DEGREE = "longitudeDegree";
    static final String LONGITUDE_MINUTE = "longitudeMinute";
    static final String LONGITUDE_SECOND = "longitudeSecond";
    static final String ALTITUDE = "altitude";
    static final String ADDRESS = "address";

    private static final String MISSING_MEMBER_MESSAGE =
                                " member is required in LispGcAddress";

    @Override
    public ObjectNode encode(LispGcAddress address, CodecContext context) {
        checkNotNull(address, "LispGcAddress cannot be null");

        final ObjectNode result = context.mapper().createObjectNode()
                .put(NORTH, address.isNorth())
                .put(LATITUDE_DEGREE, address.getLatitudeDegree())
                .put(LATITUDE_MINUTE, address.getLatitudeMinute())
                .put(LATITUDE_SECOND, address.getLatitudeSecond())
                .put(EAST, address.isEast())
                .put(LONGITUDE_DEGREE, address.getLongitudeDegree())
                .put(LONGITUDE_MINUTE, address.getLongitudeMinute())
                .put(LONGITUDE_SECOND, address.getLongitudeSecond())
                .put(ALTITUDE, address.getAltitude());

        if (address.getAddress() != null) {
            final JsonCodec<MappingAddress> addressCodec =
                    context.codec(MappingAddress.class);
            ObjectNode addressNode = addressCodec.encode(address.getAddress(), context);
            result.set(ADDRESS, addressNode);
        }

        return result;
    }

    @Override
    public LispGcAddress decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        boolean north = nullIsIllegal(json.get(NORTH),
                NORTH + MISSING_MEMBER_MESSAGE).asBoolean();
        short latitudeDegree = (short) nullIsIllegal(json.get(LATITUDE_DEGREE),
                LATITUDE_DEGREE + MISSING_MEMBER_MESSAGE).asInt();
        byte latitudeMinute = (byte) nullIsIllegal(json.get(LATITUDE_MINUTE),
                LATITUDE_MINUTE + MISSING_MEMBER_MESSAGE).asInt();
        byte latitudeSecond = (byte) nullIsIllegal(json.get(LATITUDE_SECOND),
                LATITUDE_SECOND + MISSING_MEMBER_MESSAGE).asInt();
        boolean east = nullIsIllegal(json.get(EAST),
                EAST + MISSING_MEMBER_MESSAGE).asBoolean();
        short longitudeDegree = (short) nullIsIllegal(json.get(LONGITUDE_DEGREE),
                LONGITUDE_DEGREE + MISSING_MEMBER_MESSAGE).asInt();
        byte longitudeMinute = (byte) nullIsIllegal(json.get(LONGITUDE_MINUTE),
                LONGITUDE_MINUTE + MISSING_MEMBER_MESSAGE).asInt();
        byte longitudeSecond = (byte) nullIsIllegal(json.get(LONGITUDE_SECOND),
                LONGITUDE_SECOND + MISSING_MEMBER_MESSAGE).asInt();
        int altitude = nullIsIllegal(json.get(ALTITUDE),
                ALTITUDE + MISSING_MEMBER_MESSAGE).asInt();

        ObjectNode addressJson = get(json, ADDRESS);
        MappingAddress mappingAddress = null;

        if (addressJson != null) {
            final JsonCodec<MappingAddress> addressCodec =
                    context.codec(MappingAddress.class);
            mappingAddress = addressCodec.decode(addressJson, context);
        }

        return new LispGcAddress.Builder()
                            .withIsNorth(north)
                            .withLatitudeDegree(latitudeDegree)
                            .withLatitudeMinute(latitudeMinute)
                            .withLatitudeSecond(latitudeSecond)
                            .withIsEast(east)
                            .withLongitudeDegree(longitudeDegree)
                            .withLongitudeMinute(longitudeMinute)
                            .withLongitudeSecond(longitudeSecond)
                            .withAltitude(altitude)
                            .withAddress(mappingAddress)
                            .build();
    }
}
