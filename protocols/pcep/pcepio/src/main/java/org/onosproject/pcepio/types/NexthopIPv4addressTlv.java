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

import java.util.Objects;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.protocol.PcepVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * NexthopIPv6addressTlv provides Ipv4 address of next hop.
 */
public class NexthopIPv4addressTlv implements PcepValueType {

    /*
        Reference :draft-zhao-pce-pcep-extension-for-pce-controller-01

        0                   1                   2                     3
        0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       | Type=TBD                      |          Length = 4           |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                     nexthop IPv4 address                      |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

                      NEXTHOP-IPV4-ADDRESS TLV

     */
    protected static final Logger log = LoggerFactory.getLogger(NexthopIPv4addressTlv.class);

    public static final short TYPE = (short) 65289; //to be defined
    //Length is header + value
    public static final short LENGTH = 8;
    public static final short VALUE_LENGTH = 4;

    private final int rawValue;

    /**
     * Constructor to initialize next hop IPv4 address.
     *
     * @param rawValue next hop IPv4 address
     */
    public NexthopIPv4addressTlv(int rawValue) {
        this.rawValue = rawValue;
    }

    /**
     * Return next hop IPv4 address tlv.
     *
     * @param raw of next hop IPv4 address
     * @return object of NexthopIPv4addressTlv
     */
    public static NexthopIPv4addressTlv of(final int raw) {
        return new NexthopIPv4addressTlv(raw);
    }

    /**
     * Returns next hop IPv4 address.
     *
     * @return next hop IPv4 address
     */
    public int getInt() {
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
        return VALUE_LENGTH;
    }

    @Override
    public int hashCode() {
        return Objects.hash(rawValue);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NexthopIPv4addressTlv) {
            NexthopIPv4addressTlv other = (NexthopIPv4addressTlv) obj;
            return Objects.equals(this.rawValue, other.rawValue);
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer c) {
        int iStartIndex = c.writerIndex();
        c.writeShort(TYPE);
        c.writeShort(VALUE_LENGTH);
        c.writeInt(rawValue);
        return c.writerIndex() - iStartIndex;
    }

    /**
     * Reads the channel buffer and returns object of NexthopIPv4addressTlv.
     *
     * @param c type of channel buffer
     * @return object of NexthopIPv4addressTlv
     */
    public static NexthopIPv4addressTlv read(ChannelBuffer c) {
        return NexthopIPv4addressTlv.of(c.readInt());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("Type", TYPE)
                .add("Length", VALUE_LENGTH)
                .add("Ipv4Address ", rawValue)
                .toString();
    }
}
