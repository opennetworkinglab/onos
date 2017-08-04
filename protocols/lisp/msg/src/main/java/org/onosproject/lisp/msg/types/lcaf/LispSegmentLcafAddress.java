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
 * Instance ID type LCAF address class.
 * <p>
 * Instance ID type is defined in draft-ietf-lisp-lcaf-22
 * https://tools.ietf.org/html/draft-ietf-lisp-lcaf-22#page-8
 *
 * <pre>
 * {@literal
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |           AFI = 16387         |     Rsvd1     |     Flags     |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |   Type = 2    | IID mask-len  |            Length             |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                         Instance ID                           |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |              AFI = x          |         Address  ...          |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * }</pre>
 */
public final class LispSegmentLcafAddress extends LispLcafAddress {

    private final LispAfiAddress address;
    private final int instanceId;

    /**
     * Initializes segment type LCAF address.
     *
     * @param idMaskLength Id mask length
     * @param instanceId   instance id
     * @param address      address
     */
    private LispSegmentLcafAddress(byte idMaskLength, int instanceId,
                                   LispAfiAddress address) {
        super(LispCanonicalAddressFormatEnum.SEGMENT, idMaskLength);
        this.address = address;
        this.instanceId = instanceId;
    }

    /**
     * Initializes segment type LCAF address.
     *
     * @param reserved1    reserved1
     * @param idMaskLength ID mask length
     * @param flag         flag
     * @param length       length
     * @param instanceId   instance id
     * @param address      address
     */
    private LispSegmentLcafAddress(byte reserved1, byte idMaskLength, byte flag,
                                   short length, int instanceId,
                                   LispAfiAddress address) {
        super(LispCanonicalAddressFormatEnum.SEGMENT, reserved1,
                                                    idMaskLength, flag, length);
        this.address = address;
        this.instanceId = instanceId;
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
     * Obtains instance id.
     *
     * @return instance id
     */
    public int getInstanceId() {
        return instanceId;
    }

    /**
     * Obtains id mask length.
     *
     * @return id mask length
     */
    public byte getIdMaskLength() {
        return getReserved2();
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, instanceId, getReserved2());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof LispSegmentLcafAddress) {
            final LispSegmentLcafAddress other = (LispSegmentLcafAddress) obj;
            return Objects.equals(this.address, other.address) &&
                    Objects.equals(this.instanceId, other.instanceId) &&
                    Objects.equals(this.getReserved2(), other.getReserved2());
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("address", address)
                .add("instanceId", instanceId)
                .add("idMaskLength", getReserved2())
                .toString();
    }

    public static final class SegmentAddressBuilder
                              extends LcafAddressBuilder<SegmentAddressBuilder> {
        private byte idMaskLength;
        private LispAfiAddress address;
        private int instanceId;

        /**
         * Sets identifier mask length.
         *
         * @param idMaskLength identifier mask length
         * @return SegmentAddressBuilder object
         */
        public SegmentAddressBuilder withIdMaskLength(byte idMaskLength) {
            this.idMaskLength = idMaskLength;
            return this;
        }

        /**
         * Sets instance identifer.
         *
         * @param instanceId instance identifier
         * @return SegmentAddressBuilder object
         */
        public SegmentAddressBuilder withInstanceId(int instanceId) {
            this.instanceId = instanceId;
            return this;
        }

        /**
         * Sets AFI address.
         *
         * @param address AFI address
         * @return SegmentAddressBuilder object
         */
        public SegmentAddressBuilder withAddress(LispAfiAddress address) {
            this.address = address;
            return this;
        }

        /**
         * Builds LispSegmentLcafAddress instance.
         *
         * @return LispSegmentLcafAddress instance
         */
        public LispSegmentLcafAddress build() {

            checkNotNull(address, "Must specify an address");

            return new LispSegmentLcafAddress(reserved1, idMaskLength, flag,
                                              length, instanceId, address);
        }
    }

    /**
     * Segment LCAF address reader class.
     */
    public static class SegmentLcafAddressReader
            implements LispAddressReader<LispSegmentLcafAddress> {

        @Override
        public LispSegmentLcafAddress readFrom(ByteBuf byteBuf)
                throws LispParseError, LispReaderException {

            LispLcafAddress lcafAddress = LispLcafAddress.deserializeCommon(byteBuf);

            byte idMaskLength = lcafAddress.getReserved2();

            int instanceId = (int) byteBuf.readUnsignedInt();
            LispAfiAddress address = new AfiAddressReader().readFrom(byteBuf);

            return new SegmentAddressBuilder()
                    .withIdMaskLength(idMaskLength)
                    .withInstanceId(instanceId)
                    .withAddress(address)
                    .build();
        }
    }

    /**
     * Segment LCAF address writer class.
     */
    public static class SegmentLcafAddressWriter
            implements LispAddressWriter<LispSegmentLcafAddress> {

        @Override
        public void writeTo(ByteBuf byteBuf, LispSegmentLcafAddress address)
                throws LispWriterException {

            int lcafIndex = byteBuf.writerIndex();
            LispLcafAddress.serializeCommon(byteBuf, address);

            byteBuf.writeInt(address.getInstanceId());

            new LispAfiAddress.AfiAddressWriter().writeTo(byteBuf, address.getAddress());

            LispLcafAddress.updateLength(lcafIndex, byteBuf);
        }
    }
}
