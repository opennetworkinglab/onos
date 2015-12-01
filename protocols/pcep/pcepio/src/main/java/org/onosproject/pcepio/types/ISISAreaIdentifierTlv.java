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

import java.util.Arrays;
import java.util.Objects;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.protocol.PcepVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;

/**
 * Provides ISIS Area Identifier.
 */
public class ISISAreaIdentifierTlv implements PcepValueType {

    /* Reference :[I-D.ietf-idr- ls-distribution]/3.3.1.2
     * 0                   1                   2                   3
      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |              Type=[TBD24]    |             Length            |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     //                 Area Identifier (variable)                  //
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */

    protected static final Logger log = LoggerFactory.getLogger(ISISAreaIdentifierTlv.class);

    public static final short TYPE = 107; //TODO:NEED TO HANDLE TBD24
    private short hLength;

    private final byte[] rawValue;

    /**
     * Constructor to initialize rawValue.
     *
     * @param rawValue ISIS-Area-Identifier
     * @param hLength length
     */
    public ISISAreaIdentifierTlv(byte[] rawValue, short hLength) {
        log.debug("ISISAreaIdentifierTlv");
        this.rawValue = rawValue;
        if (0 == hLength) {
            this.hLength = (short) rawValue.length;
        } else {
            this.hLength = hLength;
        }
    }

    /**
     * Returns newly created ISISAreaIdentifierTlv object.
     *
     * @param raw ISIS-Area-Identifier
     * @param hLength length
     * @return object of ISISAreaIdentifierTlv
     */
    public static ISISAreaIdentifierTlv of(final byte[] raw, short hLength) {
        return new ISISAreaIdentifierTlv(raw, hLength);
    }

    /**
     * Returns value of ISIS-Area-Identifier.
     *
     * @return byte array of rawValue
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
        return Objects.hash(Arrays.hashCode(rawValue), rawValue.length);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ISISAreaIdentifierTlv) {
            ISISAreaIdentifierTlv other = (ISISAreaIdentifierTlv) obj;
            return Objects.equals(hLength, other.hLength) && Arrays.equals(rawValue, other.rawValue);
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
     * Reads the channel buffer and returns object of ISISAreaIdentifierTlv.
     *
     * @param c input channel buffer
     * @param hLength length
     * @return object of ISISAreaIdentifierTlv
     */
    public static PcepValueType read(ChannelBuffer c, short hLength) {
        byte[] iISISAreaIdentifier = new byte[hLength];
        c.readBytes(iISISAreaIdentifier, 0, hLength);
        return new ISISAreaIdentifierTlv(iISISAreaIdentifier, hLength);
    }

    @Override
    public String toString() {
        ToStringHelper toStrHelper = MoreObjects.toStringHelper(getClass());

        toStrHelper.add("Type", TYPE);
        toStrHelper.add("Length", hLength);

        StringBuffer result = new StringBuffer();
        for (byte b : rawValue) {
            result.append(String.format("%02X ", b));
        }
        toStrHelper.add("Value", result);

        return toStrHelper.toString();
    }
}
