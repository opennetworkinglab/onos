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

import java.util.Arrays;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.protocol.PcepVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides SymbolicPathNameTlv.
 */
public class SymbolicPathNameTlv implements PcepValueType {

    /*
     *    SYMBOLIC-PATH-NAME TLV format
     *    Reference :PCEP Extensions for Stateful PCE draft-ietf-pce-stateful-pce-10
     *
         0                   1                   2                   3
         0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
         +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         |           Type=17             |       Length (variable)       |
         +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         |                                                               |
         //                      Symbolic Path Name                     //
         |                                                               |
         +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */
    protected static final Logger log = LoggerFactory.getLogger(SymbolicPathNameTlv.class);

    public static final short TYPE = 17;
    private short hLength;

    private final byte[] rawValue;

    /**
     * Constructor to initialize raw Value.
     *
     * @param rawValue Symbolic path name
     */
    public SymbolicPathNameTlv(byte[] rawValue) {
        this.rawValue = rawValue;
        this.hLength = (short) rawValue.length;
    }

    /**
     * Constructor to initialize raw Value.
     *
     * @param rawValue Symbolic path name
     * @param hLength length of Symbolic path name
     */
    public SymbolicPathNameTlv(byte[] rawValue, short hLength) {
        this.rawValue = rawValue;
        if (0 == hLength) {
            this.hLength = (short) rawValue.length;
        } else {
            this.hLength = hLength;
        }
    }

    /**
     * Creates an object of SymbolicPathNameTlv.
     *
     * @param raw Symbolic path name
     * @param hLength length of Symbolic path name
     * @return object of SymbolicPathNameTlv
     */
    public static SymbolicPathNameTlv of(final byte[] raw, short hLength) {
        return new SymbolicPathNameTlv(raw, hLength);
    }

    /**
     * Returns Symbolic path name.
     *
     * @return Symbolic path name byte array
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
        return hLength;
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
        if (obj instanceof SymbolicPathNameTlv) {
            SymbolicPathNameTlv other = (SymbolicPathNameTlv) obj;
            return Arrays.equals(this.rawValue, other.rawValue);
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer c) {
        int iLenStartIndex = c.writerIndex();
        c.writeShort(TYPE);
        c.writeShort(hLength);
        c.writeBytes(rawValue);
        return c.writerIndex() - iLenStartIndex;
    }

    /**
     * Reads channel buffer and returns object of SymbolicPathNameTlv.
     *
     * @param c of type channel buffer
     * @param hLength length of bytes to read
     * @return object of SymbolicPathNameTlv
     */
    public static SymbolicPathNameTlv read(ChannelBuffer c, short hLength) {
        byte[] symbolicPathName = new byte[hLength];
        c.readBytes(symbolicPathName, 0, hLength);
        return new SymbolicPathNameTlv(symbolicPathName, hLength);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("SymbolicPathName ", rawValue)
                .toString();
    }
}
