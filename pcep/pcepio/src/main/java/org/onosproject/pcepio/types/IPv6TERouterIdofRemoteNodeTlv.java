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
package org.onosproject.pcepio.types;

import java.util.Objects;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.protocol.PcepVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;

/**
 * Provides IPv6 TE Router Id of Remote Node.  Reference :[RFC6119]/4.1.
 */
public class IPv6TERouterIdofRemoteNodeTlv implements PcepValueType {
    protected static final Logger log = LoggerFactory.getLogger(IPv6TERouterIdofRemoteNodeTlv.class);

    public static final short TYPE = 1400; //TDB29
    public static final short LENGTH = 20;
    public static final byte VALUE_LENGTH = 18;

    private static final byte[] NONE_VAL = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    public static final IPv6TERouterIdofRemoteNodeTlv NONE = new IPv6TERouterIdofRemoteNodeTlv(NONE_VAL);

    private static final byte[] NO_MASK_VAL = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
    public static final IPv6TERouterIdofRemoteNodeTlv NO_MASK = new IPv6TERouterIdofRemoteNodeTlv(NO_MASK_VAL);
    public static final IPv6TERouterIdofRemoteNodeTlv FULL_MASK = NONE;

    private final byte[] rawValue;

    /**
     * constructor to initialize rawValue.
     *
     * @param rawValue IPv6TERouterIdofRemoteNodeTlv
     */
    public IPv6TERouterIdofRemoteNodeTlv(byte[] rawValue) {
        log.debug("IPv6TERouterIdofRemoteNodeTlv");
        this.rawValue = rawValue;
    }

    /**
     * Returns newly created IPv6TERouterIdofRemoteNodeTlv object.
     *
     * @param raw IPv6 TE Router Id of RemoteNode
     * @return object of IPv6TERouterIdofRemoteNodeTlv
     */
    public static IPv6TERouterIdofRemoteNodeTlv of(final byte[] raw) {
        //check NONE_VAL
        boolean bFoundNONE = true;
        //value starts from 3rd byte.
        for (int i = 2; i < 20; ++i) {
            if (NONE_VAL[i] != raw[i]) {
                bFoundNONE = false;
            }
        }

        if (bFoundNONE) {
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

        return new IPv6TERouterIdofRemoteNodeTlv(raw);
    }

    /**
     * Returns value of IPv6 TE Router Id of Remote Node.
     *
     * @return byte array value of rawValue
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
        return Objects.hash(rawValue);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof IPv6TERouterIdofRemoteNodeTlv) {
            IPv6TERouterIdofRemoteNodeTlv other = (IPv6TERouterIdofRemoteNodeTlv) obj;
            return Objects.equals(rawValue, other.rawValue);
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
     * Reads the channel buffer and returns object of IPv6TERouterIdofRemoteNodeTlv.
     *
     * @param c input channel buffer
     * @return object of IPv6TERouterIdofRemoteNodeTlv
     */
    public static IPv6TERouterIdofRemoteNodeTlv read20Bytes(ChannelBuffer c) {
        byte[] yTemp = new byte[20];
        c.readBytes(yTemp, 0, 20);
        return IPv6TERouterIdofRemoteNodeTlv.of(yTemp);
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
