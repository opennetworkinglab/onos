/*
 * Copyright 2017-present Open Networking Foundation
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

import org.onosproject.lisp.msg.types.LispAfiAddress;

/**
 * Abstract LISP record class that provide default implementations.
 */
public abstract class AbstractLispRecord implements LispRecord {

    protected final int recordTtl;
    protected final byte maskLength;
    protected final LispMapReplyAction action;
    protected final boolean authoritative;
    protected final short mapVersionNumber;
    protected final LispAfiAddress eidPrefixAfi;

    protected AbstractLispRecord(int recordTtl, byte maskLength,
                                 LispMapReplyAction action, boolean authoritative,
                                 short mapVersionNumber, LispAfiAddress eidPrefixAfi) {
        this.recordTtl = recordTtl;
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

    public static class AbstractRecordBuilder<T> implements RecordBuilder<T> {

        protected int recordTtl;
        protected byte maskLength;
        protected LispMapReplyAction action;
        protected boolean authoritative;
        protected short mapVersionNumber;
        protected LispAfiAddress eidPrefixAfi;

        @Override
        public T withRecordTtl(int recordTtl) {
            this.recordTtl = recordTtl;
            return (T) this;
        }

        @Override
        public T withMaskLength(byte maskLength) {
            this.maskLength = maskLength;
            return (T) this;
        }

        @Override
        public T withAction(LispMapReplyAction action) {
            this.action = action;
            return (T) this;
        }

        @Override
        public T withIsAuthoritative(boolean authoritative) {
            this.authoritative = authoritative;
            return (T) this;
        }

        @Override
        public T withMapVersionNumber(short mapVersionNumber) {
            this.mapVersionNumber = mapVersionNumber;
            return (T) this;
        }

        @Override
        public T withEidPrefixAfi(LispAfiAddress prefix) {
            this.eidPrefixAfi = prefix;
            return (T) this;
        }
    }
}
