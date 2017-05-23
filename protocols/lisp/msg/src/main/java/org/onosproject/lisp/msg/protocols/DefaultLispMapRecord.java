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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import org.onlab.util.ByteOperator;
import org.onosproject.lisp.msg.exceptions.LispParseError;
import org.onosproject.lisp.msg.protocols.DefaultLispLocator.LocatorReader;
import org.onosproject.lisp.msg.protocols.DefaultLispLocator.LocatorWriter;
import org.onosproject.lisp.msg.types.LispAfiAddress;
import org.onosproject.lisp.msg.exceptions.LispReaderException;
import org.onosproject.lisp.msg.exceptions.LispWriterException;
import org.onosproject.lisp.msg.types.LispAfiAddress.AfiAddressWriter;

import java.util.List;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of LispMapRecord.
 */
public final class DefaultLispMapRecord extends AbstractLispRecord
                                                implements LispMapRecord {

    private final List<LispLocator> locators;

    static final MapRecordWriter WRITER;
    static {
        WRITER = new MapRecordWriter();
    }

    /**
     * A private constructor that protects object instantiation from external.
     *
     * @param recordTtl        record time-to-live value
     * @param maskLength       mask length
     * @param action           lisp map reply action
     * @param authoritative    authoritative flag
     * @param mapVersionNumber map version number
     * @param eidPrefixAfi     EID prefix AFI address
     * @param locators         a collection of locators
     */
    private DefaultLispMapRecord(int recordTtl, byte maskLength,
                                 LispMapReplyAction action, boolean authoritative,
                                 short mapVersionNumber, LispAfiAddress eidPrefixAfi,
                                 List<LispLocator> locators) {
        super(recordTtl, maskLength, action, authoritative, mapVersionNumber, eidPrefixAfi);
        this.locators = locators;
    }

    @Override
    public int getLocatorCount() {
        return locators.size();
    }

    @Override
    public List<LispLocator> getLocators() {
        return ImmutableList.copyOf(locators);
    }

    @Override
    public void writeTo(ByteBuf byteBuf) throws LispWriterException {
        WRITER.writeTo(byteBuf, this);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("record TTL", recordTtl)
                .add("maskLength", maskLength)
                .add("action", action)
                .add("authoritative", authoritative)
                .add("mapVersionNumber", mapVersionNumber)
                .add("EID prefix AFI address", eidPrefixAfi)
                .add("locators", locators)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefaultLispMapRecord that = (DefaultLispMapRecord) o;
        return Objects.equal(recordTtl, that.recordTtl) &&
                Objects.equal(maskLength, that.maskLength) &&
                Objects.equal(action, that.action) &&
                Objects.equal(authoritative, that.authoritative) &&
                Objects.equal(mapVersionNumber, that.mapVersionNumber) &&
                Objects.equal(eidPrefixAfi, that.eidPrefixAfi) &&
                Objects.equal(locators, that.locators);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(recordTtl, maskLength, action, authoritative,
                                mapVersionNumber, eidPrefixAfi, locators);
    }

    public static final class DefaultMapRecordBuilder
                              extends AbstractRecordBuilder<MapRecordBuilder>
                                      implements MapRecordBuilder {

        private List<LispLocator> locators = Lists.newArrayList();

        @Override
        public MapRecordBuilder withLocators(List<LispLocator> locators) {
            if (locators != null) {
                this.locators = ImmutableList.copyOf(locators);
            }
            return this;
        }

        @Override
        public LispMapRecord build() {

            checkNotNull(eidPrefixAfi, "Must specify an EID prefix");

            return new DefaultLispMapRecord(recordTtl, maskLength, action,
                    authoritative, mapVersionNumber, eidPrefixAfi, locators);
        }
    }

    /**
     * A LISP message reader for MapRecord portion.
     */
    public static final class MapRecordReader implements LispMessageReader<LispMapRecord> {

        private static final int AUTHORITATIVE_INDEX = 4;
        private static final int RESERVED_SKIP_LENGTH = 1;

        private static final int REPLY_ACTION_SHIFT_BIT = 5;

        @Override
        public LispMapRecord readFrom(ByteBuf byteBuf) throws LispParseError,
                                                              LispReaderException {

            // Record TTL -> 32 bits
            int recordTtl = byteBuf.readInt();

            // Locator count -> 8 bits
            int locatorCount = byteBuf.readUnsignedByte();

            // EID mask length -> 8 bits
            byte maskLength = (byte) byteBuf.readUnsignedByte();

            byte actionWithFlag = (byte) byteBuf.readUnsignedByte();

            // action -> 3 bit
            int actionByte = actionWithFlag >> REPLY_ACTION_SHIFT_BIT;
            LispMapReplyAction action = LispMapReplyAction.valueOf(actionByte);
            if (action == null) {
                action = LispMapReplyAction.NoAction;
            }

            // authoritative flag -> 1 bit
            boolean authoritative = ByteOperator.getBit((byte)
                                    (actionWithFlag >> AUTHORITATIVE_INDEX), 0);

            // let's skip the reserved field
            byteBuf.skipBytes(RESERVED_SKIP_LENGTH);

            // Map version number -> 12 bits, we treat Rsvd field is all zero
            short mapVersionNumber = (short) byteBuf.readUnsignedShort();

            LispAfiAddress eidPrefixAfi =
                            new LispAfiAddress.AfiAddressReader().readFrom(byteBuf);

            List<LispLocator> locators = Lists.newArrayList();
            for (int i = 0; i < locatorCount; i++) {
                locators.add(new LocatorReader().readFrom(byteBuf));
            }

            return new DefaultMapRecordBuilder()
                        .withRecordTtl(recordTtl)
                        .withMaskLength(maskLength)
                        .withAction(action)
                        .withIsAuthoritative(authoritative)
                        .withMapVersionNumber(mapVersionNumber)
                        .withLocators(locators)
                        .withEidPrefixAfi(eidPrefixAfi)
                        .build();
        }
    }

    /**
     * A LISP message writer for MapRecord portion.
     */
    public static final class MapRecordWriter implements LispMessageWriter<LispMapRecord> {

        private static final int REPLY_ACTION_SHIFT_BIT = 5;
        private static final int AUTHORITATIVE_FLAG_SHIFT_BIT = 4;

        private static final int ENABLE_BIT = 1;
        private static final int DISABLE_BIT = 0;

        @Override
        public void writeTo(ByteBuf byteBuf, LispMapRecord message) throws LispWriterException {

            // record TTL
            byteBuf.writeInt(message.getRecordTtl());

            // locator count
            byteBuf.writeByte((byte) message.getLocators().size());

            // EID mask length
            byteBuf.writeByte(message.getMaskLength());

            // reply action
            byte action = (byte) (message.getAction().getAction() << REPLY_ACTION_SHIFT_BIT);

            // authoritative bit
            byte authoritative = DISABLE_BIT;
            if (message.isAuthoritative()) {
                authoritative = ENABLE_BIT << AUTHORITATIVE_FLAG_SHIFT_BIT;
            }

            byteBuf.writeByte((byte) (action + authoritative));

            // fill zero into reserved field
            byteBuf.writeByte((short) 0);

            // map version number
            byteBuf.writeShort(message.getMapVersionNumber());

            // EID prefix AFI with EID prefix
            AfiAddressWriter afiAddressWriter = new AfiAddressWriter();
            afiAddressWriter.writeTo(byteBuf, message.getEidPrefixAfi());

            // serialize locator
            LocatorWriter recordWriter = new LocatorWriter();
            List<LispLocator> locators = message.getLocators();
            for (LispLocator locator : locators) {
                recordWriter.writeTo(byteBuf, locator);
            }
        }
    }
}
