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
package org.onosproject.bgpio.types.attr;

import java.util.Objects;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onlab.packet.Ip4Address;
import org.onosproject.bgpio.exceptions.BGPParseException;
import org.onosproject.bgpio.types.BGPErrorType;
import org.onosproject.bgpio.types.BGPValueType;
import org.onosproject.bgpio.util.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Implements BGP attribute node router ID.
 */
public class BgpAttrRouterIdV4 implements BGPValueType {

    protected static final Logger log = LoggerFactory
            .getLogger(BgpAttrRouterIdV4.class);

    public short sType;

    /* IPv4 Router-ID of Node */
    private Ip4Address ip4RouterId;

    /**
     * Constructor to initialize the value.
     *
     * @param ip4RouterId IPV4 address of router
     * @param sType TLV type
     */
    BgpAttrRouterIdV4(Ip4Address ip4RouterId, short sType) {
        this.ip4RouterId = ip4RouterId;
        this.sType = sType;
    }

    /**
     * Reads the IPv4 Router-ID.
     *
     * @param cb ChannelBuffer
     * @param sType type
     * @return object of BgpAttrRouterIdV4
     * @throws BGPParseException while parsing BgpAttrNodeRouterId
     */
    public static BgpAttrRouterIdV4 read(ChannelBuffer cb, short sType)
            throws BGPParseException {
        byte[] ipBytes;
        Ip4Address ip4RouterId;

        short lsAttrLength = cb.readShort();

        if (4 != lsAttrLength) {
            Validation.validateLen(BGPErrorType.UPDATE_MESSAGE_ERROR,
                                   BGPErrorType.ATTRIBUTE_LENGTH_ERROR,
                                   lsAttrLength);
        }

        ipBytes = new byte[lsAttrLength];
        cb.readBytes(ipBytes);
        ip4RouterId = Ip4Address.valueOf(ipBytes);
        return new BgpAttrRouterIdV4(ip4RouterId, sType);
    }

    /**
     * Returns the IPV4 router ID.
     *
     * @return Router ID
     */
    Ip4Address getAttrRouterId() {
        return ip4RouterId;
    }

    @Override
    public short getType() {
        return sType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip4RouterId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof BgpAttrRouterIdV4) {
            BgpAttrRouterIdV4 other = (BgpAttrRouterIdV4) obj;
            return Objects.equals(ip4RouterId, other.ip4RouterId);
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer cb) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass()).omitNullValues()
                .add("ip4RouterId", ip4RouterId).toString();
    }
}
