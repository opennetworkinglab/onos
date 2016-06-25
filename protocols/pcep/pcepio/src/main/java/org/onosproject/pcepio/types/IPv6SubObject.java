/*
 * Copyright 2015-present Open Networking Laboratory
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

package org.onosproject.pcepio.types;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.protocol.PcepVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Provides IPv6 Sub Object.
 */
public class IPv6SubObject implements PcepValueType {

    /* reference :RFC 4874.
    Subobject : IPv6 address

    0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |      Type     |     Length    | IPv6 address (16 bytes)       |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    | IPv6 address (continued)                                      |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    | IPv6 address (continued)                                      |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    | IPv6 address (continued)                                      |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    | IPv6 address (continued)      | Prefix Length |      Flags    |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

      Type

         0x02  IPv6 address

      Length

         The Length contains the total length of the subobject in bytes,
         including the Type and Length fields.  The Length is always 20.

      IPv6 address

         A 128-bit unicast host address.

      Prefix length

         128

      Flags

         0x01  Local protection available

               Indicates that the link downstream of this node is
               protected via a local repair mechanism.  This flag can
               only be set if the Local protection flag was set in the
               SESSION_ATTRIBUTE object of the corresponding Path
               message.

         0x02  Local protection in use

               Indicates that a local repair mechanism is in use to
               maintain this tunnel (usually in the face of an outage
               of the link it was previously routed over).
     */
    protected static final Logger log = LoggerFactory.getLogger(IPv6SubObject.class);

    public static final short TYPE = 0x02;
    public static final short LENGTH = 20;
    public static final byte VALUE_LENGTH = 18;

    private static final byte[] NONE_VAL = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
    public static final IPv6SubObject NONE = new IPv6SubObject(NONE_VAL);

    private static final byte[] NO_MASK_VAL = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };
    public static final IPv6SubObject NO_MASK = new IPv6SubObject(NO_MASK_VAL);
    public static final IPv6SubObject FULL_MASK = NONE;

    private final byte[] rawValue;

    /**
     * constructor to initialize rawValue with ipv6 address.
     *
     * @param rawValue ipv6 address
     */
    public IPv6SubObject(byte[] rawValue) {
        this.rawValue = rawValue;
    }

    /**
     * To create instance of IPv6SubObject.
     *
     * @param raw byte array of ipv6 address
     * @return object of IPv6SubObject
     */
    public static IPv6SubObject of(final byte[] raw) {
        //check NONE_VAL
        boolean bFoundNone = true;
        //value starts from 3rd byte.
        for (int i = 2; i < 20; ++i) {
            if (NONE_VAL[i] != raw[i]) {
                bFoundNone = false;
            }
        }

        if (bFoundNone) {
            return NONE;
        }

        //check NO_MASK_VAL
        boolean bFoundNoMask = true;
        //value starts from 3rd byte.
        for (int i = 2; i < 20; ++i) {
            if (0xFF != raw[i]) {
                bFoundNoMask = false;
            }
        }
        if (bFoundNoMask) {
            return NO_MASK;
        }

        return new IPv6SubObject(raw);
    }

    /**
     * Returns value of IPv6 Sub Object.
     *
     * @return byte array of ipv6 address
     */
    public byte[] getValue() {
        return rawValue;
    }

    @Override
    public PcepVersion getVersion() {
        return PcepVersion.PCEP_1;
    }

    @Override
    public short getType() {
        return TYPE;
    }

    @Override
    public short getLength() {
        return LENGTH;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(rawValue);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof IPv6SubObject) {
            IPv6SubObject other = (IPv6SubObject) obj;
            return Arrays.equals(rawValue, other.rawValue);
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer c) {
        int iStartIndex = c.writerIndex();
        c.writeShort(TYPE);
        c.writeShort(LENGTH);
        c.writeBytes(rawValue);
        return c.writerIndex() - iStartIndex;
    }

    /**
     * Reads the channel buffer and returns object of IPv6SubObject.
     *
     * @param c type of channel buffer
     * @return object of IPv6SubObject
     */
    public static IPv6SubObject read20Bytes(ChannelBuffer c) {
        byte[] yTemp = new byte[20];
        c.readBytes(yTemp, 0, 20);
        return IPv6SubObject.of(yTemp);
    }

    @Override
    public String toString() {
        ToStringHelper toStrHelper = MoreObjects.toStringHelper(getClass());

        toStrHelper.add("Type", TYPE);
        toStrHelper.add("Length", LENGTH);

        StringBuffer result = new StringBuffer();
        for (byte b : rawValue) {
            result.append(String.format("%02X ", b));
        }
        toStrHelper.add("Value", result);

        return toStrHelper.toString();
    }
}
