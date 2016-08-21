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
import org.onosproject.lisp.msg.exceptions.LispReaderException;
import org.onosproject.lisp.msg.types.LispAfiAddress;

import java.util.List;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Default LISP map request message class.
 */
public final class DefaultLispMapRequest implements LispMapRequest {

    private final long nonce;
    private final byte recordCount;
    private final LispAfiAddress sourceEid;
    private final List<LispAfiAddress> itrRlocs;
    private final List<LispEidRecord> eidRecords;
    private final boolean authoritative;
    private final boolean mapDataPresent;
    private final boolean probe;
    private final boolean smr;
    private final boolean pitr;
    private final boolean smrInvoked;

    /**
     * A private constructor that protects object instantiation from external.
     *
     * @param nonce          nonce
     * @param recordCount    record count number
     * @param sourceEid      source EID address
     * @param itrRlocs       a collection of ITR RLOCs
     * @param eidRecords     a collection of EID records
     * @param authoritative  authoritative flag
     * @param mapDataPresent map data present flag
     * @param probe          probe flag
     * @param smr            smr flag
     * @param pitr           pitr flag
     * @param smrInvoked     smrInvoked flag
     */
    private DefaultLispMapRequest(long nonce, byte recordCount, LispAfiAddress sourceEid,
                                  List<LispAfiAddress> itrRlocs, List<LispEidRecord> eidRecords,
                                  boolean authoritative, boolean mapDataPresent, boolean probe,
                                  boolean smr, boolean pitr, boolean smrInvoked) {
        this.nonce = nonce;
        this.recordCount = recordCount;
        this.sourceEid = sourceEid;
        this.itrRlocs = itrRlocs;
        this.eidRecords = eidRecords;
        this.authoritative = authoritative;
        this.mapDataPresent = mapDataPresent;
        this.probe = probe;
        this.smr = smr;
        this.pitr = pitr;
        this.smrInvoked = smrInvoked;
    }

    @Override
    public LispType getType() {
        return LispType.LISP_MAP_REQUEST;
    }

    @Override
    public void writeTo(ByteBuf byteBuf) {
        // TODO: serialize LispMapRequest message
    }

    @Override
    public Builder createBuilder() {
        return new DefaultRequestBuilder();
    }

    @Override
    public boolean isAuthoritative() {
        return this.authoritative;
    }

    @Override
    public boolean isMapDataPresent() {
        return this.mapDataPresent;
    }

    @Override
    public boolean isProbe() {
        return this.probe;
    }

    @Override
    public boolean isSmr() {
        return this.smr;
    }

    @Override
    public boolean isPitr() {
        return this.pitr;
    }

    @Override
    public boolean isSmrInvoked() {
        return this.smrInvoked;
    }

    @Override
    public byte getRecordCount() {
        return this.recordCount;
    }

    @Override
    public long getNonce() {
        return this.nonce;
    }

    @Override
    public LispAfiAddress getSourceEid() {
        return this.sourceEid;
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
    public String toString() {
        return toStringHelper(this)
                .add("type", getType())
                .add("nonce", nonce)
                .add("recordCount", recordCount)
                .add("source EID", sourceEid)
                .add("ITR rlocs", itrRlocs)
                .add("EID records", eidRecords)
                .add("authoritative", authoritative)
                .add("mapDataPresent", mapDataPresent)
                .add("probe", probe)
                .add("SMR", smr)
                .add("Proxy ITR", pitr)
                .add("SMR Invoked", smrInvoked).toString();
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
                Objects.equal(recordCount, that.recordCount) &&
                Objects.equal(sourceEid, that.sourceEid) &&
                Objects.equal(itrRlocs, that.itrRlocs) &&
                Objects.equal(eidRecords, that.eidRecords) &&
                Objects.equal(authoritative, that.authoritative) &&
                Objects.equal(mapDataPresent, that.mapDataPresent) &&
                Objects.equal(probe, that.probe) &&
                Objects.equal(smr, that.smr) &&
                Objects.equal(pitr, that.pitr) &&
                Objects.equal(smrInvoked, that.smrInvoked);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(nonce, recordCount, sourceEid, itrRlocs, eidRecords,
                authoritative, mapDataPresent, probe, smr, pitr, smrInvoked);
    }

    public static final class DefaultRequestBuilder implements RequestBuilder {

        private long nonce;
        private byte recordCount;
        private LispAfiAddress sourceEid;
        private List<LispAfiAddress> itrRlocs = Lists.newArrayList();
        private List<LispEidRecord> eidRecords = Lists.newArrayList();
        private boolean authoritative;
        private boolean mapDataPresent;
        private boolean probe;
        private boolean smr;
        private boolean pitr;
        private boolean smrInvoked;

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
        public RequestBuilder withRecordCount(byte recordCount) {
            this.recordCount = recordCount;
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
            this.itrRlocs = ImmutableList.copyOf(itrRlocs);
            return this;
        }

        @Override
        public RequestBuilder withEidRecords(List<LispEidRecord> records) {
            this.eidRecords = ImmutableList.copyOf(records);
            return this;
        }

        @Override
        public LispMapRequest build() {
            return new DefaultLispMapRequest(nonce, recordCount, sourceEid, itrRlocs,
                    eidRecords, authoritative, mapDataPresent, probe, smr, pitr, smrInvoked);
        }
    }

    /**
     * A private LISP message reader for MapRequest message.
     */
    private static class RequestReader implements LispMessageReader<LispMapRequest> {

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
            int irc = byteBuf.readUnsignedShort();

            // record count -> 8 bits
            int recordCount = byteBuf.readUnsignedShort();

            // nonce -> 64 bits
            long nonce = byteBuf.readLong();

            LispAfiAddress sourceEid = new LispAfiAddress.AfiAddressReader().readFrom(byteBuf);

            // deserialize a collection of RLOC addresses
            List<LispAfiAddress> itrRlocs = Lists.newArrayList();
            for (int i = 0; i < irc; i++) {
                itrRlocs.add(new LispAfiAddress.AfiAddressReader().readFrom(byteBuf));
            }

            // deserialize a collection of EID records
            List<LispEidRecord> eidRecords = Lists.newArrayList();
            for (int i = 0; i < recordCount; i++) {
                eidRecords.add(new LispEidRecord.EidRecordReader().readFrom(byteBuf));
            }

            return new DefaultRequestBuilder()
                        .withIsAuthoritative(authoritative)
                        .withIsMapDataPresent(mapDataPresent)
                        .withIsProbe(probe)
                        .withIsSmr(smr)
                        .withIsPitr(pitr)
                        .withIsSmrInvoked(smrInvoked)
                        .withNonce(nonce)
                        .withRecordCount((byte) recordCount)
                        .withSourceEid(sourceEid)
                        .withEidRecords(eidRecords)
                        .withItrRlocs(itrRlocs)
                        .build();
        }
    }
}
