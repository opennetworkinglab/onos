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

import io.netty.buffer.ByteBuf;
import org.onosproject.lisp.msg.types.LispAfiAddress;

import java.util.List;

/**
 * Default LISP map request message class.
 */
public class DefaultLispMapRequest implements LispMapRequest {
    @Override
    public LispType getType() {
        return null;
    }

    @Override
    public void writeTo(ByteBuf byteBuf) {

    }

    @Override
    public Builder createBuilder() {
        return null;
    }

    @Override
    public boolean isAuthoritative() {
        return false;
    }

    @Override
    public boolean isProbe() {
        return false;
    }

    @Override
    public boolean isSmr() {
        return false;
    }

    @Override
    public boolean isPitr() {
        return false;
    }

    @Override
    public boolean isSmrInvoked() {
        return false;
    }

    @Override
    public byte getRecordCount() {
        return 0;
    }

    @Override
    public long getNonce() {
        return 0;
    }

    @Override
    public LispAfiAddress getSourceEid() {
        return null;
    }

    @Override
    public List<LispAfiAddress> getItrRlocs() {
        return null;
    }

    @Override
    public List<EidRecord> getEids() {
        return null;
    }

    public static final class DefaultRequestBuilder implements RequestBuilder {

        @Override
        public LispMessage build() {
            return null;
        }

        @Override
        public LispType getType() {
            return null;
        }

        @Override
        public RequestBuilder withIsAuthoritative(boolean isAuthoritative) {
            return null;
        }

        @Override
        public RequestBuilder withIsProbe(boolean isProbe) {
            return null;
        }

        @Override
        public RequestBuilder withIsSmr(boolean isSmr) {
            return null;
        }

        @Override
        public RequestBuilder withIsPitr(boolean isPitr) {
            return null;
        }

        @Override
        public RequestBuilder withIsSmrInvoked(boolean isSmrInvoked) {
            return null;
        }

        @Override
        public RequestBuilder withRecordCount(byte recordCount) {
            return null;
        }

        @Override
        public RequestBuilder withNonce(long nonce) {
            return null;
        }

        @Override
        public RequestBuilder withItrRloc(LispAfiAddress itrRloc) {
            return null;
        }

        @Override
        public RequestBuilder addEidRecord(EidRecord record) {
            return null;
        }
    }
}
