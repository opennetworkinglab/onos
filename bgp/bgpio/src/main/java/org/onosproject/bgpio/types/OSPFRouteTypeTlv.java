/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.bgpio.types;

import java.util.Objects;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.bgpio.exceptions.BGPParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides OSPF Route Type Tlv which contains route type.
 */
public class OSPFRouteTypeTlv implements BGPValueType {

    /* Reference :draft-ietf-idr-ls-distribution-11
      0                   1                   2                   3
      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |              Type             |             Length            |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |  Route Type   |
     +-+-+-+-+-+-+-+-+

                   Figure : OSPF Route Type TLV Format
    */

    protected static final Logger log = LoggerFactory.getLogger(OSPFRouteTypeTlv.class);

    public static final short TYPE = 264;
    public static final short LENGTH = 1;
    public static final int INTRA_AREA_TYPE = 1;
    public static final short INTER_AREA_TYPE = 2;
    public static final short EXTERNAL_TYPE_1 = 3;
    public static final short EXTERNAL_TYPE_2 = 4;
    public static final short NSSA_TYPE_1 = 5;
    public static final short NSSA_TYPE_2 = 6;
    private final byte routeType;

    /**
     * Enum for Route Type.
     */
    public enum ROUTETYPE {
        Intra_Area(1), Inter_Area(2), External_1(3), External_2(4), NSSA_1(5), NSSA_2(6);
        int value;
        ROUTETYPE(int val) {
            value = val;
        }
        public byte getType() {
            return (byte) value;
        }
    }

    /**
     * Constructor to initialize routeType.
     *
     * @param routeType Route type
     */
    public OSPFRouteTypeTlv(byte routeType) {
        this.routeType = routeType;
    }

    /**
     * Returns object of this class with specified routeType.
     *
     * @param routeType Route type
     * @return object of OSPFRouteTypeTlv
     */
    public static OSPFRouteTypeTlv of(final byte routeType) {
        return new OSPFRouteTypeTlv(routeType);
    }

    /**
     * Returns RouteType.
     *
     * @return RouteType
     * @throws BGPParseException if routeType is not matched
     */
    public ROUTETYPE getValue() throws BGPParseException {
        switch (routeType) {
        case INTRA_AREA_TYPE:
            return ROUTETYPE.Intra_Area;
        case INTER_AREA_TYPE:
            return ROUTETYPE.Inter_Area;
        case EXTERNAL_TYPE_1:
            return ROUTETYPE.External_1;
        case EXTERNAL_TYPE_2:
            return ROUTETYPE.External_2;
        case NSSA_TYPE_1:
            return ROUTETYPE.NSSA_1;
        case NSSA_TYPE_2:
            return ROUTETYPE.NSSA_2;
        default:
            throw new BGPParseException(BGPErrorType.UPDATE_MESSAGE_ERROR, (byte) 0, null);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(routeType);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof OSPFRouteTypeTlv) {
            OSPFRouteTypeTlv other = (OSPFRouteTypeTlv) obj;
            return Objects.equals(routeType, other.routeType);
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer c) {
        int iLenStartIndex = c.writerIndex();
        c.writeShort(TYPE);
        c.writeShort(LENGTH);
        c.writeByte(routeType);
        return c.writerIndex() - iLenStartIndex;
    }

    /**
     * Reads from ChannelBuffer and parses OSPFRouteTypeTlv.
     *
     * @param cb channelBuffer
     * @return object of OSPFRouteTypeTlv
     */
    public static OSPFRouteTypeTlv read(ChannelBuffer cb) {
        return OSPFRouteTypeTlv.of(cb.readByte());
    }

    @Override
    public short getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("Type", TYPE)
                .add("Length", LENGTH)
                .add("Value", routeType)
                .toString();
    }
}