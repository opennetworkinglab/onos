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

import java.util.List;

/**
 * Default LISP map notify message class.
 */
public class DefaultLispMapNotify implements LispMapNotify {

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
    public long getNonce() {
        return 0;
    }

    @Override
    public byte getRecordCount() {
        return 0;
    }

    @Override
    public short getKeyId() {
        return 0;
    }

    @Override
    public byte[] getAuthenticationData() {
        return new byte[0];
    }

    @Override
    public List<LispRecord> getLispRecords() {
        return null;
    }

    public static final class DefaultNotifyBuilder implements NotifyBuilder {

        @Override
        public LispMessage build() {
            return null;
        }

        @Override
        public LispType getType() {
            return null;
        }

        @Override
        public NotifyBuilder withNonce(long nonce) {
            return null;
        }

        @Override
        public NotifyBuilder withRecordCount(byte recordCount) {
            return null;
        }

        @Override
        public NotifyBuilder withKeyId(short keyId) {
            return null;
        }

        @Override
        public NotifyBuilder withAuthenticationData(byte[] authenticationData) {
            return null;
        }

        @Override
        public NotifyBuilder addRecord(LispRecord record) {
            return null;
        }
    }
}
