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
package org.onosproject.lisp.msg.protocols;

import com.google.common.base.Objects;
import io.netty.buffer.ByteBuf;
import org.onlab.util.ByteOperator;
import org.onosproject.lisp.msg.exceptions.LispParseError;
import org.onosproject.lisp.msg.exceptions.LispReaderException;
import org.onosproject.lisp.msg.exceptions.LispWriterException;
import org.onosproject.lisp.msg.types.LispAfiAddress;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.lisp.msg.types.LispAfiAddress.AfiAddressWriter;

/**
 * Default implementation of LispLocatorRecord.
 */
public final class DefaultLispLocatorRecord implements LispLocatorRecord {

    private final byte priority;
    private final byte weight;
    private final byte multicastPriority;
    private final byte multicastWeight;
    private final boolean localLocator;
    private final boolean rlocProbed;
    private final boolean routed;
    private final LispAfiAddress locatorAfi;

    /**
     * A private constructor that protects object instantiation from external.
     *
     * @param priority          uni-cast priority
     * @param weight            uni-cast weight
     * @param multicastPriority multi-cast priority
     * @param multicastWeight   multi-cast weight
     * @param localLocator      local locator flag
     * @param rlocProbed        RLOC probed flag
     * @param routed            routed flag
     * @param locatorAfi        locator AFI
     */
    private DefaultLispLocatorRecord(byte priority, byte weight, byte multicastPriority,
                                     byte multicastWeight, boolean localLocator, boolean rlocProbed,
                                     boolean routed, LispAfiAddress locatorAfi) {
        this.priority = priority;
        this.weight = weight;
        this.multicastPriority = multicastPriority;
        this.multicastWeight = multicastWeight;
        this.localLocator = localLocator;
        this.rlocProbed = rlocProbed;
        this.routed = routed;
        this.locatorAfi = locatorAfi;
    }

    @Override
    public byte getPriority() {
        return priority;
    }

    @Override
    public byte getWeight() {
        return weight;
    }

    @Override
    public byte getMulticastPriority() {
        return multicastPriority;
    }

    @Override
    public byte getMulticastWeight() {
        return multicastWeight;
    }

    @Override
    public boolean isLocalLocator() {
        return localLocator;
    }

    @Override
    public boolean isRlocProbed() {
        return rlocProbed;
    }

    @Override
    public boolean isRouted() {
        return routed;
    }

    @Override
    public LispAfiAddress getLocatorAfi() {
        return locatorAfi;
    }

    @Override
    public void writeTo(ByteBuf byteBuf) {

    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("priority", priority)
                .add("weight", weight)
                .add("multi-cast priority", multicastPriority)
                .add("multi-cast weight", multicastWeight)
                .add("local locator", localLocator)
                .add("RLOC probed", rlocProbed)
                .add("routed", routed)
                .add("locator AFI", locatorAfi).toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefaultLispLocatorRecord that = (DefaultLispLocatorRecord) o;
        return Objects.equal(priority, that.priority) &&
                Objects.equal(weight, that.weight) &&
                Objects.equal(multicastPriority, that.multicastPriority) &&
                Objects.equal(multicastWeight, that.multicastWeight) &&
                Objects.equal(localLocator, that.localLocator) &&
                Objects.equal(rlocProbed, that.rlocProbed) &&
                Objects.equal(routed, that.routed) &&
                Objects.equal(locatorAfi, that.locatorAfi);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(priority, weight, multicastPriority,
                multicastWeight, localLocator, rlocProbed, routed, locatorAfi);
    }

    public static final class DefaultLocatorRecordBuilder implements LocatorRecordBuilder {

        private byte priority;
        private byte weight;
        private byte multicastPriority;
        private byte multicastWeight;
        private boolean localLocator;
        private boolean rlocProbed;
        private boolean routed;
        private LispAfiAddress locatorAfi;

        @Override
        public LocatorRecordBuilder withPriority(byte priority) {
            this.priority = priority;
            return this;
        }

        @Override
        public LocatorRecordBuilder withWeight(byte weight) {
            this.weight = weight;
            return this;
        }

        @Override
        public LocatorRecordBuilder withMulticastPriority(byte priority) {
            this.multicastPriority = priority;
            return this;
        }

        @Override
        public LocatorRecordBuilder withMulticastWeight(byte weight) {
            this.multicastWeight = weight;
            return this;
        }

        @Override
        public LocatorRecordBuilder withLocalLocator(boolean localLocator) {
            this.localLocator = localLocator;
            return this;
        }

