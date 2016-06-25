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

/**
 * @author b00295750
 *
 */
package org.onosproject.pcepio.types;

import java.util.Objects;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.protocol.PcepVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides Autonomous system number sub object.
 */
public class AutonomousSystemNumberSubObject implements PcepValueType {

    /*Reference : RFC 3209 : 4.3.3.4
     *  0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |L|    Type     |     Length    |      AS number (2-octet)      |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */
    protected static final Logger log = LoggerFactory.getLogger(AutonomousSystemNumberSubObject.class);

    public static final byte TYPE = (byte) 0x32;
    public static final byte LENGTH = 4;
    public static final byte VALUE_LENGTH = 2;
    public static final byte OBJ_LENGTH = 4;
    public static final byte LBIT = 0;
    public static final int SHIFT_LBIT_POSITION = 7;
    private short asNumber;

    /**
     * Constructor to initialize AS number.
     *
     * @param asNumber AS number
     */
    public AutonomousSystemNumberSubObject(short asNumber) {
        this.asNumber = asNumber;
    }

    /**
     * Returns a new instance of AutonomousSystemNumberSubObject.
     *
     * @param asNumber AS number
     * @return object of AutonomousSystemNumberSubObject
     */
    public static AutonomousSystemNumberSubObject of(short asNumber) {
        return new AutonomousSystemNumberSubObject(asNumber);
    }

    /**
     * Returns value of AS number.
     *
     * @return value of AS number
     */
    public short getAsNumber() {
        return asNumber;
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
        return Objects.hash(asNumber);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof AutonomousSystemNumberSubObject) {
            AutonomousSystemNumberSubObject other = (AutonomousSystemNumberSubObject) obj;
            return Objects.equals(this.asNumber, other.asNumber);
        }
        return false;
    }

    /**
     * Reads the channel buffer and returns object of AutonomousSystemNumberSubObject.
     *
     * @param c type of channel buffer
     * @return object of AutonomousSystemNumberSubObject
     */
    public static PcepValueType read(ChannelBuffer c) {
        short asNumber = c.readShort();
        return new AutonomousSystemNumberSubObject(asNumber);
    }

    @Override
    public int write(ChannelBuffer c) {
        int iLenStartIndex = c.writerIndex();
        byte bValue = LBIT;
        bValue = (byte) (bValue << SHIFT_LBIT_POSITION);
        bValue = (byte) (bValue | TYPE);
        c.writeByte(bValue);
        c.writeByte(OBJ_LENGTH);
        c.writeShort(asNumber);

        return c.writerIndex() - iLenStartIndex;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("Type", TYPE)
                .add("Length", LENGTH)
                .add("AsNumber", asNumber)
                .toString();
    }
}
