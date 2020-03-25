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
package org.onosproject.bgpio.protocol.ver4;

import com.google.common.base.MoreObjects;
import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.protocol.BgpType;
import org.onosproject.bgpio.types.BgpHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.onosproject.bgpio.protocol.BgpRouteRefreshMsg;
import org.onosproject.bgpio.protocol.BgpMessageReader;
import org.onosproject.bgpio.protocol.BgpMessageWriter;
import org.onosproject.bgpio.protocol.BgpVersion;

import java.util.LinkedList;
import java.util.List;

/**
 * Provides BGP Route Refresh message.
 */
public class BgpRouteRefreshMsgVer4 implements BgpRouteRefreshMsg {

    /*
    <Route Refresh Message>::=

    0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |                                                               |
    +                                                               +
    |                                                               |
    +                                                               +
    |                           Marker                              |
    +                                                               +
    |                                                               |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |          Length               |      Type     |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |           AFI                 |    Reserved   |     SAFI      |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

    REFERENCE : RFC 2918
    */

    private static final Logger log = LoggerFactory.getLogger(BgpRouteRefreshMsgVer4.class);

    public static final byte PACKET_VERSION = 4;
    public static final int PACKET_LENGTH = 0x17;
    public static final int MARKER_LENGTH = 16;
    public static final BgpType MSG_TYPE = BgpType.ROUTE_REFRESH;
    private static final byte[] MARKER = new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};

    private static final BgpHeader DEFAULT_ROUTEREFRESH_HEADER = new BgpHeader(MARKER,
            (short) PACKET_LENGTH, BgpType.ROUTE_REFRESH.getType());

    private BgpHeader bgpMsgHeader;
    private byte version;
    private List<AfiSafiValue> afiSafiValues;

    public static final BgpRouteRefreshMsgVer4.Reader READER = new Reader();

    /**
     * Reader class for reading BGP route refresh message from channel buffer.
     */
    static class Reader implements BgpMessageReader<BgpRouteRefreshMsg> {

        @Override
        public BgpRouteRefreshMsgVer4 readFrom(ChannelBuffer cb, BgpHeader bgpHeader)
                throws BgpParseException {

            Short afi = cb.readShort();
            log.debug("AFI read from Route Refresh message = {}", afi);

            byte reserved = cb.readByte();
            log.debug("Reserved read from Route Refresh message = {}", reserved);

            byte safi = cb.readByte();
            log.debug("SAFI read from Route Refresh message = {}", safi);

            List<AfiSafiValue> afiSafiList = new LinkedList<>();

            AfiSafiValue afisafiValue = new AfiSafiValue(afi, reserved, safi);
            afiSafiList.add(afisafiValue);

            return new BgpRouteRefreshMsgVer4(DEFAULT_ROUTEREFRESH_HEADER, PACKET_VERSION, afiSafiList);
        }
    }

    /**
     * Default constructor.
     */
    public BgpRouteRefreshMsgVer4() {
        this.bgpMsgHeader = null;
        this.version = 0;
        this.afiSafiValues = null;
    }

    public BgpRouteRefreshMsgVer4(BgpHeader msgHeader, byte version, List<AfiSafiValue> afiSafiValues) {
        this.bgpMsgHeader = msgHeader;
        this.version = version;
        this.afiSafiValues = afiSafiValues;
    }

    /**
     * Builder class for BGP route refresh message.
     */
    static class Builder implements BgpRouteRefreshMsg.Builder {
        BgpHeader bgpHeader = null;
        boolean isHeaderSet = false;
        List<AfiSafiValue> afiSafiList = null;

        @Override
        public Builder setHeader(BgpHeader bgpMsgHeader) {
            this.bgpHeader = bgpMsgHeader;
            this.isHeaderSet = true;
            return this;
        }

        @Override
        public BgpRouteRefreshMsg.Builder addAfiSafiValue(short afi, byte reserved, byte safi) {
            if (afiSafiList == null) {
                afiSafiList = new LinkedList<>();
            }
            AfiSafiValue currentElement = new AfiSafiValue(afi, reserved, safi);
            afiSafiList.add(currentElement);
            return this;
        }

        @Override
        public BgpRouteRefreshMsg build() {
            BgpHeader header = isHeaderSet ? bgpHeader : DEFAULT_ROUTEREFRESH_HEADER;
            return new BgpRouteRefreshMsgVer4(header, PACKET_VERSION, afiSafiList);
        }
    }

    @Override
    public void writeTo(ChannelBuffer cb) {
        WRITER.write(cb, this);
    }

    static final Writer WRITER = new Writer();

    /**
     * Writer class for writing the BGP route refresh message to channel buffer.
     */
    static class Writer implements BgpMessageWriter<BgpRouteRefreshMsgVer4> {

        @Override
        public void write(ChannelBuffer cb, BgpRouteRefreshMsgVer4 message) {
            //Iterate over the added AFI-SAFI pair and write a new packet for each of those

            for (AfiSafiValue element : message.afiSafiValues) {
                cb.writeBytes(MARKER, 0, MARKER_LENGTH);
                cb.writeShort(PACKET_LENGTH);
                cb.writeByte(MSG_TYPE.getType());

                cb.writeShort(element.afi);
                cb.writeByte(element.reserved);
                cb.writeByte(element.safi);
            }
        }
    }

    @Override
    public BgpVersion getVersion() {
        return BgpVersion.BGP_4;
    }

    @Override
    public BgpType getType() {
        return MSG_TYPE;
    }

    @Override
    public BgpHeader getHeader() {
        return this.bgpMsgHeader;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass()).toString();
    }
}

final class AfiSafiValue {
    final short afi;
    final byte reserved;
    final byte safi;

    public AfiSafiValue(short afi, byte reserved, byte safi) {
        this.afi = afi;
        this.reserved = reserved;
        this.safi = safi;
    }
}
