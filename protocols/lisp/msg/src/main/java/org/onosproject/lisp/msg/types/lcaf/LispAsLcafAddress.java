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
 * AS Numbers type LCAF address class.
 * <p>
 * AS Number type is defined in draft-ietf-lisp-lcaf-22
 * https://tools.ietf.org/html/draft-ietf-lisp-lcaf-22#page-9
 *
 * <pre>
 * {@literal
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |           AFI = 16387         |     Rsvd1     |     Flags     |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |   Type = 3    |     Rsvd2     |             Length            |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                           AS Number                           |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |              AFI = x          |         Address  ...          |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * }</pre>
 */
public final class LispAsLcafAddress extends LispLcafAddress {

    private final LispAfiAddress address;
    private final int asNumber;

    /**
     * Initializes AS numbers type LCAF address.
     *
     * @param asNumber AS number
     * @param address  AFI address
     */
    private LispAsLcafAddress(int asNumber, LispAfiAddress address) {
        super(LispCanonicalAddressFormatEnum.AS);
        this.asNumber = asNumber;
        this.address = address;
    }

    /**
     * Obtains address.
     *
     * @return address
     */
    public LispAfiAddress getAddress() {
        return address;
    }

    /**
     * Obtains AS number.
     *
     * @return AS number
     */
    public int getAsNumber() {
        return asNumber;
    }

    @Override
    public int hashCode() {
        return Objects.hash(asNumber, address);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof LispAsLcafAddress) {
            final LispAsLcafAddress other = (LispAsLcafAddress) obj;
            return Objects.equals(this.asNumber, other.asNumber) &&
                    Objects.equals(this.address, other.address);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("asNumber", asNumber)
                .add("address", address)
                .toString();
    }

    public static final class AsAddressBuilder
                                extends LcafAddressBuilder<AsAddressBuilder> {
        private int asNumber;
        private LispAfiAddress address;

        /**
         * Sets AS number.
         *
         * @param asNumber AS number
         * @return AsAddressBuilder object
         */
        public AsAddressBuilder withAsNumber(int asNumber) {
            this.asNumber = asNumber;
            return this;
        }

        /**
         * Sets AFI address.
         *
         * @param address AFI address
         * @return AsAddressBuilder object
         */
        public AsAddressBuilder withAddress(LispAfiAddress address) {
            this.address = address;
            return this;
        }

        /**
         * Builds LispAsLcafAddress instance.
         *
         * @return LispAsLcafAddress instance
         */
        public LispAsLcafAddress build() {

            checkNotNull(address, "Must specify an address");

            return new LispAsLcafAddress(asNumber, address);
        }
    }

    /**
     * AS number LCAF address reader class.
     */
    public static class AsLcafAddressReader
                        implements LispAddressReader<LispAsLcafAddress> {

        @Override
        public LispAsLcafAddress readFrom(ByteBuf byteBuf)
                                    throws LispParseError, LispReaderException {

            LispLcafAddress.deserializeCommon(byteBuf);

            int asNumber = (int) byteBuf.readUnsignedInt();
            LispAfiAddress address = new LispAfiAddress.AfiAddressReader().readFrom(byteBuf);

            return new AsAddressBuilder()
                            .withAsNumber(asNumber)
                            .withAddress(address)
                            .build();
        }
    }

    /**
     * AS number LCAF address writer class.
     */
    public static class AsLcafAddressWriter
                        implements LispAddressWriter<LispAsLcafAddress> {

        @Override
        public void writeTo(ByteBuf byteBuf, LispAsLcafAddress address)
                                                    throws LispWriterException {

            int lcafIndex = byteBuf.writerIndex();
            LispLcafAddress.serializeCommon(byteBuf, address);

            byteBuf.writeInt(address.getAsNumber());

            new LispAfiAddress.AfiAddressWriter().writeTo(byteBuf, address.getAddress());

            LispLcafAddress.updateLength(lcafIndex, byteBuf);
        }
    }
}
