/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.ospf.protocol.lsa.types;

import com.google.common.base.MoreObjects;
import com.google.common.primitives.Bytes;

import java.util.Arrays;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.ospf.controller.OspfLsaType;
import org.onosproject.ospf.protocol.lsa.OpaqueLsaHeader;

/**
 * Representation of an Opaque LSA of type AS (11).
 */
public class OpaqueLsa11 extends OpaqueLsaHeader {
    /*
      0                   1                   2                   3
      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |            LS age             |     Options   |   9, 10 or 11 |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |  Opaque Type  |               Opaque ID                       |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |                      Advertising Router                       |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |                      LS Sequence Number                       |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |         LS checksum           |           Length              |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |                                                               |
     +                                                               +
     |                      Opaque Information                       |
     +                                                               +
     |                              ...                              |
    */
    private byte[] opaqueInfo = null;

    /**
     * Creates an instance of Opaque type 11 LSA.
     *
     * @param lsaHeader LSA header instance
     */
    public OpaqueLsa11(OpaqueLsaHeader lsaHeader) {
        populateHeader(lsaHeader);
    }

    /**
     * Reads from channel buffer and populate this.
     *
     * @param channelBuffer channelBuffer instance.
     */
    public void readFrom(ChannelBuffer channelBuffer) {
        int length = channelBuffer.readableBytes();
        opaqueInfo = new byte[length];
        channelBuffer.readBytes(opaqueInfo, 0, length);
    }

    /**
     * Returns instance as bytes.
     *
     * @return instance as bytes
     */
    public byte[] asBytes() {
        byte[] lsaMessage = null;

        byte[] lsaHeader = getOpaqueLsaHeaderAsByteArray();
        byte[] lsaBody = getLsaBodyAsByteArray();
        lsaMessage = Bytes.concat(lsaHeader, lsaBody);

        return lsaMessage;
    }

    /**
     * Get the LSA body as byte array.
     *
     * @return LSA body as byte array
     */
    public byte[] getLsaBodyAsByteArray() {
        return opaqueInfo;
    }

    @Override
    public OspfLsaType getOspfLsaType() {
        return OspfLsaType.AS_OPAQUE_LSA;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OpaqueLsa11 that = (OpaqueLsa11) o;
        return Arrays.equals(opaqueInfo, that.opaqueInfo);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(opaqueInfo);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("opaqueInfo", opaqueInfo)
                .toString();
    }
}