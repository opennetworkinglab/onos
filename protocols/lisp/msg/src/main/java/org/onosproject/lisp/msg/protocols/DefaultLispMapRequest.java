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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import org.onosproject.lisp.msg.types.LispAfiAddress;

import java.util.List;

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
        return null;
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
        public RequestBuilder addItrRloc(LispAfiAddress itrRloc) {
            this.itrRlocs.add(itrRloc);
            return this;
        }

        @Override
        public RequestBuilder addEidRecord(LispEidRecord record) {
            this.eidRecords.add(record);
            return this;
        }

        @Override
        public LispMessage build() {
            return new DefaultLispMapRequest(nonce, recordCount, sourceEid, itrRlocs,
                    eidRecords, authoritative, mapDataPresent, probe, smr, pitr, smrInvoked);
        }
    }
}
