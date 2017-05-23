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
import org.onosproject.lisp.msg.exceptions.LispWriterException;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Distinguished name address that is used by LISP Locator.
 */
public class LispDistinguishedNameAddress extends LispAfiAddress {

    private final String distinguishedName;

    /**
     * Initializes LISP locator's distinguished name address with AFI enum.
     *
     * @param distinguishedName distinguished name address
     */
    public LispDistinguishedNameAddress(String distinguishedName) {
        super(AddressFamilyIdentifierEnum.DISTINGUISHED_NAME);
        this.distinguishedName = distinguishedName;
    }

    /**
     * Obtains LISP locator's distinguished name address.
     *
     * @return distinguished name address
     */
    public String getDistinguishedName() {
        return distinguishedName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(distinguishedName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof LispDistinguishedNameAddress) {
            final LispDistinguishedNameAddress other = (LispDistinguishedNameAddress) obj;
            return Objects.equals(this.distinguishedName, other.distinguishedName);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("distinguished name", distinguishedName)
                .toString();
    }

    /**
     * Distinguished name address reader class.
     */
    public static class DistinguishedNameAddressReader
                        implements LispAddressReader<LispDistinguishedNameAddress> {

        @Override
        public LispDistinguishedNameAddress readFrom(ByteBuf byteBuf)
                                                        throws LispParseError {

            StringBuilder sb = new StringBuilder();
            byte character;
            while (byteBuf.readerIndex() < byteBuf.writerIndex()) {
                character = byteBuf.readByte();
                sb.append((char) character);
            }

            return new LispDistinguishedNameAddress(sb.toString());
        }
    }

    /**
     * Distinguished name address writer class.
     */
    public static class DistinguishedNameAddressWriter
                        implements LispAddressWriter<LispDistinguishedNameAddress> {

        @Override
        public void writeTo(ByteBuf byteBuf, LispDistinguishedNameAddress address)
                                                    throws LispWriterException {
            String distinguishedName = address.getDistinguishedName();
            byte[] nameBytes = distinguishedName.getBytes();
            for (byte nameByte : nameBytes) {
                byteBuf.writeByte(nameByte);
            }
        }
    }
}
