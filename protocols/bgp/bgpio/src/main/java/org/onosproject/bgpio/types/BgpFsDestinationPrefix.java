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
package org.onosproject.bgpio.types;

import java.nio.ByteBuffer;
import java.util.Objects;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onlab.packet.IpPrefix;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.util.Constants;
import org.onosproject.bgpio.util.Validation;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

/**
 * Provides implementation of IPv4AddressTlv.
 */
public class BgpFsDestinationPrefix implements BgpValueType {

    public static final byte FLOW_SPEC_TYPE = Constants.BGP_FLOWSPEC_DST_PREFIX;
    private byte length;
    private IpPrefix ipPrefix;
    public static final int BYTE_IN_BITS = 8;

    /**
     * Constructor to initialize parameters.
     *
     * @param length length of the prefix
     * @param ipPrefix ip prefix
     */
    public BgpFsDestinationPrefix(byte length, IpPrefix ipPrefix) {
        this.ipPrefix = Preconditions.checkNotNull(ipPrefix);
        this.length = length;
    }

    /**
     * Returns ip prefix.
     *
     * @return ipPrefix ip prefix
     */
    public IpPrefix ipPrefix() {
        return ipPrefix;
    }

    @Override
    public short getType() {
        return FLOW_SPEC_TYPE;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ipPrefix);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof BgpFsDestinationPrefix) {
            BgpFsDestinationPrefix other = (BgpFsDestinationPrefix) obj;
            return Objects.equals(this.ipPrefix, other.ipPrefix);
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer cb) {
        int iLenStartIndex = cb.writerIndex();
        cb.writeByte(FLOW_SPEC_TYPE);
        cb.writeByte(length);
        cb.writeInt(ipPrefix.getIp4Prefix().address().toInt());
        return cb.writerIndex() - iLenStartIndex;
    }

    /**
     * Reads the channel buffer and returns object of IPv4AddressTlv.
     *
     * @param cb channelBuffer
     * @return object of flow spec destination prefix
     * @throws BgpParseException while parsing BgpFsDestinationPrefix
     */
    public static BgpFsDestinationPrefix read(ChannelBuffer cb) throws BgpParseException {
        IpPrefix ipPrefix;

        int length = cb.readByte();
        if (length == 0) {
            byte[] prefix = new byte[] {0};
            ipPrefix = Validation.bytesToPrefix(prefix, length);
            return new BgpFsDestinationPrefix((byte) ipPrefix.prefixLength(), ipPrefix);
        }
        int len = length / BYTE_IN_BITS;
        int reminder = length % BYTE_IN_BITS;
        if (reminder > 0) {
            len = len + 1;
        }
        if (cb.readableBytes() < len) {
            Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR,
                    BgpErrorType.MALFORMED_ATTRIBUTE_LIST, cb.readableBytes());
        }
        byte[] prefix = new byte[len];
        cb.readBytes(prefix, 0, len);
        ipPrefix = Validation.bytesToPrefix(prefix, length);

        return new BgpFsDestinationPrefix((byte) ipPrefix.prefixLength(), ipPrefix);
    }

    /**
     * Returns object of this class with specified values.
     *
     * @param ipPrefix ip prefix
     * @param length length of ip prefix
     * @return object of this class
     */
    public static BgpFsDestinationPrefix of(final IpPrefix ipPrefix, final byte length) {
        return new BgpFsDestinationPrefix(length, ipPrefix);
    }

    @Override
    public int compareTo(Object o) {
        if (this.equals(o)) {
            return 0;
        }

        if (o instanceof BgpFsDestinationPrefix) {
            BgpFsDestinationPrefix that = (BgpFsDestinationPrefix) o;

            if (this.ipPrefix().prefixLength() == that.ipPrefix().prefixLength()) {
                ByteBuffer value1 = ByteBuffer.wrap(this.ipPrefix.address().toOctets());
                ByteBuffer value2 = ByteBuffer.wrap(that.ipPrefix.address().toOctets());
                return value1.compareTo(value2);
            }

            if (this.ipPrefix().prefixLength() > that.ipPrefix().prefixLength()) {
                return 1;
            } else if (this.ipPrefix().prefixLength() < that.ipPrefix().prefixLength()) {
                return -1;
            }
        }
        return 1;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("FLOW_SPEC_TYPE", FLOW_SPEC_TYPE)
                .add("length", length)
                .add("ipPrefix", ipPrefix).toString();
    }
}
