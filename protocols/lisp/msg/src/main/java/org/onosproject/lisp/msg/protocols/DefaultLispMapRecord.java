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
import org.onosproject.lisp.msg.types.LispAfiAddress;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Default implementation of LispMapRecord.
 */
public final class DefaultLispMapRecord implements LispMapRecord {

    private final int recordTtl;
    private final int locatorCount;
    private final byte maskLength;
    private final LispMapReplyAction action;
    private final boolean authoritative;
    private final short mapVersionNumber;
    private final LispAfiAddress eidPrefixAfi;

    /**
     * A private constructor that protects object instantiation from external.
     *
     * @param recordTtl        record time-to-live value
     * @param locatorCount     locator's count number
     * @param maskLength       mask length
     * @param action           lisp map reply action
     * @param authoritative    authoritative flag
     * @param mapVersionNumber map version number
     * @param eidPrefixAfi     EID prefix AFI address
     */
    private DefaultLispMapRecord(int recordTtl, int locatorCount, byte maskLength,
                                 LispMapReplyAction action, boolean authoritative,
                                 short mapVersionNumber, LispAfiAddress eidPrefixAfi) {
        this.recordTtl = recordTtl;
        this.locatorCount = locatorCount;
        this.maskLength = maskLength;
        this.action = action;
        this.authoritative = authoritative;
        this.mapVersionNumber = mapVersionNumber;
        this.eidPrefixAfi = eidPrefixAfi;
    }

    @Override
    public int getRecordTtl() {
        return recordTtl;
    }

    @Override
    public int getLocatorCount() {
        return locatorCount;
    }

    @Override
    public byte getMaskLength() {
        return maskLength;
    }

    @Override
    public LispMapReplyAction getAction() {
        return action;
    }

    @Override
    public boolean isAuthoritative() {
        return authoritative;
    }

    @Override
    public short getMapVersionNumber() {
        return mapVersionNumber;
    }

    @Override
    public LispAfiAddress getEidPrefixAfi() {
        return eidPrefixAfi;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("record TTL", recordTtl)
                .add("locatorCount", locatorCount)
                .add("maskLength", maskLength)
                .add("action", action)
                .add("authoritative", authoritative)
                .add("mapVersionNumber", mapVersionNumber)
                .add("EID prefix AFI address", eidPrefixAfi).toString();
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
                Objects.equal(locatorCount, that.locatorCount) &&
                Objects.equal(maskLength, that.maskLength) &&
                Objects.equal(action, that.action) &&
                Objects.equal(authoritative, that.authoritative) &&
                Objects.equal(mapVersionNumber, that.mapVersionNumber) &&
                Objects.equal(eidPrefixAfi, that.eidPrefixAfi);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(recordTtl, locatorCount, maskLength, action,
                                authoritative, mapVersionNumber, eidPrefixAfi);
    }

    public static final class DefaultMapRecordBuilder implements MapRecordBuilder {

        private int recordTtl;
        private int locatorCount;
        private byte maskLength;
        private LispMapReplyAction action;
        private boolean authoritative;
        private short mapVersionNumber;
        private LispAfiAddress eidPrefixAfi;

        @Override
        public MapRecordBuilder withRecordTtl(int recordTtl) {
            this.recordTtl = recordTtl;
            return this;
        }

        @Override
        public MapRecordBuilder withLocatorCount(int locatorCount) {
            this.locatorCount = locatorCount;
            return this;
        }

        @Override
        public MapRecordBuilder withMaskLength(byte maskLength) {
            this.maskLength = maskLength;
            return this;
        }

        @Override
        public MapRecordBuilder withAction(LispMapReplyAction action) {
            this.action = action;
            return this;
        }

        @Override
        public MapRecordBuilder withAuthoritative(boolean authoritative) {
            this.authoritative = authoritative;
            return this;
        }

        @Override
        public MapRecordBuilder withMapVersionNumber(short mapVersionNumber) {
            this.mapVersionNumber = mapVersionNumber;
            return this;
        }

        @Override
        public MapRecordBuilder withEidPrefixAfi(LispAfiAddress prefix) {
            this.eidPrefixAfi = prefix;
            return this;
        }

        @Override
        public LispMapRecord build() {
            return new DefaultLispMapRecord(recordTtl, locatorCount, maskLength,
                    action, authoritative, mapVersionNumber, eidPrefixAfi);
        }
    }
}
