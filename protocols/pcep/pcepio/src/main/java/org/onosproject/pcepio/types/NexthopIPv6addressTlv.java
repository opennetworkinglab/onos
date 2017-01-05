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
 * NexthopIPv6addressTlv provides Ipv6  address of next hop.
 */
public class NexthopIPv6addressTlv implements PcepValueType {

    /*
       Reference: draft-zhao-pce-pcep-extension-for-pce-controller-01.

        0                   1                   2                     3
        0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       | Type=TBD                      | Length = 20                   |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                                                               |
       //               nexthop IPv6 address (16 bytes)                //
       |                                                               |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

                     NEXTHOP-IPV6-ADDRESS TLV:

     */
    protected static final Logger log = LoggerFactory.getLogger(NexthopIPv6addressTlv.class);

    public static final short TYPE = 100; //to be defined
    //Length is header + value
    public static final short LENGTH = 20;
    public static final short VALUE_LENGTH = 16;

    private static final byte[] NONE_VAL = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
    public static final NexthopIPv6addressTlv NONE = new NexthopIPv6addressTlv(NONE_VAL);

    private static final byte[] NO_MASK_VAL = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };
    public static final NexthopIPv6addressTlv NO_MASK = new NexthopIPv6addressTlv(NO_MASK_VAL);
    public static final NexthopIPv6addressTlv FULL_MASK = NONE;

    private final byte[] rawValue;

    /**
     * Constructor to initialize IP address for next hop IPv6 address tlv.
     *
     * @param rawValue value of Next hop ipAddress
     */
    public NexthopIPv6addressTlv(byte[] rawValue) {
        log.debug("NexthopIPv6addressTlv");
        this.rawValue = rawValue;
    }

    /**
     * Creates next hop IPv6 address tlv.
     *
     * @param raw value of Next hop ipAddress
     * @return object of NexthopIPv6addressTlv
     */
    //logic to be checked
    public static NexthopIPv6addressTlv of(final byte[] raw) {
        //check NONE_VAL
        boolean bFoundNone = true;
        //value starts from 3rd byte.
        for (int i = 5; i < 20; ++i) {
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
        for (int i = 5; i < 20; ++i) {
            if (0xFF != raw[i]) {
                bFoundNoMask = false;
            }
        }
        if (bFoundNoMask) {
            return NO_MASK;
        }
        return new NexthopIPv6addressTlv(raw);
    }

    /**
     * Returns next hop IPv6 address.
     *
     * @return next hop IPv6 address
     */
    public byte[] getBytes() {
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
        if (obj instanceof NexthopIPv6addressTlv) {
            NexthopIPv6addressTlv other = (NexthopIPv6addressTlv) obj;
            return Arrays.equals(this.rawValue, other.rawValue);
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
     * Reads the channel buffer and returns object of NexthopIPv6addressTlv.
     *
     * @param c type of channel buffer
     * @return object of NexthopIPv6addressTlv
     */
    public static NexthopIPv6addressTlv read(ChannelBuffer c) {
        byte[] yTemp = new byte[20];
        c.readBytes(yTemp, 0, 20);
        return NexthopIPv6addressTlv.of(yTemp);
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
        toStrHelper.add("IpAddress", result);

        return toStrHelper.toString();
    }
}
