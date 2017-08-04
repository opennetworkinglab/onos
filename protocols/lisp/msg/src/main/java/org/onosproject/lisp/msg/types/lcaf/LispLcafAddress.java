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
import org.onosproject.lisp.msg.types.AddressFamilyIdentifierEnum;
import org.onosproject.lisp.msg.types.LispAddressReader;
import org.onosproject.lisp.msg.types.LispAddressWriter;
import org.onosproject.lisp.msg.types.LispAfiAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onosproject.lisp.msg.types.lcaf.LispCanonicalAddressFormatEnum.APPLICATION_DATA;
import static org.onosproject.lisp.msg.types.lcaf.LispCanonicalAddressFormatEnum.GEO_COORDINATE;
import static org.onosproject.lisp.msg.types.lcaf.LispCanonicalAddressFormatEnum.LIST;
import static org.onosproject.lisp.msg.types.lcaf.LispCanonicalAddressFormatEnum.MULTICAST;
import static org.onosproject.lisp.msg.types.lcaf.LispCanonicalAddressFormatEnum.NAT;
import static org.onosproject.lisp.msg.types.lcaf.LispCanonicalAddressFormatEnum.NONCE;
import static org.onosproject.lisp.msg.types.lcaf.LispCanonicalAddressFormatEnum.SEGMENT;
import static org.onosproject.lisp.msg.types.lcaf.LispCanonicalAddressFormatEnum.SOURCE_DEST;
import static org.onosproject.lisp.msg.types.lcaf.LispCanonicalAddressFormatEnum.TRAFFIC_ENGINEERING;


/**
 * LISP Canonical Address Formatted address class.
 *
 * <pre>
 * {@literal
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |           AFI = 16387         |     Rsvd1     |     Flags     |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |    Type       |     Rsvd2     |            Length             |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * }</pre>
 */
public class LispLcafAddress extends LispAfiAddress {

    private static final Logger log = LoggerFactory.getLogger(LispLcafAddress.class);

    private final LispCanonicalAddressFormatEnum lcafType;
    private final byte reserved1;
    private final byte reserved2;
    private final byte flag;
    private final short length;

    private static final int LCAF_AFI_CODE_BYTE_LENGTH = 2;

    private static final int LENGTH_FIELD_INDEX = 7;
    public static final int COMMON_HEADER_SIZE = 8;

    /**
     * Initializes LCAF address.
     *
     * @param lcafType  LCAF type
     * @param reserved1 reserved1 field
     * @param reserved2 reserved2 field
     * @param flag      flag field
     * @param length    length field
     */
    protected LispLcafAddress(LispCanonicalAddressFormatEnum lcafType,
                              byte reserved1, byte reserved2, byte flag, short length) {
        super(AddressFamilyIdentifierEnum.LCAF);
        this.lcafType = lcafType;
        this.reserved1 = reserved1;
        this.reserved2 = reserved2;
        this.flag = flag;
        this.length = length;
    }

    /**
     * Initializes LCAF address.
     *
     * @param lcafType  LCAF type
     * @param reserved2 reserved2 field
     * @param flag      flag field
     * @param length    length field
     */
    protected LispLcafAddress(LispCanonicalAddressFormatEnum lcafType,
                              byte reserved2, byte flag, short length) {
        super(AddressFamilyIdentifierEnum.LCAF);
        this.lcafType = lcafType;
        this.reserved2 = reserved2;
        this.flag = flag;
        this.length = length;
        this.reserved1 = 0;
    }

    /**
     * Initializes LCAF address.
     *
     * @param lcafType  LCAF type
     * @param reserved2 reserved2 field
     * @param length    length field
     */
    protected LispLcafAddress(LispCanonicalAddressFormatEnum lcafType,
                              byte reserved2, short length) {
        super(AddressFamilyIdentifierEnum.LCAF);
        this.lcafType = lcafType;
        this.reserved2 = reserved2;
        this.length = length;
        this.reserved1 = 0;
        this.flag = 0;
    }

    /**
     * Initializes LCAF address.
     *
     * @param lcafType  LCAF type
     * @param reserved2 reserved2 field
     */
    protected LispLcafAddress(LispCanonicalAddressFormatEnum lcafType, byte reserved2) {
        super(AddressFamilyIdentifierEnum.LCAF);
        this.lcafType = lcafType;
        this.reserved2 = reserved2;
        this.reserved1 = 0;
        this.flag = 0;
        this.length = 0;
    }

    /**
     * Initializes LCAF address.
     *
     * @param lcafType LCAF type
     * @param length   length field
     */
    protected LispLcafAddress(LispCanonicalAddressFormatEnum lcafType, short length) {
        super(AddressFamilyIdentifierEnum.LCAF);
        this.lcafType = lcafType;
        this.reserved1 = 0;
        this.reserved2 = 0;
        this.flag = 0;
        this.length = length;
    }

