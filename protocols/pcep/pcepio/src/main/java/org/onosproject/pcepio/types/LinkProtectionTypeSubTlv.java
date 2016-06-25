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
package org.onosproject.pcepio.types;

import java.util.Objects;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.protocol.PcepVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provide Link Protection Type.
 */

public class LinkProtectionTypeSubTlv implements PcepValueType {

    /* Reference  :[RFC5307]/1.2
     * 0                   1                   2                   3
      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |              Type=[TDB38]      |             Length=2         |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |Protection Cap | Reserved      |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */
    protected static final Logger log = LoggerFactory.getLogger(LinkProtectionTypeSubTlv.class);

    public static final short TYPE = 27;
    public static final short LENGTH = 2;
    private final byte protectionCap;
    private final byte reserved;

    /**
     * Constructor to initialize protectionCap.
     *
     * @param protectionCap Protection Cap
     */
    public LinkProtectionTypeSubTlv(byte protectionCap) {
        this.protectionCap = protectionCap;
        this.reserved = 0;
    }

    /**
     * Constructor to initialize protectionCap, reserved.
     *
     * @param protectionCap Protection Cap
     * @param reserved Reserved value
     */
    public LinkProtectionTypeSubTlv(byte protectionCap, byte reserved) {
        this.protectionCap = protectionCap;
        this.reserved = reserved;
    }

    /**
     * Returns Protection Cap.
     *
     * @return protectionCap Protection Cap
     */
    public byte getProtectionCap() {
        return protectionCap;
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
        return Objects.hash(protectionCap, reserved);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof LinkProtectionTypeSubTlv) {
            LinkProtectionTypeSubTlv other = (LinkProtectionTypeSubTlv) obj;
            return Objects.equals(protectionCap, other.protectionCap) && Objects.equals(reserved, other.reserved);
        }

        return false;
    }

    @Override
    public int write(ChannelBuffer c) {
        int iLenStartIndex = c.writerIndex();
        c.writeShort(TYPE);
        c.writeShort(LENGTH);
        c.writeByte(protectionCap);
        c.writeByte(reserved);
        return c.writerIndex() - iLenStartIndex;
    }

    /**
     * Reads the channel buffer and returns object of LinkProtectionTypeTlv.
     *
     * @param c input channel buffer
     * @return object of LinkProtectionTypeTlv
     */
    public static PcepValueType read(ChannelBuffer c) {
        byte protectionCap = c.readByte();
        byte reserved = c.readByte();
        return new LinkProtectionTypeSubTlv(protectionCap, reserved);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("Type", TYPE)
                .add("Length", LENGTH)
                .add("ProtectionCap", protectionCap)
                .toString();
    }
}
