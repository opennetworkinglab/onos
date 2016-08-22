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
package org.onosproject.lisp.msg.types;

import com.google.common.collect.ImmutableList;
import io.netty.buffer.ByteBuf;
import org.onosproject.lisp.msg.exceptions.LispParseError;
import org.onosproject.lisp.msg.exceptions.LispReaderException;
import org.onosproject.lisp.msg.exceptions.LispWriterException;

import java.util.List;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * List type LCAF address class.
 * <p>
 * List type is defined in draft-ietf-lisp-lcaf-13
 * https://tools.ietf.org/html/draft-ietf-lisp-lcaf-13#page-21
 *
 * <pre>
 * {@literal
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |           AFI = 16387         |     Rsvd1     |     Flags     |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |   Type = 1    |     Rsvd2     |         2 + 4 + 2 + 16        |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |            AFI = 1            |       IPv4 Address ...        |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |     ...  IPv4 Address         |            AFI = 2            |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                          IPv6 Address ...                     |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                     ...  IPv6 Address  ...                    |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                     ...  IPv6 Address  ...                    |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                     ...  IPv6 Address                         |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * }</pre>
 */
public final class LispListLcafAddress extends LispLcafAddress {

    private static final short LENGTH = 24;
    List<LispAfiAddress> addresses;

    /**
     * Initializes list type LCAF address.
     *
     * @param addresses a set of IPv4 and IPv6 addresses
     */
    public LispListLcafAddress(List<LispAfiAddress> addresses) {
        super(LispCanonicalAddressFormatEnum.LIST, LENGTH);
        this.addresses = addresses;
    }

    /**
     * Initializes list type LCAF address.
     *
     * @param reserved1 reserved1 field
     * @param flag flag
     * @param reserved2 reserved2 field
     * @param addresses a set of IPv4 and IPv6 addresses
     */
    public LispListLcafAddress(byte reserved1, byte reserved2, byte flag,
                               List<LispAfiAddress> addresses) {
        super(LispCanonicalAddressFormatEnum.LIST, reserved1, flag, reserved2, LENGTH);
        this.addresses = addresses;
    }

    /**
     * Obtains a set of AFI addresses including IPv4 and IPv6.
     *
     * @return a set of AFI addresses
     */
    public List<LispAfiAddress> getAddresses() {
        return ImmutableList.copyOf(addresses);
    }

    @Override
    public int hashCode() {
        return Objects.hash(addresses);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof LispListLcafAddress) {
            final LispListLcafAddress other = (LispListLcafAddress) obj;
            return Objects.equals(this.addresses, other.addresses);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("addresses", addresses)
                .toString();
    }

    /**
     * List LCAF address reader class.
     */
    public static class ListLcafAddressReader implements LispAddressReader<LispListLcafAddress> {

        @Override
        public LispListLcafAddress readFrom(ByteBuf byteBuf) throws LispParseError, LispReaderException {

            LispLcafAddress lcafAddress = LispLcafAddress.deserializeCommon(byteBuf);

            LispAfiAddress ipv4 = new LispAfiAddress.AfiAddressReader().readFrom(byteBuf);
            LispAfiAddress ipv6 = new LispAfiAddress.AfiAddressReader().readFrom(byteBuf);

            return new LispListLcafAddress(lcafAddress.getReserved1(), lcafAddress.getReserved2(),
                                           lcafAddress.getFlag(), ImmutableList.of(ipv4, ipv6));
        }
    }

    /**
     * List LCAF address writer class.
     */
    public static class ListLcafAddressWriter implements LispAddressWriter<LispListLcafAddress> {

        @Override
        public void writeTo(ByteBuf byteBuf, LispListLcafAddress address) throws LispWriterException {

            LispLcafAddress.serializeCommon(byteBuf, address);

            LispIpv4Address.Ipv4AddressWriter v4Writer = new LispIpv4Address.Ipv4AddressWriter();
            LispIpv6Address.Ipv6AddressWriter v6Writer = new LispIpv6Address.Ipv6AddressWriter();

            v4Writer.writeTo(byteBuf, (LispIpv4Address) address.getAddresses().get(0));
            v6Writer.writeTo(byteBuf, (LispIpv6Address) address.getAddresses().get(1));
        }
    }
}
