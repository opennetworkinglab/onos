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
 * Source/Dest key type LCAF address class.
 * <p>
 * Source destination key type is defined in draft-ietf-lisp-lcaf-22
 * https://tools.ietf.org/html/draft-ietf-lisp-lcaf-22#page-20
 *
 * <pre>
 * {@literal
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |           AFI = 16387         |     Rsvd1     |     Flags     |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |   Type = 12   |     Rsvd2     |             Length            |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |            Reserved           |   Source-ML   |    Dest-ML    |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |              AFI = x          |         Source-Prefix ...     |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |              AFI = x          |     Destination-Prefix ...    |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * }</pre>
 */
public final class LispSourceDestLcafAddress extends LispLcafAddress {

    private final LispAfiAddress srcPrefix;
    private final LispAfiAddress dstPrefix;
    private final byte srcMaskLength;
    private final byte dstMaskLength;
    private final short reserved;

    /**
     * Initializes source/dest key type LCAF address.
     *
     * @param reserved      reserved
     * @param srcMaskLength source mask length
     * @param dstMaskLength destination mask length
     * @param srcPrefix     source address prefix
     * @param dstPrefix     destination address prefix
     */
    private LispSourceDestLcafAddress(short reserved, byte srcMaskLength,
                                      byte dstMaskLength,
                                      LispAfiAddress srcPrefix,
                                      LispAfiAddress dstPrefix) {
        super(LispCanonicalAddressFormatEnum.SOURCE_DEST);
        this.reserved = reserved;
        this.srcMaskLength = srcMaskLength;
        this.dstMaskLength = dstMaskLength;
        this.srcPrefix = srcPrefix;
        this.dstPrefix = dstPrefix;
    }

    /**
     * Initializes source/destination key type LCAF address.
     *
     * @param reserved1     reserved1
     * @param reserved2     reserved2
     * @param flag          flag
     * @param length        length
     * @param reserved      reserved
     * @param srcMaskLength source mask length
     * @param dstMaskLength destination mask length
     * @param srcPrefix     source address prefix
     * @param dstPrefix     destination address prefix
     */
    private LispSourceDestLcafAddress(byte reserved1, byte reserved2, byte flag,
                                      short length, short reserved,
                                      byte srcMaskLength, byte dstMaskLength,
                                      LispAfiAddress srcPrefix,
                                      LispAfiAddress dstPrefix) {
        super(LispCanonicalAddressFormatEnum.SOURCE_DEST, reserved1,
                                                        reserved2, flag, length);
        this.reserved = reserved;
        this.srcMaskLength = srcMaskLength;
        this.dstMaskLength = dstMaskLength;
        this.srcPrefix = srcPrefix;
        this.dstPrefix = dstPrefix;
    }

    /**
     * Obtains source address prefix.
     *
     * @return source address prefix
     */
    public LispAfiAddress getSrcPrefix() {
        return srcPrefix;
    }

    /**
     * Obtains destination address prefix.
     *
     * @return destination address prefix
     */
    public LispAfiAddress getDstPrefix() {
        return dstPrefix;
    }

    /**
     * Obtains source mask length.
     *
     * @return source mask length
     */
    public byte getSrcMaskLength() {
        return srcMaskLength;
    }

    /**
     * Obtains destination mask length.
     *
     * @return destination mask length
     */
    public byte getDstMaskLength() {
        return dstMaskLength;
    }

    /**
     * Obtains reserved value.
     *
     * @return reserved value
     */
    public short getReserved() {
        return reserved;
    }

    @Override
    public int hashCode() {
        return Objects.hash(srcPrefix, dstPrefix, srcMaskLength, dstMaskLength, reserved);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof LispSourceDestLcafAddress) {
            final LispSourceDestLcafAddress other = (LispSourceDestLcafAddress) obj;
            return Objects.equals(this.srcPrefix, other.srcPrefix) &&
                    Objects.equals(this.dstPrefix, other.dstPrefix) &&
                    Objects.equals(this.srcMaskLength, other.srcMaskLength) &&
                    Objects.equals(this.dstMaskLength, other.dstMaskLength) &&
                    Objects.equals(this.reserved, other.reserved);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("source prefix", srcPrefix)
                .add("destination prefix", dstPrefix)
                .add("source mask length", srcMaskLength)
                .add("destination mask length", dstMaskLength)
                .add("reserved", reserved)
                .toString();
    }

