/*
 * Copyright 2017-present Open Networking Foundation
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

import io.netty.buffer.ByteBuf;
import org.onosproject.lisp.msg.exceptions.LispParseError;
import org.onosproject.lisp.msg.exceptions.LispReaderException;
import org.onosproject.lisp.msg.exceptions.LispWriterException;
import org.onosproject.lisp.msg.types.LispAddressReader;
import org.onosproject.lisp.msg.types.LispAddressWriter;
import org.onosproject.lisp.msg.types.LispAfiAddress;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Nonce locator data type LCAF address class.
 * <p>
 * Nonce locator data type is defined in draft-ietf-lisp-lcaf-22
 * https://tools.ietf.org/html/draft-ietf-lisp-lcaf-22#page-32
 *
 * <pre>
 * {@literal
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |           AFI = 16387         |     Rsvd1     |     Flags     |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |   Type = 8    |     Rsvd2     |             Length            |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |   Reserved    |                  Nonce                        |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |              AFI = x          |         Address  ...          |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * }</pre>
 */
public final class LispNonceLcafAddress extends LispLcafAddress {

    private final int nonce;
    private final LispAfiAddress address;

    /**
     * Initializes nonce locator data type LCAF address.
     *
     * @param nonce   nonce
     * @param address address
     */
    private LispNonceLcafAddress(int nonce, LispAfiAddress address) {
        super(LispCanonicalAddressFormatEnum.NONCE);
        this.nonce = nonce;
        this.address = address;
    }

    /**
     * Obtains nonce.
     *
     * @return nonce
     */
    public int getNonce() {
        return nonce;
    }

    /**
     * Obtains address.
     *
     * @return address
     */
    public LispAfiAddress getAddress() {
        return address;
    }

    @Override
    public int hashCode() {
        return Objects.hash(nonce, address);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof LispNonceLcafAddress) {
            final LispNonceLcafAddress other = (LispNonceLcafAddress) obj;
            return Objects.equals(this.nonce, other.nonce) &&
                    Objects.equals(this.address, other.address);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("nonce", nonce)
                .add("address", address)
                .toString();
    }

    public static final class NonceAddressBuilder
                        extends LcafAddressBuilder<NonceAddressBuilder> {
        private int nonce;
        private LispAfiAddress address;

        /**
         * Sets nonce.
         *
         * @param nonce nonce
         * @return NonceAddressBuilder object
         */
        public NonceAddressBuilder withNonce(int nonce) {
            this.nonce = nonce;
            return this;
        }

        /**
         * Sets address.
         *
         * @param address address
         * @return NonceAddressBuilder object
         */
        public NonceAddressBuilder withAddress(LispAfiAddress address) {
            this.address = address;
            return this;
        }

        /**
         * Builds LispNonceLcafAddress instance.
         *
         * @return LispNonceLcafAddress instance
         */
        @Override
        public LispNonceLcafAddress build() {

            checkNotNull(address, "Must specify an address");

            return new LispNonceLcafAddress(nonce, address);
        }
    }

    /**
     * Nonce LCAF address reader class.
     */
    public static class NonceLcafAddressReader
                        implements LispAddressReader<LispNonceLcafAddress> {

        private static final int NONCE_SHIFT_BIT = 16;
        private static final int RESERVED_SKIP_LENGTH = 1;

        @Override
        public LispNonceLcafAddress readFrom(ByteBuf byteBuf)
                                    throws LispParseError, LispReaderException {

            deserializeCommon(byteBuf);

            byteBuf.skipBytes(RESERVED_SKIP_LENGTH);

            // nonce -> 24 bits
            int nonceFirst = byteBuf.readShort() << NONCE_SHIFT_BIT;
            int nonceSecond = byteBuf.readInt();
            int nonce = nonceFirst + nonceSecond;

            LispAfiAddress address = new AfiAddressReader().readFrom(byteBuf);

            return new NonceAddressBuilder()
                            .withNonce(nonce)
                            .withAddress(address)
                            .build();
        }
    }

    /**
     * Nonce LCAF address writer class.
     */
    public static class NonceLcafAddressWriter
                        implements LispAddressWriter<LispNonceLcafAddress> {

        private static final int UNUSED_ZERO = 0;
        private static final int NONCE_SHIFT_BIT = 16;

        @Override
        public void writeTo(ByteBuf byteBuf, LispNonceLcafAddress address)
                                                    throws LispWriterException {
            int lcafIndex = byteBuf.writerIndex();
            LispLcafAddress.serializeCommon(byteBuf, address);

            // reserved field
            byteBuf.writeByte(UNUSED_ZERO);

            // nonce field
            int nonceFirst = address.getNonce() >> NONCE_SHIFT_BIT;
            byteBuf.writeShort((short) nonceFirst);
            int nonceSecond = address.getNonce() - (nonceFirst << NONCE_SHIFT_BIT);
            byteBuf.writeInt(nonceSecond);

            // address
            AfiAddressWriter writer = new AfiAddressWriter();
            writer.writeTo(byteBuf, address.getAddress());

            LispLcafAddress.updateLength(lcafIndex, byteBuf);
        }
    }
}
