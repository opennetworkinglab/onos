/*
 * Copyright 2017-present Open Networking Laboratory
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
 * Multicast group membership type LCAF address class.
 * <p>
 * Multicast group membership type is defined in draft-ietf-lisp-lcaf-22
 * https://tools.ietf.org/html/draft-ietf-lisp-lcaf-22#page-15
 *
 * <pre>
 * {@literal
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |           AFI = 16387         |     Rsvd1     |     Flags     |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |   Type = 9    |     Rsvd2     |             Length            |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                         Instance-ID                           |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |            Reserved           | Source MaskLen| Group MaskLen |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |              AFI = x          |   Source/Subnet Address  ...  |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |              AFI = x          |       Group Address  ...      |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * }</pre>
 */
public final class LispMulticastLcafAddress extends LispLcafAddress {

    private final int instanceId;
    private final byte srcMaskLength;
    private final byte grpMaskLength;
    private final LispAfiAddress srcAddress;
    private final LispAfiAddress grpAddress;

    /**
     * Initializes multicast type LCAF address.
     *
     * @param instanceId    instance identifier
     * @param srcMaskLength  source mask length
     * @param grpMaskLength group mask length
     * @param srcAddress    source address
     * @param grpAddress    group address
     */
    private LispMulticastLcafAddress(int instanceId, byte srcMaskLength,
                                     byte grpMaskLength, LispAfiAddress srcAddress,
                                     LispAfiAddress grpAddress) {
        super(LispCanonicalAddressFormatEnum.MULTICAST);
        this.instanceId = instanceId;
        this.srcMaskLength = srcMaskLength;
        this.grpMaskLength = grpMaskLength;
        this.srcAddress = srcAddress;
        this.grpAddress = grpAddress;
    }