    public static final class SourceDestAddressBuilder
            extends LcafAddressBuilder<SourceDestAddressBuilder> {
        private LispAfiAddress srcPrefix;
        private LispAfiAddress dstPrefix;
        private byte srcMaskLength;
        private byte dstMaskLength;
        private short reserved;

        /**
         * Sets source address prefix.
         *
         * @param srcPrefix source prefix
         * @return SourceDestAddressBuilder object
         */
        public SourceDestAddressBuilder withSrcPrefix(LispAfiAddress srcPrefix) {
            this.srcPrefix = srcPrefix;
            return this;
        }

        /**
         * Sets destination address prefix.
         *
         * @param dstPrefix destination prefix
         * @return SourceDestAddressBuilder object
         */
        public SourceDestAddressBuilder withDstPrefix(LispAfiAddress dstPrefix) {
            this.dstPrefix = dstPrefix;
            return this;
        }

        /**
         * Sets source mask length.
         *
         * @param srcMaskLength source mask length
         * @return SourceDestAddressBuilder object
         */
        public SourceDestAddressBuilder withSrcMaskLength(byte srcMaskLength) {
            this.srcMaskLength = srcMaskLength;
            return this;
        }

        /**
         * Sets destination mask length.
         *
         * @param dstMaskLength destination mask length
         * @return SourceDestAddressBuilder object
         */
        public SourceDestAddressBuilder withDstMaskLength(byte dstMaskLength) {
            this.dstMaskLength = dstMaskLength;
            return this;
        }

        /**
         * Sets reserved value.
         *
         * @param reserved reserved field value
         * @return SourceDestAddressBuilder object
         */
        public SourceDestAddressBuilder withReserved(short reserved) {
            this.reserved = reserved;
            return this;
        }

        /**
         * Builds LispSourceDestLcafAddress instance.
         *
         * @return LispSourceDestLcafAddress instance
         */
        public LispSourceDestLcafAddress build() {

            checkNotNull(srcPrefix, "Must specify a source address prefix");
            checkNotNull(dstPrefix, "Must specify a destination address prefix");

            return new LispSourceDestLcafAddress(reserved1, reserved2, flag, length,
                    reserved, srcMaskLength, dstMaskLength, srcPrefix, dstPrefix);
        }
    }

    /**
     * SourceDest LCAF address reader class.
     */
    public static class SourceDestLcafAddressReader
            implements LispAddressReader<LispSourceDestLcafAddress> {

        @Override
        public LispSourceDestLcafAddress readFrom(ByteBuf byteBuf)
                                    throws LispParseError, LispReaderException {

            deserializeCommon(byteBuf);

            short reserved = byteBuf.readShort();
            byte srcMaskLength = (byte) byteBuf.readUnsignedByte();
            byte dstMaskLength = (byte) byteBuf.readUnsignedByte();

            LispAfiAddress srcPrefix = new AfiAddressReader().readFrom(byteBuf);
            LispAfiAddress dstPrefix = new AfiAddressReader().readFrom(byteBuf);

            return new SourceDestAddressBuilder()
                    .withReserved(reserved)
                    .withSrcMaskLength(srcMaskLength)
                    .withDstMaskLength(dstMaskLength)
                    .withSrcPrefix(srcPrefix)
                    .withDstPrefix(dstPrefix)
                    .build();
        }
    }

    /**
     * SourceDest LCAF address writer class.
     */
    public static class SourceDestLcafAddressWriter
            implements LispAddressWriter<LispSourceDestLcafAddress> {

        @Override
        public void writeTo(ByteBuf byteBuf, LispSourceDestLcafAddress address)
                throws LispWriterException {

            int lcafIndex = byteBuf.writerIndex();
            serializeCommon(byteBuf, address);

            byteBuf.writeShort(address.getReserved());
            byteBuf.writeByte(address.getSrcMaskLength());
            byteBuf.writeByte(address.getDstMaskLength());
            AfiAddressWriter writer = new AfiAddressWriter();
            writer.writeTo(byteBuf, address.getSrcPrefix());
            writer.writeTo(byteBuf, address.getDstPrefix());

            updateLength(lcafIndex, byteBuf);
        }
    }
}
