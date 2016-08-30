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

import io.netty.buffer.ByteBuf;
import org.onosproject.lisp.msg.exceptions.LispParseError;
import org.onosproject.lisp.msg.exceptions.LispReaderException;
import org.onosproject.lisp.msg.exceptions.LispWriterException;

import java.util.Objects;

import static org.onosproject.lisp.msg.types.AddressFamilyIdentifierEnum.*;

/**
 * LISP Locator address typed by Address Family Identifier (AFI).
 */
public abstract class LispAfiAddress {

    private final AddressFamilyIdentifierEnum afi;

    /**
     * Initializes AFI enumeration value.
     *
     * @param afi address family identifier
     */
    protected LispAfiAddress(AddressFamilyIdentifierEnum afi) {
        this.afi = afi;
    }

    /**
     * Obtains AFI enumeration value.
     *
     * @return AFI enumeration value
     */
    public AddressFamilyIdentifierEnum getAfi() {
        return afi;
    }

    @Override
    public int hashCode() {
        return Objects.hash(afi);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        LispAfiAddress other = (LispAfiAddress) obj;
        if (afi != other.afi) {
            return false;
        }
        return true;
    }

    /**
     * AFI address reader class.
     */
    public static class AfiAddressReader implements LispAddressReader<LispAfiAddress> {

        @Override
        public LispAfiAddress readFrom(ByteBuf byteBuf)
                                    throws LispParseError, LispReaderException {

            int index = byteBuf.readerIndex();

            // AFI code -> 16 bits
            short afiCode = (short) byteBuf.getUnsignedShort(index);

            // handle IPv4 and IPv6 address
            if (afiCode == IP4.getIanaCode() ||
                afiCode == IP6.getIanaCode()) {
                return new LispIpAddress.IpAddressReader().readFrom(byteBuf);
            }

            // handle distinguished name address
            if (afiCode == DISTINGUISHED_NAME.getIanaCode()) {
                return new LispDistinguishedNameAddress.DistinguishedNameAddressReader().readFrom(byteBuf);
            }

            // handle MAC address
            if (afiCode == MAC.getIanaCode()) {
                return new LispMacAddress.MacAddressReader().readFrom(byteBuf);
            }

            // handle LCAF address
            if (afiCode == LCAF.getIanaCode()) {
                return new LispLcafAddress.LcafAddressReader().readFrom(byteBuf);
            }

            // handle autonomous system address
            if (afiCode == AS.getIanaCode()) {
                return new LispAsAddress.AsAddressReader().readFrom(byteBuf);
            }

            return null;
        }
    }

    /**
     * AFI address writer class.
     */
    public static class AfiAddressWriter implements LispAddressWriter<LispAfiAddress> {

        @Override
        public void writeTo(ByteBuf byteBuf, LispAfiAddress address) throws LispWriterException {

            // AFI code
            byteBuf.writeShort(address.getAfi().getIanaCode());

            switch (address.getAfi()) {
                case IP4:
                    new LispIpAddress.IpAddressWriter().writeTo(byteBuf, (LispIpv4Address) address);
                    break;
                case IP6:
                    new LispIpAddress.IpAddressWriter().writeTo(byteBuf, (LispIpv6Address) address);
                    break;
                case DISTINGUISHED_NAME:
                    new LispDistinguishedNameAddress.DistinguishedNameAddressWriter().writeTo(byteBuf,
                            (LispDistinguishedNameAddress) address);
                    break;
                case MAC:
                    new LispMacAddress.MacAddressWriter().writeTo(byteBuf, (LispMacAddress) address);
                    break;
                case LCAF:
                    new LispLcafAddress.LcafAddressWriter().writeTo(byteBuf, (LispLcafAddress) address);
                    break;
                case AS:
                    new LispAsAddress.AsAddressWriter().writeTo(byteBuf, (LispAsAddress) address);
                    break;
                default: break; // TODO: need log warning message
            }
        }
    }
}
