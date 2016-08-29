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
import org.onlab.packet.IpAddress;
import org.onosproject.lisp.msg.exceptions.LispParseError;
import org.onosproject.lisp.msg.exceptions.LispWriterException;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * IPv6 address that is used by LISP Locator.
 */
public class LispIpv6Address extends LispIpAddress {

    /**
     * Initializes LISP locator's IPv6 address.
     *
     * @param address IP address
     */
    public LispIpv6Address(IpAddress address) {
        super(address, AddressFamilyIdentifierEnum.IP6);
        checkArgument(address.isIp6());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof LispIpv6Address) {
            final LispIpv6Address other = (LispIpv6Address) obj;
            return Objects.equals(this.address, other.address) &&
                    Objects.equals(this.getAfi(), other.getAfi());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, getAfi());
    }

    /**
     * IPv6 address reader class.
     */
    public static class Ipv6AddressReader implements LispAddressReader<LispIpv6Address> {

        private static final int SIZE_OF_IPV6_ADDRESS = 16;

        @Override
        public LispIpv6Address readFrom(ByteBuf byteBuf) throws LispParseError {

            byte[] ipByte = new byte[SIZE_OF_IPV6_ADDRESS];
            byteBuf.readBytes(ipByte);
            IpAddress ipAddress = IpAddress.valueOf(IpAddress.Version.INET6, ipByte);

            return new LispIpv6Address(ipAddress);
        }
    }

    /**
     * Ipv6 address writer class.
     */
    public static class Ipv6AddressWriter implements LispAddressWriter<LispIpv6Address> {

        @Override
        public void writeTo(ByteBuf byteBuf, LispIpv6Address address) throws LispWriterException {
            byte[] ipByte = address.getAddress().getIp6Address().toOctets();
            byteBuf.writeBytes(ipByte);
        }
    }
}
