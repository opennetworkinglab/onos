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
package org.onosproject.bgpio.types.attr;

import java.util.Objects;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onlab.packet.Ip6Address;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.types.BgpErrorType;
import org.onosproject.bgpio.types.BgpValueType;
import org.onosproject.bgpio.util.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Implements BGP attribute IPv6 router ID.
 */
public final class BgpAttrRouterIdV6 implements BgpValueType {

    protected static final Logger log = LoggerFactory
            .getLogger(BgpAttrRouterIdV6.class);

    private final short sType;

    /* IPv4 Router-ID of Node */
    private final Ip6Address ip6RouterId;

    /**
     * Constructor to initialize the value.
     *
     * @param ip6RouterId IPV6 address of the router ID
     * @param sType TLV type
     */
    private BgpAttrRouterIdV6(Ip6Address ip6RouterId, short sType) {
        this.ip6RouterId = ip6RouterId;
        this.sType = sType;
    }

    /**
     * Returns object of this class with specified values.
     *
     * @param ip6RouterId IPV6 address of the router ID
     * @param sType TLV type
     * @return object of BgpAttrRouterIdV6
     */
    public static BgpAttrRouterIdV6 of(final Ip6Address ip6RouterId,
                                       final short sType) {
        return new BgpAttrRouterIdV6(ip6RouterId, sType);
    }

    /**
     * Reads the IPv6 Router-ID.
     *
     * @param cb ChannelBuffer
     * @param sType TLV type
     * @return object of BgpAttrRouterIdV6
     * @throws BgpParseException while parsing BgpAttrRouterIdV6
     */
    public static BgpAttrRouterIdV6 read(ChannelBuffer cb, short sType)
            throws BgpParseException {
        byte[] ipBytes;
        Ip6Address ip6RouterId;

        short lsAttrLength = cb.readShort();

        if ((lsAttrLength != 16) || (cb.readableBytes() < lsAttrLength)) {
            Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR,
                                   BgpErrorType.ATTRIBUTE_LENGTH_ERROR,
                                   lsAttrLength);
        }

        ipBytes = new byte[lsAttrLength];
        cb.readBytes(ipBytes);
        ip6RouterId = Ip6Address.valueOf(ipBytes);
        return BgpAttrRouterIdV6.of(ip6RouterId, sType);
    }

    /**
     * Returns IPV6 router ID.
     *
     * @return Router ID
     */
    public Ip6Address attrRouterId() {
        return ip6RouterId;
    }

    @Override
    public short getType() {
        return sType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip6RouterId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof BgpAttrRouterIdV6) {
            BgpAttrRouterIdV6 other = (BgpAttrRouterIdV6) obj;
            return Objects.equals(ip6RouterId, other.ip6RouterId);
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer cb) {
        // TODO This will be implemented in the next version
        return 0;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass()).omitNullValues()
                .add("ip6RouterId", ip6RouterId).toString();
    }

    @Override
    public int compareTo(Object o) {
        // TODO Auto-generated method stub
        return 0;
    }
}