    /**
     * Obtains instance identifier.
     *
     * @return instance identifier
     */
    public int getInstanceId() {
        return instanceId;
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
     * Obtains group mask length.
     *
     * @return group mask length
     */
    public byte getGrpMaskLength() {
        return grpMaskLength;
    }

    /**
     * Obtains source address.
     *
     * @return source address
     */
    public LispAfiAddress getSrcAddress() {
        return srcAddress;
    }

    /**
     * Obtains group address.
     *
     * @return group address
     */
    public LispAfiAddress getGrpAddress() {
        return grpAddress;
    }

    @Override
    public int hashCode() {
        return Objects.hash(instanceId, srcMaskLength, grpMaskLength,
                srcAddress, grpAddress);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof LispMulticastLcafAddress) {
            final LispMulticastLcafAddress other = (LispMulticastLcafAddress) obj;
            return Objects.equals(this.instanceId, other.instanceId) &&
                    Objects.equals(this.srcMaskLength, other.srcMaskLength) &&
                    Objects.equals(this.grpMaskLength, other.grpMaskLength) &&
                    Objects.equals(this.srcAddress, other.srcAddress) &&
                    Objects.equals(this.grpAddress, other.grpAddress);

        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("instance ID", instanceId)
                .add("source mask length", srcMaskLength)
                .add("group mask length", grpMaskLength)
                .add("source address", srcAddress)
                .add("group address", grpAddress)
                .toString();
    }

    public static final class MulticastAddressBuilder
                        extends LcafAddressBuilder<MulticastAddressBuilder> {
        private int instanceId;
        private byte srcMaskLength;
        private byte grpMaskLength;
        private LispAfiAddress srcAddress;
        private LispAfiAddress grpAddress;

        /**
         * Sets instance identifier.
         *
         * @param instanceId instance identifier
         * @return MulticastAddressBuilder object
         */
        public MulticastAddressBuilder withInstanceId(int instanceId) {
            this.instanceId = instanceId;
            return this;
        }

        /**
         * Sets source mask length.
         *
         * @param srcMaskLength source mask length
         * @return MulticastAddressBuilder object
         */
        public MulticastAddressBuilder withSrcMaskLength(byte srcMaskLength) {
            this.srcMaskLength = srcMaskLength;
            return this;
        }

        /**
         * Sets group mask length.
         *
         * @param grpMaskLength group mask length
         * @return MulticastAddressBuilder object
         */
        public MulticastAddressBuilder withGrpMaskLength(byte grpMaskLength) {
            this.grpMaskLength = grpMaskLength;
            return this;
        }

        /**
         * Sets source address.
         *
         * @param srcAddress source address
         * @return MulticastAddressBuilder object
         */
        public MulticastAddressBuilder withSrcAddress(LispAfiAddress srcAddress) {
            this.srcAddress = srcAddress;
            return this;
        }

        /**
         * Sets group address.
         *
         * @param grpAddress group address
         * @return MulticastAddressBuilder object
         */
        public MulticastAddressBuilder withGrpAddress(LispAfiAddress grpAddress) {
            this.grpAddress = grpAddress;
            return this;
        }

        /**
         * Builds LispMulticastLcafAddress instance.
         *
         * @return LispMulticastLcafAddress instance
         */
        @Override
        public LispMulticastLcafAddress build() {

            checkNotNull(srcAddress, "Must specify a source address");
            checkNotNull(grpAddress, "Must specify a group address");

            return new LispMulticastLcafAddress(instanceId, srcMaskLength,
                                        grpMaskLength, srcAddress, grpAddress);
        }
    }

    /**
     * Multicast LCAF address reader class.
     */
    public static class MulticastLcafAddressReader
                        implements LispAddressReader<LispMulticastLcafAddress> {

        private static final int RESERVED_SKIP_LENGTH = 2;

        @Override
        public LispMulticastLcafAddress readFrom(ByteBuf byteBuf)
                                        throws LispParseError, LispReaderException {

            LispLcafAddress.deserializeCommon(byteBuf);

            int instanceId = (int) byteBuf.readUnsignedInt();
            byteBuf.skipBytes(RESERVED_SKIP_LENGTH);
            byte srcMaskLength = (byte) byteBuf.readUnsignedByte();
            byte grpMaskLength = (byte) byteBuf.readUnsignedByte();
            LispAfiAddress srcAddress = new AfiAddressReader().readFrom(byteBuf);
            LispAfiAddress grpAddress = new AfiAddressReader().readFrom(byteBuf);

            return new MulticastAddressBuilder()
                            .withInstanceId(instanceId)
                            .withSrcMaskLength(srcMaskLength)
                            .withGrpMaskLength(grpMaskLength)
                            .withSrcAddress(srcAddress)
                            .withGrpAddress(grpAddress)
                            .build();
        }
    }

    /**
     * Multicast LCAF address writer class.
     */
    public static class MulticastLcafAddressWriter
                        implements LispAddressWriter<LispMulticastLcafAddress> {

        private static final int UNUSED_ZERO = 0;

        @Override
        public void writeTo(ByteBuf byteBuf, LispMulticastLcafAddress address)
                                                    throws LispWriterException {
            int lcafIndex = byteBuf.writerIndex();
            LispLcafAddress.serializeCommon(byteBuf, address);

            // instance identifier
            byteBuf.writeInt(address.getInstanceId());

            // reserved field
            byteBuf.writeByte(UNUSED_ZERO);
            byteBuf.writeByte(UNUSED_ZERO);

            // source mask length
            byteBuf.writeByte(address.getSrcMaskLength());

            // group mask length
            byteBuf.writeByte(address.getGrpMaskLength());

            // source address
            AfiAddressWriter srcWriter = new AfiAddressWriter();
            srcWriter.writeTo(byteBuf, address.getSrcAddress());

            // group address
            AfiAddressWriter grpWriter = new AfiAddressWriter();
            grpWriter.writeTo(byteBuf, address.getGrpAddress());

            LispLcafAddress.updateLength(lcafIndex, byteBuf);
        }
    }
}