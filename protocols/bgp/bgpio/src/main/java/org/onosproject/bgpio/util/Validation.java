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

package org.onosproject.bgpio.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.Ints;

/**
 * Provides methods to parse attribute header, validate length and type.
 */
public class Validation {
    private static final Logger log = LoggerFactory.getLogger(Validation.class);
    public static final byte FIRST_BIT = (byte) 0x80;
    public static final byte SECOND_BIT = 0x40;
    public static final byte THIRD_BIT = 0x20;
    public static final byte FOURTH_BIT = (byte) 0x10;
    public static final byte IPV4_SIZE = 4;
    private boolean firstBit;
    private boolean secondBit;
    private boolean thirdBit;
    private boolean fourthBit;
    private int len;
    private boolean isShort;

    /**
     * Constructor to initialize parameter.
     *
     * @param firstBit in AttributeFlags
     * @param secondBit in AttributeFlags
     * @param thirdBit in AttributeFlags
     * @param fourthBit in AttributeFlags
     * @param len length
     * @param isShort true if length is read as short otherwise false
     */
    Validation(boolean firstBit, boolean secondBit, boolean thirdBit, boolean fourthBit, int len, boolean isShort) {
        this.firstBit = firstBit;
        this.secondBit = secondBit;
        this.thirdBit = thirdBit;
        this.fourthBit = fourthBit;
        this.len = len;
        this.isShort = isShort;
    }

    /**
     * Parses attribute Header.
     *
     * @param cb ChannelBuffer
     * @return object of Validation
     */
    public static Validation parseAttributeHeader(ChannelBuffer cb) {

        boolean firstBit;
        boolean secondBit;
        boolean thirdBit;
        boolean fourthBit;
        boolean isShort;
        byte flags = cb.readByte();
        byte typeCode = cb.readByte();
        byte temp = flags;
        //first Bit : Optional (1) or well-known (0)
        firstBit = ((temp & FIRST_BIT) == FIRST_BIT);
        //second Bit : Transitive (1) or non-Transitive (0)
        secondBit = ((temp & SECOND_BIT) == SECOND_BIT);
        //third Bit : partial (1) or complete (0)
        thirdBit = ((temp & THIRD_BIT) == THIRD_BIT);
        //forth Bit(Extended Length bit) : Attribute Length is 1 octects (0) or 2 octects (1)
        fourthBit = ((temp & FOURTH_BIT) == FOURTH_BIT);
        int len;
        if (fourthBit) {
            isShort = true;
            short length = cb.readShort();
            len = length;
        } else {
            isShort = false;
            byte length = cb.readByte();
            len = length;
        }
        return new Validation(firstBit, secondBit, thirdBit, fourthBit, len, isShort);
    }

    /**
     * Throws exception if length is not correct.
     *
     * @param errorCode Error code
     * @param subErrCode Sub Error Code
     * @param length erroneous length
     * @throws BgpParseException for erroneous length
     */
    public static void validateLen(byte errorCode, byte subErrCode, int length) throws BgpParseException {
        byte[] errLen = Ints.toByteArray(length);
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(errLen);
        throw new BgpParseException(errorCode, subErrCode, buffer);
    }

    /**
     * Throws exception if type is not correct.
     *
     * @param errorCode Error code
     * @param subErrCode Sub Error Code
     * @param type erroneous type
     * @throws BgpParseException for erroneous type
     */
    public static void validateType(byte errorCode, byte subErrCode, int type) throws BgpParseException {
        byte[] errType = Ints.toByteArray(type);
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(errType);
        throw new BgpParseException(errorCode, subErrCode, buffer);
    }

    /**
     * Convert byte array to InetAddress.
     *
     * @param length of IpAddress
     * @param cb channelBuffer
     * @return InetAddress
     */
    public static InetAddress toInetAddress(int length, ChannelBuffer cb) {
        byte[] address = new byte[length];
        cb.readBytes(address, 0, length);
        InetAddress ipAddress = null;
        try {
            ipAddress = InetAddress.getByAddress(address);
        } catch (UnknownHostException e) {
             log.info("InetAddress convertion failed");
        }
        return ipAddress;
    }

    /**
     * Returns first bit in type flags.
     *
     * @return first bit in type flags
     */
    public boolean getFirstBit() {
        return this.firstBit;
    }

    /**
     * Returns second bit in type flags.
     *
     * @return second bit in type flags
     */
    public boolean getSecondBit() {
        return this.secondBit;
    }

    /**
     * Returns third bit in type flags.
     *
     * @return third bit in type flags
     */
    public boolean getThirdBit() {
        return this.thirdBit;
    }

    /**
     * Returns fourth bit in type flags.
     *
     * @return fourth bit in type flags
     */
    public boolean getFourthBit() {
        return this.fourthBit;
    }

    /**
     * Returns attribute length.
     *
     * @return attribute length
     */
    public int getLength() {
        return this.len;
    }

    /**
     * Returns whether attribute length read in short or byte.
     *
     * @return whether attribute length read in short or byte
     */
    public boolean isShort() {
        return this.isShort;
    }

    /**
     * Converts byte array of prefix value to IpPrefix object.
     *
     * @param value byte array of prefix value
     * @param length prefix length in bits
     * @return object of IpPrefix
     */
    public static IpPrefix bytesToPrefix(byte[] value, int length) {
        if (value.length != IPV4_SIZE) {
            value = Arrays.copyOf(value, IPV4_SIZE);
        }
        IpPrefix ipPrefix = IpPrefix.valueOf(IpAddress.Version.INET, value, length);
        return ipPrefix;
    }
}