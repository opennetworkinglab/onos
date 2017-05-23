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
package org.onosproject.lisp.msg.types.lcaf;

import com.google.common.collect.ImmutableList;
import io.netty.buffer.ByteBuf;
import org.onosproject.lisp.msg.exceptions.LispParseError;
import org.onosproject.lisp.msg.exceptions.LispReaderException;
import org.onosproject.lisp.msg.exceptions.LispWriterException;
import org.onosproject.lisp.msg.types.AddressFamilyIdentifierEnum;
import org.onosproject.lisp.msg.types.LispAddressReader;
import org.onosproject.lisp.msg.types.LispAddressWriter;
import org.onosproject.lisp.msg.types.LispAfiAddress;
import org.onosproject.lisp.msg.types.LispIpv4Address;
import org.onosproject.lisp.msg.types.LispIpv4Address.Ipv4AddressWriter;
import org.onosproject.lisp.msg.types.LispIpv6Address;
import org.onosproject.lisp.msg.types.LispIpv6Address.Ipv6AddressWriter;

import java.util.List;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * List type LCAF address class.
 * <p>
 * List type is defined in draft-ietf-lisp-lcaf-22
 * https://tools.ietf.org/html/draft-ietf-lisp-lcaf-22#page-23
 *
 * <pre>
 * {@literal
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |           AFI = 16387         |     Rsvd1     |     Flags     |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |   Type = 1    |     Rsvd2     |            Length             |
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

        checkArgument(checkAddressValidity(addresses), "Malformed addresses, please " +
                "specify IPv4 address first, and then specify IPv6 address");

        this.addresses = addresses;
    }

    /**
     * Initializes list type LCAF address.
     *
     * @param reserved1 reserved1 field
     * @param flag      flag
     * @param reserved2 reserved2 field
     * @param addresses a set of IPv4 and IPv6 addresses
     */
    public LispListLcafAddress(byte reserved1, byte reserved2, byte flag,
                               List<LispAfiAddress> addresses) {
        super(LispCanonicalAddressFormatEnum.LIST, reserved1, flag, reserved2, LENGTH);

        checkArgument(checkAddressValidity(addresses), "Malformed addresses, please " +
                "specify IPv4 address first, and then specify IPv6 address");

        this.addresses = addresses;
    }

    /**
     * Checks the validity of a collection of input addresses.
     *
     * @param addresses a collection of addresses
     * @return validity result
     */
    private boolean checkAddressValidity(List<LispAfiAddress> addresses) {
        // we need to make sure that the address collection is comprised of
        // one IPv4 address and one IPv6 address

        if (addresses == null || addresses.size() != 2) {
            return false;
        }

        LispAfiAddress ipv4 = addresses.get(0);
        LispAfiAddress ipv6 = addresses.get(1);

        return ipv4.getAfi() == AddressFamilyIdentifierEnum.IP4 &&
                ipv6.getAfi() == AddressFamilyIdentifierEnum.IP6;

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
    public static class ListLcafAddressReader
                        implements LispAddressReader<LispListLcafAddress> {

        @Override
        public LispListLcafAddress readFrom(ByteBuf byteBuf)
                                    throws LispParseError, LispReaderException {

            LispLcafAddress lcafAddress = LispLcafAddress.deserializeCommon(byteBuf);

            LispAfiAddress.AfiAddressReader reader = new LispAfiAddress.AfiAddressReader();

            LispAfiAddress ipv4 = reader.readFrom(byteBuf);
            LispAfiAddress ipv6 = reader.readFrom(byteBuf);

            return new LispListLcafAddress(lcafAddress.getReserved1(),
                                            lcafAddress.getReserved2(),
                                            lcafAddress.getFlag(),
                                            ImmutableList.of(ipv4, ipv6));
        }
    }

    /**
     * List LCAF address writer class.
     */
    public static class ListLcafAddressWriter
                        implements LispAddressWriter<LispListLcafAddress> {

        private static final int IPV4_ADDRESS_INDEX = 0;
        private static final int IPV6_ADDRESS_INDEX = 1;

        @Override
        public void writeTo(ByteBuf byteBuf, LispListLcafAddress address)
                                                    throws LispWriterException {

            int lcafIndex = byteBuf.writerIndex();
            LispLcafAddress.serializeCommon(byteBuf, address);

            Ipv4AddressWriter v4Writer = new Ipv4AddressWriter();
            Ipv6AddressWriter v6Writer = new Ipv6AddressWriter();

            LispAfiAddress ipv4 = address.getAddresses().get(IPV4_ADDRESS_INDEX);
            LispAfiAddress ipv6 = address.getAddresses().get(IPV6_ADDRESS_INDEX);

            // IPv4 address
            byteBuf.writeShort(ipv4.getAfi().getIanaCode());
            v4Writer.writeTo(byteBuf, (LispIpv4Address) ipv4);

            // IPv6 address
            byteBuf.writeShort(ipv6.getAfi().getIanaCode());
            v6Writer.writeTo(byteBuf, (LispIpv6Address) ipv6);

            LispLcafAddress.updateLength(lcafIndex, byteBuf);
        }
    }
}