    /**
     * Initializes LCAF address.
     *
     * @param lcafType LCAF type
     */
    protected LispLcafAddress(LispCanonicalAddressFormatEnum lcafType) {
        super(AddressFamilyIdentifierEnum.LCAF);
        this.lcafType = lcafType;
        this.reserved1 = 0;
        this.reserved2 = 0;
        this.flag = 0;
        this.length = 0;
    }

    /**
     * Obtains LCAF type.
     *
     * @return LCAF type
     */
    public LispCanonicalAddressFormatEnum getType() {
        return lcafType;
    }

    /**
     * Obtains LCAF reserved1 value.
     *
     * @return LCAF reserved1 value
     */
    public byte getReserved1() {
        return reserved1;
    }

    /**
     * Obtains LCAF reserved2 value.
     *
     * @return LCAF reserved2 value
     */
    public byte getReserved2() {
        return reserved2;
    }

    /**
     * Obtains LCAF flag value.
     *
     * @return LCAF flag value
     */
    public byte getFlag() {
        return flag;
    }

    /**
     * Obtains LCAF length value.
     *
     * @return LCAF length value
     */
    public short getLength() {
        return length;
    }

    /**
     * Deserializes common fields from byte buffer.
     *
     * @param byteBuf byte buffer
     * @return LispLcafAddress with filled common data fields
     */
    public static LispLcafAddress deserializeCommon(ByteBuf byteBuf) {

        // let's skip first and second two bytes ,
        // because it represents LCAF AFI code
        byteBuf.skipBytes(LCAF_AFI_CODE_BYTE_LENGTH);

        // reserved1 -> 8 bits
        byte reserved1 = (byte) byteBuf.readUnsignedByte();

        // flags -> 8 bits
        byte flag = (byte) byteBuf.readUnsignedByte();

        // LCAF type -> 8 bits
        byte lcafType = (byte) byteBuf.readUnsignedByte();

        // reserved2 -> 8bits
        byte reserved2 = (byte) byteBuf.readUnsignedByte();

        // length -> 16 bits
        short length = (short) byteBuf.readUnsignedShort();

        return new LispLcafAddress(LispCanonicalAddressFormatEnum.valueOf(lcafType),
                reserved1, reserved2, flag, length);
    }

    /**
     * Updates the header length field value based on the size of LISP header.
     *
     * @param lcafIndex the index of LCAF address, because LCAF address is
     *                  contained inside LISP control message, so to correctly
     *                  find the right LCAF length index, we need to know the
     *                  absolute lcaf index inside LISP control message byte buf
     * @param byteBuf   netty byte buffer
     */
    public static void updateLength(int lcafIndex, ByteBuf byteBuf) {
        byteBuf.setByte(lcafIndex + LENGTH_FIELD_INDEX,
                        byteBuf.writerIndex() - COMMON_HEADER_SIZE - lcafIndex);
    }

    /**
     * Serializes common fields to byte buffer.
     *
     * @param byteBuf byte buffer
     * @param address LISP LCAF address instance
     */
    public static void serializeCommon(ByteBuf byteBuf, LispLcafAddress address) {

        byteBuf.writeShort(AddressFamilyIdentifierEnum.LCAF.getIanaCode());
        byteBuf.writeByte(address.getReserved1());
        byteBuf.writeByte(address.getFlag());
        byteBuf.writeByte(address.getType().getLispCode());
        byteBuf.writeByte(address.getReserved2());
        byteBuf.writeShort(address.getLength());
    }

    @Override
    public int hashCode() {
        return Objects.hash(lcafType, reserved1, reserved2, flag, length);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof LispLcafAddress) {
            final LispLcafAddress other = (LispLcafAddress) obj;
            return Objects.equals(this.lcafType, other.lcafType) &&
                    Objects.equals(this.reserved1, other.reserved1) &&
                    Objects.equals(this.reserved2, other.reserved2) &&
                    Objects.equals(this.flag, other.flag) &&
                    Objects.equals(this.length, other.length);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("lcafType", lcafType)
                .add("reserved1", reserved1)
                .add("reserved2", reserved2)
                .add("flag", flag)
                .add("length", length)
                .toString();
    }

    protected static class LcafAddressBuilder<T> {

        protected byte reserved1;
        protected byte flag;
        protected byte lcafType;
        protected byte reserved2;
        protected short length;

        /**
         * Sets reserved1 value.
         *
         * @param reserved1 reserved1 value
         * @return LcafAddressBuilder object
         */
        public T withReserved1(byte reserved1) {
            this.reserved1 = reserved1;
            return (T) this;
        }

        /**
         * Sets flag.
         *
         * @param flag flag boolean
         * @return LcafAddressBuilder object
         */
        public T withFlag(byte flag) {
            this.flag = flag;
            return (T) this;
        }

        /**
         * Sets LCAF type.
         *
         * @param lcafType LCAF type
         * @return LcafAddressBuilder object
         */
        public T withLcafType(byte lcafType) {
            this.lcafType = lcafType;
            return (T) this;
        }

