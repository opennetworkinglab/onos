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
package org.onosproject.lisp.msg.protocols;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import org.onlab.util.ByteOperator;
import org.onosproject.lisp.msg.protocols.LispEidRecord.EidRecordReader;
import org.onosproject.lisp.msg.types.LispAfiAddress;
import org.onosproject.lisp.msg.exceptions.LispParseError;
import org.onosproject.lisp.msg.exceptions.LispReaderException;
import org.onosproject.lisp.msg.exceptions.LispWriterException;
import org.onosproject.lisp.msg.types.LispAfiAddress.AfiAddressReader;
import org.onosproject.lisp.msg.types.LispAfiAddress.AfiAddressWriter;

import java.util.List;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static org.onosproject.lisp.msg.protocols.LispEidRecord.EidRecordWriter;

/**
 * Default LISP map request message class.
 */
public final class DefaultLispMapRequest extends AbstractLispMessage
        implements LispMapRequest {

    private final long nonce;
    private final LispAfiAddress sourceEid;
    private final List<LispAfiAddress> itrRlocs;
    private final List<LispEidRecord> eidRecords;
    private final boolean authoritative;
    private final boolean mapDataPresent;
    private final boolean probe;
    private final boolean smr;
    private final boolean pitr;
    private final boolean smrInvoked;
    private final int replyRecord;

    static final RequestWriter WRITER;
    static {
        WRITER = new RequestWriter();
    }

    /**
     * A private constructor that protects object instantiation from external.
     *
     * @param nonce          nonce
     * @param sourceEid      source EID address
     * @param itrRlocs       a collection of ITR RLOCs
     * @param eidRecords     a collection of EID records
     * @param authoritative  authoritative flag
     * @param mapDataPresent map data present flag
     * @param probe          probe flag
     * @param smr            smr flag
     * @param pitr           pitr flag
     * @param smrInvoked     smrInvoked flag
     * @param replyReocrd    size of map-reply record
     */
    private DefaultLispMapRequest(long nonce, LispAfiAddress sourceEid,
                                  List<LispAfiAddress> itrRlocs,
                                  List<LispEidRecord> eidRecords,
                                  boolean authoritative, boolean mapDataPresent,
                                  boolean probe, boolean smr, boolean pitr,
                                  boolean smrInvoked, int replyReocrd) {
        this.nonce = nonce;
        this.sourceEid = sourceEid;
        this.itrRlocs = itrRlocs;
        this.eidRecords = eidRecords;
        this.authoritative = authoritative;
        this.mapDataPresent = mapDataPresent;
        this.probe = probe;
        this.smr = smr;
        this.pitr = pitr;
        this.smrInvoked = smrInvoked;
        this.replyRecord = replyReocrd;
    }

    @Override
    public LispType getType() {
        return LispType.LISP_MAP_REQUEST;
    }

    @Override
    public void writeTo(ByteBuf byteBuf) throws LispWriterException {
        WRITER.writeTo(byteBuf, this);
    }

    @Override
    public Builder createBuilder() {
        return new DefaultRequestBuilder();
    }

    @Override
    public boolean isAuthoritative() {
        return authoritative;
    }

    @Override
    public boolean isMapDataPresent() {
        return mapDataPresent;
    }

    @Override
    public boolean isProbe() {
        return probe;
    }

    @Override
    public boolean isSmr() {
        return smr;
    }

    @Override
    public boolean isPitr() {
        return pitr;
    }

    @Override
    public boolean isSmrInvoked() {
        return smrInvoked;
    }

    @Override
    public int getRecordCount() {
        return eidRecords.size();
    }

    @Override
    public long getNonce() {
        return nonce;
    }

    @Override
    public LispAfiAddress getSourceEid() {
        return sourceEid;
    }

    @Override
    public List<LispAfiAddress> getItrRlocs() {
        return ImmutableList.copyOf(itrRlocs);
    }

    @Override
    public List<LispEidRecord> getEids() {
        return ImmutableList.copyOf(eidRecords);
    }

    @Override
    public int getReplyRecord() {
        return replyRecord;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("type", getType())
                .add("nonce", nonce)
                .add("source EID", sourceEid)
                .add("ITR rlocs", itrRlocs)
                .add("EID records", eidRecords)
                .add("authoritative", authoritative)
                .add("mapDataPresent", mapDataPresent)
                .add("probe", probe)
                .add("SMR", smr)
                .add("Proxy ITR", pitr)
                .add("SMR Invoked", smrInvoked)
                .add("Size of reply record", replyRecord).toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefaultLispMapRequest that = (DefaultLispMapRequest) o;
        return Objects.equal(nonce, that.nonce) &&
                Objects.equal(sourceEid, that.sourceEid) &&
                Objects.equal(authoritative, that.authoritative) &&
                Objects.equal(mapDataPresent, that.mapDataPresent) &&
                Objects.equal(probe, that.probe) &&
                Objects.equal(smr, that.smr) &&
                Objects.equal(pitr, that.pitr) &&
                Objects.equal(smrInvoked, that.smrInvoked) &&
                Objects.equal(replyRecord, that.replyRecord);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(nonce, sourceEid, authoritative,
                mapDataPresent, probe, smr, pitr, smrInvoked, replyRecord);
    }

    public static final class DefaultRequestBuilder implements RequestBuilder {

        private long nonce;
        private LispAfiAddress sourceEid;
        private List<LispAfiAddress> itrRlocs = Lists.newArrayList();
        private List<LispEidRecord> eidRecords = Lists.newArrayList();
        private boolean authoritative;
        private boolean mapDataPresent;
        private boolean probe;
        private boolean smr;
        private boolean pitr;
        private boolean smrInvoked;
        private int replyRecord;

        @Override
        public LispType getType() {
            return LispType.LISP_MAP_REQUEST;
        }

        @Override
        public RequestBuilder withIsAuthoritative(boolean authoritative) {
            this.authoritative = authoritative;
            return this;
        }

        @Override
        public RequestBuilder withIsProbe(boolean probe) {
            this.probe = probe;
            return this;
        }

        @Override
        public RequestBuilder withIsMapDataPresent(boolean mapDataPresent) {
            this.mapDataPresent = mapDataPresent;
            return this;
        }

        @Override
        public RequestBuilder withIsSmr(boolean smr) {
            this.smr = smr;
            return this;
        }

        @Override
        public RequestBuilder withIsPitr(boolean pitr) {
            this.pitr = pitr;
            return this;
        }

        @Override
        public RequestBuilder withIsSmrInvoked(boolean smrInvoked) {
            this.smrInvoked = smrInvoked;
            return this;
        }

        @Override
        public RequestBuilder withNonce(long nonce) {
            this.nonce = nonce;
            return this;
        }

        @Override
        public RequestBuilder withSourceEid(LispAfiAddress sourceEid) {
            this.sourceEid = sourceEid;
            return this;
        }

        @Override
        public RequestBuilder withItrRlocs(List<LispAfiAddress> itrRlocs) {
            if (itrRlocs != null) {
                this.itrRlocs = ImmutableList.copyOf(itrRlocs);
            }
            return this;
        }

        @Override
        public RequestBuilder withEidRecords(List<LispEidRecord> records) {
            if (records != null) {
                this.eidRecords = ImmutableList.copyOf(records);
            }
            return this;
        }

        @Override
        public RequestBuilder withReplyRecord(int replyRecord) {
            this.replyRecord = replyRecord;
            return this;
        }

        @Override
        public LispMapRequest build() {

            checkArgument((itrRlocs != null) && (!itrRlocs.isEmpty()), "Must have an ITR RLOC entry");

            return new DefaultLispMapRequest(nonce, sourceEid, itrRlocs, eidRecords,
                    authoritative, mapDataPresent, probe, smr, pitr, smrInvoked, replyRecord);
        }
    }

    /**
     * A LISP message reader for MapRequest message.
     */
    public static final class RequestReader implements LispMessageReader<LispMapRequest> {

        private static final int AUTHORITATIVE_INDEX = 3;
        private static final int MAP_DATA_PRESENT_INDEX = 2;
        private static final int PROBE_INDEX = 1;
        private static final int SMR_INDEX = 0;
        private static final int PITR_INDEX = 7;
        private static final int SMR_INVOKED_INDEX = 6;

        @Override
        public LispMapRequest readFrom(ByteBuf byteBuf) throws LispParseError, LispReaderException {

            if (byteBuf.readerIndex() != 0) {
                return null;
            }

            byte typeWithFlags = byteBuf.readByte();

            // authoritative -> 1 bit
            boolean authoritative = ByteOperator.getBit(typeWithFlags, AUTHORITATIVE_INDEX);

            // mapDataPresent -> 1 bit
            boolean mapDataPresent = ByteOperator.getBit(typeWithFlags, MAP_DATA_PRESENT_INDEX);

            // probe -> 1 bit
            boolean probe = ByteOperator.getBit(typeWithFlags, PROBE_INDEX);

            // smr -> 1 bit
            boolean smr = ByteOperator.getBit(typeWithFlags, SMR_INDEX);

            byte reservedWithFlags = byteBuf.readByte();

            // pitr -> 1 bit
            boolean pitr = ByteOperator.getBit(reservedWithFlags, PITR_INDEX);

            // smrInvoked -> 1 bit
            boolean smrInvoked = ByteOperator.getBit(reservedWithFlags, SMR_INVOKED_INDEX);

            // let's skip reserved field, only obtains ITR counter value
            // assume that first 3 bits are all set as 0,
            // remain 5 bits represent Itr Rloc Counter (IRC)
            int irc = byteBuf.readUnsignedByte();

            // record count -> 8 bits
            int recordCount = byteBuf.readUnsignedByte();

            // nonce -> 64 bits
            long nonce = byteBuf.readLong();

            LispAfiAddress sourceEid = new AfiAddressReader().readFrom(byteBuf);

            // deserialize a collection of RLOC addresses
            List<LispAfiAddress> itrRlocs = Lists.newArrayList();
            for (int i = 0; i < irc + 1; i++) {
                itrRlocs.add(new AfiAddressReader().readFrom(byteBuf));
            }

            // deserialize a collection of EID records
            List<LispEidRecord> eidRecords = Lists.newArrayList();
            for (int i = 0; i < recordCount; i++) {
                eidRecords.add(new EidRecordReader().readFrom(byteBuf));
            }

            // reply record -> 32 bits
            int replyRecord = 0;

            // only obtains the reply record when map data present bit is set
            if (mapDataPresent) {
                replyRecord = byteBuf.readInt();
            }

            return new DefaultRequestBuilder()
                        .withIsAuthoritative(authoritative)
                        .withIsMapDataPresent(mapDataPresent)
                        .withIsProbe(probe)
                        .withIsSmr(smr)
                        .withIsPitr(pitr)
                        .withIsSmrInvoked(smrInvoked)
                        .withNonce(nonce)
                        .withSourceEid(sourceEid)
                        .withEidRecords(eidRecords)
                        .withItrRlocs(itrRlocs)
                        .withReplyRecord(replyRecord)
                        .build();
        }
    }

    /**
     * A LISP message writer for MapRequest message.
     */
    public static final class RequestWriter implements LispMessageWriter<LispMapRequest> {

        private static final int REQUEST_SHIFT_BIT = 4;

        private static final int AUTHORITATIVE_SHIFT_BIT = 3;
        private static final int MAP_DATA_PRESENT_SHIFT_BIT = 2;
        private static final int PROBE_SHIFT_BIT = 1;

        private static final int PITR_SHIFT_BIT = 7;
        private static final int SMR_INVOKED_SHIFT_BIT = 6;

        private static final int ENABLE_BIT = 1;
        private static final int DISABLE_BIT = 0;

        @Override
        public void writeTo(ByteBuf byteBuf, LispMapRequest message) throws LispWriterException {

            // specify LISP message type
            byte msgType = (byte) (LispType.LISP_MAP_REQUEST.getTypeCode() << REQUEST_SHIFT_BIT);

            // authoritative flag
            byte authoritative = DISABLE_BIT;
            if (message.isAuthoritative()) {
                authoritative = (byte) (ENABLE_BIT << AUTHORITATIVE_SHIFT_BIT);
            }

            // map data present flag
            byte mapDataPresent = DISABLE_BIT;
            if (message.isMapDataPresent()) {
                mapDataPresent = (byte) (ENABLE_BIT << MAP_DATA_PRESENT_SHIFT_BIT);
            }

            // probe flag
            byte probe = DISABLE_BIT;
            if (message.isProbe()) {
                probe = (byte) (ENABLE_BIT << PROBE_SHIFT_BIT);
            }

            // SMR flag
            byte smr = DISABLE_BIT;
            if (message.isSmr()) {
                smr = (byte) ENABLE_BIT;
            }

            byteBuf.writeByte((byte) (msgType + authoritative + mapDataPresent + probe + smr));

            // PITR flag bit
            byte pitr = DISABLE_BIT;
            if (message.isPitr()) {
                pitr = (byte) (ENABLE_BIT << PITR_SHIFT_BIT);
            }

            // SMR invoked flag bit
            byte smrInvoked = DISABLE_BIT;
            if (message.isSmrInvoked()) {
                smrInvoked = (byte) (ENABLE_BIT << SMR_INVOKED_SHIFT_BIT);
            }

            byteBuf.writeByte((byte) (pitr + smrInvoked));

            // ITR Rloc count
            byteBuf.writeByte((byte) message.getItrRlocs().size() - 1);

            // record count
            byteBuf.writeByte(message.getEids().size());

            // nonce
            byteBuf.writeLong(message.getNonce());

            // Source EID AFI with Source EID address
            AfiAddressWriter afiAddressWriter = new AfiAddressWriter();
            afiAddressWriter.writeTo(byteBuf, message.getSourceEid());

            // ITR RLOCs
            List<LispAfiAddress> rlocs = message.getItrRlocs();
            for (LispAfiAddress rloc : rlocs) {
                afiAddressWriter.writeTo(byteBuf, rloc);
            }

            // EID records
            EidRecordWriter recordWriter = new EidRecordWriter();
            List<LispEidRecord> records = message.getEids();

            for (LispEidRecord record : records) {
                recordWriter.writeTo(byteBuf, record);
            }

            // reply record
            byteBuf.writeInt(message.getReplyRecord());
        }
    }
}
