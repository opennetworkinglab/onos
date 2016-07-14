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
 * Provides IPv4 Sub Object.
 */
public class IPv4SubObject implements PcepValueType {

    /*Reference : RFC 4874:3.1.1
     *  0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |L|    Type     |     Length    | IPv4 address (4 bytes)        |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    | IPv4 address (continued)      | Prefix Length |      Resvd    |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */
    protected static final Logger log = LoggerFactory.getLogger(IPv4SubObject.class);

    public static final byte TYPE = 0x01;
    public static final byte LENGTH = 8;
    public static final byte VALUE_LENGTH = 6;
    public static final byte OBJ_LENGTH = 8;
    public static final byte LBIT = 0;
    public static final int SHIFT_LBIT_POSITION = 7;
    private int ipAddress;
    private byte prefixLen;
    private byte resvd;

    /**
     * Constructor to initialize ipv4 address.
     *
     * @param ipAddr ipv4 address
     */
    public IPv4SubObject(int ipAddr) {
        this.ipAddress = ipAddr;
    }

    /**
     * constructor to initialize ipAddress, prefixLen and resvd.
     *
     * @param ipAddress ipv4 address
     * @param prefixLen prefix length
     * @param resvd reserved flags value
     */
    public IPv4SubObject(int ipAddress, byte prefixLen, byte resvd) {
        this.ipAddress = ipAddress;
        this.prefixLen = prefixLen;
        this.resvd = resvd;
    }

    /**
     * Returns a new instance of IPv4SubObject.
     *
     * @param ipAddress ipv4 address
     * @param prefixLen prefix length
     * @param resvd reserved flags value
     * @return object of IPv4SubObject
     */
    public static IPv4SubObject of(int ipAddress, byte prefixLen, byte resvd) {
        return new IPv4SubObject(ipAddress, prefixLen, resvd);
    }

    /**
     * Returns prefixLen of IPv4 IP address.
     *
     * @return byte  value of rawValue
     */
    public byte getPrefixLen() {
        return prefixLen;
    }

    /**
     * Returns value of IPv4 IP address.
     *
     * @return int value of ipv4 address
     */
    public int getIpAddress() {
        return ipAddress;
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
        return Objects.hash(ipAddress, prefixLen, resvd);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof IPv4SubObject) {
            IPv4SubObject other = (IPv4SubObject) obj;
            return Objects.equals(this.ipAddress, other.ipAddress) && Objects.equals(this.prefixLen, other.prefixLen)
                    && Objects.equals(this.resvd, other.resvd);
        }
        return false;
    }

    /**
     * Reads the channel buffer and returns object of IPv4SubObject.
     *
     * @param c type of channel buffer
     * @return object of IPv4SubObject
     */
    public static PcepValueType read(ChannelBuffer c) {
        int ipAddess = c.readInt();
        byte prefixLen = c.readByte();
        byte resvd = c.readByte();
        return new IPv4SubObject(ipAddess, prefixLen, resvd);
    }

    @Override
    public int write(ChannelBuffer c) {
        int iLenStartIndex = c.writerIndex();
        byte bValue = LBIT;
        bValue = (byte) (bValue << SHIFT_LBIT_POSITION);
        bValue = (byte) (bValue | TYPE);
        c.writeByte(bValue);
        c.writeByte(OBJ_LENGTH);
        c.writeInt(ipAddress);
        c.writeByte(prefixLen);
        c.writeByte(resvd);

        return c.writerIndex() - iLenStartIndex;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("Type", TYPE)
                .add("Length", LENGTH)
                .add("IPv4Address", ipAddress)
                .add("PrefixLength", prefixLen)
                .toString();
    }
}
