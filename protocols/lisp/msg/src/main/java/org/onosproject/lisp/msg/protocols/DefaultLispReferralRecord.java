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

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import org.onlab.packet.DeserializationException;
import org.onlab.util.ByteOperator;
import org.onosproject.lisp.msg.exceptions.LispParseError;
import org.onosproject.lisp.msg.exceptions.LispReaderException;
import org.onosproject.lisp.msg.exceptions.LispWriterException;
import org.onosproject.lisp.msg.protocols.DefaultLispReferral.ReferralWriter;
import org.onosproject.lisp.msg.types.LispAfiAddress;
import org.onosproject.lisp.msg.types.LispAfiAddress.AfiAddressWriter;

import java.util.List;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default LISP referral record class.
 */
public final class DefaultLispReferralRecord extends AbstractLispRecord
                                                implements LispReferralRecord {

    private final boolean incomplete;
    private final List<LispReferral> referrals;
    private final List<LispSignature> signatures;

    static final ReferralRecordWriter WRITER;
    static {
        WRITER = new ReferralRecordWriter();
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
     * @param incomplete       incomplete flag value
     * @param referrals        a collection referrals
     * @param signatures       a collection signatures
     */
    private DefaultLispReferralRecord(int recordTtl, byte maskLength,
                                      LispMapReplyAction action, boolean authoritative,
                                      short mapVersionNumber, LispAfiAddress eidPrefixAfi,
                                      boolean incomplete, List<LispReferral> referrals,
                                      List<LispSignature> signatures) {
        super(recordTtl, maskLength, action, authoritative, mapVersionNumber, eidPrefixAfi);
        this.incomplete = incomplete;
        this.referrals = referrals;
        this.signatures = signatures;
    }

    @Override
    public int getReferralCount() {
        return referrals.size();
    }

    @Override
    public int getSignatureCount() {
        return signatures.size();
    }

    @Override
    public boolean isIncomplete() {
        return incomplete;
    }

    @Override
    public List<LispReferral> getReferrals() {
        return ImmutableList.copyOf(referrals);
    }

    @Override
    public List<LispSignature> getSignatures() {
        return ImmutableList.copyOf(signatures);
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
                .add("incomplete", incomplete)
                .add("referrals", referrals)
                .add("signatures", signatures)
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
        DefaultLispReferralRecord that = (DefaultLispReferralRecord) o;
        return Objects.equal(recordTtl, that.recordTtl) &&
                Objects.equal(maskLength, that.maskLength) &&
                Objects.equal(action, that.action) &&
                Objects.equal(authoritative, that.authoritative) &&
                Objects.equal(mapVersionNumber, that.mapVersionNumber) &&
                Objects.equal(eidPrefixAfi, that.eidPrefixAfi) &&
                Objects.equal(referrals, that.referrals) &&
                Objects.equal(signatures, that.signatures);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(recordTtl, maskLength, action, authoritative,
                mapVersionNumber, eidPrefixAfi, incomplete, referrals, signatures);
    }

    public static final class DefaultReferralRecordBuilder
                                extends AbstractRecordBuilder<ReferralRecordBuilder>
                                        implements ReferralRecordBuilder {

        private boolean incomplete;
        private List<LispReferral> referrals = Lists.newArrayList();
        private List<LispSignature> signatures = Lists.newArrayList();

        @Override
        public ReferralRecordBuilder withReferrals(List<LispReferral> referrals) {
            if (referrals != null) {
                this.referrals = ImmutableList.copyOf(referrals);
            }
            return this;
        }

        @Override
        public ReferralRecordBuilder withSignatures(List<LispSignature> signatures) {
            if (signatures != null) {
                this.signatures = ImmutableList.copyOf(signatures);
            }
            return this;
        }

        @Override
        public ReferralRecordBuilder withIsIncomplete(boolean incomplete) {
            this.incomplete = incomplete;
            return this;
        }

        @Override
        public LispReferralRecord build() {

            checkNotNull(eidPrefixAfi, "Must specify an EID prefix");

            return new DefaultLispReferralRecord(recordTtl, maskLength, action,
                                authoritative, mapVersionNumber, eidPrefixAfi,
                                incomplete, referrals, signatures);
        }
    }

    /**
     * A LISP message reader for ReferralRecord portion.
     */
    public static final class ReferralRecordReader
                                implements LispMessageReader<LispReferralRecord> {

        private static final int INCOMPLETE_INDEX = 3;
        private static final int AUTHORITATIVE_INDEX = 4;

        private static final int REPLY_ACTION_SHIFT_BIT = 5;
        private static final int RESERVED_SKIP_LENGTH = 1;

        @Override
        public LispReferralRecord readFrom(ByteBuf byteBuf)
                                            throws LispParseError, LispReaderException,
                                                        DeserializationException {

            // Record TTL -> 32 bits
            int recordTtl = byteBuf.readInt();

            // referral count -> 8 bits
            int referralCount = byteBuf.readUnsignedByte();

            // EID mask length -> 8 bits
            byte maskLength = (byte) byteBuf.readUnsignedByte();

            byte actionWithFlag = (byte) byteBuf.readUnsignedByte();

            // action -> 3 bits
            int actionByte = actionWithFlag >> REPLY_ACTION_SHIFT_BIT;
            LispMapReplyAction action = LispMapReplyAction.valueOf(actionByte);
            if (action == null) {
                action = LispMapReplyAction.NoAction;
            }

            // authoritative flag -> 1 bit
            boolean authoritative = ByteOperator.getBit((byte)
                                    (actionWithFlag >> AUTHORITATIVE_INDEX), 0);

            // incomplete flag -> 1 bit
            boolean incomplete = ByteOperator.getBit((byte)
                                 (actionWithFlag >> INCOMPLETE_INDEX), 0);

            // let's skip the reserved field
            byteBuf.skipBytes(RESERVED_SKIP_LENGTH);

            // Map version number -> 12 bits, we treat Rsvd field is all zero
            short mapVersionNumber = (short) byteBuf.readUnsignedShort();

            LispAfiAddress eidPrefixAfi =
                    new LispAfiAddress.AfiAddressReader().readFrom(byteBuf);

            List<LispReferral> referrals = Lists.newArrayList();
            for (int i = 0; i < referralCount; i++) {
                referrals.add(new DefaultLispReferral.ReferralReader().readFrom(byteBuf));
            }

            return new DefaultReferralRecordBuilder()
                            .withRecordTtl(recordTtl)
                            .withMaskLength(maskLength)
                            .withAction(action)
                            .withIsAuthoritative(authoritative)
                            .withIsIncomplete(incomplete)
                            .withMapVersionNumber(mapVersionNumber)
                            .withReferrals(referrals)
                            .withEidPrefixAfi(eidPrefixAfi)
                            .build();
        }
    }

    /**
     * A LISP message writer for ReferralRecord portion.
     */
    public static final class ReferralRecordWriter
                                implements LispMessageWriter<LispReferralRecord> {

        private static final int REPLY_ACTION_SHIFT_BIT = 5;
        private static final int INCOMPLETE_SHIFT_BIT = 3;
        private static final int AUTHORITATIVE_SHIFT_BIT = 4;

        private static final int ENABLE_BIT = 1;
        private static final int DISABLE_BIT = 0;

        private static final int UNUSED_ZERO = 0;

        @Override
        public void writeTo(ByteBuf byteBuf, LispReferralRecord message)
                                                    throws LispWriterException {
            // record TTL
            byteBuf.writeInt(message.getRecordTtl());

            // referral count
            byteBuf.writeByte((byte) message.getReferrals().size());

            // EID mask length
            byteBuf.writeByte(message.getMaskLength());

            // reply action
            byte action = (byte) (message.getAction().getAction() << REPLY_ACTION_SHIFT_BIT);

            // authoritative bit
            byte authoritative = DISABLE_BIT;
            if (message.isAuthoritative()) {
                authoritative = ENABLE_BIT << AUTHORITATIVE_SHIFT_BIT;
            }

            // incomplete bit
            byte incomplete = DISABLE_BIT;
            if (message.isIncomplete()) {
                incomplete = ENABLE_BIT << INCOMPLETE_SHIFT_BIT;
            }

            byteBuf.writeByte((byte) (action + authoritative + incomplete));

            // fill zero into reserved field
            byteBuf.writeByte((short) UNUSED_ZERO);

            // map version number
            byteBuf.writeShort(message.getMapVersionNumber());

            // EID prefix AFI with EID prefix
            AfiAddressWriter afiAddressWriter = new AfiAddressWriter();
            afiAddressWriter.writeTo(byteBuf, message.getEidPrefixAfi());

            // serialize referrals
            ReferralWriter referralWriter = new ReferralWriter();
            List<LispReferral> referrals = message.getReferrals();
            for (LispReferral referral : referrals) {
                referralWriter.writeTo(byteBuf, referral);
            }
        }
    }
}
