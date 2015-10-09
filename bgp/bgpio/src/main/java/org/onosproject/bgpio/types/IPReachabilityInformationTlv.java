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
package org.onosproject.bgpio.types;

import java.util.Objects;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onlab.packet.IpPrefix;
import org.onosproject.bgpio.util.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides IP Reachability InformationTlv Tlv which contains IP Prefix.
 */
public class IPReachabilityInformationTlv implements BGPValueType {

    /*
     * Reference :draft-ietf-idr-ls-distribution-11

      0                   1                   2                   3
      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |              Type             |             Length            |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     | Prefix Length | IP Prefix (variable)                         //
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

             Figure 14: IP Reachability Information TLV Format
    */

    protected static final Logger log = LoggerFactory.getLogger(IPReachabilityInformationTlv.class);

    public static final short TYPE = 265;
    public static final int ONE_BYTE_LEN = 8;
    private byte prefixLen;
    private byte[] ipPrefix;
    public short length;

    /**
     * Constructor to initialize parameters.
     *
     * @param prefixLen length of IP Prefix
     * @param ipPrefix IP Prefix
     * @param length length of value field
     */
    public IPReachabilityInformationTlv(byte prefixLen, byte[] ipPrefix, short length) {
        this.ipPrefix = ipPrefix;
        this.prefixLen = prefixLen;
        this.length = length;
    }

    /**
     * Returns IP Prefix.
     *
     * @return IP Prefix
     */
    public IpPrefix getPrefixValue() {
        IpPrefix prefix = Validation.bytesToPrefix(ipPrefix, prefixLen);
        return prefix;
    }

    /**
     * Returns IP Prefix length.
     *
     * @return IP Prefix length
     */
    public byte getPrefixLen() {
        return this.prefixLen;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ipPrefix, prefixLen);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof IPReachabilityInformationTlv) {
            IPReachabilityInformationTlv other = (IPReachabilityInformationTlv) obj;
            return Objects.equals(prefixLen, other.prefixLen) && Objects.equals(ipPrefix, other.ipPrefix);
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer cb) {
        int iLenStartIndex = cb.writerIndex();
        cb.writeShort(TYPE);
        cb.writeShort(length);
        cb.writeByte(prefixLen);
        cb.writeBytes(ipPrefix);
        return cb.writerIndex() - iLenStartIndex;
    }

    /**
     * Reads the channel buffer and returns object of IPReachabilityInformationTlv.
     *
     * @param cb ChannelBuffer
     * @param length of value field
     * @return object of IPReachabilityInformationTlv
     */
    public static IPReachabilityInformationTlv read(ChannelBuffer cb, short length) {
        byte preficLen = cb.readByte();
        byte[] prefix;
        if (preficLen == 0) {
            prefix = new byte[] {0};
        } else {
            int len = preficLen / ONE_BYTE_LEN;
            int reminder = preficLen % ONE_BYTE_LEN;
            if (reminder > 0) {
                len = len + 1;
            }
            prefix = new byte[len];
            cb.readBytes(prefix, 0, len);
        }
        return IPReachabilityInformationTlv.of(preficLen, prefix, length);
    }

    public static IPReachabilityInformationTlv of(final byte preficLen, final byte[] prefix, final short  length) {
        return new IPReachabilityInformationTlv(preficLen, prefix, length);
    }
    @Override
    public short getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("Type", TYPE)
                .add("Length", length)
                .add("Prefixlength", getPrefixLen())
                .add("Prefixvalue", getPrefixValue())
                .toString();
    }
}