        /**
         * Sets reserved2 value.
         *
         * @param reserved2 reserved2 value
         * @return LcafAddressBuilder object
         */
        public T withReserved2(byte reserved2) {
            this.reserved2 = reserved2;
            return (T) this;
        }

        /**
         * Sets length value.
         *
         * @param length length value
         * @return LcafAddressBuilder object
         */
        public T withLength(short length) {
            this.length = length;
            return (T) this;
        }

        /**
         * Builds LispLcafAddress object.
         *
         * @return LispLcafAddress instance
         */
        public LispLcafAddress build() {
            return new LispLcafAddress(LispCanonicalAddressFormatEnum
                                               .valueOf(lcafType),
                                       reserved1, reserved2, flag, length);
        }
    }

    /**
     * LISP LCAF reader class.
     */
    public static class LcafAddressReader
            implements LispAddressReader<LispLcafAddress> {

        private static final int LCAF_TYPE_FIELD_INDEX = 4;

        @Override
        public LispLcafAddress readFrom(ByteBuf byteBuf)
                throws LispParseError, LispReaderException {

            int index = byteBuf.readerIndex();

            // LCAF type -> 8 bits
            byte lcafType = (byte) byteBuf.getUnsignedByte(index + LCAF_TYPE_FIELD_INDEX);

            if (lcafType == APPLICATION_DATA.getLispCode()) {
                return new LispAppDataLcafAddress.AppDataLcafAddressReader().readFrom(byteBuf);
            }

            if (lcafType == NAT.getLispCode()) {
                return new LispNatLcafAddress.NatLcafAddressReader().readFrom(byteBuf);
            }

            if (lcafType == LIST.getLispCode()) {
                return new LispListLcafAddress.ListLcafAddressReader().readFrom(byteBuf);
            }

            if (lcafType == SEGMENT.getLispCode()) {
                return new LispSegmentLcafAddress.SegmentLcafAddressReader().readFrom(byteBuf);
            }

            if (lcafType == GEO_COORDINATE.getLispCode()) {
                return new LispGeoCoordinateLcafAddress.GeoCoordinateLcafAddressReader().readFrom(byteBuf);
            }

            if (lcafType == NONCE.getLispCode()) {
                return new LispNonceLcafAddress.NonceLcafAddressReader().readFrom(byteBuf);
            }

            if (lcafType == MULTICAST.getLispCode()) {
                return new LispMulticastLcafAddress.MulticastLcafAddressReader().readFrom(byteBuf);
            }

            if (lcafType == SOURCE_DEST.getLispCode()) {
                return new LispSourceDestLcafAddress.SourceDestLcafAddressReader().readFrom(byteBuf);
            }

            if (lcafType == TRAFFIC_ENGINEERING.getLispCode()) {
                return new LispTeLcafAddress.TeLcafAddressReader().readFrom(byteBuf);
            }

            log.warn("Unsupported LCAF type, please specify a correct LCAF type");

            return null;
        }
    }

    /**
     * LISP LCAF address writer class.
     */
    public static class LcafAddressWriter
            implements LispAddressWriter<LispLcafAddress> {

        @Override
        public void writeTo(ByteBuf byteBuf, LispLcafAddress address)
                throws LispWriterException {
            switch (address.getType()) {
                case APPLICATION_DATA:
                    new LispAppDataLcafAddress.AppDataLcafAddressWriter().writeTo(byteBuf,
                            (LispAppDataLcafAddress) address);
                    break;
                case NAT:
                    new LispNatLcafAddress.NatLcafAddressWriter().writeTo(byteBuf,
                            (LispNatLcafAddress) address);
                    break;
                case LIST:
                    new LispListLcafAddress.ListLcafAddressWriter().writeTo(byteBuf,
                            (LispListLcafAddress) address);
                    break;
                case SEGMENT:
                    new LispSegmentLcafAddress.SegmentLcafAddressWriter().writeTo(byteBuf,
                            (LispSegmentLcafAddress) address);
                    break;
                case GEO_COORDINATE:
                    new LispGeoCoordinateLcafAddress.GeoCoordinateLcafAddressWriter().writeTo(byteBuf,
                            (LispGeoCoordinateLcafAddress) address);
                    break;
                case NONCE:
                    new LispNonceLcafAddress.NonceLcafAddressWriter().writeTo(byteBuf,
                            (LispNonceLcafAddress) address);
                    break;
                case MULTICAST:
                    new LispMulticastLcafAddress.MulticastLcafAddressWriter().writeTo(byteBuf,
                            (LispMulticastLcafAddress) address);
                    break;
                case SOURCE_DEST:
                    new LispSourceDestLcafAddress.SourceDestLcafAddressWriter().writeTo(byteBuf,
                            (LispSourceDestLcafAddress) address);
                    break;
                case TRAFFIC_ENGINEERING:
                    new LispTeLcafAddress.TeLcafAddressWriter().writeTo(byteBuf,
                            (LispTeLcafAddress) address);
                    break;
                default:
                    log.warn("Unsupported LCAF type, please specify a correct LCAF type");
                    break;
            }
        }
    }
}
