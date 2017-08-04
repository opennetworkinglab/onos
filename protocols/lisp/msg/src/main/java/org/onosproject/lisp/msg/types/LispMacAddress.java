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
import org.onlab.packet.MacAddress;
import org.onosproject.lisp.msg.exceptions.LispParseError;
import org.onosproject.lisp.msg.exceptions.LispWriterException;

import java.util.Objects;

/**
 * MAC address that is used by LISP Locator.
 */
public class LispMacAddress extends LispAfiAddress {

    protected final MacAddress address;

    /**
     * Initializes MAC address.
     *
     * @param address MAC address
     */
    public LispMacAddress(MacAddress address) {
        super(AddressFamilyIdentifierEnum.MAC);
        this.address = address;
    }

    /**
     * Obtains LISP locator's MAC address.
     *
     * @return MAC address
     */
    public MacAddress getAddress() {
        return address;
    }

    @Override
    public int hashCode() {
        return address.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof LispMacAddress) {
            final LispMacAddress other = (LispMacAddress) obj;
            return Objects.equals(this.address, other.address) &&
                    Objects.equals(this.getAfi(), other.getAfi());
        }
        return false;
    }

    @Override
    public String toString() {
        return address.toString();
    }

    /**
     * MAC address reader class.
     */
    public static class MacAddressReader implements LispAddressReader<LispMacAddress> {

        private static final int SIZE_OF_MAC_ADDRESS = 6;

        @Override
        public LispMacAddress readFrom(ByteBuf byteBuf) throws LispParseError {

            byte[] macByte = new byte[SIZE_OF_MAC_ADDRESS];
            byteBuf.readBytes(macByte);
            MacAddress macAddress = MacAddress.valueOf(macByte);

            return new LispMacAddress(macAddress);
        }
    }

    /**
     * MAC address writer class.
     */
    public static class MacAddressWriter implements LispAddressWriter<LispMacAddress> {

        @Override
        public void writeTo(ByteBuf byteBuf, LispMacAddress address)
                                                throws LispWriterException {
            byte[] macByte = address.getAddress().toBytes();
            byteBuf.writeBytes(macByte);
        }
    }
}