        @Override
        public LocatorRecordBuilder withRlocProbed(boolean rlocProbed) {
            this.rlocProbed = rlocProbed;
            return this;
        }

        @Override
        public LocatorRecordBuilder withRouted(boolean routed) {
            this.routed = routed;
            return this;
        }

        @Override
        public LocatorRecordBuilder withLocatorAfi(LispAfiAddress locatorAfi) {
            this.locatorAfi = locatorAfi;
            return this;
        }

        @Override
        public LispLocatorRecord build() {

            checkNotNull(locatorAfi, "Must specify a locator address");

            return new DefaultLispLocatorRecord(priority, weight, multicastPriority,
                    multicastWeight, localLocator, rlocProbed, routed, locatorAfi);
        }
    }

    /**
     * A LISP message reader for LocatorRecord portion.
     */
    public static final class LocatorRecordReader implements LispMessageReader<LispLocatorRecord> {

        private static final int SKIP_UNUSED_FLAG_LENGTH = 1;
        private static final int LOCAL_LOCATOR_INDEX = 2;
        private static final int RLOC_PROBED_INDEX = 1;
        private static final int ROUTED_INDEX = 0;

        @Override
        public LispLocatorRecord readFrom(ByteBuf byteBuf) throws LispParseError, LispReaderException {

            // priority -> 8 bits
            byte priority = (byte) byteBuf.readUnsignedByte();

            // weight -> 8 bits
            byte weight = (byte) byteBuf.readUnsignedByte();

            // multi-cast priority -> 8 bits
            byte multicastPriority = (byte) byteBuf.readUnsignedByte();

            // multi-cast weight -> 8 bits
            byte multicastWeight = (byte) byteBuf.readUnsignedByte();

            // let's skip unused flags
            byteBuf.skipBytes(SKIP_UNUSED_FLAG_LENGTH);

            byte flags = byteBuf.readByte();

            // local locator flag -> 1 bit
            boolean localLocator = ByteOperator.getBit(flags, LOCAL_LOCATOR_INDEX);

            // rloc probe flag -> 1 bit
            boolean rlocProbed = ByteOperator.getBit(flags, RLOC_PROBED_INDEX);

            // routed flag -> 1 bit
            boolean routed = ByteOperator.getBit(flags, ROUTED_INDEX);

            LispAfiAddress address = new LispAfiAddress.AfiAddressReader().readFrom(byteBuf);

            return new DefaultLocatorRecordBuilder()
                        .withPriority(priority)
                        .withWeight(weight)
                        .withMulticastPriority(multicastPriority)
                        .withMulticastWeight(multicastWeight)
                        .withLocalLocator(localLocator)
                        .withRlocProbed(rlocProbed)
                        .withRouted(routed)
                        .withLocatorAfi(address)
                        .build();
        }
    }

    /**
     * A LISP message writer for LocatorRecord portion.
     */
    public static final class LocatorRecordWriter implements LispMessageWriter<LispLocatorRecord> {

        private static final int LOCAL_LOCATOR_SHIFT_BIT = 2;
        private static final int PROBED_SHIFT_BIT = 1;

        private static final int ENABLE_BIT = 1;
        private static final int DISABLE_BIT = 0;

        @Override
        public void writeTo(ByteBuf byteBuf, LispLocatorRecord message) throws LispWriterException {

            // priority
            byteBuf.writeByte(message.getPriority());

            // weight
            byteBuf.writeByte(message.getWeight());

            // multicast priority
            byteBuf.writeByte(message.getMulticastPriority());

            // multicast weight
            byteBuf.writeByte(message.getMulticastWeight());

            // unused flags
            byteBuf.writeByte((short) 0);

            // localLocator flag
            short localLocator = DISABLE_BIT;
            if (message.isLocalLocator()) {
                localLocator = (byte) (ENABLE_BIT << LOCAL_LOCATOR_SHIFT_BIT);
            }

            // rlocProbed flag
            short probed = DISABLE_BIT;
            if (message.isRlocProbed()) {
                probed = (byte) (ENABLE_BIT << PROBED_SHIFT_BIT);
            }

            // routed flag
            short routed = DISABLE_BIT;
            if (message.isRouted()) {
                routed = (byte) ENABLE_BIT;
            }

            byteBuf.writeByte((byte) (localLocator + probed + routed));

            // EID prefix AFI with EID prefix
            AfiAddressWriter afiAddressWriter = new AfiAddressWriter();
            afiAddressWriter.writeTo(byteBuf, message.getLocatorAfi());
        }
    }
}
