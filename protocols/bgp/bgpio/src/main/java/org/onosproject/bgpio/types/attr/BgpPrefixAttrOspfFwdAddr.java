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
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Address;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.types.BgpErrorType;
import org.onosproject.bgpio.types.BgpValueType;
import org.onosproject.bgpio.util.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Implements BGP prefix OSPF Forwarding address attribute.
 */
public class BgpPrefixAttrOspfFwdAddr implements BgpValueType {

    protected static final Logger log = LoggerFactory
            .getLogger(BgpPrefixAttrOspfFwdAddr.class);

    public static final int ATTR_PREFIX_OSPFFWDADDR = 1156;
    public static final int IPV4_LEN = 4;
    public static final int IPV6_LEN = 16;

    /* OSPF Forwarding Address */
    private final short lsAttrLength;
    private final Ip4Address ip4RouterId;
    private final Ip6Address ip6RouterId;

    /**
     * Constructor to initialize the value.
     *
     * @param lsAttrLength length of the IP address
     * @param ip4RouterId Valid IPV4 address if length is 4 else null
     * @param ip6RouterId Valid IPV6 address if length is 16 else null
     */
    public BgpPrefixAttrOspfFwdAddr(short lsAttrLength, Ip4Address ip4RouterId,
                                    Ip6Address ip6RouterId) {
        this.lsAttrLength = lsAttrLength;
        this.ip4RouterId = ip4RouterId;
        this.ip6RouterId = ip6RouterId;
    }

    /**
     * Returns object of this class with specified values.
     *
     * @param lsAttrLength length of the IP address
     * @param ip4RouterId Valid IPV4 address if length is 4 else null
     * @param ip6RouterId Valid IPV6 address if length is 16 else null
     * @return object of BgpPrefixAttrOspfFwdAddr
     */
    public static BgpPrefixAttrOspfFwdAddr of(final short lsAttrLength,
                                              final Ip4Address ip4RouterId,
                                              final Ip6Address ip6RouterId) {
        return new BgpPrefixAttrOspfFwdAddr(lsAttrLength, ip4RouterId,
                                            ip6RouterId);
    }

    /**
     * Reads the OSPF Forwarding Address.
     *
     * @param cb ChannelBuffer
     * @return object of BgpPrefixAttrOSPFFwdAddr
     * @throws BgpParseException while parsing BgpPrefixAttrOspfFwdAddr
     */
    public static BgpPrefixAttrOspfFwdAddr read(ChannelBuffer cb)
            throws BgpParseException {
        short lsAttrLength;
        byte[] ipBytes;
        Ip4Address ip4RouterId = null;
        Ip6Address ip6RouterId = null;

        lsAttrLength = cb.readShort();
        ipBytes = new byte[lsAttrLength];

        if ((cb.readableBytes() < lsAttrLength)) {
            Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR,
                                   BgpErrorType.ATTRIBUTE_LENGTH_ERROR,
                                   lsAttrLength);
        }

        cb.readBytes(ipBytes);

        if (IPV4_LEN == lsAttrLength) {
            ip4RouterId = Ip4Address.valueOf(ipBytes);
        } else if (IPV6_LEN == lsAttrLength) {
            ip6RouterId = Ip6Address.valueOf(ipBytes);
        }

        return BgpPrefixAttrOspfFwdAddr.of(lsAttrLength, ip4RouterId,
                                           ip6RouterId);
    }

    /**
     * Returns IPV4 Address of OSPF forwarding address.
     *
     * @return IPV4 address
     */
    public Ip4Address ospfv4FwdAddr() {
        return ip4RouterId;
    }

    /**
     * Returns IPV6 Address of OSPF forwarding address.
     *
     * @return IPV6 address
     */
    public Ip6Address ospfv6FwdAddr() {
        return ip6RouterId;
    }

    /**
     * Returns OSPF forwarding address length.
     *
     * @return length of the ip address
     */
    public short ospfFwdAddrLen() {
        return lsAttrLength;
    }

    @Override
    public short getType() {
        return ATTR_PREFIX_OSPFFWDADDR;
    }

    @Override
    public int hashCode() {
        if (IPV4_LEN == lsAttrLength) {
            return Objects.hash(ip4RouterId);
        } else {
            return Objects.hash(ip6RouterId);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof BgpPrefixAttrOspfFwdAddr) {
            BgpPrefixAttrOspfFwdAddr other = (BgpPrefixAttrOspfFwdAddr) obj;
            if (IPV4_LEN == lsAttrLength) {
                return Objects.equals(ip4RouterId, other.ip4RouterId);
            } else {
                return Objects.equals(ip6RouterId, other.ip6RouterId);
            }
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
        if (IPV4_LEN == lsAttrLength) {
            return MoreObjects.toStringHelper(getClass()).omitNullValues()
                    .add("ip4RouterId", ip4RouterId).toString();
        } else {
            return MoreObjects.toStringHelper(getClass()).omitNullValues()
                    .add("ip6RouterId", ip6RouterId).toString();
        }
    }

    @Override
    public int compareTo(Object o) {
        // TODO Auto-generated method stub
        return 0;
    }
}
