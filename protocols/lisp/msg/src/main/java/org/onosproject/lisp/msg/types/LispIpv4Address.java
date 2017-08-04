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
import org.onosproject.lisp.msg.exceptions.LispWriterException;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * IPv4 address that is used by LISP Locator.
 */
public class LispIpv4Address extends LispIpAddress {

    /**
     * Initializes LISP locator's IPv4 address.
     *
     * @param address IP address
     */
    public LispIpv4Address(IpAddress address) {
        super(address, AddressFamilyIdentifierEnum.IP4);
        checkArgument(address.isIp4());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof LispIpv4Address) {
            final LispIpv4Address other = (LispIpv4Address) obj;
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
     * IPv4 address reader class.
     */
    public static class Ipv4AddressReader implements LispAddressReader<LispIpv4Address> {

        private static final int SIZE_OF_IPV4_ADDRESS = 4;

        @Override
        public LispIpv4Address readFrom(ByteBuf byteBuf) throws LispParseError {

            byte[] ipByte = new byte[SIZE_OF_IPV4_ADDRESS];
            byteBuf.readBytes(ipByte);
            IpAddress ipAddress = IpAddress.valueOf(IpAddress.Version.INET, ipByte);

            return new LispIpv4Address(ipAddress);
        }
    }

    /**
     * IPv4 address writer class.
     */
    public static class Ipv4AddressWriter implements LispAddressWriter<LispIpv4Address> {

        @Override
        public void writeTo(ByteBuf byteBuf, LispIpv4Address address) throws LispWriterException {
            byte[] ipByte = address.getAddress().getIp4Address().toOctets();
            byteBuf.writeBytes(ipByte);
        }
    }
}
