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
package org.onosproject.lisp.msg.protocols;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import org.onlab.packet.DeserializationException;
import org.onosproject.lisp.msg.exceptions.LispParseError;
import org.onosproject.lisp.msg.exceptions.LispReaderException;
import org.onosproject.lisp.msg.exceptions.LispWriterException;
import org.onosproject.lisp.msg.protocols.DefaultLispReferralRecord.ReferralRecordReader;
import org.onosproject.lisp.msg.protocols.DefaultLispReferralRecord.ReferralRecordWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onosproject.lisp.msg.protocols.LispType.LISP_MAP_REFERRAL;

/**
 * Default LISP referral message class.
 */
public final class DefaultLispMapReferral extends AbstractLispMessage
                                    implements LispMapReferral {

    private static final Logger log =
                         LoggerFactory.getLogger(DefaultLispMapReferral.class);

    private final long nonce;
    private final List<LispReferralRecord> referralRecords;

    static final MapReferralWriter WRITER;

    static {
        WRITER = new MapReferralWriter();
    }

    /**
     * A private constructor that protects object instantiation from external.
     *
     * @param nonce           nonce
     * @param referralRecords a collection of referral records
     */
    private DefaultLispMapReferral(long nonce,
                                   List<LispReferralRecord> referralRecords) {
        this.nonce = nonce;
        this.referralRecords = referralRecords;
    }

    @Override
    public LispType getType() {
        return LISP_MAP_REFERRAL;
    }

    @Override
    public void writeTo(ByteBuf byteBuf) throws LispWriterException {
        WRITER.writeTo(byteBuf, this);
    }

    @Override
    public Builder createBuilder() {
        return new DefaultMapReferralBuilder();
    }

    @Override
    public int getRecordCount() {
        return referralRecords.size();
    }

    @Override
    public long getNonce() {
        return nonce;
    }

    @Override
    public List<LispReferralRecord> getReferralRecords() {
        return ImmutableList.copyOf(referralRecords);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("type", getType())
                .add("nonce", nonce)
                .add("referralRecords", referralRecords)
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

        DefaultLispMapReferral that = (DefaultLispMapReferral) o;
        return Objects.equals(nonce, that.nonce);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(nonce);
    }

    public static final class DefaultMapReferralBuilder
                                                implements MapReferralBuilder {

        private long nonce;
        private List<LispReferralRecord> referralRecords = Lists.newArrayList();

        @Override
        public LispType getType() {
            return LISP_MAP_REFERRAL;
        }

        @Override
        public MapReferralBuilder withNonce(long nonce) {
            this.nonce = nonce;
            return this;
        }

        @Override
        public MapReferralBuilder withReferralRecords(List<LispReferralRecord> records) {
            if (referralRecords != null) {
                this.referralRecords = ImmutableList.copyOf(records);
            }
            return this;
        }

        @Override
        public LispMapReferral build() {
            return new DefaultLispMapReferral(nonce, referralRecords);
        }
    }

    /**
     * A LISP message reader for MapReferral message.
     */
    public static final class MapReferralReader
                                implements LispMessageReader<LispMapReferral> {

        private static final int RESERVED_SKIP_LENGTH = 3;

        @Override
        public LispMapReferral readFrom(ByteBuf byteBuf) throws LispParseError,
                                LispReaderException, DeserializationException {

            if (byteBuf.readerIndex() != 0) {
                return null;
            }

            // let's skip the reserved field
            byteBuf.skipBytes(RESERVED_SKIP_LENGTH);

            // record count -> 8 bits
            byte recordCount = (byte) byteBuf.readUnsignedByte();

            // nonce -> 64 bits
            long nonce = byteBuf.readLong();

            List<LispReferralRecord> referralRecords = Lists.newArrayList();
            for (int i = 0; i < recordCount; i++) {
                referralRecords.add(new ReferralRecordReader().readFrom(byteBuf));
            }

            return new DefaultMapReferralBuilder()
                            .withNonce(nonce)
                            .withReferralRecords(referralRecords)
                            .build();
        }
    }

    /**
     * A LISP message writer for MapReferral message.
     */
    public static final class MapReferralWriter
                                implements LispMessageWriter<LispMapReferral> {

        private static final int REFERRAL_SHIFT_BIT = 4;

        private static final int UNUSED_ZERO = 0;

        @Override
        public void writeTo(ByteBuf byteBuf, LispMapReferral message)
                                                    throws LispWriterException {

            // specify LISP message type
            byte msgType =
                    (byte) (LISP_MAP_REFERRAL.getTypeCode() << REFERRAL_SHIFT_BIT);

            // fill zero into reserved field
            byteBuf.writeShort(UNUSED_ZERO);
            byteBuf.writeByte(UNUSED_ZERO);

            // record count
            byteBuf.writeByte(message.getReferralRecords().size());

            // nonce
            byteBuf.writeLong(message.getNonce());

            // serialize referral records
            ReferralRecordWriter writer = new ReferralRecordWriter();
            List<LispReferralRecord> records = message.getReferralRecords();

            for (LispReferralRecord record : records) {
                writer.writeTo(byteBuf, record);
            }
        }
    }
}
