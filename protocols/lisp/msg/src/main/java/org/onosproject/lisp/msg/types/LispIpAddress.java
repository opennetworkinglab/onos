/*
 * Copyright 2016-present Open Networking Foundation
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
import org.onlab.packet.IpAddress;
import org.onosproject.lisp.msg.exceptions.LispParseError;
import org.onosproject.lisp.msg.exceptions.LispReaderException;
import org.onosproject.lisp.msg.exceptions.LispWriterException;
import org.onosproject.lisp.msg.types.LispIpv4Address.Ipv4AddressWriter;
import org.onosproject.lisp.msg.types.LispIpv6Address.Ipv6AddressWriter;

/**
 * IP address that is used by LISP Locator.
 */
public abstract class LispIpAddress extends LispAfiAddress {

    protected final IpAddress address;

    /**
     * Initializes LISP locator's IP address with AFI enum.
     *
     * @param address IP address
     * @param afi AFI enum
     */
    protected LispIpAddress(IpAddress address, AddressFamilyIdentifierEnum afi) {
        super(afi);
        this.address = address;
    }

    /**
     * Obtains LISP locator's IP address.
     *
     * @return IP address
     */
    public IpAddress getAddress() {
        return address;
    }

    @Override
    public int hashCode() {
        return address.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return address.equals(obj);
    }

    @Override
    public String toString() {
        return address.toString();
    }

    /**
     * IP address reader class.
     */
    public static class IpAddressReader implements LispAddressReader<LispIpAddress> {

        @Override
        public LispIpAddress readFrom(ByteBuf byteBuf)
                                    throws LispParseError, LispReaderException {

            // AFI code -> 16 bits
            short afiCode = (short) byteBuf.readUnsignedShort();

            if (afiCode == 1) {
                return new LispIpv4Address.Ipv4AddressReader().readFrom(byteBuf);
            } else if (afiCode == 2) {
                return new LispIpv6Address.Ipv6AddressReader().readFrom(byteBuf);
            }

            return null;
        }
    }

    /**
     * IP address writer class.
     */
    public static class IpAddressWriter implements LispAddressWriter<LispIpAddress> {

        @Override
        public void writeTo(ByteBuf byteBuf, LispIpAddress address)
                                                    throws LispWriterException {
            if (address.getAddress().isIp4()) {
                new Ipv4AddressWriter().writeTo(byteBuf, (LispIpv4Address) address);
            }
            if (address.getAddress().isIp6()) {
                new Ipv6AddressWriter().writeTo(byteBuf, (LispIpv6Address) address);
            }
        }
    }
}
